package tetris.piece;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("블록 종류와 고정 칸 저장값")
class PieceTypeTest {
    @ParameterizedTest(name = "{0}의 칸 저장값은 {1}")
    @CsvSource({"I, 1", "O, 2", "T, 3", "S, 4", "Z, 5", "J, 6", "L, 7"})
    void eachTypeHasTheDocumentedCellValue(PieceType type, int cellValue) {
        assertEquals(cellValue, type.cellValue());
        assertEquals(type, PieceType.fromCellValue(cellValue));
    }

    @ParameterizedTest(name = "칸 저장값 {0}은 블록 종류로 변환할 수 없음")
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 8, Integer.MAX_VALUE})
    void rejectsEmptyAndUnknownCellValues(int cellValue) {
        assertThrows(IllegalArgumentException.class, () -> PieceType.fromCellValue(cellValue));
    }
}
