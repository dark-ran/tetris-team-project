package tetris.board;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;

@DisplayName("보드 배치 검사, 블록 고정과 줄 삭제")
class BoardTest {
    @Test
    @DisplayName("새 보드는 20행 10열이며 모든 칸이 비어 있음")
    void newBoardHasTwentyEmptyRowsAndTenColumns() {
        Board board = new Board();
        int[][] snapshot = board.snapshot();

        assertEquals(20, snapshot.length);
        for (int[] row : snapshot) {
            assertEquals(10, row.length);
            assertArrayEquals(new int[10], row);
        }
    }

    @ParameterizedTest(name = "{0}의 네 방향을 검사하고 같은 칸 저장값으로 고정")
    @EnumSource(PieceType.class)
    void allPieceOrientationsCanBePlacedAndStored(PieceType type) {
        Tetromino piece = new Tetromino(type);
        for (int rotationCount = 0; rotationCount < 4; rotationCount++) {
            Board board = new Board();
            assertTrue(board.canPlace(piece, 5, 3));
            assertEquals(0, occupiedCellCount(board));
            board.lock(piece, 5, 3);

            assertEquals(4, occupiedCellCount(board));
            for (int[] cell : piece.cells()) {
                assertEquals(type.cellValue(), board.snapshot()[5 + cell[0]][3 + cell[1]]);
            }
            assertFalse(board.canPlace(piece, 5, 3));
            piece = piece.rotateClockwise();
        }
    }

    @ParameterizedTest(name = "{0}, 회전 {1}회, 원점 ({2}, {3})의 배치 결과는 {4}")
    @CsvSource({
            "O, 0, 0, 0, true", "O, 0, 18, 8, true",
            "O, 0, -1, 0, false", "O, 0, 0, -1, false",
            "O, 0, 19, 0, false", "O, 0, 0, 9, false",
            "I, 0, -1, 0, true", "I, 0, 18, 6, true",
            "I, 0, -2, 0, false", "I, 0, 19, 0, false",
            "I, 0, 0, -1, false", "I, 0, 0, 7, false",
            "I, 1, 0, -2, true", "I, 1, 16, 7, true",
            "I, 1, 0, -3, false", "I, 1, 0, 8, false",
            "I, 1, -1, 0, false", "I, 1, 17, 0, false",
            "T, 0, 18, 7, true", "T, 0, 19, 7, false",
            "I, 0, -2147483648, 0, false", "I, 0, 2147483647, 0, false",
            "I, 0, 0, -2147483648, false", "I, 0, 0, 2147483647, false"})
    void placementUsesOccupiedCellsAndChecksAllFourBoundaries(
            PieceType type, int rotationCount, int row, int column, boolean expected) {
        Tetromino piece = new Tetromino(type);
        for (int count = 0; count < rotationCount; count++) {
            piece = piece.rotateClockwise();
        }
        assertEquals(expected, new Board().canPlace(piece, row, column));
    }

    @Test
    @DisplayName("회전 기준 정사각형의 빈 부분은 충돌로 처리하지 않음")
    void emptySpaceInsideTheRotationSquareDoesNotCollide() {
        Board board = new Board();
        board.lock(new Tetromino(PieceType.I).rotateClockwise(), 2, -2);

        assertTrue(board.canPlace(new Tetromino(PieceType.T), 0, 0));
        board.lock(new Tetromino(PieceType.T), 0, 0);
        assertEquals(8, occupiedCellCount(board));
    }

    @Test
    @DisplayName("회전으로 벽을 넘으면 거부하며 원래 블록과 보드를 유지함")
    void boardDoesNotMoveARotatedPieceAwayFromTheWall() {
        Board board = new Board();
        Tetromino vertical = new Tetromino(PieceType.I).rotateClockwise();

        assertTrue(board.canPlace(vertical, 0, 7));
        assertFalse(board.canPlace(vertical.rotateClockwise(), 0, 7));
        assertTrue(board.canPlace(vertical, 0, 7));
        assertEquals(0, occupiedCellCount(board));
    }

