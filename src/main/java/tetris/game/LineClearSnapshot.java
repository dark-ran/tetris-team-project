package tetris.game;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import tetris.board.Board;
import tetris.piece.ItemType;

/** 삭제와 공격 줄 추가 직전의 보드. sequence는 해당 삭제까지의 누적 삭제 줄 수이다. */
public final class LineClearSnapshot implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private final int sequence;
    private final int[][] cells;
    private final ItemType[][] items;
    private final List<Integer> rows;

    LineClearSnapshot(int sequence, Board board, List<Integer> rows) {
        this(sequence, board.snapshot(), itemsFrom(board), rows);
    }

    LineClearSnapshot(int sequence, int[][] cells, ItemType[][] items, List<Integer> rows) {
        if (rows.isEmpty() || sequence < rows.size() || rows.stream().distinct().count() != rows.size()) {
            throw new IllegalArgumentException("삭제 줄과 누적 삭제 수가 필요합니다.");
        }
        for (int row : rows) Objects.checkIndex(row, Board.ROWS);
        this.sequence = sequence;
        this.rows = List.copyOf(rows);
        this.cells = new int[Board.ROWS][];
        this.items = new ItemType[Board.ROWS][];
        for (int row = 0; row < Board.ROWS; row++) {
            this.cells[row] = cells[row].clone();
            this.items[row] = items[row].clone();
        }
    }

    private static ItemType[][] itemsFrom(Board board) {
        ItemType[][] items = new ItemType[Board.ROWS][Board.COLUMNS];
        for (int row = 0; row < Board.ROWS; row++) {
            for (int column = 0; column < Board.COLUMNS; column++) {
                items[row][column] = board.itemAt(row, column);
            }
        }
        return items;
    }

    public int sequence() { return sequence; }
    public List<Integer> rows() { return rows; }

    public int cellAt(int row, int column) {
        return cells[Objects.checkIndex(row, Board.ROWS)][Objects.checkIndex(column, Board.COLUMNS)];
    }

    public ItemType itemAt(int row, int column) {
        return items[Objects.checkIndex(row, Board.ROWS)][Objects.checkIndex(column, Board.COLUMNS)];
    }
}
