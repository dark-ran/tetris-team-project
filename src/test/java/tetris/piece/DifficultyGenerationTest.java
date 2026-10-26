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

    @ParameterizedTest @EnumSource(Difficulty.class)
    void pieceDistributionAtOneThousandExecutions(Difficulty difficulty) {
        int samples = 1_000;
        PieceQueue queue = new PieceQueue(new Random(20261026 + difficulty.ordinal()), difficulty);
        EnumMap<PieceType, Integer> counts = new EnumMap<>(PieceType.class);
        for (int i = 0; i < samples; i++) {
            counts.merge(queue.next(), 1, Integer::sum);
        }

        int totalWeight = difficulty == Difficulty.NORMAL ? 70 : difficulty.iWeight() + 60;
        System.out.printf("=== [1,000 Runs] Difficulty: %s (Total Weight: %d) ===%n", difficulty, totalWeight);
        System.out.printf("%-6s | %-6s | %-8s | %-12s | %-12s | %-14s | %-14s%n",
                "Piece", "Weight", "Count", "Actual Rate", "Expect Rate", "Abs Error (%p)", "Rel Error (%)");
        System.out.println("-----------------------------------------------------------------------------------------");

        for (PieceType type : PieceType.standardTypes()) {
            int weight = (difficulty == Difficulty.NORMAL) ? 10 : (type == PieceType.I ? difficulty.iWeight() : 10);
            double expectedCount = (double) samples * weight / totalWeight;
            int actualCount = counts.getOrDefault(type, 0);

            double actualRate = (double) actualCount / samples;
            double expectedRate = (double) weight / totalWeight;
            double absError = Math.abs(actualRate - expectedRate);
            double relError = Math.abs(actualCount - expectedCount) / expectedCount;

            System.out.printf("%-6s | %-6d | %-8d | %11.4f%% | %11.4f%% | %13.4f%%p | %13.4f%%%n",
                    type, weight, actualCount, actualRate * 100, expectedRate * 100, absError * 100, relError * 100);

            // Absolute error <= 5%p
            assertTrue(absError <= 0.05,
                    () -> String.format("1,000 runs %s %s absolute error %.4f > 0.05", difficulty, type, absError));
        }
        System.out.println();
    }

    @ParameterizedTest @EnumSource(Difficulty.class)
    void pieceDistributionStaysWithinFivePercentOfExpectedWeight(Difficulty difficulty) {
        int samples = 100_000;
        PieceQueue queue = new PieceQueue(new Random(20261026 + difficulty.ordinal()), difficulty);
        EnumMap<PieceType, Integer> counts = new EnumMap<>(PieceType.class);
        for (int i = 0; i < samples; i++) {
            counts.merge(queue.next(), 1, Integer::sum);
        }

        int totalWeight = difficulty == Difficulty.NORMAL ? 70 : difficulty.iWeight() + 60;
        System.out.printf("=== [100,000 Runs] Difficulty: %s (Total Weight: %d) ===%n", difficulty, totalWeight);
        System.out.printf("%-6s | %-6s | %-8s | %-12s | %-12s | %-14s | %-14s%n",
                "Piece", "Weight", "Count", "Actual Rate", "Expect Rate", "Abs Error (%p)", "Rel Error (%)");
        System.out.println("-----------------------------------------------------------------------------------------");

        for (PieceType type : PieceType.standardTypes()) {
            int weight = (difficulty == Difficulty.NORMAL) ? 10 : (type == PieceType.I ? difficulty.iWeight() : 10);
            double expectedCount = (double) samples * weight / totalWeight;
            int actualCount = counts.getOrDefault(type, 0);

            double actualRate = (double) actualCount / samples;
            double expectedRate = (double) weight / totalWeight;
            double absError = Math.abs(actualRate - expectedRate);
            double relError = Math.abs(actualCount - expectedCount) / expectedCount;

            System.out.printf("%-6s | %-6d | %-8d | %11.4f%% | %11.4f%% | %13.4f%%p | %13.4f%%%n",
                    type, weight, actualCount, actualRate * 100, expectedRate * 100, absError * 100, relError * 100);

            // Relative error <= 5%
            assertTrue(relError <= 0.05,
                    () -> String.format("%s %s count: %d, expected: %.2f, relative error: %.4f (limit: 0.05)",
                            difficulty, type, actualCount, expectedCount, relError));
            // Absolute error <= 5%p
            assertTrue(absError <= 0.05,
                    () -> String.format("%s %s rate: %.4f, expected: %.4f, absolute error: %.4f (limit: 0.05)",
                            difficulty, type, actualRate, expectedRate, absError));
        }
        System.out.println();
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
