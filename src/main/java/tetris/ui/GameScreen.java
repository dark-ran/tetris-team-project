package tetris.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import tetris.game.GameState;
import tetris.game.GameAction;
import tetris.input.InputHandler;
import tetris.settings.GameSettings;

/** Renders game snapshots and hosts the Esc menu without replacing the game view. */
public class GameScreen extends JPanel {
    static { AppTheme.install(); }
    private final BoardPreview boardPreview = new BoardPreview();
    private final JLabel scoreLabel = new JLabel("0");
    private final JLabel levelLabel = new JLabel("1");
    private final NextPiecePreview nextPiecePreview = new NextPiecePreview();
    private final JPanel controls = ScreenSupport.verticalPanel();
    private final JPanel content = ScreenSupport.transparentPanel(new GridBagLayout());
    private final GameMenuOverlay menu;
    private final Runnable onMenu;
    private final Map<Integer, String> boundKeys = new LinkedHashMap<>();
    private Map<String, Integer> keys = new LinkedHashMap<>(GameSettings.DEFAULT_KEY_BINDINGS);
    private InputHandler inputHandler;

    public GameScreen(Runnable onMenu, Runnable onBack, Runnable onExit) {
        this.onMenu = Objects.requireNonNull(onMenu);
        ScreenSupport.prepareScreen(this, "게임", "Esc로 게임 메뉴를 열 수 있습니다.");
        content.add(playfield(), playfieldPlacement());
        menu = new GameMenuOverlay(content);
        menu.setActions(this::hideMenu, () -> {}, onBack, onExit);
        JLayeredPane stage = new JLayeredPane() {
            @Override public void doLayout() {
                content.setBounds(0, 0, getWidth(), getHeight());
                menu.setBounds(0, 0, getWidth(), getHeight());
            }
        };
        stage.setPreferredSize(new Dimension(850, 600));
        stage.add(content, JLayeredPane.DEFAULT_LAYER);
        stage.add(menu, JLayeredPane.MODAL_LAYER);
        add(stage, BorderLayout.CENTER);
        add(ScreenSupport.actionRow(ScreenSupport.button("게임 메뉴 (Esc)", onMenu)), BorderLayout.SOUTH);
        bindKeys();
    }

    public void setInputHandler(InputHandler handler) { inputHandler = Objects.requireNonNull(handler); }
    public void setMenuActions(Runnable resume, Runnable settings, Runnable startMenu, Runnable exit) {
        menu.setActions(resume, settings, startMenu, exit);
    }
    public void showMenu() { menu.open(); }
    public void hideMenu() { menu.setVisible(false); showScreen(); }
    public boolean isMenuVisible() { return menu.isVisible(); }

    public void applySettings(GameSettings settings) {
        Objects.requireNonNull(settings);
        keys = new LinkedHashMap<>(settings.keyBindings());
        boardPreview.setAppearance(settings.colorVisionMode(), settings.piecePatternsEnabled());
        nextPiecePreview.setAppearance(settings.colorVisionMode(), settings.piecePatternsEnabled());
        bindKeys();
        repaint();
    }

