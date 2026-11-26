package tetris.settings;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import tetris.game.GameAction;
import tetris.game.Difficulty;

/**
 * Settings data value object.
 * Holds window size, color vision palette, independent piece patterns, and key bindings.
 */
public final class GameSettings {
    public static final int PRESET_SMALL = 0;
    public static final int PRESET_MEDIUM = 1;
    public static final int PRESET_LARGE = 2;
    public static final int PRESET_COUNT = 3;

    public static final Dimension DIMENSION_SMALL = new Dimension(900, 760);
    public static final Dimension DIMENSION_MEDIUM = new Dimension(1040, 860);
    public static final Dimension DIMENSION_LARGE = new Dimension(1280, 1024);

    public static final List<Integer> PRESETS = List.of(PRESET_SMALL, PRESET_MEDIUM, PRESET_LARGE);

    public static final Map<String, Integer> DEFAULT_KEY_BINDINGS;

    // 대전 모드 기본 키 설정
    public static final Map<String, Integer> DEFAULT_P1_KEY_BINDINGS;
    public static final Map<String, Integer> DEFAULT_P2_KEY_BINDINGS;

    static {
        Map<String, Integer> defaults = new LinkedHashMap<>();
        defaults.put(GameAction.LEFT.name(), KeyEvent.VK_LEFT);
        defaults.put(GameAction.RIGHT.name(), KeyEvent.VK_RIGHT);
        defaults.put(GameAction.DOWN.name(), KeyEvent.VK_DOWN);
        defaults.put(GameAction.ROTATE_CLOCKWISE.name(), KeyEvent.VK_UP);
        defaults.put(GameAction.HARD_DROP.name(), KeyEvent.VK_SPACE);
        defaults.put(GameAction.QUIT.name(), KeyEvent.VK_Q);
        DEFAULT_KEY_BINDINGS = Collections.unmodifiableMap(defaults);

        // P1 기본 키
        Map<String, Integer> p1 = new LinkedHashMap<>();
        p1.put(GameAction.LEFT.name(), KeyEvent.VK_A);
        p1.put(GameAction.RIGHT.name(), KeyEvent.VK_D);
        p1.put(GameAction.DOWN.name(), KeyEvent.VK_S);
        p1.put(GameAction.ROTATE_CLOCKWISE.name(), KeyEvent.VK_W);
        p1.put(GameAction.HARD_DROP.name(), KeyEvent.VK_F);
        DEFAULT_P1_KEY_BINDINGS = Collections.unmodifiableMap(p1);

        // P2 기본 키
        Map<String, Integer> p2 = new LinkedHashMap<>();
        p2.put(GameAction.LEFT.name(), KeyEvent.VK_LEFT);
        p2.put(GameAction.RIGHT.name(), KeyEvent.VK_RIGHT);
        p2.put(GameAction.DOWN.name(), KeyEvent.VK_DOWN);
        p2.put(GameAction.ROTATE_CLOCKWISE.name(), KeyEvent.VK_UP);
        p2.put(GameAction.HARD_DROP.name(), KeyEvent.VK_ENTER);
        DEFAULT_P2_KEY_BINDINGS = Collections.unmodifiableMap(p2);
    }

    private final int windowSizePreset;
    private final ColorVisionMode colorVisionMode;
    private final boolean piecePatternsEnabled;
    private final Map<String, Integer> keyBindings;
    private final Difficulty difficulty;

    private final Map<String, Integer> p1KeyBindings;
    private final Map<String, Integer> p2KeyBindings;

    /**
     * Creates default game settings.
     */
    public GameSettings() {
        this(PRESET_MEDIUM, false, DEFAULT_KEY_BINDINGS);
    }

    /**
     * Creates game settings with specified values.
     *
     * @param windowSizePreset window size preset index (0 = Small, 1 = Medium, 2 = Large)
     * @param colorBlindMode true if color-blind accessible mode is enabled
     * @param keyBindings action-to-keyCode mapping
     */
    public GameSettings(int windowSizePreset, boolean colorBlindMode, Map<String, Integer> keyBindings) {
        this(windowSizePreset, colorBlindMode ? ColorVisionMode.DEUTAN : ColorVisionMode.NORMAL,
                colorBlindMode, keyBindings);
    }

    public GameSettings(int windowSizePreset, ColorVisionMode mode, boolean patterns,
            Map<String, Integer> keyBindings) {
        this(windowSizePreset, mode, patterns, keyBindings, Difficulty.NORMAL);
    }

