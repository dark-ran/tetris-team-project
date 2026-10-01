package tetris.settings;

import java.awt.Dimension;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Manages retrieval, modification, validation, and restoration of game settings.
 * Follows the project architecture boundary for settings management.
 */
public class SettingsService {
    private final SettingsRepository repository;
    private final List<Consumer<GameSettings>> changeListeners = new CopyOnWriteArrayList<>();
    private GameSettings currentSettings;

    public SettingsService() {
        this(new SettingsRepository());
    }

    public SettingsService(SettingsRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.currentSettings = repository.load();
    }

    public GameSettings current() {
        return currentSettings;
    }

    public void update(GameSettings settings) {
        Objects.requireNonNull(settings, "settings must not be null");
        validate(settings);

        this.currentSettings = settings;
        this.repository.save(settings);
        notifyListeners(settings);
    }

    public void restoreDefaults() {
        update(GameSettings.defaultSettings());
    }

    /**
     * Returns the currently selected window size preset index (0, 1, or 2).
     */
    public int windowSizePreset() {
        return currentSettings.windowSizePreset();
    }

    /**
     * Returns the Dimension corresponding to the current window size preset.
     */
    public Dimension currentWindowDimension() {
        return currentSettings.windowDimension();
    }

    /**
     * Returns the human-readable label of the currently selected preset.
     */
    public String currentWindowSizePresetLabel() {
        return currentSettings.windowSizePresetLabel();
    }

    /**
     * Sets the window size preset to one of the 3 predefined sizes (0: Small, 1: Medium, 2: Large).
     * Validates the preset, persists to repository, and notifies all registered listeners.
     *
     * @param preset preset index between 0 and 2
     * @throws IllegalArgumentException if preset is outside [0, 2]
     */
    public void setWindowSizePreset(int preset) {
        if (!GameSettings.isValidPreset(preset)) {
            throw new IllegalArgumentException("Invalid window size preset: " + preset
                    + ". Expected one of: " + GameSettings.PRESETS);
        }
        update(currentSettings.withWindowSizePreset(preset));
    }

    /**
     * Cycles to the next available window size preset (Small -> Medium -> Large -> Small).
     *
     * @return the new preset index
     */
    public int cycleWindowSizePreset() {
        int nextPreset = (currentSettings.windowSizePreset() + 1) % GameSettings.PRESET_COUNT;
        setWindowSizePreset(nextPreset);
        return nextPreset;
    }

    /**
     * Returns the list of all available preset indices.
     */
    public List<Integer> availableWindowSizePresets() {
        return GameSettings.PRESETS;
    }

    /**
     * Returns the Dimension for a given preset index.
     */
    public Dimension presetDimension(int preset) {
        return GameSettings.presetDimension(preset);
    }

    /**
     * Returns the label for a given preset index.
     */
    public String presetLabel(int preset) {
        return GameSettings.presetLabel(preset);
    }

    public void addChangeListener(Consumer<GameSettings> listener) {
        changeListeners.add(Objects.requireNonNull(listener, "listener must not be null"));
    }

    public void removeChangeListener(Consumer<GameSettings> listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners(GameSettings settings) {
        for (Consumer<GameSettings> listener : changeListeners) {
            listener.accept(settings);
        }
    }

    private void validate(GameSettings settings) {
        if (!GameSettings.isValidPreset(settings.windowSizePreset())) {
            throw new IllegalArgumentException("Invalid window size preset: " + settings.windowSizePreset());
        }

        Set<Integer> assignedKeys = new HashSet<>();
        for (Map.Entry<String, Integer> entry : settings.keyBindings().entrySet()) {
            Integer keyCode = entry.getValue();
            if (keyCode != null && !assignedKeys.add(keyCode)) {
                throw new IllegalArgumentException(
                        "Duplicate key assignment detected for key code " + keyCode
                                + " on action " + entry.getKey());
            }
        }
    }
}
