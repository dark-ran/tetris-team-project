package tetris.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import tetris.game.BattleMode;
import tetris.game.BattleOutcome;
import tetris.game.PlayerSnapshot;
import tetris.settings.ColorVisionMode;

/**
 * 로컬 2인 대전과 P2P 대전이 함께 쓰는 대전 화면. 한 창에 왼쪽·오른쪽 플레이어를 나란히 그린다.
 *
 * <p>화면은 {@link PlayerSnapshot}만 받아 그리며 게임 진행은 하지 않는다. 모든 메서드는 Swing 이벤트 스레드에서 호출한다.
 * 게임 스레드에서는 {@code SwingUtilities.invokeLater}로 {@link #render}를 요청한다.</p>
 *
 * <ul>
 *   <li>로컬 대전: 왼쪽 P1, 오른쪽 P2</li>
 *   <li>P2P 대전: 왼쪽 나, 오른쪽 상대. 지연·끊김은 {@link #setStatusMessage}로 표시</li>
 * </ul>
 */
public class BattleScreen extends JPanel {
    static { AppTheme.install(); }

    private final JLabel modeLabel = new JLabel("대전");
    private final JLabel timerLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel("", SwingConstants.RIGHT);
    private final BattlePlayerPanel left;
    private final BattlePlayerPanel right;
    private final JLabel leftGuide = ScreenSupport.mutedLabel("");
    private final JLabel rightGuide = ScreenSupport.mutedLabel("");
    private final ResultOverlay result = new ResultOverlay();
    private final List<KeyStroke> boundKeys = new ArrayList<>();

    public BattleScreen() {
        this("P1", "P2");
    }

    public BattleScreen(String leftName, String rightName) {
        super(new BorderLayout(0, 10));
        setBackground(AppTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(14, 18, 12, 18));
        left = new BattlePlayerPanel(Objects.requireNonNull(leftName));
        right = new BattlePlayerPanel(Objects.requireNonNull(rightName));
        add(header(), BorderLayout.NORTH);

        JPanel players = ScreenSupport.transparentPanel(new GridLayout(1, 2, 24, 0));
        players.add(framed(left));
        players.add(framed(right));
        JLayeredPane stage = new JLayeredPane() {
            @Override public void doLayout() {
                players.setBounds(0, 0, getWidth(), getHeight());
                result.setBounds(0, 0, getWidth(), getHeight());
            }
        };
        stage.setPreferredSize(new Dimension(840, 600));
        stage.add(players, JLayeredPane.DEFAULT_LAYER);
        stage.add(result, JLayeredPane.MODAL_LAYER);
        add(stage, BorderLayout.CENTER);

        JPanel guides = ScreenSupport.transparentPanel(new GridLayout(1, 2, 24, 0));
        guides.add(leftGuide);
        guides.add(rightGuide);
        add(guides, BorderLayout.SOUTH);
    }

