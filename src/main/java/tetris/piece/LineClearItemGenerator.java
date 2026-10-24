package tetris.piece;

import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.random.RandomGenerator;

/** 일반 블록의 네 칸 중 한 칸을 균등 선택하여 L 아이템을 붙인다. */
public final class LineClearItemGenerator implements UnaryOperator<Tetromino> {
    private final RandomGenerator random;

    public LineClearItemGenerator(RandomGenerator random) {
        this.random = Objects.requireNonNull(random);
    }

    @Override public Tetromino apply(Tetromino piece) {
        return piece.withItem(ItemType.LINE_CLEAR, random.nextInt(piece.cells().length));
    }
}
