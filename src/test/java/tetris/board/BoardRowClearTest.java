package tetris.board;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;

class BoardRowClearTest {
    @Test void selectedIncompleteRowsCollapseTogether() {
        Board board = new Board();
        Tetromino square = new Tetromino(PieceType.O);
        board.lock(square, 16, 0);
        board.lock(square, 18, 6);
        assertEquals(2, board.clearRows(Set.of(17, 19)));
        int[][] cells = board.snapshot();
        assertEquals(PieceType.O.cellValue(), cells[18][0]);
        assertEquals(PieceType.O.cellValue(), cells[19][6]);
        assertEquals(0, cells[17][0]);
        assertEquals(0, cells[19][0]);
    }

    @Test void invalidRowLeavesBoardUnchanged() {
        Board board = new Board();
        board.lock(new Tetromino(PieceType.O), 18, 0);
        int[][] before = board.snapshot();
        assertThrows(IllegalArgumentException.class, () -> board.clearRows(Set.of(19, 20)));
        assertArrayEquals(before, board.snapshot());
    }
}
