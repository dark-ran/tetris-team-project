package tetris.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import javax.swing.*;
import tetris.settings.GameSettings;
import tetris.settings.ColorVisionMode;
import tetris.piece.PieceType;

/** Edits a draft; the controller validates and persists it before applying it to the game. */
public class SettingsScreen extends JPanel {
    static { AppTheme.install(); }
    public record Values(int preset, ColorVisionMode colorVisionMode, boolean patternsEnabled,
            Map<String, Integer> keys) {
        public Values { Objects.requireNonNull(colorVisionMode); keys = Map.copyOf(keys); }
        public Values(int preset, boolean colorBlind, Map<String, Integer> keys) {
            this(preset, colorBlind ? ColorVisionMode.DEUTAN : ColorVisionMode.NORMAL, colorBlind, keys);
        }
        public boolean colorBlind() { return colorVisionMode != ColorVisionMode.NORMAL; }
    }
    private final JComboBox<String> preset = new JComboBox<>(new String[] {"작게 (900 × 760)", "보통 (1040 × 860)", "크게 (1280 × 1024)"});
    private final JComboBox<ColorVisionMode> colorMode = new JComboBox<>(ColorVisionMode.values());
    private final JCheckBox patterns = new JCheckBox("블록 패턴 표시");
    private final JPanel previews = ScreenSupport.transparentPanel(new GridLayout(1, 7, 10, 0));
    private final JLabel patternHint = ScreenSupport.mutedLabel(" ");
    private final Map<String, Integer> keys = new LinkedHashMap<>();
    private final Map<String, JButton> keyButtons = new LinkedHashMap<>();
    private final JLabel feedback = new JLabel(" ");
    private final JButton saveButton, restoreButton, resetButton, backButton;
    private Consumer<Values> onSave;
    private Runnable onRestore, onReset;
    private IntPredicate onWindowSizePreview;
    private boolean refreshing;
    private int confirmedPreset = GameSettings.PRESET_MEDIUM;

