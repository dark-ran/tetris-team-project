package tetris.settings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import tetris.game.Difficulty;

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
        properties.setProperty("color.vision.mode", settings.colorVisionMode().name());
        properties.setProperty("piece.patterns.enabled", String.valueOf(settings.piecePatternsEnabled()));
        properties.setProperty("difficulty", settings.difficulty().name());

        for (Map.Entry<String, Integer> entry : settings.keyBindings().entrySet()) {
            properties.setProperty("key." + entry.getKey(), String.valueOf(entry.getValue()));
        }

        // P1 대전 키 설정 저장
        for (Map.Entry<String, Integer> entry : settings.p1KeyBindings().entrySet()) {
            properties.setProperty("battle.p1.key." + entry.getKey(), String.valueOf(entry.getValue()));
        }

        // P2 대전 키 설정 저장
        for (Map.Entry<String, Integer> entry : settings.p2KeyBindings().entrySet()) {
            properties.setProperty("battle.p2.key." + entry.getKey(), String.valueOf(entry.getValue()));
        }

        Path temporary = null;
        try {
            Path target = filePath.toAbsolutePath();
            Files.createDirectories(target.getParent());
            temporary = Files.createTempFile(target.getParent(), "settings-", ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(temporary)) {
                properties.store(writer, "Tetris User Settings");
            }
            replaceAtomically(temporary, target);
        } catch (IOException e) {
            throw new UncheckedIOException("설정을 저장하지 못했습니다: " + filePath, e);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    /** Kept separate so failed replacements can be tested without changing OS permissions. */
    protected void replaceAtomically(Path temporary, Path target) throws IOException {
        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
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

        String modeName = properties.getProperty("color.vision.mode");
        ColorVisionMode mode = modeName == null
                ? (colorBlind ? ColorVisionMode.DEUTAN : ColorVisionMode.NORMAL)
                : ColorVisionMode.valueOf(modeName.trim());
        String patternsValue = properties.getProperty("piece.patterns.enabled");
        boolean patterns = mode.defaultPatternsEnabled();
        if (patternsValue != null) {
            if (!patternsValue.trim().equals("true") && !patternsValue.trim().equals("false"))
                throw new IllegalArgumentException("Invalid pattern setting");
            patterns = Boolean.parseBoolean(patternsValue.trim());
        }
        Difficulty difficulty = Difficulty.NORMAL;
        try {
            difficulty = Difficulty.valueOf(properties.getProperty("difficulty", "NORMAL").trim());
        } catch (IllegalArgumentException ignored) {
            // An unknown/new difficulty must not discard the user's other saved settings.
        }

        // P1/P2 대전 키 설정 불러오기
        Map<String, Integer> p1KeyBindings = loadBattleKeyBindings(
                properties, "battle.p1.key.", GameSettings.DEFAULT_P1_KEY_BINDINGS);

        Map<String, Integer> p2KeyBindings = loadBattleKeyBindings(
                properties, "battle.p2.key.", GameSettings.DEFAULT_P2_KEY_BINDINGS);

        // 대전 키 설정 검증
        try {
            p1KeyBindings = GameSettings.normalizeBattleKeyBindings(
                    p1KeyBindings, GameSettings.DEFAULT_P1_KEY_BINDINGS);

            p2KeyBindings = GameSettings.normalizeBattleKeyBindings(
                    p2KeyBindings, GameSettings.DEFAULT_P2_KEY_BINDINGS);

            GameSettings.validateBattleKeyBindings(p1KeyBindings, p2KeyBindings);
        } catch (IllegalArgumentException e) {
            p1KeyBindings = GameSettings.DEFAULT_P1_KEY_BINDINGS;
            p2KeyBindings = GameSettings.DEFAULT_P2_KEY_BINDINGS;
        }

        return new GameSettings(preset, mode, patterns, keyBindings, difficulty,
                p1KeyBindings, p2KeyBindings);
    }

    // 플레이어별 대전 키 설정 불러오기
    private Map<String, Integer> loadBattleKeyBindings(
            Properties properties, String prefix, Map<String, Integer> defaults) {
        Map<String, Integer> bindings = new HashMap<>(defaults);

        for (String actionName : defaults.keySet()) {
            String value = properties.getProperty(prefix + actionName);
            if (value != null) {
                try {
                    bindings.put(actionName, Integer.parseInt(value.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return bindings;
    }
}