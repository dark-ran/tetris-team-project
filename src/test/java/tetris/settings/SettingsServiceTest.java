package tetris.settings;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tetris.game.GameAction;

class SettingsServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultInitializationLoadsDefaults() {
        SettingsRepository repository = new SettingsRepository(tempDir.resolve("test_settings.properties"));
        SettingsService service = new SettingsService(repository);

        assertEquals(GameSettings.defaultSettings(), service.current());
    }

    @Test
    void updatePersistsAndNotifiesListeners() {
        SettingsRepository repository = new SettingsRepository(tempDir.resolve("test_settings.properties"));
        SettingsService service = new SettingsService(repository);

        AtomicBoolean notified = new AtomicBoolean(false);
        service.addChangeListener(settings -> notified.set(true));

        GameSettings updated = service.current().withWindowSizePreset(GameSettings.PRESET_SMALL);
        service.update(updated);

        assertEquals(GameSettings.PRESET_SMALL, service.current().windowSizePreset());
        assertTrue(notified.get());

        // Verify persisted to repository
        SettingsService reloaded = new SettingsService(repository);
        assertEquals(GameSettings.PRESET_SMALL, reloaded.current().windowSizePreset());
    }

    @Test
    void duplicateKeyBindingsAreRejected() {
        SettingsRepository repository = new SettingsRepository(tempDir.resolve("test_settings.properties"));
        SettingsService service = new SettingsService(repository);

        Map<String, Integer> duplicateKeys = new HashMap<>(service.current().keyBindings());
        // Map both LEFT and RIGHT to KeyEvent.VK_LEFT
        duplicateKeys.put(GameAction.LEFT.name(), KeyEvent.VK_LEFT);
        duplicateKeys.put(GameAction.RIGHT.name(), KeyEvent.VK_LEFT);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.update(service.current().withKeyBindings(duplicateKeys)));
        assertTrue(ex.getMessage().contains("Duplicate key assignment detected"));
    }

    @Test
    void restoreDefaultsResetsAllSettings() {
        SettingsRepository repository = new SettingsRepository(tempDir.resolve("test_settings.properties"));
        SettingsService service = new SettingsService(repository);

        // Make modifications
        service.update(service.current()
                .withWindowSizePreset(GameSettings.PRESET_LARGE)
                .withColorBlindMode(true));

        assertEquals(GameSettings.PRESET_LARGE, service.current().windowSizePreset());
        assertTrue(service.current().colorBlindMode());

        // Restore defaults
        service.restoreDefaults();

        assertEquals(GameSettings.PRESET_MEDIUM, service.current().windowSizePreset());
        assertFalse(service.current().colorBlindMode());
    }

    @Test
    void chooseThreeDifferentWindowSizePresets() {
        SettingsRepository repository = new SettingsRepository(tempDir.resolve("test_settings.properties"));
        SettingsService service = new SettingsService(repository);

        assertEquals(List.of(0, 1, 2), service.availableWindowSizePresets());

        // Choose Preset 0 (Small)
        service.setWindowSizePreset(GameSettings.PRESET_SMALL);
        assertEquals(GameSettings.PRESET_SMALL, service.windowSizePreset());
        assertEquals(GameSettings.DIMENSION_SMALL, service.currentWindowDimension());
        assertEquals("Small (900×760)", service.currentWindowSizePresetLabel());

        // Choose Preset 1 (Medium)
        service.setWindowSizePreset(GameSettings.PRESET_MEDIUM);
        assertEquals(GameSettings.PRESET_MEDIUM, service.windowSizePreset());
        assertEquals(GameSettings.DIMENSION_MEDIUM, service.currentWindowDimension());
        assertEquals("Medium (1040×860)", service.currentWindowSizePresetLabel());

        // Choose Preset 2 (Large)
        service.setWindowSizePreset(GameSettings.PRESET_LARGE);
        assertEquals(GameSettings.PRESET_LARGE, service.windowSizePreset());
        assertEquals(GameSettings.DIMENSION_LARGE, service.currentWindowDimension());
        assertEquals("Large (1280×1024)", service.currentWindowSizePresetLabel());

        // Verify invalid preset throws
        assertThrows(IllegalArgumentException.class, () -> service.setWindowSizePreset(3));
        assertThrows(IllegalArgumentException.class, () -> service.setWindowSizePreset(-1));
    }

    @Test
    void cycleWindowSizePresetTransitionsThroughAllSizes() {
        SettingsRepository repository = new SettingsRepository(tempDir.resolve("test_settings.properties"));
        SettingsService service = new SettingsService(repository);

        // Starts at Medium (1)
        assertEquals(GameSettings.PRESET_MEDIUM, service.windowSizePreset());

        // 1 -> 2 (Large)
        int next = service.cycleWindowSizePreset();
        assertEquals(GameSettings.PRESET_LARGE, next);
        assertEquals(GameSettings.PRESET_LARGE, service.windowSizePreset());

        // 2 -> 0 (Small)
        next = service.cycleWindowSizePreset();
        assertEquals(GameSettings.PRESET_SMALL, next);
        assertEquals(GameSettings.PRESET_SMALL, service.windowSizePreset());

        // 0 -> 1 (Medium)
        next = service.cycleWindowSizePreset();
        assertEquals(GameSettings.PRESET_MEDIUM, next);
        assertEquals(GameSettings.PRESET_MEDIUM, service.windowSizePreset());
    }
}
