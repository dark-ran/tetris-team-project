package tetris.piece;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("블록 모양과 시계방향 회전")
class TetrominoTest {
    @ParameterizedTest(name = "{0}을 {1}회 회전한 네 칸의 위치")
    @MethodSource("expectedShapes")
    void matchesTheExpectedShapeInEveryOrientation(PieceType type, int rotationCount, String[] rows) {
        Tetromino piece = new Tetromino(type);
        for (int count = 0; count < rotationCount; count++) {
            piece = piece.rotateClockwise();
        }

        assertEquals(type, piece.type());
        assertEquals(rows.length, piece.rotationSize());
        assertArrayEquals(cellsFromRows(rows), piece.cells());
        assertEquals(4, piece.cells().length);
    }

    @ParameterizedTest(name = "{0}은 네 번 회전하면 원래 모양")
    @EnumSource(PieceType.class)
    void fourRotationsReturnToTheInitialShapeWithoutChangingTheOriginal(PieceType type) {
        Tetromino original = new Tetromino(type);
        int[][] initialCells = original.cells();
        Tetromino rotated = original;
        for (int count = 0; count < 4; count++) {
            Tetromino previous = rotated;
            rotated = previous.rotateClockwise();
            assertNotSame(previous, rotated);
        }

        assertArrayEquals(initialCells, original.cells());
        assertArrayEquals(initialCells, rotated.cells());
    }

    @ParameterizedTest(name = "{0}의 반환 좌표 수정은 블록에 영향을 주지 않음")
    @EnumSource(PieceType.class)
    void returnedCoordinatesDoNotExposeTheStoredShape(PieceType type) {
        Tetromino piece = new Tetromino(type);
        int[][] expected = piece.cells();
        int[][] modified = piece.cells();
        modified[0][0] = 100;
        modified[1] = new int[] {100, 100};

        assertArrayEquals(expected, piece.cells());
        assertArrayEquals(expected, new Tetromino(type).cells());
    }

    @Test
    @DisplayName("블록 종류가 없으면 생성하지 않음")
    void rejectsMissingPieceType() {
        assertThrows(NullPointerException.class, () -> new Tetromino(null));
    }

    private static Stream<Arguments> expectedShapes() {
        return Stream.of(
                shape(PieceType.I, 0, "....", "IIII", "....", "...."),
                shape(PieceType.I, 1, "..I.", "..I.", "..I.", "..I."),
                shape(PieceType.I, 2, "....", "....", "IIII", "...."),
                shape(PieceType.I, 3, ".I..", ".I..", ".I..", ".I.."),
                shape(PieceType.O, 0, "OO", "OO"),
                shape(PieceType.O, 1, "OO", "OO"),
                shape(PieceType.O, 2, "OO", "OO"),
                shape(PieceType.O, 3, "OO", "OO"),
                shape(PieceType.T, 0, ".T.", "TTT", "..."),
                shape(PieceType.T, 1, ".T.", ".TT", ".T."),
                shape(PieceType.T, 2, "...", "TTT", ".T."),
                shape(PieceType.T, 3, ".T.", "TT.", ".T."),
                shape(PieceType.S, 0, ".SS", "SS.", "..."),
                shape(PieceType.S, 1, ".S.", ".SS", "..S"),
                shape(PieceType.S, 2, "...", ".SS", "SS."),
                shape(PieceType.S, 3, "S..", "SS.", ".S."),
                shape(PieceType.Z, 0, "ZZ.", ".ZZ", "..."),
                shape(PieceType.Z, 1, "..Z", ".ZZ", ".Z."),
                shape(PieceType.Z, 2, "...", "ZZ.", ".ZZ"),
                shape(PieceType.Z, 3, ".Z.", "ZZ.", "Z.."),
                shape(PieceType.J, 0, "J..", "JJJ", "..."),
                shape(PieceType.J, 1, ".JJ", ".J.", ".J."),
                shape(PieceType.J, 2, "...", "JJJ", "..J"),
                shape(PieceType.J, 3, ".J.", ".J.", "JJ."),
                shape(PieceType.L, 0, "..L", "LLL", "..."),
                shape(PieceType.L, 1, ".L.", ".L.", ".LL"),
                shape(PieceType.L, 2, "...", "LLL", "L.."),
                shape(PieceType.L, 3, "LL.", ".L.", ".L."));
    }

    private static Arguments shape(PieceType type, int rotationCount, String... rows) {
        return Arguments.of(type, rotationCount, rows);
    }

    private static int[][] cellsFromRows(String[] rows) {
        List<int[]> cells = new ArrayList<>();
        for (int row = 0; row < rows.length; row++) {
            for (int column = 0; column < rows[row].length(); column++) {
                if (rows[row].charAt(column) != '.') {
                    cells.add(new int[] {row, column});
                }
            }
        }
        return cells.toArray(int[][]::new);
    }
}
