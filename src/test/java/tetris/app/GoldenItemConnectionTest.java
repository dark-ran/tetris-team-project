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
import tetris.board.Board;
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

class GoldenItemConnectionTest {
    @TempDir Path directory;

    @ParameterizedTest @EnumSource(value = GameAction.class, names = {"TICK", "DOWN", "HARD_DROP"})
    void normalPieceClearingEarlierGoldenCellsGetsFiveTimesLineScoreOnly(GameAction action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = sequence(golden(PieceType.I), plain(PieceType.I), plain(PieceType.O));
            AppController app = app(engine);
            app.startGame(GameMode.ITEM);
            dropAt(app, engine, 0);
            assertEquals(19, engine.state().score(), "황금 블록의 낙하 점수는 기본 배율이다.");
            Board earlier = engine.state().board();
            dropAt(app, engine, 4);
            moveToColumn(app, engine, 8);
            Tetromino completingPiece = engine.state().currentPiece();
            assertEquals(ItemType.NONE, completingPiece.itemType());
            assertEquals(38, engine.state().score());
            int steps = 0;
            while (engine.state().currentPiece() == completingPiece) {
                app.handleAction(action);
                assertTrue(++steps <= 20);
            }
            assertEquals(556, engine.state().score(), "낙하 19+19+18점과 줄 삭제 500점.");
            assertEquals(1, engine.state().totalClearedRows());
            assertNoGoldenCells(engine.state().board());
            assertEquals(ItemType.GOLDEN, earlier.itemAt(19, 0), "이전 상태 사본은 이후 삭제에 영향받지 않는다.");
            app.exit();
        });
    }

    @Test void multipleGoldenPiecesAndTwoRowsApplyFiveOnce() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = sequence(golden(PieceType.O), golden(PieceType.O),
                    plain(PieceType.O), plain(PieceType.O), plain(PieceType.O));
            AppController app = app(engine);
            app.startGame(GameMode.ITEM);
            for (int column : new int[] {0, 2, 4, 6, 8}) dropAt(app, engine, column);
            assertEquals(1590, engine.state().score(), "낙하 90점 + 두 줄 300×5점. 배율은 중첩하지 않는다.");
            assertEquals(2, engine.state().totalClearedRows());
            assertArrayEquals(new int[20][10], engine.state().board().snapshot());
            assertNoGoldenCells(engine.state().board());
            app.exit();
        });
    }

    @Test void fourRowClearUsesFiveTimesBonusAndAcceleratedDropKeepsItsOwnRate() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Tetromino[] pieces = new Tetromino[10];
            for (int index = 0; index < pieces.length; index++)
                pieces[index] = (index == 0 ? golden(PieceType.I) : plain(PieceType.I)).rotateClockwise();
            GameEngine engine = sequence(pieces);
            AppController app = app(engine);
            app.startGame(GameMode.ITEM);
            for (int column = 0; column < 10; column++) dropAt(app, engine, column - 2);
            assertEquals(4176, engine.state().score(), "낙하 9×16+32점 + 네 줄 800×5점.");
            assertEquals(4, engine.state().totalClearedRows());
            assertNoGoldenCells(engine.state().board());
            app.exit();
        });
    }

    @Test void goldenPieceCompletingARowKeepsSurvivingCellsForAnotherBonus() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = sequence(plain(PieceType.I), plain(PieceType.I), golden(PieceType.O),
                    plain(PieceType.I), plain(PieceType.I));
            AppController app = app(engine);
            app.startGame(GameMode.ITEM);
            for (int column : new int[] {0, 4, 8}) dropAt(app, engine, column);
            assertEquals(556, engine.state().score());
            assertEquals(1, engine.state().totalClearedRows());
            assertEquals(ItemType.GOLDEN, engine.state().board().itemAt(19, 8));
            assertEquals(ItemType.GOLDEN, engine.state().board().itemAt(19, 9));
            dropAt(app, engine, 0);
            dropAt(app, engine, 4);
            assertEquals(1094, engine.state().score());
            assertEquals(2, engine.state().totalClearedRows());
            assertNoGoldenCells(engine.state().board());
            app.exit();
        });
    }

    @Test void goldenCellsAboveTheDeletedRowDoNotBoostItAndMoveDownWithTheirRow() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = sequence(plain(PieceType.O), plain(PieceType.O), golden(PieceType.O),
                    plain(PieceType.I), plain(PieceType.I));
            AppController app = app(engine);
            app.startGame(GameMode.ITEM);
            for (int column : new int[] {0, 0, 0, 2, 6}) dropAt(app, engine, column);
            assertEquals(186, engine.state().score(), "다른 줄에 있는 황금 칸은 배율에 포함하지 않는다.");
            assertEquals(1, engine.state().totalClearedRows());
            for (int row : new int[] {15, 16})
                for (int column : new int[] {0, 1})
                    assertEquals(ItemType.GOLDEN, engine.state().board().itemAt(row, column));
            app.exit();
        });
    }

    @Test void lineItemDeletingAnIncompleteGoldenRowAlsoGetsTheBonus() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = sequence(golden(PieceType.O),
                    plain(PieceType.I).withItem(ItemType.LINE_CLEAR, 0));
            AppController app = app(engine);
            app.startGame(GameMode.ITEM);
            dropAt(app, engine, 0);
            dropAt(app, engine, 2);
            assertEquals(537, engine.state().score(), "낙하 18+19점 + L이 삭제한 황금 줄 500점.");
            assertEquals(1, engine.state().totalClearedRows());
            assertEquals(ItemType.GOLDEN, engine.state().board().itemAt(19, 0));
            assertEquals(ItemType.GOLDEN, engine.state().board().itemAt(19, 1));
            app.exit();
        });
    }

    @ParameterizedTest @EnumSource(value = ItemType.class, names = {"WEIGHT", "BOMB"})
    void destructionItemsEraseGoldenMetadataWithoutBonusOrLaterGhostBonus(ItemType type) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = sequence(golden(PieceType.O),
                    type == ItemType.WEIGHT ? Tetromino.weight() : Tetromino.bomb(),
                    plain(PieceType.I), plain(PieceType.I), plain(PieceType.O));
            AppController app = app(engine);
            app.startGame(GameMode.ITEM);
            app.handleAction(GameAction.HARD_DROP);
            assertEquals(18, engine.state().score());
            app.handleAction(GameAction.HARD_DROP);
            assertEquals(18, engine.state().score());
            assertEquals(0, engine.state().totalClearedRows());
            assertArrayEquals(new int[20][10], engine.state().board().snapshot());
            assertNoGoldenCells(engine.state().board());
            for (int column : new int[] {0, 4, 8}) dropAt(app, engine, column);
            assertEquals(174, engine.state().score(), "황금 정보가 지워진 자리의 일반 줄은 100점이다.");
            assertEquals(1, engine.state().totalClearedRows());
            app.exit();
        });
    }

    @ParameterizedTest @EnumSource(GameMode.class)
    void productionRewardPreviewSpawnAndDelayedBonusAreConnectedOnlyInItemMode(GameMode mode) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = new GameEngine(new Random(42) {
                private int pieces;
                @Override public int nextInt(int bound) {
                    if (bound == 7) return pieces++ % 3 == 2 ? PieceType.O.ordinal() : PieceType.I.ordinal();
                    if (bound == 4) return 3; // 실제 생성기에서 황금 아이템을 선택한다.
                    return super.nextInt(bound);
                }
            });
            AppController app = app(engine);
            app.startGame(mode);
            while (engine.state().totalClearedRows() < 10) {
                assertEquals(ItemType.NONE, engine.state().currentPiece().itemType());
                assertEquals(ItemType.NONE, engine.state().upcomingPieces().getFirst().itemType());
                dropAt(app, engine, engine.state().currentPiece().type() == PieceType.O ? 8
                        : engine.state().board().snapshot()[19][0] == 0 ? 0 : 4);
                assertFalse(engine.isGameOver());
                assertTrue(engine.state().spawnedPieces() < 40);
            }
            Tetromino preview = engine.state().upcomingPieces().getFirst();
            assertEquals(mode == GameMode.ITEM ? ItemType.GOLDEN : ItemType.NONE, preview.itemType());
            assertEquals(ItemType.NONE, engine.state().currentPiece().itemType());
            dropAt(app, engine, 8);
            assertSame(preview, engine.state().currentPiece());
            if (mode == GameMode.ITEM) {
                long score = engine.state().score();
                dropAt(app, engine, 0);
                assertEquals(score + 38, engine.state().score(), "배율은 황금 블록 낙하에 적용하지 않는다.");
                assertEquals(10, engine.state().totalClearedRows());
                for (int column = 0; column < 4; column++)
                    assertEquals(ItemType.GOLDEN, engine.state().board().itemAt(19, column));
                score = engine.state().score();
                dropAt(app, engine, 4);
                assertEquals(score + 38 + 500, engine.state().score());
                assertEquals(11, engine.state().totalClearedRows(), "보상 기준의 줄 수는 5배로 늘어나지 않는다.");
                assertNoGoldenCells(engine.state().board());
            }
            app.restartGame();
            assertEquals(mode, engine.state().mode());
            assertEquals(0, engine.state().totalClearedRows());
            assertNoGoldenCells(engine.state().board());
            assertEquals(ItemType.NONE, engine.state().currentPiece().itemType());
            app.exit();
        });
    }

    private static Tetromino plain(PieceType type) { return new Tetromino(type); }
    private static Tetromino golden(PieceType type) { return plain(type).withItem(ItemType.GOLDEN); }

    private static GameEngine sequence(Tetromino... pieces) {
        return new GameEngine(() -> {
            AtomicInteger index = new AtomicInteger();
            return PieceQueue.generatedBy(() -> index.get() < pieces.length
                    ? pieces[index.getAndIncrement()] : plain(PieceType.I), 1);
        }, new CenteredPieceSpawnPolicy());
    }

    private AppController app(GameEngine engine) {
        GameLoop loop = new GameLoop() { @Override public void start() { } };
        return new AppController(() -> {}, engine, loop, null, null,
                new ScoreBoardService(new ScoreRepository(directory.resolve("scores"))), null, null);
    }

    private static void dropAt(AppController app, GameEngine engine, int column) {
        moveToColumn(app, engine, column);
        app.handleAction(GameAction.HARD_DROP);
    }

    private static void moveToColumn(AppController app, GameEngine engine, int column) {
        while (engine.state().currentColumn() != column) {
            int before = engine.state().currentColumn();
            app.handleAction(before < column ? GameAction.RIGHT : GameAction.LEFT);
            assertNotEquals(before, engine.state().currentColumn());
        }
    }

    private static void assertNoGoldenCells(Board board) {
        for (int row = 0; row < 20; row++)
            for (int column = 0; column < 10; column++)
                assertNotEquals(ItemType.GOLDEN, board.itemAt(row, column));
    }
}
