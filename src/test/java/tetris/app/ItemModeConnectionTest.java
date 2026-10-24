package tetris.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Path;
import java.util.Random;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tetris.game.GameAction;
import tetris.game.GameEngine;
import tetris.game.GameMode;
import tetris.loop.GameLoop;
import tetris.scoreboard.ScoreBoardService;
import tetris.scoreboard.ScoreRepository;

class ItemModeConnectionTest {
    @TempDir Path directory;

    @Test void itemModeSurvivesPauseRestartAndScoreRegistration() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = new GameEngine(new Random(21));
            ScoreRepository repository = new ScoreRepository(directory.resolve("scores"));
            GameLoop loop = new GameLoop() { @Override public void start() { } };
            AppController app = new AppController(() -> {}, engine, loop, ignored -> {},
                    ignored -> {}, new ScoreBoardService(repository), null, ignored -> {});
            app.startGame(GameMode.ITEM);
            app.pauseGame();
            app.resumeGame();
            assertEquals(GameMode.ITEM, engine.state().mode());
            app.restartGame();
            assertEquals(GameMode.ITEM, engine.state().mode());
            for (int i = 0; i < 100 && !engine.isGameOver(); i++) app.handleAction(GameAction.HARD_DROP);
            assertTrue(engine.isGameOver());
            app.registerScore("item player");
            assertEquals(GameMode.ITEM, repository.load().getFirst().mode());
            app.startGame();
            assertEquals(GameMode.NORMAL, engine.state().mode());
            app.exit();
        });
    }
}
