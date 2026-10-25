
package tetris.game;

/**
 * 게임 난이도.
 *
 * I 블록 가중치:
 * EASY 12, NORMAL 10, HARD 8
 *
 * 나머지 6종 블록 가중치는 각각 10이다.
 */
public enum Difficulty {
    EASY(12, 80),
    NORMAL(10, 100),
    HARD(8, 120);

    private final int iWeight;
    private final long intervalDecreaseMillis;

    Difficulty(int iWeight, long intervalDecreaseMillis) {
        this.iWeight = iWeight;
        this.intervalDecreaseMillis = intervalDecreaseMillis;
    }

    public int iWeight() {
        return iWeight;
    }

    public long intervalDecreaseMillis() {
        return intervalDecreaseMillis;
    }
}
