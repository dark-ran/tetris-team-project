package tetris.app;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
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

class BombItemConnectionTest {
    @TempDir Path directory;

    @ParameterizedTest @EnumSource(value = GameAction.class, names = {"TICK", "DOWN", "HARD_DROP"})
    void bombGivesNoDropOrBlastScoreAndNormalScoringResumes(GameAction action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = new GameEngine(() -> {
                AtomicInteger index = new AtomicInteger();
                return PieceQueue.generatedBy(() -> switch (index.getAndIncrement()) {
                    case 0 -> new Tetromino(PieceType.O);
                    case 1 -> Tetromino.bomb();
                    default -> new Tetromino(PieceType.I);
                }, 1);
            }, new CenteredPieceSpawnPolicy());
            AppController app = app(engine);
            app.startGame(GameMode.ITEM);
            app.handleAction(GameAction.HARD_DROP);
            assertEquals(18, engine.state().score());
            Tetromino bomb = engine.state().currentPiece();
            assertEquals(ItemType.BOMB, bomb.itemType());
            int steps = 0;
            while (engine.state().currentPiece() == bomb) {
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

    @ParameterizedTest @EnumSource(GameMode.class)
    void realTenRowRewardPreviewsAndSpawnsBombOnlyInItemMode(GameMode mode) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = new GameEngine(new Random(42) {
                private int pieces;
                @Override public int nextInt(int bound) {
                    if (bound == 7) return pieces++ % 3 == 2 ? PieceType.O.ordinal() : PieceType.I.ordinal();
                    if (bound == 5) return 2; // 실사용 생성기의 폭탄 선택 경로.
                    return super.nextInt(bound);
                }
            });
            AppController app = app(engine);
            app.startGame(mode);
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
            assertEquals(mode == GameMode.ITEM ? ItemType.BOMB : ItemType.NONE, preview.itemType());
            assertEquals(ItemType.NONE, engine.state().currentPiece().itemType());
            moveToColumn(app, engine, 8);
            app.handleAction(GameAction.HARD_DROP);
            assertSame(preview, engine.state().currentPiece());
            assertEquals(10, engine.state().totalClearedRows());
            if (mode == GameMode.ITEM) {
                moveToColumn(app, engine, 7);
                long score = engine.state().score();
                int[][] expected = engine.state().board().snapshot();
                int centerRow = engine.state().landingRow();
                for (int row = Math.max(0, centerRow - 2); row <= Math.min(19, centerRow + 2); row++)
                    for (int col = 6; col <= 9; col++) expected[row][col] = 0;
                app.handleAction(GameAction.HARD_DROP);
                assertEquals(score, engine.state().score(), "속도가 증가해도 폭탄 점수는 0이다.");
                assertArrayEquals(expected, engine.state().board().snapshot());
                assertEquals(10, engine.state().totalClearedRows(), "개별 칸 삭제는 다음 보상에 포함하지 않는다.");
                assertEquals(ItemType.NONE, engine.state().upcomingPieces().getFirst().itemType());
            }
            app.restartGame();
            assertEquals(mode, engine.state().mode());
            assertEquals(0, engine.state().totalClearedRows());
            assertEquals(ItemType.NONE, engine.state().currentPiece().itemType());
            assertEquals(ItemType.NONE, engine.state().upcomingPieces().getFirst().itemType());
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
