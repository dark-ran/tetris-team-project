package tetris.app;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tetris.game.GameAction;
import tetris.game.GameEngine;
import tetris.game.GamePhase;
import tetris.game.GameState;
import tetris.loop.GameLoop;
import tetris.scoreboard.ScoreBoardService;
import tetris.scoreboard.ScoreRepository;
import tetris.settings.ColorVisionMode;
import tetris.settings.GameSettings;
import tetris.settings.SettingsRepository;
import tetris.settings.SettingsService;
import tetris.ui.SettingsScreen;

class WindowSizeSettingsTest {
    enum Decision { KEEP, CANCEL, SAVE_FAILURE }
    enum Change { SIZE_SELECTOR, RESTORE_DEFAULTS }
    @TempDir Path directory;

    static Stream<Arguments> changes() {
        return Arrays.stream(Change.values()).flatMap(change -> Arrays.stream(Decision.values())
                .map(decision -> Arguments.of(change, decision)));
    }

    @ParameterizedTest @MethodSource("changes")
    void immediatePreviewCommitsOnlyTheAcceptedChangeOrRestoresWithoutLosingDrafts(Change change, Decision decision) throws Exception {
        AtomicBoolean failSave = new AtomicBoolean();
        SettingsRepository repository = new SettingsRepository(directory.resolve("settings")) {
            @Override protected void replaceAtomically(Path temporary, Path target) throws IOException {
                if (failSave.get()) throw new IOException("simulated full disk");
                super.replaceAtomically(temporary, target);
            }
        };
        GameSettings original = GameSettings.defaultSettings().withColorVisionMode(ColorVisionMode.DEUTAN)
                .withPiecePatternsEnabled(false);
        if (change == Change.RESTORE_DEFAULTS) original = original.withWindowSizePreset(GameSettings.PRESET_SMALL);
        GameSettings before = original;
        GameSettings accepted = change == Change.SIZE_SELECTOR ? before.withWindowSizePreset(GameSettings.PRESET_LARGE)
                : GameSettings.defaultSettings();
        repository.save(original);
        byte[] originalFile = Files.readAllBytes(repository.filePath());
        SettingsService service = new SettingsService(repository);
        AtomicReference<AppController> reference = new AtomicReference<>();
        AtomicInteger confirmations = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            AppController app = new AppController(() -> {}, new GameEngine(), new GameLoop(), null, null,
                    new ScoreBoardService(new ScoreRepository(directory.resolve("scores"))), service, null,
                    (owner, previous, proposed) -> {
                        confirmations.incrementAndGet();
                        assertEquals(before.windowDimension(), previous);
                        assertEquals(accepted.windowDimension(), proposed);
                        assertEquals(proposed, reference.get().view().getPreferredSize(), "Apply before asking");
                        assertEquals(before, service.current(), "Do not save before the decision");
                        assertEquals(before, repository.load());
                        assertEquals(AppState.SETTINGS, reference.get().state());
                        assertEquals(GamePhase.PAUSED, reference.get().snapshot().game().phase());
                        return decision != Decision.CANCEL;
                    });
            reference.set(app);
            try {
                app.start(); app.startGame(); app.handleAction(GameAction.HARD_DROP); app.showSettings();
                GameState paused = app.snapshot().game();
                SettingsScreen editor = descendants(app.view()).filter(SettingsScreen.class::isInstance)
                        .map(SettingsScreen.class::cast).findFirst().orElseThrow();
                var draftKeys = new HashMap<>(before.keyBindings()); draftKeys.put("LEFT", KeyEvent.VK_A);
                editor.showValues(new SettingsScreen.Values(before.windowSizePreset(), ColorVisionMode.MONOCHROME,
                        false, draftKeys));
                failSave.set(decision == Decision.SAVE_FAILURE);
                JComboBox<?> size = descendants(editor).filter(JComboBox.class::isInstance)
                        .map(JComboBox.class::cast).findFirst().orElseThrow();
                if (change == Change.SIZE_SELECTOR) size.setSelectedIndex(GameSettings.PRESET_LARGE);
                else descendants(editor).filter(JButton.class::isInstance).map(JButton.class::cast)
                        .filter(button -> button.getText().equals("기본값 복구")).findFirst().orElseThrow().doClick(0);
                assertEquals(1, confirmations.get(), "Programmatic restoration must not open another confirmation");
                GameSettings expected = decision == Decision.KEEP ? accepted : before;
                assertEquals(expected, service.current());
                assertEquals(expected, repository.load());
                assertEquals(expected, app.snapshot().settings());
                assertEquals(expected.windowDimension(), app.view().getPreferredSize());
                assertEquals(expected.windowSizePreset(), size.getSelectedIndex());
                boolean reset = change == Change.RESTORE_DEFAULTS && decision == Decision.KEEP;
                assertEquals(reset ? ColorVisionMode.NORMAL : ColorVisionMode.MONOCHROME, editor.values().colorVisionMode());
                assertFalse(editor.values().patternsEnabled(), "Preserve a user's pattern override");
                assertEquals(reset ? accepted.keyBindings() : draftKeys, editor.values().keys(),
                        "Size confirmation must preserve unsaved key edits unless restoring defaults");
                if (decision != Decision.KEEP) {
                    try { assertArrayEquals(originalFile, Files.readAllBytes(repository.filePath())); }
                    catch (IOException ex) { throw new UncheckedIOException(ex); }
                }
                if (decision == Decision.SAVE_FAILURE) assertTrue(descendants(editor)
                        .filter(JLabel.class::isInstance).map(JLabel.class::cast)
                        .anyMatch(label -> label.getText().contains("설정을 저장하지 못했습니다")));
                GameState after = app.snapshot().game();
                assertEquals(AppState.SETTINGS, app.state());
                assertEquals(GamePhase.PAUSED, after.phase());
                assertEquals(paused.currentPiece(), after.currentPiece());
                assertEquals(paused.currentRow(), after.currentRow());
                assertEquals(paused.currentColumn(), after.currentColumn());
                assertEquals(paused.nextPieces(), after.nextPieces());
                assertEquals(paused.score(), after.score());
                assertTrue(Arrays.deepEquals(paused.board().snapshot(), after.board().snapshot()));
            } finally { app.exit(); }
        });
    }

    private static Stream<Component> descendants(Component component) {
        return component instanceof Container container
                ? Stream.concat(Stream.of(component), Arrays.stream(container.getComponents()).flatMap(WindowSizeSettingsTest::descendants))
                : Stream.of(component);
    }
}
