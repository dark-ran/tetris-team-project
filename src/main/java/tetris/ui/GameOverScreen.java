package tetris.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 최종 점수 표시와 재시작·메뉴 이동을 담당한다.
 *
 * <p>현재 사용 기능: 최종 점수 표시, 재시작·스코어보드·메뉴·프로그램 종료 콜백.</p>
 * <p>Supports name entry, persistence retry and Space to start a new game.</p>
 */
public class GameOverScreen extends JPanel {
    static { AppTheme.install(); }
    private static final String SCORE_UNMEASURED = "최종 점수: 미측정 (게임 연결 전)";

    private final JLabel scoreLabel = new JLabel();
    private final JButton restartButton;
    private final JPanel registration = ScreenSupport.verticalPanel();
    private final JTextField nameField = new JTextField(20);
    private final JTextArea errorLabel = new JTextArea(" ", 2, 0);
    private Consumer<String> onRegister;
    private Runnable onSkip;
    private BooleanSupplier onRetry;
    private Supplier<String> saveError = () -> null;
    public void setOnRegister(Consumer<String> callback) { onRegister = Objects.requireNonNull(callback); }
    public void setOnSkip(Runnable callback) { onSkip = Objects.requireNonNull(callback); }
    public void setOnRetrySave(BooleanSupplier callback) { onRetry = Objects.requireNonNull(callback); }
    public void setSaveErrorSupplier(Supplier<String> supplier) { saveError = Objects.requireNonNull(supplier); }
    public void showSaveError(String message) { errorLabel.setText(message == null ? " " : message); }
    public void showScreen(long score, boolean canRegister) {
        showScreen(score); registration.setVisible(canRegister); nameField.setText("");
        showSaveError(saveError.get());
    }


    public GameOverScreen(Runnable onRestart, Runnable onScoreboard, Runnable onBack, Runnable onExit) {
        ScreenSupport.prepareScreen(this, "게임 종료",
                "Space로 새 게임을 시작하거나 기록을 등록할 수 있습니다.");
        restartButton = ScreenSupport.primaryButton("다시 시작", onRestart);

        JPanel result = ScreenSupport.framedPanel(new BorderLayout(0, 16));
        result.setPreferredSize(new java.awt.Dimension(500, 310));
        scoreLabel.setFont(AppTheme.font(Font.BOLD, 24f));
        scoreLabel.setHorizontalAlignment(JLabel.CENTER);
        result.add(scoreLabel, BorderLayout.NORTH);
        JLabel guide = ScreenSupport.mutedLabel("Space  다시 시작     ·     Esc  시작 메뉴");
        guide.setHorizontalAlignment(JLabel.CENTER);
        result.add(guide, BorderLayout.SOUTH);
        JLabel nameGuide = new JLabel("이름을 입력해 기록을 등록하세요 (1~20자).");
        nameGuide.setAlignmentX(0.5f);
        nameGuide.setMaximumSize(new java.awt.Dimension(
                Integer.MAX_VALUE, nameGuide.getPreferredSize().height));
        nameGuide.setHorizontalAlignment(JLabel.LEFT);
        registration.add(nameGuide);
        registration.add(Box.createVerticalStrut(10)); registration.add(nameField);
        JButton register = ScreenSupport.primaryButton("기록 등록", () -> {
            try { if (onRegister != null) onRegister.accept(nameField.getText()); showSaveError(saveError.get()); }
            catch (IllegalArgumentException ex) { showSaveError(ex.getMessage()); }
        });
        JButton skip = ScreenSupport.button("등록 건너뛰기", () -> { if (onSkip != null) onSkip.run(); });
        JButton retry = ScreenSupport.button("저장 재시도", () -> { if (onRetry != null) onRetry.getAsBoolean(); showSaveError(saveError.get()); });
        registration.add(ScreenSupport.actionRow(register, skip, retry));
        errorLabel.setFont(nameGuide.getFont());
        errorLabel.setForeground(AppTheme.YELLOW);
        errorLabel.setOpaque(false);
        errorLabel.setEditable(false);
        errorLabel.setFocusable(false);
        errorLabel.setLineWrap(true);
        errorLabel.setWrapStyleWord(true);
        errorLabel.setAlignmentX(0.5f);
        errorLabel.setMaximumSize(new java.awt.Dimension(
                Integer.MAX_VALUE, errorLabel.getPreferredSize().height));
        registration.add(errorLabel); registration.setVisible(false); result.add(registration, BorderLayout.CENTER);
        JPanel center = ScreenSupport.transparentPanel(new GridBagLayout());
        center.add(result);
        add(center, BorderLayout.CENTER);
        add(ScreenSupport.actionRow(restartButton,
                ScreenSupport.button("스코어보드", onScoreboard),
                ScreenSupport.button("시작 메뉴", onBack),
                ScreenSupport.dangerButton("프로그램 종료", onExit)), BorderLayout.SOUTH);
        SwingKeyBindings.backOnEscape(this, onBack);
        SwingKeyBindings.bindToScreen(this, KeyEvent.VK_SPACE, "restart-game", onRestart);
        // A pressed Space in the name editor must remain text input, not restart the game.
        nameField.getInputMap(javax.swing.JComponent.WHEN_FOCUSED).put(
                javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "name-space");
        nameField.getActionMap().put("name-space", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { }
        });
        bindSpaceToButtons(this, onRestart);
    }

    private static void bindSpaceToButtons(java.awt.Component component, Runnable restart) {
        if (component instanceof JButton button) {
            button.getInputMap(javax.swing.JComponent.WHEN_FOCUSED).put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "restart-game");
            button.getInputMap(javax.swing.JComponent.WHEN_FOCUSED).put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true), "ignore-space-release");
            button.getActionMap().put("restart-game", new javax.swing.AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent event) { restart.run(); }
            });
        }
        if (component instanceof java.awt.Container container) for (java.awt.Component child : container.getComponents()) bindSpaceToButtons(child, restart);
    }

    /** 게임 미연결 상태의 미리보기. 실제 점수 대신 미측정을 표시한다. */
    public void showScreen() {
        registration.setVisible(false);
        scoreLabel.setText(SCORE_UNMEASURED);
        SwingKeyBindings.focus(restartButton);
    }

    public void showScreen(long score) {
        registration.setVisible(false);
        scoreLabel.setText("최종 점수: " + score);
        SwingKeyBindings.focus(restartButton);
    }
}
