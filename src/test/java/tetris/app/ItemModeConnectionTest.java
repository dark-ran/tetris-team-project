package tetris.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import java.nio.file.Path;
import java.util.Random;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.game.GameAction;
import tetris.game.GameEngine;
import tetris.game.GameMode;
import tetris.game.CenteredPieceSpawnPolicy;
import tetris.loop.GameLoop;
import tetris.piece.ItemType;
import tetris.piece.PieceQueue;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;
import tetris.scoreboard.ScoreBoardService;
import tetris.scoreboard.ScoreRepository;

class ItemModeConnectionTest {
    @TempDir Path directory;

    @ParameterizedTest
    @EnumSource(value = GameAction.class, names = {"TICK", "DOWN", "HARD_DROP"})
    void productionGeneratorRewardsTenClearsAndItemUsesNormalLockAndScoreFlow(GameAction lockingAction) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = new GameEngine(new Random(42) {
                private int pieces;
                @Override public int nextInt(int bound) {
                    if (bound == 7) return pieces++ % 3 == 2 ? PieceType.O.ordinal() : PieceType.I.ordinal();
                    if (bound == 5) return 0; // 이 검증은 L 아이템의 점수·삭제 흐름을 대상으로 한다.
                    return super.nextInt(bound);
                }
            });
            GameLoop loop = new GameLoop() { @Override public void start() { } };
            AppController app = new AppController(() -> {}, engine, loop, null, null,
                    new ScoreBoardService(new ScoreRepository(directory.resolve("production-scores"))), null, null);
            app.startGame(GameMode.ITEM);
            while (engine.state().totalClearedRows() < 10) {
                assertEquals(ItemType.NONE, engine.state().currentPiece().itemType());
                assertEquals(ItemType.NONE, engine.state().upcomingPieces().getFirst().itemType());
                int column = engine.state().currentPiece().type() == PieceType.O ? 8
                        : engine.state().board().snapshot()[19][0] == 0 ? 0 : 4;
                moveToColumn(app, engine, column);
                app.handleAction(GameAction.HARD_DROP);
                assertFalse(engine.isGameOver());
                assertTrue(engine.state().spawnedPieces() < 40);
            }
            Tetromino preview = engine.state().upcomingPieces().getFirst();
            assertEquals(ItemType.LINE_CLEAR, preview.itemType());
            assertEquals(ItemType.NONE, engine.state().currentPiece().itemType());
            moveToColumn(app, engine, 8);
            app.handleAction(GameAction.HARD_DROP);
            assertSame(preview, engine.state().currentPiece());
            assertEquals(PieceType.I, preview.type());
            moveToColumn(app, engine, 0);
            int[][] before = engine.state().board().snapshot();
            long previousScore = engine.state().score();
            assertEquals(10, engine.state().totalClearedRows());
            assertTrue(app.snapshot().gravityIntervalMillis() < 1000);
            int actions = 0;
            while (engine.state().currentPiece() == preview) {
                app.handleAction(lockingAction);
                assertTrue(++actions <= 20);
            }
            assertEquals(11, engine.state().totalClearedRows());
            assertEquals(previousScore + 19 * 2 + 100, engine.state().score());
            int[][] after = engine.state().board().snapshot();
            assertArrayEquals(new int[10], after[0]);
            for (int row = 1; row < 20; row++) assertArrayEquals(before[row - 1], after[row]);
            app.restartGame();
            assertEquals(GameMode.ITEM, engine.state().mode());
            assertEquals(0, engine.state().totalClearedRows());
            assertEquals(ItemType.NONE, engine.state().currentPiece().itemType());
            assertEquals(ItemType.NONE, engine.state().upcomingPieces().getFirst().itemType());
            app.exit();
        });
    }

    private static void moveToColumn(AppController app, GameEngine engine, int column) {
        while (engine.state().currentColumn() != column) {
            int previous = engine.state().currentColumn();
            app.handleAction(previous < column ? GameAction.RIGHT : GameAction.LEFT);
            assertTrue(engine.state().currentColumn() != previous);
        }
    }

    @Test void lineItemUsesTheExistingLineScoreAndDropScore() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Tetromino item = new Tetromino(PieceType.I).withItem(ItemType.LINE_CLEAR, 2);
            GameEngine engine = new GameEngine(() -> PieceQueue.generatedBy(() -> item, 1), new CenteredPieceSpawnPolicy());
            GameLoop loop = new GameLoop() { @Override public void start() { } };
            AppController app = new AppController(() -> {}, engine, loop, ignored -> {}, ignored -> {},
                    new ScoreBoardService(new ScoreRepository(directory.resolve("line-scores"))), null, ignored -> {});
            app.startGame(GameMode.ITEM);
            app.handleAction(GameAction.HARD_DROP);
            assertEquals(1, engine.state().totalClearedRows());
            assertEquals(119, engine.state().score());
            app.exit();
        });
    }

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
