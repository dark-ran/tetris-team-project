package tetris.game;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.board.Board;
import tetris.piece.ItemType;
import tetris.piece.PieceQueue;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;

class LineClearItemEngineTest {
    @Test void incompleteMarkedRowIsClearedWhenPieceLocks() {
        GameEngine engine = fixed(new Tetromino(PieceType.O).withItem(ItemType.LINE_CLEAR, 0));
        engine.newGame(GameMode.ITEM);
        GameActionResult result = engine.apply(GameAction.HARD_DROP);
        assertEquals(1, result.clearedRows());
        assertEquals(1, engine.state().totalClearedRows());
        int[][] cells = engine.state().board().snapshot();
        assertEquals(2, Arrays.stream(cells).flatMapToInt(Arrays::stream).filter(v -> v != 0).count());
        assertEquals(PieceType.O.cellValue(), cells[19][4]);
        assertEquals(PieceType.O.cellValue(), cells[19][5]);
        assertEquals(0, cells[18][4]);
    }

    @Test void fullMarkedRowIsCountedOnceAndOtherFullRowsAreAlsoCleared() {
        for (int markerIndex : new int[] {0, 2}) {
            AtomicInteger count = new AtomicInteger();
            GameEngine engine = new GameEngine(() -> PieceQueue.generatedBy(() -> {
                Tetromino piece = new Tetromino(PieceType.O);
                return count.getAndIncrement() == 4 ? piece.withItem(ItemType.LINE_CLEAR, markerIndex) : piece;
            }, 1), new CenteredPieceSpawnPolicy());
            engine.newGame(GameMode.ITEM);
            GameActionResult result = null;
            for (int column = 0; column < Board.COLUMNS; column += 2) {
                moveToColumn(engine, column);
                result = engine.apply(GameAction.HARD_DROP);
            }
            assertNotNull(result);
            assertEquals(2, result.clearedRows());
            assertEquals(2, engine.state().totalClearedRows());
            assertEquals(0, Arrays.stream(engine.state().board().snapshot()).flatMapToInt(Arrays::stream).sum());
        }
    }

    @Test void incompleteMarkedRowAndAnotherFullRowAreRemovedTogether() {
        AtomicInteger count = new AtomicInteger();
        GameEngine engine = new GameEngine(() -> PieceQueue.generatedBy(() -> switch (count.getAndIncrement()) {
            case 0, 1 -> new Tetromino(PieceType.I);
            default -> new Tetromino(PieceType.O).withItem(ItemType.LINE_CLEAR, 0);
        }, 1), new CenteredPieceSpawnPolicy());
        engine.newGame(GameMode.ITEM);
        moveToColumn(engine, 0);
        engine.apply(GameAction.HARD_DROP);
        moveToColumn(engine, 4);
        engine.apply(GameAction.HARD_DROP);
        moveToColumn(engine, 8);
        assertEquals(2, engine.apply(GameAction.HARD_DROP).clearedRows());
        assertEquals(0, Arrays.stream(engine.state().board().snapshot()).flatMapToInt(Arrays::stream).sum());
    }

    @Test void ordinaryLShapeHasNoItemEffectButSquareItemCanRotate() {
        GameEngine ordinary = fixed(new Tetromino(PieceType.L));
        ordinary.newGame(GameMode.ITEM);
        assertEquals(0, ordinary.apply(GameAction.HARD_DROP).clearedRows());
        GameEngine item = fixed(new Tetromino(PieceType.O).withItem(ItemType.LINE_CLEAR, 0));
        item.newGame(GameMode.ITEM);
        Tetromino original = item.state().currentPiece();
        assertTrue(item.apply(GameAction.ROTATE_CLOCKWISE).changed());
        assertArrayEquals(original.cells(), item.state().currentPiece().cells());
        assertEquals(0, item.state().currentPiece().itemRow());
        assertEquals(1, item.state().currentPiece().itemColumn());
        assertEquals(0, original.itemColumn());
    }

