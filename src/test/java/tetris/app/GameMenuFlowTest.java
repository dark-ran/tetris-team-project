package tetris.app;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tetris.game.*;
import tetris.loop.GameLoop;
import tetris.scoreboard.*;
import tetris.settings.*;

class GameMenuFlowTest {
    @TempDir Path directory;
    private static class Loop extends GameLoop {
        int pauses, resumes;
        @Override public void start() {}
        @Override public void stop() {}
        @Override public void pause() { pauses++; }
        @Override public void resume() { resumes++; }
        @Override public void setIntervalMillis(long millis) {}
    }

    @Test void settingsReturnToPausedMenuAndResumePreservesTheWholeGame() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = new GameEngine(new Random(21));
            Loop loop = new Loop();
            List<GameSettings> applied = new ArrayList<>();
            GameSettings initial = new GameSettings(), changed = new GameSettings();
            SettingsService settings = new SettingsService() {
                GameSettings current = initial;
                @Override public GameSettings current() { return current; }
                @Override public void update(GameSettings next) { current = next; }
            };
            AppController app = new AppController(() -> {}, engine, loop, null, ignored -> {},
                    new ScoreBoardService(new ScoreRepository(directory.resolve("scores"))), settings, applied::add);
            app.start(); app.startGame();
            app.handleAction(GameAction.DOWN); app.handleAction(GameAction.HARD_DROP);
            GameState before = engine.state();
            app.showGameMenu();
            assertEquals(AppState.GAME_MENU, app.state());
            assertEquals(GamePhase.PAUSED, engine.state().phase());
            assertEquals(1, loop.pauses);
            app.handleAction(GameAction.HARD_DROP);
            assertUnchanged(before, engine.state());
            app.showSettings();
            assertEquals(AppState.SETTINGS, app.state());
            app.updateSettings(changed);
            assertSame(changed, applied.getLast());
            app.closeSettings();
            assertEquals(AppState.GAME_MENU, app.state());
            assertEquals(0, loop.resumes);
            assertUnchanged(before, engine.state());
            app.resumeGame();
            assertEquals(AppState.GAME, app.state());
            assertEquals(GamePhase.RUNNING, engine.state().phase());
            assertEquals(1, loop.resumes);
            assertUnchanged(before, engine.state());
            app.handleAction(GameAction.DOWN);
            assertEquals(before.score() + 1, engine.state().score());
            app.showGameMenu(); app.showStartMenu(); app.startGame();
            assertEquals(0, engine.state().score());
            assertEquals(1, engine.state().spawnedPieces());
            app.exit();
        });
    }

    @Test void failedSettingsUpdateKeepsTheGamePausedAndRetainsAppliedSettings() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameSettings initial = new GameSettings();
            SettingsService settings = new SettingsService() {
                @Override public GameSettings current() { return initial; }
                @Override public void update(GameSettings next) {
                    throw new java.io.UncheckedIOException(new java.io.IOException("denied"));
                }
            };
            AppController app = new AppController(() -> {}, new GameEngine(), new Loop(), null, ignored -> {},
                    new ScoreBoardService(new ScoreRepository(directory.resolve("scores"))), settings, ignored -> {});
            app.start(); app.startGame(); app.showGameMenu(); app.showSettings();
            assertThrows(java.io.UncheckedIOException.class, () -> app.updateSettings(new GameSettings()));
            assertSame(initial, app.snapshot().settings());
            assertEquals(GamePhase.PAUSED, app.snapshot().game().phase());
            app.closeSettings(); app.resumeGame(); app.exit();
        });
    }

    private static void assertUnchanged(GameState before, GameState after) {
        assertArrayEquals(before.board().snapshot(), after.board().snapshot());
        assertEquals(before.currentPiece().type(), after.currentPiece().type());
        assertArrayEquals(before.currentPiece().cells(), after.currentPiece().cells());
        assertEquals(before.currentRow(), after.currentRow());
        assertEquals(before.currentColumn(), after.currentColumn());
        assertEquals(before.nextPieces(), after.nextPieces());
        assertEquals(before.score(), after.score());
        assertEquals(before.level(), after.level());
        assertEquals(before.spawnedPieces(), after.spawnedPieces());
        assertEquals(before.totalClearedRows(), after.totalClearedRows());
    }
}
