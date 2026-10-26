package tetris.piece;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import tetris.game.CenteredPieceSpawnPolicy;

class BombItemTest {
    @Test void bombIsOneMarkedCellAndRotationKeepsItsShape() {
        Tetromino bomb = Tetromino.bomb();
        assertArrayEquals(new int[][] {{0, 0}}, bomb.cells());
        assertEquals(ItemType.BOMB, bomb.itemType());
        assertEquals("B", bomb.itemType().symbol());
        assertTrue(bomb.itemType().isStandalone());
        assertTrue(bomb.hasItemAt(0, 0));
        assertFalse(bomb.hasItemAt(0, 1));
        assertTrue(bomb.hasSameLayout(bomb.rotateClockwise()));
        assertFalse(bomb.hasSameLayout(new Tetromino(bomb.type())));
        bomb.cells()[0][0] = 99;
        assertArrayEquals(new int[][] {{0, 0}}, bomb.cells());
        CenteredPieceSpawnPolicy spawn = new CenteredPieceSpawnPolicy();
        assertEquals(0, spawn.startingRow(bomb));
        assertEquals(4, spawn.startingColumn(bomb));
        assertThrows(IllegalArgumentException.class, () -> bomb.withItem(ItemType.LINE_CLEAR, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Tetromino(PieceType.O).withItem(ItemType.BOMB, 0));
    }
}
