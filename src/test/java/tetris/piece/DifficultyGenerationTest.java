package tetris.piece;

import static org.junit.jupiter.api.Assertions.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.game.Difficulty;

class DifficultyGenerationTest {
    @ParameterizedTest @EnumSource(Difficulty.class)
    void everyRouletteTicketHasExactlyTheSpecifiedWeight(Difficulty difficulty) {
        int total = difficulty == Difficulty.NORMAL ? 7 : difficulty.iWeight() + 60;
        EnumMap<PieceType, Integer> counts = new EnumMap<>(PieceType.class);
        for (int ticket = 0; ticket < total; ticket++) {
            int selection = ticket;
            PieceQueue queue = new PieceQueue(new Random(0) {
                @Override public int nextInt(int bound) {
                    assertEquals(total, bound);
                    return selection;
                }
            }, difficulty);
            PieceType preview = queue.preview().getFirst();
            assertEquals(preview, queue.next());
            counts.merge(preview, 1, Integer::sum);
        }
        for (PieceType type : PieceType.standardTypes()) {
            int expected = difficulty == Difficulty.NORMAL ? 1 : type == PieceType.I ? difficulty.iWeight() : 10;
            assertEquals(expected, counts.get(type));
        }
        assertFalse(counts.containsKey(PieceType.SINGLE));
    }

    @Test void itemReplacementPreservesEarlierPreviewsAndReturnsToStandardGeneration() {
        PieceQueue queue = new PieceQueue(new Random(42), 3, Difficulty.EASY);
        List<PieceType> old = queue.preview();
        queue.replaceLastPreview(PieceType.SINGLE);
        assertEquals(old.subList(0, 2), queue.preview().subList(0, 2));
        assertEquals(old.get(0), queue.next());
        assertEquals(old.get(1), queue.next());
        assertEquals(PieceType.SINGLE, queue.next());
        for (int i = 0; i < 100; i++) assertNotEquals(PieceType.SINGLE, queue.next());
        assertThrows(NullPointerException.class, () -> queue.replaceLastPreview(null));
        assertThrows(NullPointerException.class, () -> new PieceQueue(new Random(), (Difficulty) null));
    }
}
