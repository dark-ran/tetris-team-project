package tetris.piece;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class GoldenItemTest {
    @ParameterizedTest @EnumSource(PieceType.class)
    void allSevenShapesKeepFourGoldenCellsThroughEveryRotation(PieceType type) {
        Tetromino plain = new Tetromino(type);
        Tetromino golden = plain.withItem(ItemType.GOLDEN);
        Tetromino original = golden;
        for (int turn = 0; turn < 4; turn++) {
            assertArrayEquals(plain.cells(), golden.cells());
            assertEquals(plain.rotationSize(), golden.rotationSize());
            assertEquals(type, golden.type());
            assertEquals(ItemType.GOLDEN, golden.itemType());
            for (int[] cell : golden.cells()) assertTrue(golden.hasItemAt(cell[0], cell[1]));
            assertFalse(golden.hasItemAt(-1, -1));
            golden = golden.rotateClockwise();
            plain = plain.rotateClockwise();
        }
        assertTrue(original.hasSameLayout(golden));
        assertEquals(ItemType.NONE, plain.itemType());
        assertFalse(ItemType.GOLDEN.isStandalone());
        assertEquals("G", ItemType.GOLDEN.symbol());
        assertEquals(5, ItemType.GOLDEN.lineScoreMultiplier());
    }

    @Test void onlyPlainPiecesAcceptWholePieceItemsAndGoldenCannotBeAttachedToOneCell() {
        Tetromino plain = new Tetromino(PieceType.T);
        assertThrows(NullPointerException.class, () -> plain.withItem(null));
        for (ItemType type : new ItemType[] {ItemType.NONE, ItemType.LINE_CLEAR, ItemType.WEIGHT, ItemType.BOMB})
            assertThrows(IllegalArgumentException.class, () -> plain.withItem(type));
        for (Tetromino item : new Tetromino[] {plain.withItem(ItemType.GOLDEN),
                plain.withItem(ItemType.LINE_CLEAR, 0), Tetromino.weight(), Tetromino.bomb()})
            assertThrows(IllegalArgumentException.class, () -> item.withItem(ItemType.GOLDEN));
        assertThrows(IllegalArgumentException.class, () -> plain.withItem(ItemType.GOLDEN, 0));
        assertThrows(IllegalArgumentException.class,
                () -> plain.withItem(ItemType.GOLDEN).withItem(ItemType.LINE_CLEAR, 0));
    }
}
