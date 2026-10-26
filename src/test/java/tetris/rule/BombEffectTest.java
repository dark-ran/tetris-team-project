package tetris.rule;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tetris.board.Board;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;

class BombEffectTest {
    @ParameterizedTest
    @CsvSource({"1,3", "2,4", "3,4", "4,5", "5,4", "6,4", "7,4"})
    void contactPatternChoosesCenterAndOnlyItsFiveByFiveAreaIsErased(int mask, int expectedColumn) {
        Board board = new Board();
        board.lock(new Tetromino(PieceType.O), 8, 0);
        board.lock(new Tetromino(PieceType.O), 8, 7);
        for (int index = 0; index < 3; index++) {
            if ((mask & (1 << index)) != 0)
                board.lock(new Tetromino(PieceType.I).rotateClockwise(), 11, 1 + index);
        }
        Tetromino bomb = Tetromino.bomb();
        assertTrue(board.canPlace(bomb, 10, 3));
        assertFalse(board.canPlace(bomb, 11, 3), "한 칸이라도 닿으면 더 내려갈 수 없다.");
        int[][] before = board.snapshot();
        BombEffect effect = new BombEffect();
        assertEquals(new LockEffect.TargetCell(10, expectedColumn),
                effect.targetCell(board, bomb, 10, 3).orElseThrow());
        assertArrayEquals(before, board.snapshot(), "중심 조회는 블록을 삭제하지 않는다.");
        int[][] expected = board.snapshot();
        for (int row = 8; row <= 12; row++)
            for (int col = expectedColumn - 2; col <= expectedColumn + 2; col++) expected[row][col] = 0;
        effect.onLock(board, bomb, 10, 3);
        assertArrayEquals(expected, board.snapshot());
        assertTrue(effect.rowsToClear(board, bomb, 10, 3).isEmpty());
    }

    @Test void floorAndNoContactPreviewUseMiddleCell() {
        Board board = new Board();
        Tetromino bomb = Tetromino.bomb();
        BombEffect effect = new BombEffect();
        for (int column : new int[] {0, 3, 7}) {
            assertEquals(new LockEffect.TargetCell(19, column + 1),
                    effect.targetCell(board, bomb, 19, column).orElseThrow());
            effect.onLock(board, bomb, 19, column);
        }
        assertEquals(new LockEffect.TargetCell(5, 4), effect.targetCell(board, bomb, 5, 3).orElseThrow());
        assertArrayEquals(new int[20][10], board.snapshot());
        assertTrue(LockEffect.FULL_ROWS.targetCell(board, new Tetromino(PieceType.O), 0, 4).isEmpty());
    }
}
