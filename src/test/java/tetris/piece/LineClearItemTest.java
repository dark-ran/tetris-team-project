package tetris.piece;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LineClearItemTest {
    @ParameterizedTest @EnumSource(PieceType.class)
    void markerStaysOnItsCellThroughFourRotationsWithoutChangingOriginal(PieceType type) {
        Tetromino plain = new Tetromino(type);
        for (int index = 0; index < 4; index++) {
            Tetromino original = plain.withItem(ItemType.LINE_CLEAR, index);
            Tetromino rotated = original;
            for (int turn = 0; turn < 4; turn++) {
                int marked = 0;
                for (int[] cell : rotated.cells()) {
                    if (rotated.hasItemAt(cell[0], cell[1])) marked++;
                }
                assertEquals(1, marked);
                int row = rotated.itemRow(), column = rotated.itemColumn();
                rotated = rotated.rotateClockwise();
                assertEquals(column, rotated.itemRow());
                assertEquals(rotated.rotationSize() - 1 - row, rotated.itemColumn());
            }
            assertTrue(original.hasSameLayout(rotated));
            assertEquals(plain.cells()[index][0], original.itemRow());
            assertEquals(plain.cells()[index][1], original.itemColumn());
        }
        assertEquals(ItemType.NONE, plain.itemType());
        assertEquals(-1, plain.itemRow());
        assertEquals(-1, plain.itemColumn());
        assertFalse(plain.hasItemAt(0, 0));
    }

    @Test void randomAttachmentSelectsAllFourCellsAndKeepsTheShape() {
        LineClearItemGenerator generator = new LineClearItemGenerator(new Random(624));
        Tetromino plain = new Tetromino(PieceType.I);
        int[] counts = new int[4];
        for (int i = 0; i < 4000; i++) {
            Tetromino item = generator.apply(plain);
            assertEquals(ItemType.LINE_CLEAR, item.itemType());
            assertArrayEquals(plain.cells(), item.cells());
            counts[item.itemColumn()]++;
        }
        for (int count : counts) assertTrue(count > 800 && count < 1200);
        assertEquals("L", ItemType.LINE_CLEAR.symbol());
        assertEquals("줄 삭제 (L)", ItemType.LINE_CLEAR.displayName());
    }

    @Test void invalidAttachmentsAreRejected() {
        Tetromino piece = new Tetromino(PieceType.T);
        assertThrows(NullPointerException.class, () -> new LineClearItemGenerator(null));
        assertThrows(NullPointerException.class, () -> piece.withItem(null, 0));
        assertThrows(IllegalArgumentException.class, () -> piece.withItem(ItemType.NONE, 0));
        assertThrows(IllegalArgumentException.class, () -> piece.withItem(ItemType.LINE_CLEAR, -1));
        assertThrows(IllegalArgumentException.class, () -> piece.withItem(ItemType.LINE_CLEAR, 4));
        assertFalse(piece.hasSameLayout(new Tetromino(PieceType.O)));
    }
}
