package tetris.piece;

import static org.junit.jupiter.api.Assertions.*;
import java.util.EnumMap;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ItemGeneratorTest {
    @Test void productionPoolSelectsAllThreeItemsWithinFivePercentOfExpectedCount() {
        ItemGenerator generator = new ItemGenerator(new Random(624));
        EnumMap<ItemType, Integer> counts = new EnumMap<>(ItemType.class);
        Tetromino plain = new Tetromino(PieceType.T);
        int samples = 100_000;
        for (int i = 0; i < samples; i++) {
            Tetromino item = generator.apply(plain);
            counts.merge(item.itemType(), 1, Integer::sum);
            switch (item.itemType()) {
                case LINE_CLEAR -> assertArrayEquals(plain.cells(), item.cells());
                case WEIGHT -> assertArrayEquals(Tetromino.weight().cells(), item.cells());
                case BOMB -> assertArrayEquals(Tetromino.bomb().cells(), item.cells());
                default -> fail("지급할 아이템을 선택해야 한다.");
            }
        }
        double expected = samples / 3.0;
        assertEquals(3, counts.size());
        for (ItemType type : new ItemType[] {ItemType.LINE_CLEAR, ItemType.WEIGHT, ItemType.BOMB}) {
            assertTrue(Math.abs(counts.get(type) - expected) <= expected * 0.05, type.toString());
        }
        System.out.printf("Item pool / %d: %s%n", samples, counts);
        assertEquals(ItemType.NONE, plain.itemType());
        assertThrows(NullPointerException.class, () -> new ItemGenerator(null));
        assertThrows(NullPointerException.class, () -> generator.apply(null));
    }
}
