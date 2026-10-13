package tetris.settings;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tetris.game.GameAction;

class SettingsRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void loadReturnsDefaultsWhenFileDoesNotExist() {
        Path nonExistent = tempDir.resolve("non_existent_settings.properties");
        SettingsRepository repository = new SettingsRepository(nonExistent);

        GameSettings settings = repository.load();
        assertEquals(GameSettings.defaultSettings(), settings);
    }

    @Test
    void saveAndReloadSettingsSuccessfully() {
        Path filePath = tempDir.resolve("saved_settings.properties");
        SettingsRepository repository = new SettingsRepository(filePath);

        Map<String, Integer> customKeys = new HashMap<>(GameSettings.DEFAULT_KEY_BINDINGS);
        customKeys.put(GameAction.LEFT.name(), KeyEvent.VK_A);
        customKeys.put(GameAction.RIGHT.name(), KeyEvent.VK_D);

        GameSettings original = new GameSettings(GameSettings.PRESET_LARGE, true, customKeys);
        repository.save(original);

        assertTrue(Files.exists(filePath));

        GameSettings loaded = repository.load();
        assertEquals(GameSettings.PRESET_LARGE, loaded.windowSizePreset());
        assertTrue(loaded.colorBlindMode());
        assertEquals(KeyEvent.VK_A, loaded.keyBindings().get(GameAction.LEFT.name()));
        assertEquals(KeyEvent.VK_D, loaded.keyBindings().get(GameAction.RIGHT.name()));
        assertEquals(original, loaded);
    }

    @Test
    void loadReturnsDefaultsWhenFileIsCorrupted() throws IOException {
        Path corrupted = tempDir.resolve("corrupted.properties");
        Files.writeString(corrupted, "window.size.preset=invalid_number\ncolor.blind.mode=not_boolean\n");

        SettingsRepository repository = new SettingsRepository(corrupted);
        GameSettings loaded = repository.load();

        assertNotNull(loaded);
        assertEquals(GameSettings.PRESET_MEDIUM, loaded.windowSizePreset());
    }

    @Test
    void saveCreatesParentDirectoryIfMissing() {
        Path nested = tempDir.resolve("nested/dir/settings.properties");
        SettingsRepository repository = new SettingsRepository(nested);

        repository.save(GameSettings.defaultSettings());
        assertTrue(Files.exists(nested));
    }
}