    public GameSettings(int windowSizePreset, ColorVisionMode mode, boolean patterns,
            Map<String, Integer> keyBindings, Difficulty difficulty) {
        this(windowSizePreset, mode, patterns, keyBindings, difficulty,
                DEFAULT_P1_KEY_BINDINGS, DEFAULT_P2_KEY_BINDINGS);
    }

    // 대전 모드 키 설정을 포함한 생성자
    public GameSettings(int windowSizePreset, ColorVisionMode mode, boolean patterns,
            Map<String, Integer> keyBindings, Difficulty difficulty,
            Map<String, Integer> p1KeyBindings, Map<String, Integer> p2KeyBindings) {
        if (!isValidPreset(windowSizePreset)) {
            throw new IllegalArgumentException("Invalid window size preset: " + windowSizePreset
                    + ". Expected between " + PRESET_SMALL + " and " + PRESET_LARGE);
        }
        Objects.requireNonNull(keyBindings, "keyBindings must not be null");

        this.windowSizePreset = windowSizePreset;
        this.colorVisionMode = Objects.requireNonNull(mode, "colorVisionMode must not be null");
        this.piecePatternsEnabled = patterns;
        this.keyBindings = normalizeKeyBindings(keyBindings);
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty must not be null");

        this.p1KeyBindings = normalizeBattleKeyBindings(p1KeyBindings, DEFAULT_P1_KEY_BINDINGS);
        this.p2KeyBindings = normalizeBattleKeyBindings(p2KeyBindings, DEFAULT_P2_KEY_BINDINGS);
        validateBattleKeyBindings(this.p1KeyBindings, this.p2KeyBindings);
    }

    public Difficulty difficulty() { return difficulty; }

    public GameSettings withDifficulty(Difficulty next) {
        return new GameSettings(windowSizePreset, colorVisionMode, piecePatternsEnabled, keyBindings, next,
                p1KeyBindings, p2KeyBindings);
    }

    /** Completes partial bindings before validating; Esc always opens the game menu. */
    public static Map<String, Integer> normalizeKeyBindings(Map<String, Integer> bindings) {
        return normalizeBattleKeyBindings(bindings, DEFAULT_KEY_BINDINGS);
    }

    // 싱글 플레이와 대전 모드의 키 설정 검증
    public static Map<String, Integer> normalizeBattleKeyBindings(
            Map<String, Integer> bindings, Map<String, Integer> defaults) {
        Objects.requireNonNull(bindings, "keyBindings must not be null");
        Map<String, Integer> complete = new LinkedHashMap<>(defaults);
        for (Map.Entry<String, Integer> entry : bindings.entrySet()) {
            if (!defaults.containsKey(entry.getKey())) {
                throw new IllegalArgumentException("Unsupported configurable action: " + entry.getKey());
            }
            Integer code = entry.getValue();
            if (code == null || code <= KeyEvent.VK_UNDEFINED || code == KeyEvent.VK_ESCAPE) {
                throw new IllegalArgumentException("Invalid or reserved key code for " + entry.getKey());
            }
            complete.put(entry.getKey(), code);
        }
        java.util.Set<Integer> assigned = new java.util.HashSet<>();
        for (Integer code : complete.values()) {
            if (!assigned.add(code)) throw new IllegalArgumentException("Duplicate key assignment detected for key code " + code);
        }
        return Collections.unmodifiableMap(complete);
    }

    // P1/P2 키 중복 검사
    public static void validateBattleKeyBindings(
            Map<String, Integer> p1, Map<String, Integer> p2) {
        for (Integer code : p1.values()) {
            if (p2.containsValue(code)) {
                throw new IllegalArgumentException("Duplicate P1/P2 key: " + code);
            }
        }
    }

    /**
     * Checks if a preset index is valid.
     */
    public static boolean isValidPreset(int preset) {
        return preset >= PRESET_SMALL && preset <= PRESET_LARGE;
    }

    /**
     * Returns a human-readable label for the given preset index.
     */
    public static String presetLabel(int preset) {
        return switch (preset) {
            case PRESET_SMALL -> "Small (900×760)";
            case PRESET_MEDIUM -> "Medium (1040×860)";
            case PRESET_LARGE -> "Large (1280×1024)";
            default -> throw new IllegalArgumentException("Unknown preset: " + preset);
        };
    }

