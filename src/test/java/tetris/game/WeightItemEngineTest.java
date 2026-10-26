package tetris.game;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.piece.ItemType;
import tetris.piece.PieceQueue;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;

class WeightItemEngineTest {
    @ParameterizedTest @EnumSource(value = GameAction.class, names = {"TICK", "DOWN", "HARD_DROP"})
    void crushesThePathWithoutScoringRowsOrLeavingItsBody(GameAction action) {
        GameEngine engine = sequence(new Tetromino(PieceType.O), Tetromino.weight());
        engine.newGame(GameMode.ITEM);
        moveToColumn(engine, 4);
        engine.apply(GameAction.HARD_DROP);
        Tetromino weight = engine.state().currentPiece();
        assertEquals(ItemType.WEIGHT, weight.itemType());
        assertEquals(18, engine.state().landingRow());
        int[][] before = engine.state().board().snapshot();
        for (int i = 0; i < 3; i++) engine.state();
        assertArrayEquals(before, engine.state().board().snapshot(), "착지 위치 조회는 블록을 지우지 않는다.");
        assertTrue(engine.apply(GameAction.LEFT).changed());
        assertTrue(engine.apply(GameAction.RIGHT).changed());
        assertFalse(engine.apply(GameAction.ROTATE_CLOCKWISE).changed());
        int steps = 0;
        while (engine.state().currentPiece() == weight) {
            assertEquals(0, engine.apply(action).clearedRows());
            assertTrue(++steps <= 20);
        }
        assertArrayEquals(new int[20][10], engine.state().board().snapshot());
        assertEquals(0, engine.state().totalClearedRows());
        assertEquals(ItemType.NONE, engine.state().currentPiece().itemType());
        assertTrue(engine.apply(GameAction.LEFT).changed(), "다음 일반 블록은 다시 좌우로 움직인다.");
    }

    @Test void contactLocksHorizontalMovementEvenAfterTheObstacleIsGone() {
        Tetromino square = new Tetromino(PieceType.O);
        GameEngine engine = sequence(square, square, square, square, square,
                new Tetromino(PieceType.I), Tetromino.weight());
        engine.newGame(GameMode.ITEM);
        for (int i = 0; i < 6; i++) {
            moveToColumn(engine, 0);
            engine.apply(GameAction.HARD_DROP);
        }
        moveToColumn(engine, 2);
        for (int i = 0; i < 7; i++) engine.apply(GameAction.TICK);
        assertEquals(7, engine.state().currentRow());
        assertFalse(engine.apply(GameAction.LEFT).changed());
        assertFalse(engine.apply(GameAction.RIGHT).changed());
        engine.apply(GameAction.TICK);
        assertTrue(engine.state().board().canPlace(engine.state().currentPiece(), 9, 2));
        assertFalse(engine.apply(GameAction.LEFT).changed(), "아래가 비어도 최초 접촉 상태를 유지한다.");
        assertFalse(engine.apply(GameAction.RIGHT).changed());
        engine.apply(GameAction.HARD_DROP);
        int[][] remaining = engine.state().board().snapshot();
        assertEquals(22, Arrays.stream(remaining).flatMapToInt(Arrays::stream).filter(v -> v != 0).count());
        for (int row = 10; row < 20; row++) {
            assertEquals(PieceType.O.cellValue(), remaining[row][0]);
            assertEquals(PieceType.O.cellValue(), remaining[row][1]);
            for (int col = 2; col < 10; col++) assertEquals(0, remaining[row][col]);
        }
        assertEquals(PieceType.I.cellValue(), remaining[9][0]);
        assertEquals(PieceType.I.cellValue(), remaining[9][1]);
    }

    private static GameEngine sequence(Tetromino... pieces) {
        return new GameEngine(() -> {
            AtomicInteger index = new AtomicInteger();
            return PieceQueue.generatedBy(() -> index.get() < pieces.length
                    ? pieces[index.getAndIncrement()] : new Tetromino(PieceType.I), 1);
        }, new CenteredPieceSpawnPolicy());
    }

    private static void moveToColumn(GameEngine engine, int column) {
        while (engine.state().currentColumn() != column) {
            assertTrue(engine.apply(engine.state().currentColumn() < column
                    ? GameAction.RIGHT : GameAction.LEFT).changed());
        }
    }
}
