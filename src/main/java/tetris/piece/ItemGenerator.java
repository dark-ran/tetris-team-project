package tetris.piece;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.random.RandomGenerator;

/** 지급 시 사용할 아이템을 같은 확률로 고른다. 새 아이템은 목록에 생성기를 추가한다. */
public final class ItemGenerator implements UnaryOperator<Tetromino> {
    private final RandomGenerator random;
    private final List<UnaryOperator<Tetromino>> generators;

    public ItemGenerator(RandomGenerator random) {
        this.random = Objects.requireNonNull(random);
        generators = List.of(new LineClearItemGenerator(random), ignored -> Tetromino.weight(),
                ignored -> Tetromino.bomb(), piece -> piece.withItem(ItemType.GOLDEN),
                ignored -> Tetromino.singleCell());
    }

    @Override public Tetromino apply(Tetromino piece) {
        Objects.requireNonNull(piece);
        return generators.get(random.nextInt(generators.size())).apply(piece);
    }
}
