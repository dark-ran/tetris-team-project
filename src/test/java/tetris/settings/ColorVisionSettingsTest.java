package tetris.settings;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ColorVisionSettingsTest {
    @TempDir Path directory;

    @ParameterizedTest @EnumSource(ColorVisionMode.class)
    void everyModePersistsWithPatternsIndependentlyEnabledOrDisabled(ColorVisionMode mode) {
        SettingsRepository repository = new SettingsRepository(directory.resolve("settings"));
        for (boolean patterns : new boolean[] {false, true}) {
            GameSettings settings = new GameSettings(2, mode, patterns, GameSettings.DEFAULT_KEY_BINDINGS);
            repository.save(settings);
            assertEquals(settings, repository.load());
            assertEquals(mode, settings.withWindowSizePreset(0).colorVisionMode());
            assertEquals(patterns, settings.withKeyBindings(GameSettings.DEFAULT_KEY_BINDINGS).piecePatternsEnabled());
        }
        GameSettings selected = new GameSettings().withColorVisionMode(mode);
        assertEquals(mode.defaultPatternsEnabled(), selected.piecePatternsEnabled());
    }

    @Test void legacyBooleanSettingsLoadAsTheAssistanceModeWithPatterns() throws Exception {
        Path path = directory.resolve("settings");
        Files.writeString(path, "window.size.preset=0\ncolor.blind.mode=true\n");
        GameSettings settings = new SettingsRepository(path).load();
        assertEquals(ColorVisionMode.DEUTAN, settings.colorVisionMode());
        assertTrue(settings.piecePatternsEnabled());
        assertEquals(0, settings.windowSizePreset());
    }

    @Test void invalidModeAndPatternValuesRecoverSafely() throws Exception {
        Path path = directory.resolve("settings");
        for (String invalid : new String[] {"color.vision.mode=UNKNOWN", "color.vision.mode=PROTAN\npiece.patterns.enabled=maybe"}) {
            Files.writeString(path, invalid);
            assertEquals(new GameSettings(), new SettingsRepository(path).load());
        }
    }
}
