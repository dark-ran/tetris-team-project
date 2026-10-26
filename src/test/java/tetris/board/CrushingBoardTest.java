package tetris.board;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;

class CrushingBoardTest {
    @Test void boundsQueryIgnoresObstaclesAndRemovalOnlyErasesOccupiedFootprint() {
        Board board = new Board();
        board.lock(new Tetromino(PieceType.O), 17, 0);
        board.lock(new Tetromino(PieceType.I), 18, 0);
        int[][] before = board.snapshot();
        Tetromino weight = Tetromino.weight();
        assertTrue(board.withinBounds(weight, 18, 0));
        assertFalse(board.canPlace(weight, 18, 0));
        assertArrayEquals(before, board.snapshot());
        assertEquals(5, board.removeCells(weight, 18, 0));
        int[][] expected = board.snapshot();
        assertEquals(PieceType.O.cellValue(), expected[17][0]);
        assertEquals(PieceType.O.cellValue(), expected[17][1]);
        assertEquals(PieceType.O.cellValue(), expected[18][0]);
        assertEquals(0, board.removeCells(weight, 18, 0));
        assertArrayEquals(expected, board.snapshot());
        assertThrows(IllegalArgumentException.class, () -> board.removeCells(weight, 18, 7));
        assertThrows(IllegalArgumentException.class, () -> board.removeCells(weight, 19, 0));
        assertThrows(NullPointerException.class, () -> board.removeCells(null, 0, 0));
        assertThrows(NullPointerException.class, () -> board.withinBounds(null, 0, 0));
        assertArrayEquals(expected, board.snapshot());
    }
}
