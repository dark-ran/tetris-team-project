package tetris.app;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.game.CenteredPieceSpawnPolicy;
import tetris.game.GameAction;
import tetris.game.GameEngine;
import tetris.game.GameMode;
import tetris.loop.GameLoop;
import tetris.piece.ItemType;
import tetris.piece.PieceQueue;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;
import tetris.scoreboard.ScoreBoardService;
import tetris.scoreboard.ScoreRepository;

class WeightItemConnectionTest {
    @TempDir Path directory;

    @ParameterizedTest @EnumSource(value = GameAction.class, names = {"TICK", "DOWN", "HARD_DROP"})
    void weightGivesNoDropOrDestructionScoreAndNormalScoringResumes(GameAction action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = new GameEngine(() -> {
                AtomicInteger index = new AtomicInteger();
                return PieceQueue.generatedBy(() -> switch (index.getAndIncrement()) {
                    case 0 -> new Tetromino(PieceType.O);
                    case 1 -> Tetromino.weight();
                    default -> new Tetromino(PieceType.I);
                }, 1);
            }, new CenteredPieceSpawnPolicy());
            AppController app = app(engine);
            app.startGame(GameMode.ITEM);
            app.handleAction(GameAction.RIGHT);
            app.handleAction(GameAction.HARD_DROP);
            assertEquals(18, engine.state().score());
            Tetromino weight = engine.state().currentPiece();
            assertEquals(ItemType.WEIGHT, weight.itemType());
            int steps = 0;
            while (engine.state().currentPiece() == weight) {
                app.handleAction(action);
                assertEquals(18, engine.state().score());
                assertTrue(++steps <= 20);
            }
            assertArrayEquals(new int[20][10], engine.state().board().snapshot());
            assertEquals(0, engine.state().totalClearedRows());
            app.handleAction(GameAction.HARD_DROP);
            assertEquals(37, engine.state().score());
            app.exit();
        });
    }

    @Test void productionWeightRewardAppearsAfterTenRowsAndPreviewSpawnsUnchanged() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = new GameEngine(new Random(42) {
                private int pieces;
                @Override public int nextInt(int bound) {
                    if (bound == 7) return pieces++ % 3 == 2 ? PieceType.O.ordinal() : PieceType.I.ordinal();
                    if (bound == 3) return 1; // 실사용 생성기의 W 선택 경로를 검증한다.
                    return super.nextInt(bound);
                }
            });
            AppController app = app(engine);
            app.startGame(GameMode.ITEM);
            while (engine.state().totalClearedRows() < 10) {
                assertEquals(ItemType.NONE, engine.state().currentPiece().itemType());
                assertEquals(ItemType.NONE, engine.state().upcomingPieces().getFirst().itemType());
                moveToColumn(app, engine, engine.state().currentPiece().type() == PieceType.O ? 8
                        : engine.state().board().snapshot()[19][0] == 0 ? 0 : 4);
                app.handleAction(GameAction.HARD_DROP);
                assertFalse(engine.isGameOver());
                assertTrue(engine.state().spawnedPieces() < 40);
            }
            Tetromino preview = engine.state().upcomingPieces().getFirst();
            assertEquals(ItemType.WEIGHT, preview.itemType());
            moveToColumn(app, engine, 8);
            app.handleAction(GameAction.HARD_DROP);
            assertSame(preview, engine.state().currentPiece());
            assertEquals(10, engine.state().totalClearedRows());
            assertTrue(app.snapshot().gravityIntervalMillis() < 1000);
            long score = engine.state().score();
            app.handleAction(GameAction.HARD_DROP);
            assertEquals(score, engine.state().score(), "속도가 증가해도 무게추 점수는 0이다.");
            assertEquals(10, engine.state().totalClearedRows());
            app.restartGame();
            assertEquals(0, engine.state().totalClearedRows());
            assertEquals(ItemType.NONE, engine.state().currentPiece().itemType());
            app.exit();
        });
    }

    private AppController app(GameEngine engine) {
        GameLoop loop = new GameLoop() { @Override public void start() { } };
        return new AppController(() -> {}, engine, loop, null, null,
                new ScoreBoardService(new ScoreRepository(directory.resolve("scores"))), null, null);
    }

    private static void moveToColumn(AppController app, GameEngine engine, int column) {
        while (engine.state().currentColumn() != column) {
            int before = engine.state().currentColumn();
            app.handleAction(before < column ? GameAction.RIGHT : GameAction.LEFT);
            assertNotEquals(before, engine.state().currentColumn());
        }
    }
}
