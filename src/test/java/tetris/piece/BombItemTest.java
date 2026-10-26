package tetris.piece;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import tetris.game.CenteredPieceSpawnPolicy;

class BombItemTest {
    @Test void dynamiteHasThreeCellsAndOneCenterLetter() {
        Tetromino bomb = Tetromino.bomb();
        int[][] expected = {{0, 0}, {0, 1}, {0, 2}};
        assertArrayEquals(expected, bomb.cells());
        assertEquals(ItemType.BOMB, bomb.itemType());
        assertEquals("B", bomb.itemType().symbol());
        assertTrue(bomb.itemType().isStandalone());
        assertTrue(bomb.hasItemAt(0, 1));
        assertFalse(bomb.hasItemAt(0, 0));
        assertFalse(bomb.hasItemAt(0, 2));
        assertFalse(bomb.hasItemAt(1, 1));
        assertTrue(bomb.rotateClockwise().hasItemAt(1, 2), "모델 회전에서도 표시 위치를 보존한다.");
        assertFalse(bomb.hasSameLayout(new Tetromino(bomb.type())));
        bomb.cells()[0][0] = 99;
        assertArrayEquals(expected, bomb.cells());
        CenteredPieceSpawnPolicy spawn = new CenteredPieceSpawnPolicy();
        assertEquals(0, spawn.startingRow(bomb));
        assertEquals(3, spawn.startingColumn(bomb));
        assertThrows(IllegalArgumentException.class, () -> bomb.withItem(ItemType.LINE_CLEAR, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Tetromino(PieceType.O).withItem(ItemType.BOMB, 0));
    }
}
