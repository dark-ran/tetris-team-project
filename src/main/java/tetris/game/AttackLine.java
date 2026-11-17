package tetris.game;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import tetris.board.Board;

/**
 * 상대에게 넘어가는 공격 줄 한 줄. 각 열이 채워져 있는지만 보관한다.
 * 줄을 삭제하게 만든 마지막 블록의 칸은 빈 칸으로 남는다.
 */
public final class AttackLine implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private final boolean[] filled;

    public AttackLine(boolean[] filled) {
        Objects.requireNonNull(filled, "공격 줄의 칸 정보가 필요합니다.");
        if (filled.length != Board.COLUMNS) {
            throw new IllegalArgumentException("공격 줄은 " + Board.COLUMNS + "칸이어야 합니다.");
        }
        this.filled = filled.clone();
    }

    /** 열별로 채워진 칸이면 true. 수정해도 원본에 영향을 주지 않는 사본이다. */
    public boolean[] filled() {
        return filled.clone();
    }

    public boolean isFilled(int column) {
        return filled[Objects.checkIndex(column, Board.COLUMNS)];
    }

    @Override public boolean equals(Object other) {
        return other instanceof AttackLine line && Arrays.equals(filled, line.filled);
    }

    @Override public int hashCode() {
        return Arrays.hashCode(filled);
    }

    @Override public String toString() {
        StringBuilder text = new StringBuilder("AttackLine[");
        for (boolean cell : filled) text.append(cell ? '#' : '.');
        return text.append(']').toString();
    }
}
