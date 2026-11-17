package tetris.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tetris.piece.ItemType;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;

@DisplayName("보드 아래 공격 줄 추가")
class BoardGarbageTest {
    private static boolean[] row(String pattern) {
        boolean[] filled = new boolean[Board.COLUMNS];
        for (int column = 0; column < Board.COLUMNS; column++) filled[column] = pattern.charAt(column) == '#';
        return filled;
    }

    @Test
    @DisplayName("기존 칸과 아이템을 위로 올리고 아래에 회색 칸 줄을 추가한다")
    void shiftsExistingCellsUpAndAddsGarbageRows() {
        Board board = new Board();
        board.lock(new Tetromino(PieceType.O).withItem(ItemType.LINE_CLEAR, 0), 18, 0);
        assertFalse(board.addRowsAtBottom(new boolean[][] {row("#########."), row(".#########")}));
        int[][] cells = board.snapshot();
        assertEquals(PieceType.O.cellValue(), cells[16][0]);
        assertEquals(ItemType.LINE_CLEAR, board.itemAt(16, 0));
        assertEquals(Board.GARBAGE_CELL, cells[18][0]);
        assertEquals(Board.EMPTY_CELL, cells[18][9]);
        assertEquals(Board.EMPTY_CELL, cells[19][0]);
        assertEquals(ItemType.NONE, board.itemAt(19, 1));
    }

    @Test
    @DisplayName("위로 밀려 나가는 칸이 있으면 true")
    void reportsOverflow() {
        Board board = new Board();
        board.lock(new Tetromino(PieceType.O), 0, 0);
        assertTrue(board.addRowsAtBottom(new boolean[][] {row("#.........")}));
        assertFalse(new Board().addRowsAtBottom(new boolean[0][]));
    }

    @Test
    @DisplayName("공격 줄로 꽉 찬 줄은 삭제 대상이다")
    void fullGarbageRowIsCleared() {
        Board board = new Board();
        board.addRowsAtBottom(new boolean[][] {row("##########")});
        assertEquals(1, board.clearFullRows());
    }

    @Test
    @DisplayName("잘못된 줄은 거부한다")
    void rejectsInvalidRows() {
        Board board = new Board();
        assertThrows(NullPointerException.class, () -> board.addRowsAtBottom(null));
        assertThrows(IllegalArgumentException.class, () -> board.addRowsAtBottom(new boolean[][] {null}));
        assertThrows(IllegalArgumentException.class, () -> board.addRowsAtBottom(new boolean[][] {new boolean[3]}));
        assertThrows(IllegalArgumentException.class,
                () -> board.addRowsAtBottom(new boolean[Board.ROWS + 1][Board.COLUMNS]));
    }
}
