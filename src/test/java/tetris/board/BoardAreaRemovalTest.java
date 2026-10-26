package tetris.board;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;

class BoardAreaRemovalTest {
    @ParameterizedTest
    @CsvSource({"10,4,25", "0,0,9", "0,9,9", "19,0,9", "19,9,9", "10,0,15", "19,4,15"})
    void clipsFiveByFiveAreaAndPreservesEveryOtherCell(int centerRow, int centerColumn, int count) {
        Board board = filledBoard();
        int[][] expected = board.snapshot();
        for (int row = Math.max(0, centerRow - 2); row <= Math.min(19, centerRow + 2); row++)
            for (int col = Math.max(0, centerColumn - 2); col <= Math.min(9, centerColumn + 2); col++)
                expected[row][col] = 0;
        assertEquals(count, board.removeArea(centerRow, centerColumn, 2));
        assertArrayEquals(expected, board.snapshot(), "영역 밖의 칸은 내려오거나 이동하지 않는다.");
        assertEquals(0, board.removeArea(centerRow, centerColumn, 2));
        assertArrayEquals(expected, board.snapshot());
    }

    @ParameterizedTest
    @CsvSource({"-1,0,2", "20,0,2", "0,-1,2", "0,10,2", "0,0,-1"})
    void invalidAreaDoesNotChangeBoard(int row, int column, int radius) {
        Board board = filledBoard();
        int[][] before = board.snapshot();
        assertThrows(IllegalArgumentException.class, () -> board.removeArea(row, column, radius));
        assertArrayEquals(before, board.snapshot());
    }

    @Test void zeroRadiusErasesOnlyCenterAndLargeRadiusDoesNotOverflow() {
        Board board = filledBoard();
        assertEquals(1, board.removeArea(10, 4, 0));
        assertEquals(199, board.removeArea(10, 4, Integer.MAX_VALUE));
        assertArrayEquals(new int[20][10], board.snapshot());
    }

    private static Board filledBoard() {
        Board board = new Board();
        for (int row = 0; row < 20; row += 2)
            for (int col = 0; col < 10; col += 2)
                board.lock(new Tetromino(PieceType.O), row, col);
        return board;
    }
}
