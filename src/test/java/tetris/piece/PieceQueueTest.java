package tetris.piece;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("다음 블록 선택과 미리보기")
class PieceQueueTest {
    @Test
    @DisplayName("기본 미리보기는 다음에 생성할 블록 한 개")
    void defaultQueuePreparesOnePieceAndReturnsItNext() {
        PieceQueue queue = new PieceQueue();
        List<PieceType> preview = queue.preview();

        assertEquals(1, preview.size());
        assertEquals(preview.getFirst(), queue.next());
        assertEquals(1, queue.preview().size());
    }

    @ParameterizedTest(name = "선택 값 {0}은 {1} 블록")
    @CsvSource({"0, I", "1, O", "2, T", "3, S", "4, Z", "5, J", "6, L"})
    void everySelectionIndexMapsToExactlyOnePiece(int selectionIndex, PieceType expected) {
        SequenceRandomGenerator randomGenerator = new SequenceRandomGenerator(selectionIndex);
        PieceQueue queue = new PieceQueue(randomGenerator);

        assertEquals(List.of(expected), queue.preview());
        assertEquals(expected, queue.next());
        assertEquals(2, randomGenerator.selectionCount());
    }

    @Test
    @DisplayName("조회는 무작위 선택이나 다음 블록 순서를 변경하지 않음")
    void repeatedPreviewDoesNotConsumePiecesOrRandomSelections() {
        SequenceRandomGenerator randomGenerator = new SequenceRandomGenerator(0, 1, 2, 3);
        PieceQueue queue = new PieceQueue(randomGenerator, 3);

        assertEquals(List.of(PieceType.I, PieceType.O, PieceType.T), queue.preview());
        assertEquals(List.of(PieceType.I, PieceType.O, PieceType.T), queue.preview());
        assertEquals(3, randomGenerator.selectionCount());
        assertEquals(PieceType.I, queue.next());
        assertEquals(List.of(PieceType.O, PieceType.T, PieceType.S), queue.preview());
        assertEquals(4, randomGenerator.selectionCount());
    }

    @Test
    @DisplayName("모든 생성에서 같은 블록이 연속 선택될 수 있음")
    void independentSelectionsAllowTheSamePieceToRepeat() {
        PieceQueue queue = new PieceQueue(new SequenceRandomGenerator(0));
        for (int count = 0; count < 20; count++) {
            assertEquals(PieceType.I, queue.next());
        }
    }

    @Test
    @DisplayName("미리보기는 수정할 수 없고 이후 생성에도 이전 목록을 유지함")
    void previewIsAnUnmodifiableCopyOfTheUpcomingPieces() {
        PieceQueue queue = new PieceQueue(new SequenceRandomGenerator(0, 1, 2, 3), 3);
        List<PieceType> previousPreview = queue.preview();

        assertThrows(UnsupportedOperationException.class, () -> previousPreview.set(0, PieceType.L));
        assertThrows(UnsupportedOperationException.class, () -> previousPreview.add(PieceType.L));
        queue.next();
        assertEquals(List.of(PieceType.I, PieceType.O, PieceType.T), previousPreview);
        assertEquals(List.of(PieceType.O, PieceType.T, PieceType.S), queue.preview());
    }

    @Test
    @DisplayName("같은 초기값의 무작위 선택기로 생성 순서를 재현할 수 있음")
    void equalRandomSeedsReproduceTheSameSequence() {
        PieceQueue firstQueue = new PieceQueue(new Random(20261001L), 3);
        PieceQueue secondQueue = new PieceQueue(new Random(20261001L), 3);
        for (int count = 0; count < 200; count++) {
            assertEquals(firstQueue.preview(), secondQueue.preview());
            assertEquals(firstQueue.next(), secondQueue.next());
        }
    }

    @ParameterizedTest(name = "미리보기 개수 {0}은 허용하지 않음")
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 0})
    void rejectsNonPositivePreviewCounts(int previewCount) {
        assertThrows(IllegalArgumentException.class,
                () -> new PieceQueue(new Random(1L), previewCount));
    }

    @Test
    @DisplayName("무작위 선택기가 없으면 생성하지 않음")
    void rejectsMissingRandomGenerator() {
        assertThrows(NullPointerException.class, () -> new PieceQueue(null));
        assertThrows(NullPointerException.class, () -> new PieceQueue(null, 3));
    }

    /** 선택 결과를 지정해 확률에 의존하지 않고 생성 순서와 선택 범위를 검증한다. */
    private static final class SequenceRandomGenerator implements RandomGenerator {
        private final int[] selections;
        private int selectionCount;

        private SequenceRandomGenerator(int... selections) {
            this.selections = selections.clone();
        }

        @Override
        public int nextInt(int bound) {
            assertEquals(7, bound, "일곱 종류 전체를 같은 선택 범위로 사용해야 한다");
            return selections[selectionCount++ % selections.length];
        }

        @Override
        public long nextLong() {
            throw new AssertionError("블록 선택은 범위를 지정한 정수 선택을 사용해야 한다");
        }

        private int selectionCount() {
            return selectionCount;
        }
    }
}