    @ParameterizedTest @EnumSource(GameMode.class)
    void tenthClearDecoratesNewPreviewAndThatExactItemSpawnsNext(GameMode mode) {
        AtomicInteger generatedItems = new AtomicInteger();
        GameEngine engine = fillingEngine(1, generatedItems);
        engine.newGame(mode);
        while (engine.state().totalClearedRows() < 9) dropForFilling(engine);
        assertEquals(0, generatedItems.get());
        Tetromino previousPreview = null;
        while (engine.state().totalClearedRows() < 10) {
            previousPreview = engine.state().upcomingPieces().getFirst();
            dropForFilling(engine);
        }
        assertEquals(10, engine.state().totalClearedRows());
        assertSame(previousPreview, engine.state().currentPiece());
        Tetromino rewardedPreview = engine.state().upcomingPieces().getFirst();
        assertEquals(mode == GameMode.ITEM ? ItemType.LINE_CLEAR : ItemType.NONE, rewardedPreview.itemType());
        assertEquals(mode == GameMode.ITEM ? 1 : 0, generatedItems.get());
        dropForFilling(engine);
        assertSame(rewardedPreview, engine.state().currentPiece());
        if (mode == GameMode.ITEM) {
            while (engine.state().totalClearedRows() < 20) dropForFilling(engine);
            assertEquals(2, generatedItems.get());
        }
        engine.newGame(mode);
        assertEquals(0, engine.state().totalClearedRows());
        assertEquals(ItemType.NONE, engine.state().currentPiece().itemType());
        assertEquals(ItemType.NONE, engine.state().upcomingPieces().getFirst().itemType());
    }

    @Test void rewardIsAppendedAfterExistingPreviews() {
        GameEngine engine = fillingEngine(3, new AtomicInteger());
        engine.newGame(GameMode.ITEM);
        while (engine.state().totalClearedRows() < 9) dropForFilling(engine);
        var previous = engine.state().upcomingPieces();
        while (engine.state().totalClearedRows() < 10) {
            previous = engine.state().upcomingPieces();
            dropForFilling(engine);
        }
        assertEquals(10, engine.state().totalClearedRows());
        assertSame(previous.get(0), engine.state().currentPiece());
        assertSame(previous.get(1), engine.state().upcomingPieces().get(0));
        assertSame(previous.get(2), engine.state().upcomingPieces().get(1));
        assertEquals(ItemType.LINE_CLEAR, engine.state().upcomingPieces().get(2).itemType());
    }

    @Test void crossingTenByClearingFourRowsStillGrantsOneItem() {
        AtomicInteger generatedItems = new AtomicInteger();
        GameEngine engine = new GameEngine(() -> PieceQueue.generatedBy(() -> new Tetromino(PieceType.I), 1),
                new CenteredPieceSpawnPolicy(), piece -> {
                    generatedItems.incrementAndGet();
                    return piece.withItem(ItemType.LINE_CLEAR, 0);
                });
        engine.newGame(GameMode.ITEM);
        for (int batch = 0; batch < 3; batch++) {
            for (int column = 0; column < 10; column++) {
                engine.apply(GameAction.DOWN);
                engine.apply(GameAction.ROTATE_CLOCKWISE);
                moveToColumn(engine, column - 2);
                engine.apply(GameAction.HARD_DROP);
            }
        }
        assertEquals(12, engine.state().totalClearedRows());
        assertEquals(1, generatedItems.get());
        assertEquals(ItemType.LINE_CLEAR, engine.state().upcomingPieces().getFirst().itemType());
    }

    private static GameEngine fixed(Tetromino piece) {
        return new GameEngine(() -> PieceQueue.generatedBy(() -> piece, 1), new CenteredPieceSpawnPolicy());
    }

    private static GameEngine fillingEngine(int previews, AtomicInteger generatedItems) {
        return new GameEngine(() -> {
            AtomicInteger count = new AtomicInteger();
            return PieceQueue.generatedBy(() -> new Tetromino(count.getAndIncrement() % 3 == 2
                    ? PieceType.O : PieceType.I), previews);
        }, new CenteredPieceSpawnPolicy(), piece -> {
            generatedItems.incrementAndGet();
            return piece.withItem(ItemType.LINE_CLEAR, 0);
        });
    }

    private static void dropForFilling(GameEngine engine) {
        assertFalse(engine.isGameOver());
        int column = engine.state().currentPiece().type() == PieceType.O ? 8
                : engine.state().board().snapshot()[19][0] == 0 ? 0 : 4;
        moveToColumn(engine, column);
        engine.apply(GameAction.HARD_DROP);
        assertTrue(engine.state().spawnedPieces() < 120, "줄 삭제 준비가 끝나야 한다.");
    }

    private static void moveToColumn(GameEngine engine, int column) {
        while (engine.state().currentColumn() != column) {
            assertTrue(engine.apply(engine.state().currentColumn() < column
                    ? GameAction.RIGHT : GameAction.LEFT).changed());
        }
    }
}
