package tetris.board;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.piece.ItemType;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;
import tetris.rule.LineClearBonus;

class GoldenBoardTest {
    @ParameterizedTest @EnumSource(PieceType.class)
    void lockingAndCopyingKeepOriginalColorsAndGoldenMetadataForEveryShape(PieceType type) {
        Tetromino piece = new Tetromino(type).withItem(ItemType.GOLDEN);
        for (int turn = 0; turn < 4; turn++) {
            Board board = new Board();
            board.lock(piece, 5, 3);
            Board copy = board.copy();
            for (int[] cell : piece.cells()) {
                assertEquals(type.cellValue(), board.snapshot()[5 + cell[0]][3 + cell[1]]);
                assertEquals(ItemType.GOLDEN, copy.itemAt(5 + cell[0], 3 + cell[1]));
            }
            copy.reset();
            for (int[] cell : piece.cells())
                assertEquals(ItemType.GOLDEN, board.itemAt(5 + cell[0], 3 + cell[1]));
            assertAllOrdinary(copy);
            piece = piece.rotateClockwise();
        }
    }

    @Test void rowCompactionMovesGoldenMetadataWithCellsAndClearsVacatedRows() {
        Board board = new Board();
        board.lock(new Tetromino(PieceType.O).withItem(ItemType.GOLDEN), 16, 0);
        board.lock(new Tetromino(PieceType.I), 18, 4);
        assertEquals(2, board.clearRows(Set.of(19, 17)));
        for (int row = 0; row < 20; row++)
            for (int col = 0; col < 10; col++)
                assertEquals(row == 18 && col < 2 ? ItemType.GOLDEN : ItemType.NONE, board.itemAt(row, col));
        assertEquals(PieceType.O.cellValue(), board.snapshot()[18][0]);
        board.clearRows(Set.of(0));
        assertEquals(ItemType.GOLDEN, board.itemAt(18, 0));
        board.clearRows(Set.of(18));
        assertArrayEquals(new int[20][10], board.snapshot());
        assertAllOrdinary(board);
    }

    @Test void footprintAndAreaRemovalEraseMetadataAndReusedCellsHaveNoStaleBonus() {
        Board board = new Board();
        board.lock(new Tetromino(PieceType.I).withItem(ItemType.GOLDEN), 18, 0);
        board.lock(new Tetromino(PieceType.O).withItem(ItemType.GOLDEN), 16, 6);
        assertEquals(1, board.removeArea(19, 0, 0));
        assertEquals(ItemType.NONE, board.itemAt(19, 0));
        assertEquals(ItemType.GOLDEN, board.itemAt(19, 1));
        assertEquals(3, board.removeCells(new Tetromino(PieceType.I), 18, 0));
        assertEquals(4, board.removeArea(17, 7, 2));
        assertAllOrdinary(board);
        board.lock(new Tetromino(PieceType.O), 16, 6);
        assertEquals(1, LineClearBonus.multiplier(board, Set.of(16, 17, 19)));
        assertAllOrdinary(board);
    }

    @Test void invalidMutationsKeepMetadataAndBoundsQueriesRejectInvalidCoordinates() {
        Board board = new Board();
        Tetromino golden = new Tetromino(PieceType.O).withItem(ItemType.GOLDEN);
        board.lock(golden, 18, 8);
        int[][] before = board.snapshot();
        ItemType[][] items = items(board);
        assertThrows(IllegalArgumentException.class, () -> board.lock(golden, 19, 0));
        assertThrows(IllegalArgumentException.class, () -> board.lock(golden, 18, 8));
        assertThrows(IllegalArgumentException.class, () -> board.clearRows(Set.of(19, 20)));
        assertThrows(IllegalArgumentException.class, () -> board.removeArea(20, 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> board.itemAt(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.itemAt(0, 10));
        assertArrayEquals(before, board.snapshot());
        assertArrayEquals(items, items(board));
        board.reset();
        assertAllOrdinary(board);
    }

    private static ItemType[][] items(Board board) {
        ItemType[][] items = new ItemType[20][10];
        for (int row = 0; row < 20; row++)
            for (int col = 0; col < 10; col++) items[row][col] = board.itemAt(row, col);
        return items;
    }

    private static void assertAllOrdinary(Board board) {
        for (ItemType[] row : items(board))
            for (ItemType item : row) assertEquals(ItemType.NONE, item);
    }
}
