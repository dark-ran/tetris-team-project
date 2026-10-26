package tetris.piece;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import tetris.game.CenteredPieceSpawnPolicy;

class WeightItemTest {
    @Test void weightHasSixMarkedCellsAndSpawnsAtTheTopWithWidthFour() {
        Tetromino weight = Tetromino.weight();
        int[][] expected = {{0, 1}, {0, 2}, {1, 0}, {1, 1}, {1, 2}, {1, 3}};
        assertArrayEquals(expected, weight.cells());
        assertEquals(ItemType.WEIGHT, weight.itemType());
        assertEquals("W", weight.itemType().symbol());
        assertTrue(weight.itemType().isStandalone());
        for (int[] cell : expected) assertTrue(weight.hasItemAt(cell[0], cell[1]));
        assertFalse(weight.hasItemAt(0, 0));
        assertFalse(weight.hasItemAt(1, 4));
        weight.cells()[0][0] = 99;
        assertArrayEquals(expected, weight.cells());
        CenteredPieceSpawnPolicy spawn = new CenteredPieceSpawnPolicy();
        assertEquals(0, spawn.startingRow(weight));
        assertEquals(3, spawn.startingColumn(weight));
        assertThrows(IllegalArgumentException.class, () -> weight.withItem(ItemType.LINE_CLEAR, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Tetromino(PieceType.O).withItem(ItemType.WEIGHT, 0));
    }

}
