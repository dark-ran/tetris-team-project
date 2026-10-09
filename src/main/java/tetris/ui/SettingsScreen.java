package tetris.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.*;
import tetris.settings.GameSettings;

/** Edits a draft; the controller validates and persists it before applying it to the game. */
public class SettingsScreen extends JPanel {
    static { AppTheme.install(); }
    public record Values(int preset, boolean colorBlind, Map<String, Integer> keys) {
        public Values { keys = Map.copyOf(keys); }
    }
    private final JComboBox<String> preset = new JComboBox<>(new String[] {"작게 (900 × 760)", "보통 (1040 × 860)", "크게 (1280 × 1024)"});
    private final JCheckBox colorBlind = new JCheckBox("블록에 구분 문자 표시");
    private final Map<String, Integer> keys = new LinkedHashMap<>();
    private final Map<String, JButton> keyButtons = new LinkedHashMap<>();
    private final JLabel feedback = new JLabel(" ");
    private final JButton saveButton, restoreButton, resetButton, backButton;
    private Consumer<Values> onSave;
    private Runnable onRestore, onReset;

    public SettingsScreen(Runnable onBack) {
        ScreenSupport.prepareScreen(this, "설정", "변경한 설정은 저장 후 적용됩니다. Esc는 게임 메뉴 전용 키입니다.");
        JPanel fields = new JPanel(new GridLayout(0, 2, 18, 12)); fields.setOpaque(false);
        fields.add(new JLabel("화면 크기")); fields.add(preset); preset.setSelectedIndex(1);
        fields.add(new JLabel("색각이상 모드")); colorBlind.setOpaque(false); colorBlind.setForeground(AppTheme.TEXT); fields.add(colorBlind);
        Map<String, String> names = Map.of("LEFT", "왼쪽", "RIGHT", "오른쪽", "DOWN", "아래로 한 칸", "ROTATE_CLOCKWISE", "시계방향 회전", "HARD_DROP", "즉시 낙하", "QUIT", "프로그램 종료");
        String[] actions = {"LEFT", "RIGHT", "DOWN", "ROTATE_CLOCKWISE", "HARD_DROP", "QUIT"};
        int[] defaults = {KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_UP, KeyEvent.VK_SPACE, KeyEvent.VK_Q};
        for (int i = 0; i < actions.length; i++) {
            String action = actions[i]; keys.put(action, defaults[i]);
            boolean[] capturing = {false};
            JButton key = ScreenSupport.button(SwingKeyBindings.keyText(defaults[i]), () -> { capturing[0] = true; feedback.setText("지정할 키를 누르세요. Esc는 사용할 수 없습니다."); });
            key.addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent event) {
                    if (!capturing[0]) return;
                    capturing[0] = false;
                    event.consume();
                    if (event.getKeyCode() == KeyEvent.VK_ESCAPE) { feedback.setText("Esc는 게임 메뉴 전용 키입니다."); return; }
                    keys.put(action, event.getKeyCode()); key.setText(SwingKeyBindings.keyText(event.getKeyCode())); feedback.setText("저장하면 새 조작 키가 적용됩니다.");
                }
            });
            keyButtons.put(action, key); fields.add(new JLabel(names.get(action))); fields.add(key);
        }
        JPanel frame = ScreenSupport.framedPanel(new BorderLayout(0, 18)); frame.add(fields);
        feedback.setForeground(AppTheme.YELLOW); frame.add(feedback, BorderLayout.SOUTH);
        add(frame, BorderLayout.CENTER);
        saveButton = ScreenSupport.primaryButton("설정 저장", () -> runSafely(() -> onSave.accept(values())));
        restoreButton = ScreenSupport.button("기본값 복구", () -> runSafely(onRestore));
        resetButton = ScreenSupport.dangerButton("기록 초기화", () -> runSafely(onReset));
        backButton = ScreenSupport.button("돌아가기", onBack);
        saveButton.setEnabled(false); restoreButton.setEnabled(false); resetButton.setEnabled(false);
        add(ScreenSupport.actionRow(restoreButton, resetButton, saveButton, backButton), BorderLayout.SOUTH);
        SwingKeyBindings.backOnEscape(this, onBack);
    }
    public void setOnSaveValues(Consumer<Values> callback) { onSave = Objects.requireNonNull(callback); saveButton.setEnabled(true); }
    public void setOnRestoreDefaults(Runnable callback) { onRestore = Objects.requireNonNull(callback); restoreButton.setEnabled(true); }
    public void setOnResetScores(Runnable callback) { onReset = Objects.requireNonNull(callback); resetButton.setEnabled(true); }
    public Values values() { return new Values(preset.getSelectedIndex(), colorBlind.isSelected(), keys); }
    public void showValues(Values current) {
        preset.setSelectedIndex(current.preset()); colorBlind.setSelected(current.colorBlind());
        current.keys().forEach((action, code) -> { if (keyButtons.containsKey(action)) { keys.put(action, code); keyButtons.get(action).setText(SwingKeyBindings.keyText(code)); } });
        feedback.setText(" ");
    }
    public void showSettings(GameSettings current) { showValues(new Values(current.windowSizePreset(), current.colorBlindMode(), current.keyBindings())); }
    public void showMessage(String message) { feedback.setText(message == null ? " " : message); }
    private void runSafely(Runnable action) {
        try { action.run(); feedback.setText("완료되었습니다."); }
        catch (IllegalArgumentException | java.io.UncheckedIOException ex) { feedback.setText(ex.getMessage()); }
    }
    public void showScreen() { SwingKeyBindings.focus(backButton); }
}
