package tetris.app;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tetris.game.GameEngine;
import tetris.game.GameState;

import tetris.loop.GameLoop;
import tetris.scoreboard.ScoreBoardService;
import tetris.scoreboard.ScoreRepository;

class AppApiConnectionTest {
    @TempDir
    Path directory;

    private static final class Loop extends GameLoop {
        long interval;
        int stops;

        @Override
        public void start() {
        }

        @Override
        public void stop() {
            stops++;
        }

        @Override
        public void pause() {
        }

        @Override
        public void resume() {
        }

        @Override
        public void setIntervalMillis(long millis) {
            interval = millis;
        }

        Runnable tickListener;

        @Override
        public void setTickListener(Runnable listener) {
            tickListener = listener;
        }

        void tick() {
            tickListener.run();
        }
    }

    private ScoreBoardService scores() {
        return new ScoreBoardService(new ScoreRepository(directory.resolve("scores")));
    }

    @Test
    void registeredTickAndDefaultIntervalApiDriveTheRealEngine() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Loop loop = new Loop();
            List<GameState> rendered = new ArrayList<>();
            AppController app = new AppController(() -> {
            }, new GameEngine(), loop, null,
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

}
