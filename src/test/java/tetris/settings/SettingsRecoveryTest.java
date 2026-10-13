package tetris.settings;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsRecoveryTest {
    @TempDir Path directory;

    @Test void partialBindingsAreCompletedBeforeSaveAndRemainIdenticalAfterReload() {
        SettingsRepository repository = new SettingsRepository(directory.resolve("settings"));
        SettingsService service = new SettingsService(repository);
        GameSettings changed = service.current().withKeyBindings(Map.of("LEFT", KeyEvent.VK_A));
        service.update(changed);
        assertEquals(GameSettings.DEFAULT_KEY_BINDINGS.size(), changed.keyBindings().size());
        assertEquals(changed, new SettingsService(repository).current());
        assertThrows(IllegalArgumentException.class,
                () -> changed.withKeyBindings(Map.of("LEFT", KeyEvent.VK_RIGHT)));
    }

    @Test void reservedMissingAndNonUserActionsCannotBeAssigned() {
        GameSettings initial = new GameSettings();
        assertThrows(IllegalArgumentException.class,
                () -> initial.withKeyBindings(Map.of("LEFT", KeyEvent.VK_ESCAPE)));
        assertThrows(IllegalArgumentException.class,
                () -> initial.withKeyBindings(Map.of("TOGGLE_PAUSE", KeyEvent.VK_P)));
        assertThrows(IllegalArgumentException.class,
                () -> initial.withKeyBindings(Map.of("TICK", KeyEvent.VK_A)));
        Map<String, Integer> nullKey = new LinkedHashMap<>();
        nullKey.put("LEFT", null);
        assertThrows(IllegalArgumentException.class, () -> initial.withKeyBindings(nullKey));
    }

    @Test void duplicateOrReservedStoredKeysFallBackToValidDefaults() throws IOException {
        Path file = directory.resolve("settings");
        for (String contents : new String[] {"key.LEFT=39\n", "key.LEFT=27\n", "key.LEFT=0\n"}) {
            Files.writeString(file, contents);
            SettingsService service = new SettingsService(new SettingsRepository(file));
            assertEquals(GameSettings.defaultSettings(), service.current());
            assertDoesNotThrow(() -> service.setWindowSizePreset(0));
        }
    }

    @Test void legacyPauseAssignmentIsMigratedToFixedEscapeMenu() throws IOException {
        Path file = directory.resolve("settings");
        Files.writeString(file, "key.TOGGLE_PAUSE=80\nkey.LEFT=65\n");
        GameSettings loaded = new SettingsRepository(file).load();
        assertFalse(loaded.keyBindings().containsKey("TOGGLE_PAUSE"));
        assertEquals(KeyEvent.VK_A, loaded.keyBindings().get("LEFT"));
        assertEquals(KeyEvent.VK_SPACE, loaded.keyBindings().get("HARD_DROP"));
    }

    @Test void failedSavePreservesCurrentSettingsFileAndDoesNotNotifyListeners() throws IOException {
        Path file = directory.resolve("settings");
        GameSettings initial = new GameSettings();
        new SettingsRepository(file).save(initial);
        byte[] before = Files.readAllBytes(file);
        SettingsRepository repository = new SettingsRepository(file) {
            @Override protected void replaceAtomically(Path temporary, Path target) throws IOException {
                throw new IOException("replacement denied");
            }
        };
        SettingsService service = new SettingsService(repository);
        AtomicInteger notices = new AtomicInteger();
        service.addChangeListener(settings -> notices.incrementAndGet());
        assertThrows(UncheckedIOException.class,
                () -> service.update(initial.withColorBlindMode(true)));
        assertEquals(initial, service.current());
        assertEquals(0, notices.get());
        assertArrayEquals(before, Files.readAllBytes(file));
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }

    @Test void directoryAtSettingsPathReportsWriteFailure() throws IOException {
        Path blocked = Files.createDirectory(directory.resolve("settings"));
        assertThrows(UncheckedIOException.class,
                () -> new SettingsRepository(blocked).save(new GameSettings()));
    }
}