    private JPanel header() {
        modeLabel.setFont(AppTheme.font(Font.BOLD, 18f));
        modeLabel.setForeground(AppTheme.TEXT);
        timerLabel.setFont(AppTheme.font(Font.BOLD, 20f));
        timerLabel.setForeground(AppTheme.CYAN);
        timerLabel.setVisible(false);
        statusLabel.setFont(AppTheme.font(Font.BOLD, 14f));
        statusLabel.setVisible(false);
        JPanel header = ScreenSupport.transparentPanel(new GridLayout(1, 3, 10, 0));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER),
                BorderFactory.createEmptyBorder(0, 0, 8, 0)));
        header.add(modeLabel);
        header.add(timerLabel);
        header.add(statusLabel);
        return header;
    }

    private static JPanel framed(JComponent content) {
        JPanel frame = new JPanel(new BorderLayout());
        frame.setBackground(AppTheme.SURFACE);
        frame.setBorder(AppTheme.frameBorder(10, 12));
        frame.add(content);
        return frame;
    }

    /** 대전 모드 이름을 왼쪽 위에 표시한다. 시간제한이 아니면 남은 시간도 숨긴다. */
    public void setMode(BattleMode mode) {
        modeLabel.setText(Objects.requireNonNull(mode).toString());
        if (!mode.isTimeLimited()) hideTimer();
    }

    public void setPlayerNames(String leftName, String rightName) {
        left.setPlayerName(Objects.requireNonNull(leftName));
        right.setPlayerName(Objects.requireNonNull(rightName));
    }

    /** 두 플레이어 화면을 갱신한다. 아직 정보가 없으면 null을 넘겨도 된다. */
    public void render(PlayerSnapshot leftPlayer, PlayerSnapshot rightPlayer) {
        left.render(leftPlayer);
        right.render(rightPlayer);
    }

    /** 시간제한 모드의 남은 시간을 mm:ss로 표시한다. 10초 이하면 빨간색이다. */
    public void setRemainingMillis(long millis) {
        long seconds = (Math.max(0, millis) + 999) / 1000;
        timerLabel.setText(String.format("%d:%02d", seconds / 60, seconds % 60));
        timerLabel.setForeground(seconds <= 10 ? AppTheme.RED : AppTheme.CYAN);
        timerLabel.setVisible(true);
    }

    public void hideTimer() {
        timerLabel.setVisible(false);
    }

    /** 오른쪽 위 상태 문구. P2P의 "랙 걸린 상태" 같은 알림에 쓴다. null이나 빈 문자열이면 숨긴다. */
    public void setStatusMessage(String message, boolean warning) {
        boolean visible = message != null && !message.isBlank();
        statusLabel.setText(visible ? message : "");
        statusLabel.setForeground(warning ? AppTheme.ORANGE : AppTheme.MUTED);
        statusLabel.setVisible(visible);
    }

    /** 화면 아래 조작 안내. 예: "A/D 이동 · W 회전 · F 바로 내리기". */
    public void setControlsGuide(String leftText, String rightText) {
        leftGuide.setText(leftText == null ? "" : leftText);
        rightGuide.setText(rightText == null ? "" : rightText);
    }

    public void setAppearance(ColorVisionMode mode, boolean patterns) {
        left.setAppearance(mode, patterns);
        right.setAppearance(mode, patterns);
    }

    /**
     * 창 어디에 포커스가 있어도 동작하는 키를 연결한다. 누를 때와 뗄 때를 따로 받을 수 있어
     * 두 사람이 동시에 키를 누르는 경우 눌림 상태를 직접 관리할 수 있다.
     * 결과 화면이 떠 있는 동안에는 호출하지 않는다.
     *
     * @param onRelease 뗄 때 동작. 필요 없으면 null
     */
    public void bindKey(int keyCode, Runnable onPress, Runnable onRelease) {
        Objects.requireNonNull(onPress);
        bind(KeyStroke.getKeyStroke(keyCode, 0, false), onPress);
        if (onRelease != null) bind(KeyStroke.getKeyStroke(keyCode, 0, true), onRelease);
    }

    private void bind(KeyStroke stroke, Runnable action) {
        String name = "battle-key-" + stroke;
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(stroke, name);
        getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                if (!result.isVisible()) action.run();
            }
        });
        boundKeys.add(stroke);
    }

    /** bindKey로 연결한 키를 모두 해제한다. */
    public void clearKeys() {
        for (KeyStroke stroke : boundKeys) {
            Object name = getInputMap(WHEN_IN_FOCUSED_WINDOW).get(stroke);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).remove(stroke);
            if (name != null) getActionMap().remove(name);
        }
        boundKeys.clear();
    }

    /**
     * 승패 결과를 화면 위에 띄운다. 버튼 문구와 동작은 부르는 쪽이 정한다.
     * 로컬: "다시 하기" / "시작 메뉴", P2P: "대기실로" 등.
     *
     * @param secondaryLabel 두 번째 버튼이 필요 없으면 null
     */
    public void showResult(BattleOutcome outcome, String primaryLabel, Runnable primary,
            String secondaryLabel, Runnable secondary) {
        Objects.requireNonNull(outcome);
        if (!outcome.isFinished()) throw new IllegalArgumentException("끝난 대전의 결과만 표시할 수 있습니다.");
        result.show(resultTitle(outcome), left.playerName() + " " + left.scoreText() + "점  :  "
                + right.playerName() + " " + right.scoreText() + "점",
                primaryLabel, primary, secondaryLabel, secondary);
    }

    public void hideResult() {
        result.setVisible(false);
        showScreen();
    }

    public boolean isResultVisible() {
        return result.isVisible();
    }

    /** 화면을 보여줄 때 호출한다. 버튼이 아닌 보드에 포커스를 두어 Enter가 버튼을 누르지 않게 한다. */
    public void showScreen() {
        SwingKeyBindings.focus(left.board());
    }

    String resultTitle(BattleOutcome outcome) {
        return switch (outcome) {
            case PLAYER1_WIN -> left.playerName() + " 승리";
            case PLAYER2_WIN -> right.playerName() + " 승리";
            case DRAW -> "무승부";
            case ONGOING -> "";
        };
    }

    BattlePlayerPanel leftPanel() { return left; }
    BattlePlayerPanel rightPanel() { return right; }
    String timerText() { return timerLabel.isVisible() ? timerLabel.getText() : ""; }
    String statusText() { return statusLabel.isVisible() ? statusLabel.getText() : ""; }
    String resultHeading() { return result.heading.getText(); }
    String resultDetail() { return result.detail.getText(); }
    List<JButton> resultButtons() { return result.buttons(); }

    /** 대전 화면 위에 반투명하게 겹치는 결과 패널. */
    private static final class ResultOverlay extends JPanel {
        private final JLabel heading = new JLabel("", SwingConstants.CENTER);
        private final JLabel detail = new JLabel("", SwingConstants.CENTER);
        private final JPanel actions = ScreenSupport.transparentPanel(new GridLayout(1, 0, 10, 0));

        ResultOverlay() {
            super(new GridBagLayout());
            setOpaque(false);
            setVisible(false);
            heading.setFont(AppTheme.font(Font.BOLD, 30f));
            heading.setForeground(AppTheme.YELLOW);
            detail.setForeground(AppTheme.TEXT);
            JPanel box = ScreenSupport.verticalPanel();
            for (JComponent part : new JComponent[] {heading, detail, actions}) {
                part.setAlignmentX(Component.CENTER_ALIGNMENT);
            }
            box.add(heading);
            box.add(Box.createVerticalStrut(10));
            box.add(detail);
            box.add(Box.createVerticalStrut(20));
            actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            box.add(actions);
            box.add(Box.createVerticalGlue());
            JPanel frame = ScreenSupport.framedPanel(new BorderLayout());
            frame.add(box);
            frame.setPreferredSize(new Dimension(380, 200));
            add(frame);
        }

        void show(String title, String scores, String primaryLabel, Runnable primary,
                String secondaryLabel, Runnable secondary) {
            heading.setText(title);
            detail.setText(scores);
            actions.removeAll();
            JButton first = ScreenSupport.primaryButton(Objects.requireNonNull(primaryLabel),
                    Objects.requireNonNull(primary));
            actions.add(first);
            if (secondaryLabel != null) {
                actions.add(ScreenSupport.button(secondaryLabel, Objects.requireNonNull(secondary)));
            }
            SwingKeyBindings.menuNavigation(this, buttons());
            setVisible(true);
            revalidate();
            repaint();
            SwingKeyBindings.focus(first);
        }

        List<JButton> buttons() {
            List<JButton> list = new ArrayList<>();
            for (Component component : actions.getComponents()) {
                if (component instanceof JButton button) list.add(button);
            }
            return list;
        }

        @Override protected void paintComponent(Graphics g) {
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRect(0, 0, getWidth(), getHeight());
            super.paintComponent(g);
        }
    }
}
