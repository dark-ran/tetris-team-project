package tetris.ui;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import javax.swing.*;
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
    private final JPanel nextPiecePanel = ScreenSupport.transparentPanel(new BorderLayout());
    private final JPanel controls = ScreenSupport.verticalPanel();
    private final JPanel content = ScreenSupport.transparentPanel(new BorderLayout(28, 0));
    private final GameMenuOverlay menu;
    private final Runnable onMenu;
    private final Map<Integer, String> boundKeys = new LinkedHashMap<>();
    private Map<String, Integer> keys = defaultKeys();
    private InputHandler inputHandler;
    private boolean colorBlind;

    public GameScreen(Runnable onMenu, Runnable onBack, Runnable onExit) {
        this.onMenu = Objects.requireNonNull(onMenu);
        ScreenSupport.prepareScreen(this, "게임", "Esc로 게임 메뉴를 열 수 있습니다.");
        content.add(boardFrame(), BorderLayout.CENTER);
        content.add(statusSidebar(), BorderLayout.EAST);
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

    private static Map<String, Integer> defaultKeys() {
        Map<String, Integer> defaults = new LinkedHashMap<>();
        defaults.put("LEFT", KeyEvent.VK_LEFT); defaults.put("RIGHT", KeyEvent.VK_RIGHT);
        defaults.put("DOWN", KeyEvent.VK_DOWN); defaults.put("ROTATE_CLOCKWISE", KeyEvent.VK_UP);
        defaults.put("HARD_DROP", KeyEvent.VK_SPACE); defaults.put("QUIT", KeyEvent.VK_Q);
        return defaults;
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
        colorBlind = settings.colorBlindMode();
        boardPreview.setColorBlindMode(colorBlind);
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
            controls.add(Box.createVerticalStrut(6));
            controls.add(new JLabel("[" + SwingKeyBindings.keyText(code) + "]  " + label));
        });
        controls.add(Box.createVerticalStrut(8));
        controls.add(ScreenSupport.mutedLabel("[Esc]  게임 메뉴"));
        controls.revalidate(); controls.repaint();
    }

    private JPanel boardFrame() {
        JPanel area = ScreenSupport.framedPanel(new BorderLayout(0, 12));
        area.add(ScreenSupport.sectionTitle("보드 20 × 10"), BorderLayout.NORTH);
        area.add(boardPreview, BorderLayout.CENTER);
        return area;
    }
    private JPanel statusSidebar() {
        JPanel panel = ScreenSupport.framedPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(260, 0));
        JPanel sections = ScreenSupport.verticalPanel();
        sections.add(informationSection("점수", scoreLabel));
        sections.add(Box.createVerticalStrut(16));
        sections.add(informationSection("레벨", levelLabel));
        sections.add(Box.createVerticalStrut(16));
        JPanel next = ScreenSupport.transparentPanel(new BorderLayout(0, 10));
        next.setAlignmentX(Component.LEFT_ALIGNMENT);
        next.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
        next.add(ScreenSupport.sectionTitle("다음 블록"), BorderLayout.NORTH);
        next.add(nextPiecePanel, BorderLayout.CENTER);
        next.add(ScreenSupport.mutedLabel("다음에 생성될 블록"), BorderLayout.SOUTH);
        sections.add(next); sections.add(Box.createVerticalStrut(20));
        controls.setAlignmentX(Component.LEFT_ALIGNMENT); sections.add(controls);
        sections.add(Box.createVerticalGlue()); panel.add(sections);
        return panel;
    }
    private JPanel informationSection(String heading, JLabel value) {
        JPanel section = ScreenSupport.transparentPanel(new BorderLayout(0, 8));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        value.setFont(AppTheme.font(Font.BOLD, 21f)); value.setForeground(AppTheme.TEXT);
        section.add(ScreenSupport.sectionTitle(heading), BorderLayout.NORTH);
        section.add(value, BorderLayout.CENTER); return section;
    }
    public void showScreen() { SwingKeyBindings.focus(boardPreview); }
    public void render(GameState state) {
        if (state == null) return;
        boardPreview.render(state);
        scoreLabel.setText(Long.toString(state.score())); levelLabel.setText(Integer.toString(state.level()));
        nextPiecePanel.removeAll();
        if (!state.nextPieces().isEmpty()) nextPiecePanel.add(new TetrominoMark(state.nextPieces().getFirst(), colorBlind));
        nextPiecePanel.revalidate(); nextPiecePanel.repaint();
    }
}
