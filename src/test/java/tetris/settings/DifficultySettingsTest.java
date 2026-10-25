package tetris.settings;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.game.Difficulty;

class DifficultySettingsTest {
    @TempDir Path directory;

    @ParameterizedTest @EnumSource(Difficulty.class)
    void savesReloadsAndPreservesDifficultyAcrossOtherEdits(Difficulty difficulty) {
        GameSettings settings = new GameSettings().withDifficulty(difficulty)
                .withWindowSizePreset(0).withColorBlindMode(true).withPiecePatternsEnabled(false)
                .withKeyBindings(GameSettings.DEFAULT_KEY_BINDINGS);
        assertEquals(difficulty, settings.difficulty());
        SettingsRepository repository = new SettingsRepository(directory.resolve("settings"));
        repository.save(settings);
        assertEquals(settings, new SettingsRepository(repository.filePath()).load());
        assertEquals(settings.hashCode(), repository.load().hashCode());
        SettingsService service = new SettingsService(repository);
        service.setWindowSizePreset(2);
        assertEquals(difficulty, service.current().difficulty());
        service.restoreDefaults();
        assertEquals(Difficulty.NORMAL, repository.load().difficulty());
    }

    @Test void oldAndUnknownDifficultySettingsKeepOtherPreferences() throws Exception {
        Path path = directory.resolve("settings");
        Files.writeString(path, "window.size.preset=0\n");
        assertEquals(Difficulty.NORMAL, new SettingsRepository(path).load().difficulty());
        Files.writeString(path, "window.size.preset=0\ndifficulty=UNKNOWN\n");
        GameSettings recovered = new SettingsRepository(path).load();
        assertEquals(0, recovered.windowSizePreset());
        assertEquals(Difficulty.NORMAL, recovered.difficulty());
        assertNotEquals(new GameSettings(), new GameSettings().withDifficulty(Difficulty.HARD));
        assertThrows(NullPointerException.class, () -> new GameSettings().withDifficulty(null));
    }
}