    /**
     * Factory method returning the default settings configuration.
     */
    public static GameSettings defaultSettings() {
        return new GameSettings();
    }

    public int windowSizePreset() {
        return windowSizePreset;
    }

    public String windowSizePresetLabel() {
        return presetLabel(windowSizePreset);
    }

    public boolean colorBlindMode() {
        return colorVisionMode != ColorVisionMode.NORMAL;
    }

    public ColorVisionMode colorVisionMode() { return colorVisionMode; }
    public boolean piecePatternsEnabled() { return piecePatternsEnabled; }

    public Map<String, Integer> keyBindings() {
        return keyBindings;
    }

    // P1/P2 키 설정 조회
    public Map<String, Integer> p1KeyBindings() {
        return p1KeyBindings;
    }

    public Map<String, Integer> p2KeyBindings() {
        return p2KeyBindings;
    }

    /**
     * Resolves the Dimension corresponding to the current window size preset.
     */
    public Dimension windowDimension() {
        return presetDimension(windowSizePreset);
    }

    /**
     * Returns the Dimension for a specific preset index.
     */
    public static Dimension presetDimension(int preset) {
        return switch (preset) {
            case PRESET_SMALL -> new Dimension(DIMENSION_SMALL);
            case PRESET_MEDIUM -> new Dimension(DIMENSION_MEDIUM);
            case PRESET_LARGE -> new Dimension(DIMENSION_LARGE);
            default -> throw new IllegalArgumentException("Unknown preset: " + preset);
        };
    }

    public GameSettings withWindowSizePreset(int preset) {
        return new GameSettings(preset, colorVisionMode, piecePatternsEnabled, keyBindings, difficulty,
                p1KeyBindings, p2KeyBindings);
    }

    public GameSettings withColorBlindMode(boolean enabled) {
        return withColorVisionMode(enabled ? ColorVisionMode.DEUTAN : ColorVisionMode.NORMAL);
    }

    public GameSettings withColorVisionMode(ColorVisionMode mode) {
        return new GameSettings(windowSizePreset, mode, mode.defaultPatternsEnabled(), keyBindings, difficulty,
                p1KeyBindings, p2KeyBindings);
    }

    public GameSettings withPiecePatternsEnabled(boolean enabled) {
        return new GameSettings(windowSizePreset, colorVisionMode, enabled, keyBindings, difficulty,
                p1KeyBindings, p2KeyBindings);
    }

    public GameSettings withKeyBindings(Map<String, Integer> bindings) {
        return new GameSettings(windowSizePreset, colorVisionMode, piecePatternsEnabled, bindings, difficulty,
                p1KeyBindings, p2KeyBindings);
    }

    // P1/P2 키 설정 변경
    public GameSettings withP1KeyBindings(Map<String, Integer> bindings) {
        return withBattleKeyBindings(bindings, p2KeyBindings);
    }

    public GameSettings withP2KeyBindings(Map<String, Integer> bindings) {
        return withBattleKeyBindings(p1KeyBindings, bindings);
    }

    public GameSettings withBattleKeyBindings(
            Map<String, Integer> p1, Map<String, Integer> p2) {
        return new GameSettings(windowSizePreset, colorVisionMode, piecePatternsEnabled,
                keyBindings, difficulty, p1, p2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameSettings that)) return false;
        return windowSizePreset == that.windowSizePreset
                && colorVisionMode == that.colorVisionMode
                && piecePatternsEnabled == that.piecePatternsEnabled
                && difficulty == that.difficulty
                && Objects.equals(keyBindings, that.keyBindings)
                && Objects.equals(p1KeyBindings, that.p1KeyBindings)
                && Objects.equals(p2KeyBindings, that.p2KeyBindings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(windowSizePreset, colorVisionMode, piecePatternsEnabled, keyBindings, difficulty,
                p1KeyBindings, p2KeyBindings);
    }

    @Override
    public String toString() {
        return "GameSettings{" +
                "windowSizePreset=" + windowSizePreset +
                " (" + windowSizePresetLabel() + ")" +
                ", colorVisionMode=" + colorVisionMode +
                ", piecePatternsEnabled=" + piecePatternsEnabled +
                ", difficulty=" + difficulty +
                ", keyBindings=" + keyBindings +
                ", p1KeyBindings=" + p1KeyBindings +
                ", p2KeyBindings=" + p2KeyBindings +
                '}';
    }
}