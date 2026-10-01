package tetris.settings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Handles persistent storage of game settings in a user-specific directory.
 * Safely handles missing files, read/write errors, and data corruption by falling back to defaults.
 */
public class SettingsRepository {
    private static final String DEFAULT_SUBDIR = ".tetris";
    private static final String DEFAULT_FILENAME = "settings.properties";

    private final Path filePath;

    public SettingsRepository() {
        this(defaultPath());
    }

    public SettingsRepository(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath must not be null");
    }

    public static Path defaultPath() {
        return Paths.get(System.getProperty("user.home"), DEFAULT_SUBDIR, DEFAULT_FILENAME);
    }

    public Path filePath() {
        return filePath;
    }

    public GameSettings load() {
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            return GameSettings.defaultSettings();
        }

        Properties properties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            properties.load(reader);
            return parseSettings(properties);
        } catch (Exception e) {
            // If file is corrupted or unreadable, safely fall back to default settings
            return GameSettings.defaultSettings();
        }
    }

    public void save(GameSettings settings) {
        Objects.requireNonNull(settings, "settings must not be null");

        Properties properties = new Properties();
        properties.setProperty("window.size.preset", String.valueOf(settings.windowSizePreset()));
        properties.setProperty("color.blind.mode", String.valueOf(settings.colorBlindMode()));

        for (Map.Entry<String, Integer> entry : settings.keyBindings().entrySet()) {
            properties.setProperty("key." + entry.getKey(), String.valueOf(entry.getValue()));
        }

        try {
            Path parent = filePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                properties.store(writer, "Tetris User Settings");
            }
        } catch (IOException e) {
            System.err.println("Warning: Failed to save settings to " + filePath + ": " + e.getMessage());
        }
    }

    private GameSettings parseSettings(Properties properties) {
        int preset = GameSettings.PRESET_MEDIUM;
        try {
            String presetStr = properties.getProperty("window.size.preset");
            if (presetStr != null) {
                int parsed = Integer.parseInt(presetStr.trim());
                if (parsed >= GameSettings.PRESET_SMALL && parsed <= GameSettings.PRESET_LARGE) {
                    preset = parsed;
                }
            }
        } catch (NumberFormatException ignored) {
        }

        boolean colorBlind = false;
        String colorBlindStr = properties.getProperty("color.blind.mode");
        if (colorBlindStr != null) {
            colorBlind = Boolean.parseBoolean(colorBlindStr.trim());
        }

        Map<String, Integer> keyBindings = new HashMap<>(GameSettings.DEFAULT_KEY_BINDINGS);
        for (String actionName : GameSettings.DEFAULT_KEY_BINDINGS.keySet()) {
            String propVal = properties.getProperty("key." + actionName);
            if (propVal != null) {
                try {
                    keyBindings.put(actionName, Integer.parseInt(propVal.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return new GameSettings(preset, colorBlind, keyBindings);
    }
}