    @Test
    @DisplayName("마지막 칸만 보드 밖이어도 일부 칸을 고정하지 않음")
    void outOfBoundsLockDoesNotPartiallyWriteTheBoard() {
        Board board = new Board();
        board.lock(new Tetromino(PieceType.T), 4, 2);
        int[][] before = board.snapshot();

        assertThrows(IllegalArgumentException.class,
                () -> board.lock(new Tetromino(PieceType.I), 18, 7));
        assertArrayEquals(before, board.snapshot());
    }

    @Test
    @DisplayName("일부 칸만 기존 블록과 겹쳐도 기존 값과 빈 칸을 유지함")
    void collidingLockDoesNotOverwriteOrPartiallyWriteTheBoard() {
        Board board = new Board();
        board.lock(new Tetromino(PieceType.O), 15, 8);
        int[][] before = board.snapshot();

        assertThrows(IllegalArgumentException.class,
                () -> board.lock(new Tetromino(PieceType.I), 15, 6));
        assertArrayEquals(before, board.snapshot());
    }

    @Test
    @DisplayName("블록이 없으면 배치와 고정을 거부하고 보드를 유지함")
    void missingPieceCannotBePlacedOrLocked() {
        Board board = new Board();
        int[][] before = board.snapshot();

        assertThrows(NullPointerException.class, () -> board.canPlace(null, 0, 0));
        assertThrows(NullPointerException.class, () -> board.lock(null, 0, 0));
        assertArrayEquals(before, board.snapshot());
    }

    @Test
    @DisplayName("보드 사본의 수정과 이후 고정은 이전 상태에 영향을 주지 않음")
    void snapshotIsAnIndependentCopyOfEveryRow() {
        Board board = new Board();
        board.lock(new Tetromino(PieceType.O), 0, 0);
        int[][] previous = board.snapshot();
        int[][] modified = board.snapshot();
        modified[0][0] = 7;
        modified[1] = new int[Board.COLUMNS];

        assertEquals(PieceType.O.cellValue(), board.snapshot()[0][0]);
        assertEquals(PieceType.O.cellValue(), board.snapshot()[1][0]);
        board.lock(new Tetromino(PieceType.T), 5, 2);
        assertEquals(0, previous[5][3]);
    }

    @Test
    @DisplayName("초기화 후 모든 칸을 다시 사용할 수 있음")
    void resetClearsAllStoredPiecesAndCanBeRepeated() {
        Board board = new Board();
        board.lock(new Tetromino(PieceType.T), 0, 0);
        board.lock(new Tetromino(PieceType.O), 18, 8);

        board.reset();
        board.reset();
        assertArrayEquals(new int[20][10], board.snapshot());
        assertTrue(board.canPlace(new Tetromino(PieceType.T), 0, 0));
    }

    @Test
    @DisplayName("완성된 줄이 없으면 삭제 수는 0이며 보드를 유지함")
    void noFullRowsLeavesTheBoardUnchanged() {
        Board board = new Board();
        board.lock(new Tetromino(PieceType.T), 0, 0);
        board.lock(new Tetromino(PieceType.O), 18, 8);
        int[][] before = board.snapshot();

        assertEquals(0, board.clearFullRows());
        assertArrayEquals(before, board.snapshot());
    }

    @Test
    @DisplayName("맨 아래 완성된 줄 한 개를 삭제하고 위쪽 줄을 내림")
    void clearsOneBottomRowAndPreservesTheColorsAboveIt() {
        Board board = new Board();
        fillSingleRow(board, 19);

        assertEquals(1, board.clearFullRows());
        assertEquals(2, occupiedCellCount(board));
        assertEquals(PieceType.O.cellValue(), board.snapshot()[19][8]);
        assertEquals(PieceType.O.cellValue(), board.snapshot()[19][9]);
        assertArrayEquals(new int[10], board.snapshot()[18]);
        assertEquals(0, board.clearFullRows());
    }

    @Test
    @DisplayName("맨 위 완성된 줄을 삭제해도 아래 줄의 위치는 유지함")
    void clearingTheTopRowDoesNotMoveTheRowsBelowIt() {
        Board board = new Board();
        fillSingleRow(board, 0);
        board.lock(new Tetromino(PieceType.T), 8, 2);
        int[][] expected = board.snapshot();
        expected[0] = new int[10];

        assertEquals(1, board.clearFullRows());
        assertArrayEquals(expected, board.snapshot());
    }

