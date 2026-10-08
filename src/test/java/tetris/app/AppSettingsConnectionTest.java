package tetris.app;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tetris.game.GameAction;
import tetris.game.GameEngine;
import tetris.game.GameState;
import tetris.loop.GameLoop;
import tetris.scoreboard.ScoreBoardService;
import tetris.scoreboard.ScoreRepository;
import tetris.settings.GameSettings;
import tetris.settings.SettingsService;

class AppSettingsConnectionTest {
    @TempDir Path directory;

    private static class TestLoop extends GameLoop {
        @Override public void start() { }
        @Override public void stop() { }
        @Override public void pause() { }
        @Override public void resume() { }
    }

    private ScoreBoardService scores() {
        return new ScoreBoardService(new ScoreRepository(directory.resolve("scores")));
    }

    @Test void rendererReceivesLatestEngineState() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicReference<GameState> delivered = new AtomicReference<>();
            AppController app = new AppController(() -> {}, new GameEngine(), new TestLoop(),
                    ignored -> {}, delivered::set, scores(), null, null);
            app.startGame();
            app.handleAction(GameAction.DOWN);
            assertEquals(1, delivered.get().score());
            assertEquals(app.snapshot().game().currentRow(), delivered.get().currentRow());
            app.exit();
        });
    }

    @Test void loopFailuresArePropagatedWithoutReplacement() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            UnsupportedOperationException failure = new UnsupportedOperationException("loop not implemented");
            GameLoop loop = new GameLoop() {
                @Override public void stop() { throw failure; }
            };
            AppController app = new AppController(() -> {}, new GameEngine(), loop,
                    ignored -> {}, ignored -> {}, scores(), null, null);
            assertSame(failure, assertThrows(UnsupportedOperationException.class, app::startGame));
        });
    }

    @Test void defaultIntervalRouteUsesLoopApiWithoutHidingUnimplementedFailure() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AppController app = new AppController(() -> {}, new GameEngine(), new TestLoop() {
                        @Override public void setIntervalMillis(long millis) { throw new UnsupportedOperationException("낙하 간격 변경 실패"); }
                    }, null, ignored -> {}, scores(), null, null);
            UnsupportedOperationException failure = assertThrows(UnsupportedOperationException.class, app::startGame);
            assertTrue(failure.getMessage().contains("낙하 간격 변경"));
        });
    }

    @Test void settingsFailuresArePropagatedWithoutAnArtificialFallback() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            UnsupportedOperationException failure = new UnsupportedOperationException("settings not implemented");
            SettingsService failing = new SettingsService() {
                @Override public GameSettings current() { throw failure; }
            };
            AppController app = new AppController(() -> {}, new GameEngine(), new TestLoop(),
                    ignored -> {}, ignored -> {}, scores(), failing, null);
            assertSame(failure, assertThrows(UnsupportedOperationException.class, app::start));
            SettingsService readable = new SettingsService() {
                @Override public GameSettings current() { return new GameSettings(); }
            };
            AppController unconnected = new AppController(() -> {}, new GameEngine(), new TestLoop(),
                    ignored -> {}, ignored -> {}, scores(), readable, settings -> { throw new UnsupportedOperationException("settings application failed"); });
            assertThrows(UnsupportedOperationException.class, unconnected::start);
            assertNull(unconnected.snapshot().settings());
        });
    }

    @Test void rendererFailuresArePropagated() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            UnsupportedOperationException failure = new UnsupportedOperationException("render not implemented");
            AppController app = new AppController(() -> {}, new GameEngine(), new TestLoop(),
                    ignored -> {}, state -> { throw failure; }, scores(), null, null);
            assertSame(failure, assertThrows(UnsupportedOperationException.class, app::startGame));
        });
    }
}