    public SettingsScreen(Runnable onBack) {
        ScreenSupport.prepareScreen(this, "설정", "창 크기는 즉시 적용한 뒤 유지 여부를 확인합니다. 색각 모드·패턴·조작 키는 저장 후 적용됩니다.");
        JPanel fields = new JPanel(new GridLayout(0, 2, 18, 12)); fields.setOpaque(false);
        fields.add(new JLabel("화면 크기")); fields.add(preset); preset.setSelectedIndex(1);
        fields.add(new JLabel("색각 모드")); fields.add(colorMode);
        fields.add(new JLabel("패턴")); patterns.setOpaque(false); patterns.setForeground(AppTheme.TEXT); fields.add(patterns);
        colorMode.addActionListener(event -> {
            if (!refreshing) patterns.setSelected(((ColorVisionMode) colorMode.getSelectedItem()).defaultPatternsEnabled());
            refreshPreviews();
        });
        patterns.addItemListener(event -> { if (!refreshing) refreshPreviews(); });
        preset.addActionListener(event -> previewWindowSize());
        JPanel keyFields = ScreenSupport.transparentPanel(new GridLayout(0, 2, 18, 8));
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
            keyButtons.put(action, key); keyFields.add(new JLabel(names.get(action))); keyFields.add(key);
        }
        JPanel frame = ScreenSupport.framedPanel(new BorderLayout(0, 18));
        JPanel editor = ScreenSupport.transparentPanel(new BorderLayout(0, 18)); editor.add(fields, BorderLayout.NORTH);
        JPanel appearance = ScreenSupport.transparentPanel(new BorderLayout(0, 12));
        appearance.add(ScreenSupport.sectionTitle("7종 블록 미리보기"), BorderLayout.NORTH);
        previews.setPreferredSize(new Dimension(560, 80)); appearance.add(previews, BorderLayout.CENTER);
        appearance.add(patternHint, BorderLayout.SOUTH); editor.add(appearance, BorderLayout.CENTER);
        editor.add(keyFields, BorderLayout.SOUTH); frame.add(editor, BorderLayout.NORTH);
        feedback.setForeground(AppTheme.YELLOW);
        JScrollPane scroll = new JScrollPane(frame, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.getVerticalScrollBar().setUnitIncrement(20);
        JPanel content = ScreenSupport.transparentPanel(new BorderLayout(0, 10));
        content.add(scroll, BorderLayout.CENTER); content.add(feedback, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
        saveButton = ScreenSupport.primaryButton("설정 저장", () -> runSafely(() -> onSave.accept(values())));
        restoreButton = ScreenSupport.button("기본값 복구", () -> runSafely(onRestore));
        resetButton = ScreenSupport.dangerButton("기록 초기화", () -> runSafely(onReset));
        backButton = ScreenSupport.button("돌아가기", onBack);
        saveButton.setEnabled(false); restoreButton.setEnabled(false); resetButton.setEnabled(false);
        add(ScreenSupport.actionRow(restoreButton, resetButton, saveButton, backButton), BorderLayout.SOUTH);
        SwingKeyBindings.backOnEscape(this, onBack);
        refreshPreviews();
    }
    public void setOnSaveValues(Consumer<Values> callback) { onSave = Objects.requireNonNull(callback); saveButton.setEnabled(true); }
    public void setOnRestoreDefaults(Runnable callback) { onRestore = Objects.requireNonNull(callback); restoreButton.setEnabled(true); }
    public void setOnResetScores(Runnable callback) { onReset = Objects.requireNonNull(callback); resetButton.setEnabled(true); }
    public void setOnWindowSizePreview(IntPredicate callback) { onWindowSizePreview = Objects.requireNonNull(callback); }
    public Values values() { return new Values(preset.getSelectedIndex(), (ColorVisionMode) colorMode.getSelectedItem(), patterns.isSelected(), keys); }
    public void showValues(Values current) {
        refreshing = true;
        try {
            confirmedPreset = current.preset(); preset.setSelectedIndex(current.preset());
            colorMode.setSelectedItem(current.colorVisionMode()); patterns.setSelected(current.patternsEnabled());
            current.keys().forEach((action, code) -> { if (keyButtons.containsKey(action)) { keys.put(action, code); keyButtons.get(action).setText(SwingKeyBindings.keyText(code)); } });
            feedback.setText(" ");
        } finally { refreshing = false; }
        refreshPreviews();
    }
    public void showSettings(GameSettings current) { showValues(new Values(current.windowSizePreset(), current.colorVisionMode(), current.piecePatternsEnabled(), current.keyBindings())); }
    public void showMessage(String message) { feedback.setText(message == null ? " " : message); }
    private void runSafely(Runnable action) {
        feedback.setText(" ");
        try { action.run(); if (feedback.getText().isBlank()) feedback.setText("완료되었습니다."); }
        catch (IllegalArgumentException | java.io.UncheckedIOException ex) { feedback.setText(ex.getMessage()); }
    }
    public void showScreen() { SwingKeyBindings.focus(backButton); }

    private void refreshPreviews() {
        ColorVisionMode selected = (ColorVisionMode) colorMode.getSelectedItem();
        previews.removeAll();
        for (PieceType type : PieceType.values()) {
            JPanel tile = ScreenSupport.transparentPanel(new BorderLayout(0, 6));
            JPanel mark = ScreenSupport.transparentPanel(new GridBagLayout());
            mark.add(new TetrominoMark(type, selected, patterns.isSelected())); tile.add(mark, BorderLayout.CENTER);
            JLabel name = new JLabel(type.name(), JLabel.CENTER); tile.add(name, BorderLayout.SOUTH); previews.add(tile);
        }
        patternHint.setText(selected == ColorVisionMode.MONOCHROME
                ? "단색 모드에서는 블록 패턴 표시를 권장합니다."
                : "패턴은 블록을 회전해도 같은 방향으로 표시됩니다. Esc는 게임 메뉴 전용 키입니다.");
        previews.revalidate(); previews.repaint();
    }

    private void previewWindowSize() {
        int next = preset.getSelectedIndex(), previous = confirmedPreset;
        if (refreshing || next == previous) return;
        if (onWindowSizePreview == null) { confirmedPreset = next; return; }
        try {
            if (onWindowSizePreview.test(next)) {
                confirmedPreset = next; feedback.setText("변경한 창 크기를 유지했습니다."); return;
            }
            showMessage("이전 창 크기로 돌아갔습니다.");
        } catch (IllegalArgumentException | java.io.UncheckedIOException ex) { showMessage(ex.getMessage()); }
        refreshing = true;
        try { confirmedPreset = previous; preset.setSelectedIndex(previous); }
        finally { refreshing = false; }
    }
}