    @Test
    @DisplayName("연속된 두 줄을 빠짐없이 삭제하고 남은 줄의 색과 순서를 유지함")
    void clearsAdjacentRowsAndPreservesTheOrderOfSurvivingRows() {
        Board board = new Board();
        fillTwoRows(board, 18);
        board.lock(new Tetromino(PieceType.T), 6, 3);
        board.lock(new Tetromino(PieceType.I), 2, 0);
        int[][] before = board.snapshot();

        assertEquals(2, board.clearFullRows());
        int[][] after = board.snapshot();
        assertArrayEquals(new int[10], after[0]);
        assertArrayEquals(new int[10], after[1]);
        for (int row = 2; row < 20; row++) {
            assertArrayEquals(before[row - 2], after[row]);
        }
    }

    @Test
    @DisplayName("떨어져 있는 두 줄을 삭제하고 그 사이 줄을 보존함")
    void clearsSeparatedRowsWithoutLosingTheRowBetweenThem() {
        Board board = new Board();
        fillSingleRow(board, 19);
        fillSingleRow(board, 17);
        board.lock(new Tetromino(PieceType.T), 6, 3);
        int[][] before = board.snapshot();

        assertEquals(2, board.clearFullRows());
        int[][] after = board.snapshot();
        assertArrayEquals(before[18], after[19]);
        assertArrayEquals(before[16], after[18]);
        for (int row = 2; row < 18; row++) {
            assertArrayEquals(before[row - 2], after[row]);
        }
        assertArrayEquals(new int[10], after[0]);
        assertArrayEquals(new int[10], after[1]);
    }

    @Test
    @DisplayName("중간 줄을 삭제해도 아래쪽 블록은 이동하지 않음")
    void clearingMiddleRowsOnlyMovesTheRowsAboveThem() {
        Board board = new Board();
        fillTwoRows(board, 10);
        board.lock(new Tetromino(PieceType.T), 4, 3);
        board.lock(new Tetromino(PieceType.I), 18, 2);
        int[][] before = board.snapshot();

        assertEquals(2, board.clearFullRows());
        int[][] after = board.snapshot();
        for (int row = 12; row < 20; row++) {
            assertArrayEquals(before[row], after[row]);
        }
        for (int row = 2; row < 12; row++) {
            assertArrayEquals(before[row - 2], after[row]);
        }
        assertArrayEquals(new int[10], after[0]);
        assertArrayEquals(new int[10], after[1]);
    }

    @Test
    @DisplayName("한 번에 네 줄을 삭제할 수 있음")
    void clearsFourFullRowsAtOnce() {
        Board board = new Board();
        fillTwoRows(board, 16);
        fillTwoRows(board, 18);

        assertEquals(4, board.clearFullRows());
        assertArrayEquals(new int[20][10], board.snapshot());
    }

    @Test
    @DisplayName("모든 줄이 완성된 보드도 전부 삭제할 수 있음")
    void clearsAllTwentyRowsAndCanBeUsedAgain() {
        Board board = new Board();
        for (int row = 0; row < 20; row += 2) {
            fillTwoRows(board, row);
        }

        assertEquals(20, board.clearFullRows());
        assertArrayEquals(new int[20][10], board.snapshot());
        board.lock(new Tetromino(PieceType.O), 18, 8);
        assertEquals(4, occupiedCellCount(board));
    }

    /** 실제 블록 고정 기능으로 검증할 보드를 준비한다. */
    private static void fillSingleRow(Board board, int row) {
        board.lock(new Tetromino(PieceType.I), row - 1, 0);
        board.lock(new Tetromino(PieceType.I), row - 1, 4);
        board.lock(new Tetromino(PieceType.O), Math.max(0, row - 1), 8);
    }

    private static void fillTwoRows(Board board, int row) {
        for (int column = 0; column < 10; column += 2) {
            board.lock(new Tetromino(PieceType.O), row, column);
        }
    }

    private static long occupiedCellCount(Board board) {
        return Arrays.stream(board.snapshot()).flatMapToInt(Arrays::stream)
                .filter(cellValue -> cellValue != Board.EMPTY_CELL).count();
    }
}
