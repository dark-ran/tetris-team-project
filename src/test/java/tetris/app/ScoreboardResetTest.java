package tetris.app;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tetris.game.GameAction;
import tetris.game.GameEngine;
import tetris.game.GameState;
import tetris.loop.GameLoop;
import tetris.scoreboard.ScoreBoardService;
import tetris.scoreboard.ScoreEntry;
import tetris.scoreboard.ScoreRepository;
import tetris.settings.SettingsRepository;
import tetris.settings.SettingsService;
import tetris.ui.ScoreboardScreen;
import tetris.ui.SettingsScreen;

class ScoreboardResetTest {
    enum Screen { SCOREBOARD, SETTINGS }
    enum Outcome { SUCCESS, DELETE_FAILURE, PENDING_SAVE_FAILURE }
    @TempDir Path directory;

    private final class ResetRepository extends ScoreRepository {
        boolean failClear, failSave;
        int clears;
        ResetRepository() { super(directory.resolve("scores.tsv")); }
        @Override public void save(List<ScoreEntry> entries) {
            if (failSave) throw new UncheckedIOException("simulated save failure", new IOException("locked"));
            super.save(entries);
        }
        @Override public void clear() {
            clears++;
            if (failClear) throw new UncheckedIOException("simulated delete failure", new IOException("locked"));
            super.clear();
        }
    }

    static Stream<Arguments> resetPaths() {
        return Arrays.stream(Screen.values()).flatMap(screen -> Arrays.stream(Outcome.values())
                .map(outcome -> Arguments.of(screen, outcome)));
    }

    @ParameterizedTest @MethodSource("resetPaths")
    void buttonsResetStorageAndViewTogetherOrPreserveBothOnFailure(Screen source, Outcome outcome) throws Exception {
        ResetRepository repository = new ResetRepository();
        ScoreBoardService service = new ScoreBoardService(repository); service.register("old", 500);
        SwingUtilities.invokeAndWait(() -> {
            AppController app = app(service);
            try {
                app.start(); app.startGame();
                for (int i = 0; i < 100 && app.state() == AppState.GAME; i++) app.handleAction(GameAction.HARD_DROP);
                assertEquals(AppState.GAME_OVER, app.state());
                repository.failSave = outcome == Outcome.PENDING_SAVE_FAILURE;
                app.registerScore("new"); app.showScoreboard();
                if (source == Screen.SETTINGS) app.showSettings();
                JPanel editor = source == Screen.SETTINGS ? find(app, SettingsScreen.class) : find(app, ScoreboardScreen.class);
                JTable table = tree(find(app, ScoreboardScreen.class)).filter(JTable.class::isInstance)
                        .map(JTable.class::cast).findFirst().orElseThrow();
                List<ScoreEntry> before = service.top(); byte[] saved = read(repository.path());
                int selected = table.getSelectedRow();
                ScoreEntry highlighted = app.snapshot().lastRegisteredScore(), pending = app.pendingScore();
                GameState game = app.snapshot().game(); var settings = app.snapshot().settings();
                long interval = app.snapshot().gravityIntervalMillis();
                AppState state = app.state();
                repository.failClear = outcome == Outcome.DELETE_FAILURE;
                tree(editor).filter(JButton.class::isInstance).map(JButton.class::cast)
                        .filter(button -> button.getText().equals("기록 초기화")).findFirst().orElseThrow().doClick(0);
                assertEquals(state, app.state(), "Reset must stay on the current screen");
                assertEquals(settings, app.snapshot().settings()); assertEquals(interval, app.snapshot().gravityIntervalMillis());
                assertEquals(game.score(), app.snapshot().game().score());
                assertEquals(game.phase(), app.snapshot().game().phase());
                assertTrue(Arrays.deepEquals(game.board().snapshot(), app.snapshot().game().board().snapshot()));
                if (outcome == Outcome.SUCCESS) {
                    assertTrue(service.top().isEmpty()); assertFalse(Files.exists(repository.path()));
                    assertEquals(0, table.getRowCount()); assertEquals(-1, table.getSelectedRow());
                    assertEquals(0, app.snapshot().highScore()); assertNull(app.snapshot().lastRegisteredScore());
                    assertNull(app.snapshot().persistenceError()); assertNull(app.pendingScore());
                    assertMessage(editor, "기록을 초기화했습니다.");
                } else {
                    assertEquals(before, service.top());
                    assertEquals(before, new ScoreBoardService(new ScoreRepository(repository.path())).top());
                    assertArrayEquals(saved, read(repository.path()));
                    assertEquals(before.size(), table.getRowCount()); assertEquals(selected, table.getSelectedRow());
                    assertEquals(highlighted, app.snapshot().lastRegisteredScore()); assertEquals(500, app.snapshot().highScore());
                    assertEquals(pending, app.pendingScore()); assertNotNull(app.snapshot().persistenceError());
                    assertMessage(editor, "초기화하지 못했습니다");
                }
                assertEquals(outcome == Outcome.PENDING_SAVE_FAILURE ? 0 : 1, repository.clears,
                        "Unresolved score saves must block storage clearing");
                assertEquals(outcome == Outcome.PENDING_SAVE_FAILURE, app.snapshot().awaitingScoreName());
            } finally { repository.failSave = false; app.exit(); }
        });
    }

    @Test void resetMethodReportsFailureSupportsRepeatedResetAndDoesNothingAfterExit() throws Exception {
        ResetRepository repository = new ResetRepository();
        ScoreBoardService service = new ScoreBoardService(repository); service.register("old", 500);
        SwingUtilities.invokeAndWait(() -> {
            AppController app = app(service);
            try {
                app.start(); app.showScoreboard(); repository.failClear = true;
                assertFalse(app.resetScoreboard()); assertEquals(500, service.highScore());
                repository.failClear = false;
                assertTrue(app.resetScoreboard()); assertTrue(app.resetScoreboard());
                assertEquals(0, service.highScore());
                app.exit(); int calls = repository.clears;
                assertFalse(app.resetScoreboard()); assertEquals(calls, repository.clears);
            } finally { app.exit(); }
        });
    }

    private AppController app(ScoreBoardService scores) {
        return new AppController(() -> {}, new GameEngine(new Random(21)), new GameLoop(), null, null, scores,
                new SettingsService(new SettingsRepository(directory.resolve("settings"))), null);
    }
    private static byte[] read(Path path) {
        try { return Files.readAllBytes(path); } catch (IOException ex) { throw new UncheckedIOException(ex); }
    }
    private static <T> T find(AppController app, Class<T> type) {
        return tree(app.view()).filter(type::isInstance).map(type::cast).findFirst().orElseThrow();
    }
    private static void assertMessage(JPanel screen, String text) {
        assertTrue(tree(screen).filter(JLabel.class::isInstance).map(JLabel.class::cast)
                .anyMatch(label -> label.getText().contains(text)));
    }
    private static Stream<Component> tree(Component component) {
        return component instanceof Container container
                ? Stream.concat(Stream.of(component), Arrays.stream(container.getComponents()).flatMap(ScoreboardResetTest::tree))
                : Stream.of(component);
    }
}
