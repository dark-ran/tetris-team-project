package tetris.app;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tetris.game.GameAction;
import tetris.game.GameEngine;
import tetris.game.GameState;
import tetris.input.InputHandler;
import tetris.input.KeyMapper;
import tetris.loop.GameLoop;
import tetris.scoreboard.*;
import tetris.settings.*;
import tetris.ui.*;

class AppApiConnectionTest {
    @TempDir Path directory;

    private static final class Loop extends GameLoop {
        long interval;
        int stops;
        @Override public void start() { }
        @Override public void stop() { stops++; }
        @Override public void pause() { }
        @Override public void resume() { }
        @Override public void setIntervalMillis(long millis) { interval = millis; }
        void tick() { emitTick(); }
    }

    private ScoreBoardService scores() {
        return new ScoreBoardService(new ScoreRepository(directory.resolve("scores")));
    }

    private static <T> T screen(AppController app, Class<T> type) {
        return java.util.Arrays.stream(app.view().getComponents()).filter(type::isInstance)
                .map(type::cast).findFirst().orElseThrow();
    }

    @Test void registeredTickAndDefaultIntervalApiDriveTheRealEngine() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Loop loop = new Loop();
            List<GameState> rendered = new ArrayList<>();
            AppController app = new AppController(() -> {}, new GameEngine(), loop, null,
                    rendered::add, scores(), null, null);
            app.startGame();
            int row = app.snapshot().game().currentRow();
            assertEquals(1000, loop.interval);
            loop.tick();
            assertEquals(row + 1, app.snapshot().game().currentRow());
            assertEquals(1, rendered.getLast().score());
            app.pauseGame();
            loop.tick();
            assertEquals(row + 1, app.snapshot().game().currentRow());
            app.resumeGame();
            loop.tick();
            assertEquals(row + 2, app.snapshot().game().currentRow());
            app.exit();
            assertEquals(2, loop.stops);
            loop.tick();
            assertEquals(2, app.snapshot().game().score());
        });
    }

    @Test void nameSkipRetryAndRankingDataAreConnectedToUiApis() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            class FailingRepository extends ScoreRepository {
                boolean fail = true;
                FailingRepository() { super(directory.resolve("scores")); }
                @Override public void save(List<ScoreEntry> entries) {
                    if (fail) throw new java.io.UncheckedIOException(new java.io.IOException("locked"));
                    super.save(entries);
                }
            }
            FailingRepository repository = new FailingRepository();
            GameEngine engine = new GameEngine(new java.util.Random(21));
            AppController app = new AppController(() -> {}, engine, new Loop(), null,
                    ignored -> {}, new ScoreBoardService(repository), null, null);
            app.startGame();
            for (int i = 0; i < 100 && !engine.isGameOver(); i++) app.handleAction(GameAction.HARD_DROP);
            GameOverScreen ui = screen(app, GameOverScreen.class);
            assertTrue(ui.canRegisterScore());
            assertThrows(IllegalArgumentException.class, () -> ui.submitScoreName("a\tb"));
            ui.submitScoreName("player");
            assertNotNull(ui.scoreSaveError());
            assertFalse(ui.retryScoreSave());
            ui.skipRegistration();
            assertTrue(app.snapshot().awaitingScoreName());
            repository.fail = false;
            assertTrue(ui.retryScoreSave());
            assertNull(ui.scoreSaveError());
            assertEquals(AppState.SCOREBOARD, app.state());
            assertEquals(new ScoreEntry("player", engine.state().score()),
                    screen(app, ScoreboardScreen.class).highlightedEntry());
            app.startGame();
            for (int i = 0; i < 100 && !engine.isGameOver(); i++) app.handleAction(GameAction.HARD_DROP);
            ui.skipRegistration();
            assertEquals(AppState.SCOREBOARD, app.state());
            assertFalse(app.snapshot().awaitingScoreName());
            assertNull(screen(app, ScoreboardScreen.class).highlightedEntry());
            assertEquals(1, repository.load().size());
            app.exit();
        });
    }

    @Test void settingsUiRequestsReachServiceAndRankingReset() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameSettings initial = new GameSettings();
            SettingsService service = new SettingsService() {
                GameSettings current = initial;
                @Override public GameSettings current() { return current; }
                @Override public void update(GameSettings next) { current = next; }
                @Override public void restoreDefaults() { current = initial; }
            };
            ScoreBoardService scores = scores();
            scores.register("player", 100);
            List<GameSettings> applied = new ArrayList<>();
            AppController app = new AppController(() -> {}, new GameEngine(), new Loop(), null,
                    ignored -> {}, scores, service, applied::add);
            app.start();
            SettingsScreen ui = screen(app, SettingsScreen.class);
            GameSettings next = new GameSettings();
            ui.submitSettings(next);
            assertSame(next, applied.getLast());
            ui.restoreDefaults();
            assertSame(initial, applied.getLast());
            assertEquals(100, app.snapshot().highScore());
            ui.resetScores();
            assertEquals(0, app.snapshot().highScore());
            app.exit();
        });
    }

    @Test void inputBridgeUsesMapperAndIgnoresUnmappedKeys() {
        InputHandler input = new InputHandler(new KeyMapper() {
            @Override public GameAction map(int code) { return code == 40 ? GameAction.DOWN : null; }
        });
        List<GameAction> actions = new ArrayList<>();
        input.setActionListener(actions::add);
        input.handleKey(40);
        input.handleKey(0);
        assertEquals(List.of(GameAction.DOWN), actions);
        assertThrows(UnsupportedOperationException.class, () -> {
            InputHandler unimplemented = new InputHandler();
            unimplemented.setActionListener(actions::add);
            unimplemented.handleKey(40);
        });
    }
}