    private void bindKeys() {
        boundKeys.forEach((code, name) -> {
            getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).remove(KeyStroke.getKeyStroke(code, 0));
            getActionMap().remove(name);
        });
        boundKeys.clear();
        keys.forEach((name, code) -> {
            if (code == KeyEvent.VK_ESCAPE || name.equals("TOGGLE_PAUSE") || name.equals("TICK")) return;
            String id = "game-" + name;
            boundKeys.put(code, id);
            SwingKeyBindings.bindToScreen(this, code, id, () -> {
                if (!menu.isVisible() && inputHandler != null) inputHandler.handleKey(code);
            });
        });
        SwingKeyBindings.bindToScreen(this, KeyEvent.VK_ESCAPE, "game-menu", () -> {
            if (!menu.isVisible()) onMenu.run();
        });
        controls.removeAll();
        controls.add(ScreenSupport.sectionTitle("조작"));
        keys.forEach((name, code) -> {
            if (name.equals("TOGGLE_PAUSE") || name.equals("TICK")) return;
            String label = switch (GameAction.valueOf(name)) {
                case LEFT -> "왼쪽"; case RIGHT -> "오른쪽"; case DOWN -> "아래로 한 칸";
                case ROTATE_CLOCKWISE -> "시계방향 회전"; case HARD_DROP -> "즉시 낙하";
                case QUIT -> "프로그램 종료"; default -> "";
            };
            controls.add(Box.createVerticalStrut(4));
            controls.add(new JLabel("[" + KeyEvent.getKeyText(code) + "]  " + label));
        });
        controls.add(Box.createVerticalStrut(8));
        controls.add(ScreenSupport.mutedLabel("[Esc]  게임 메뉴"));
        controls.revalidate(); controls.repaint();
    }

    private JPanel playfield() {
        JPanel playfield = ScreenSupport.transparentPanel(new BorderLayout(18, 0));
        playfield.add(boardFrame(), BorderLayout.CENTER);
        playfield.add(statusSidebar(), BorderLayout.EAST);
        // 작은 창에서는 높이만 줄여 보드의 폭까지 최소 크기로 축소되는 것을 막는다.
        playfield.setMinimumSize(new Dimension(playfield.getPreferredSize().width, 0));
        return playfield;
    }
    private static GridBagConstraints playfieldPlacement() {
        GridBagConstraints placement = new GridBagConstraints();
        placement.fill = GridBagConstraints.VERTICAL;
        placement.weighty = 1;
        return placement;
    }
    private JPanel boardFrame() {
        JPanel area = ScreenSupport.framedPanel(new BorderLayout(0, 12));
        area.add(ScreenSupport.sectionTitle("보드 20 × 10"), BorderLayout.NORTH);
        area.add(boardPreview, BorderLayout.CENTER);
        return area;
    }
    private JPanel nextPieceCard() {
        JPanel next = ScreenSupport.framedPanel(new BorderLayout(0, 8));
        next.add(ScreenSupport.sectionTitle("다음 블록"), BorderLayout.NORTH);
        JPanel centered = ScreenSupport.transparentPanel(new GridBagLayout());
        centered.add(nextPiecePreview);
        next.add(centered, BorderLayout.CENTER);
        return next;
    }
    private JPanel statusSidebar() {
        JPanel panel = ScreenSupport.transparentPanel(new BorderLayout(0, 12));
        panel.setPreferredSize(new Dimension(240, 0));
        panel.add(nextPieceCard(), BorderLayout.NORTH);
        JPanel details = ScreenSupport.framedPanel(new BorderLayout());
        JPanel sections = ScreenSupport.verticalPanel();
        sections.add(informationSection("점수", scoreLabel));
        sections.add(Box.createVerticalStrut(8));
        sections.add(informationSection("레벨", levelLabel));
        sections.add(Box.createVerticalStrut(12));
        controls.setAlignmentX(Component.LEFT_ALIGNMENT); sections.add(controls);
        sections.add(Box.createVerticalGlue()); details.add(sections);
        panel.add(details, BorderLayout.CENTER);
        return panel;
    }
    private JPanel informationSection(String heading, JLabel value) {
        JPanel section = ScreenSupport.transparentPanel(new BorderLayout(0, 4));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        value.setFont(AppTheme.font(Font.BOLD, 21f)); value.setForeground(AppTheme.TEXT);
        section.add(ScreenSupport.sectionTitle(heading), BorderLayout.NORTH);
        section.add(value, BorderLayout.CENTER); return section;
    }
    public void showScreen() { SwingKeyBindings.focus(boardPreview); }
    public void render(GameState state) {
        if (state == null) return;
        boardPreview.render(state);
        scoreLabel.setText(Long.toString(state.score())); levelLabel.setText(Integer.toString(state.level()));
        nextPiecePreview.setPiece(state.nextPieces().isEmpty() ? null : state.nextPieces().getFirst());
    }
}
