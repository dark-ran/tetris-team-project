package tetris.settings;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import tetris.game.GameAction;

/**
 * Settings data value object.
 * Holds window size preset, color-blind mode toggle, and key binding mappings.
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

    static {
        Map<String, Integer> defaults = new LinkedHashMap<>();
        defaults.put(GameAction.LEFT.name(), KeyEvent.VK_LEFT);
        defaults.put(GameAction.RIGHT.name(), KeyEvent.VK_RIGHT);
        defaults.put(GameAction.DOWN.name(), KeyEvent.VK_DOWN);
        defaults.put(GameAction.ROTATE_CLOCKWISE.name(), KeyEvent.VK_UP);
        defaults.put(GameAction.HARD_DROP.name(), KeyEvent.VK_SPACE);
        defaults.put(GameAction.TOGGLE_PAUSE.name(), KeyEvent.VK_P);
        defaults.put(GameAction.QUIT.name(), KeyEvent.VK_Q);
        DEFAULT_KEY_BINDINGS = Collections.unmodifiableMap(defaults);
    }

    private final int windowSizePreset;
    private final boolean colorBlindMode;
    private final Map<String, Integer> keyBindings;

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
        if (!isValidPreset(windowSizePreset)) {
            throw new IllegalArgumentException("Invalid window size preset: " + windowSizePreset
                    + ". Expected between " + PRESET_SMALL + " and " + PRESET_LARGE);
        }
        Objects.requireNonNull(keyBindings, "keyBindings must not be null");

        this.windowSizePreset = windowSizePreset;
        this.colorBlindMode = colorBlindMode;
        this.keyBindings = Collections.unmodifiableMap(new LinkedHashMap<>(keyBindings));
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
        return colorBlindMode;
    }

    public Map<String, Integer> keyBindings() {
        return keyBindings;
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
        return new GameSettings(preset, this.colorBlindMode, this.keyBindings);
    }

    public GameSettings withColorBlindMode(boolean enabled) {
        return new GameSettings(this.windowSizePreset, enabled, this.keyBindings);
    }

    public GameSettings withKeyBindings(Map<String, Integer> bindings) {
        return new GameSettings(this.windowSizePreset, this.colorBlindMode, bindings);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameSettings that)) return false;
        return windowSizePreset == that.windowSizePreset
                && colorBlindMode == that.colorBlindMode
                && Objects.equals(keyBindings, that.keyBindings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(windowSizePreset, colorBlindMode, keyBindings);
    }

    @Override
    public String toString() {
        return "GameSettings{" +
                "windowSizePreset=" + windowSizePreset +
                " (" + windowSizePresetLabel() + ")" +
                ", colorBlindMode=" + colorBlindMode +
                ", keyBindings=" + keyBindings +
                '}';
    }
}
