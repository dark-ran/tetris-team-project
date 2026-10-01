package tetris.piece;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** 일곱 종류를 매번 독립적으로 균등 선택하고, 생성 순서대로 다음 블록을 보관한다. */
public final class PieceQueue {
    public static final int DEFAULT_PREVIEW_COUNT = 1;

    private static final PieceType[] PIECE_TYPES = PieceType.values();

    private final RandomGenerator randomGenerator;
    private final Deque<PieceType> upcomingPieces = new ArrayDeque<>();

    /** 기본 무작위 선택기를 사용하며 다음 블록 한 개를 미리 준비한다. */
    public PieceQueue() {
        this(RandomGenerator.getDefault(), DEFAULT_PREVIEW_COUNT);
    }

    /** 재현 가능한 테스트를 위해 무작위 선택기를 지정할 수 있다. */
    public PieceQueue(RandomGenerator randomGenerator) {
        this(randomGenerator, DEFAULT_PREVIEW_COUNT);
    }

    /** 무작위 선택기와 한 개 이상의 미리보기 개수를 지정한다. */
    public PieceQueue(RandomGenerator randomGenerator, int previewCount) {
        this.randomGenerator = Objects.requireNonNull(randomGenerator, "무작위 선택기가 필요합니다.");
        if (previewCount < 1) {
            throw new IllegalArgumentException("다음 블록 미리보기 개수는 한 개 이상이어야 합니다.");
        }
        for (int index = 0; index < previewCount; index++) {
            upcomingPieces.addLast(selectRandomPiece());
        }
    }

    /** 미리보기의 첫 블록을 꺼내고 마지막에 새 블록 한 개를 준비한다. */
    public PieceType next() {
        PieceType replacement = selectRandomPiece();
        PieceType nextPiece = upcomingPieces.removeFirst();
        upcomingPieces.addLast(replacement);
        return nextPiece;
    }

    /** 목록의 첫 블록이 다음 생성 대상이다. 조회로 순서가 바뀌지 않으며 반환 목록은 수정할 수 없다. */
    public List<PieceType> preview() {
        return List.copyOf(upcomingPieces);
    }

    private PieceType selectRandomPiece() {
        return PIECE_TYPES[randomGenerator.nextInt(PIECE_TYPES.length)];
    }
}
