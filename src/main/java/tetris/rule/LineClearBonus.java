package tetris.rule;

import java.util.Set;
import tetris.board.Board;

/** 삭제할 줄에 남아 있는 아이템의 최대 배율을 한 번 적용한다. 배율을 곱해서 중첩하지 않는다. */
public final class LineClearBonus {
    private LineClearBonus() { }

    public static int multiplier(Board board, Set<Integer> rows) {
        int multiplier = 1;
        for (int row : rows)
            for (int column = 0; column < Board.COLUMNS; column++)
                multiplier = Math.max(multiplier, board.itemAt(row, column).lineScoreMultiplier());
        return multiplier;
    }
}
