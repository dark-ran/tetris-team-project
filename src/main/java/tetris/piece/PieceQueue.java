
package tetris.piece;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.random.RandomGenerator;
import tetris.game.Difficulty;

/**
 * 난이도별 가중치로 일반 블록을 선택한다.
 *
 * 내부에서는 Tetromino 전체 모양을 보관한다.
 * 기존 PieceType 기반 API도 호환성을 위해 유지한다.
 */
public final class PieceQueue {
    public static final int DEFAULT_PREVIEW_COUNT = 1;

    private static final PieceType[] PIECE_TYPES =
        PieceType.standardTypes().toArray(PieceType[]::new);

    private final Supplier<Tetromino> generator;
    private final Deque<Tetromino> upcomingPieces = new ArrayDeque<>();

    public PieceQueue() {
        this(RandomGenerator.getDefault(),
                DEFAULT_PREVIEW_COUNT,
                Difficulty.NORMAL);
    }

    public PieceQueue(RandomGenerator randomGenerator) {
        this(randomGenerator,
                DEFAULT_PREVIEW_COUNT,
                Difficulty.NORMAL);
    }

    public PieceQueue(RandomGenerator randomGenerator, int previewCount) {
        this(randomGenerator, previewCount, Difficulty.NORMAL);
    }

    public PieceQueue(
            RandomGenerator randomGenerator,
            int previewCount,
            Difficulty difficulty) {
        this(randomPieces(randomGenerator, difficulty), previewCount);
    }

    private PieceQueue(Supplier<Tetromino> generator, int previewCount) {
        this.generator = Objects.requireNonNull(
                generator, "블록 생성기가 필요합니다.");

        if (previewCount < 1) {
            throw new IllegalArgumentException(
                    "다음 블록 미리보기 개수는 한 개 이상이어야 합니다.");
        }

        for (int index = 0; index < previewCount; index++) {
            upcomingPieces.addLast(generate());
        }
    }

    public PieceQueue(RandomGenerator randomGenerator, Difficulty difficulty) {
        this(randomGenerator, DEFAULT_PREVIEW_COUNT, difficulty);
    }

    /**
     * 기존 코드 호환용: 블록 종류만 반환한다.
     * 게임 엔진에서는 nextPiece()를 사용해야 특수 모양이 유지된다.
     */
    public PieceType next() {
        return nextPiece().type();
    }

    /**
     * 다음 블록 객체 전체를 반환한다.
     */
    public Tetromino nextPiece() {
        return nextPiece(UnaryOperator.identity());
    }

    /**
     * 기존 미리보기의 첫 블록을 꺼낸 후,
     * 새로 추가되는 블록에만 변환 함수를 적용한다.
     *
     * 나중에 아이템 모드에서 1칸 블록을 보상으로 지급할 때:
     *
     * queue.nextPiece(ignored -> Tetromino.singleCell());
     *
     * 기존 미리보기 블록은 변경되지 않는다.
     */
    public Tetromino nextPiece(
            UnaryOperator<Tetromino> decorateReplacement) {
        Objects.requireNonNull(
                decorateReplacement, "블록 변환기가 필요합니다.");

        Tetromino replacement = Objects.requireNonNull(
                decorateReplacement.apply(generate()),
                "새 블록이 null일 수 없습니다.");

        Tetromino nextPiece = upcomingPieces.removeFirst();
        upcomingPieces.addLast(replacement);

        return nextPiece;
    }

    /**
     * 기존 코드 호환용: 미리보기 블록 종류만 반환한다.
     */
    public List<PieceType> preview() {
        return upcomingPieces.stream()
                .map(Tetromino::type)
                .toList();
    }

    public void replaceLastPreview(PieceType type) {
        Objects.requireNonNull(type, "블록 종류가 필요합니다.");

        upcomingPieces.removeLast();
        upcomingPieces.addLast(new Tetromino(type));
    }

    /**
     * 실제 모양까지 포함한 미리보기 데이터.
     */
    public List<Tetromino> previewPieces() {
        return List.copyOf(upcomingPieces);
    }

    /**
     * 특수 블록이나 고정 순서 블록의 독립 검증을 위한 공급기.
     */
    public static PieceQueue generatedBy(
            Supplier<Tetromino> generator,
            int previewCount) {
        return new PieceQueue(generator, previewCount);
    }

    private Tetromino generate() {
        return Objects.requireNonNull(
                generator.get(), "생성한 블록이 null일 수 없습니다.");
    }

    private static Supplier<Tetromino> randomPieces(
            RandomGenerator random,
            Difficulty difficulty) {
        Objects.requireNonNull(
                random, "무작위 선택기가 필요합니다.");
        Objects.requireNonNull(
                difficulty, "난이도가 필요합니다.");

        return () -> new Tetromino(selectRandomPiece(random, difficulty));
    }

    private static PieceType selectRandomPiece(
            RandomGenerator random,
            Difficulty difficulty) {

        /*
         * NORMAL은 일곱 종류의 가중치가 모두 10이므로
         * 기존 nextInt(7) 균등 추첨과 수학적으로 동일하다.
         *
         * 기존 NORMAL 테스트의 난수 호출 계약도 유지한다.
         */
        if (difficulty == Difficulty.NORMAL) {
            return PIECE_TYPES[random.nextInt(PIECE_TYPES.length)];
        }

        /*
         * EASY/HARD는 I 블록의 가중치만 달라진다.
         * EASY 총가중치 72, HARD 총가중치 68.
         */
        int totalWeight = difficulty.iWeight()
                + 10 * (PIECE_TYPES.length - 1);

        int selected = random.nextInt(totalWeight);

        for (PieceType type : PIECE_TYPES) {
            int weight = type == PieceType.I
                    ? difficulty.iWeight()
                    : 10;

            if (selected < weight) {
                return type;
            }

            selected -= weight;
        }

        throw new IllegalStateException("블록 가중치 선택 실패");
    }
}
