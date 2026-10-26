package tetris.piece;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * 일반 블록과 아이템의 종류·모양을 보관한다. 일반 블록은 네 칸이다.
 * 좌표는 회전 기준 정사각형 안의 [행, 열]이며, 생성 후에는 모양이 바뀌지 않는다.
 * 보드 위의 위치와 회전 가능 여부는 게임 엔진과 보드에서 관리한다.
 */
public final class Tetromino {
    private final PieceType type;
    private final int rotationSize;
    private final int[][] occupiedCells;
    private final ItemType itemType;
    private final int itemRow;
    private final int itemColumn;

    /** 지정한 종류의 초기 모양을 생성한다. */
    public Tetromino(PieceType type) {
        this.type = Objects.requireNonNull(type, "블록 종류가 필요합니다.");
        rotationSize = switch (type) {
            case I -> 4;
            case O -> 2;
            default -> 3;
        };
        occupiedCells = switch (type) {
            case I -> new int[][] {{1, 0}, {1, 1}, {1, 2}, {1, 3}};
            case O -> new int[][] {{0, 0}, {0, 1}, {1, 0}, {1, 1}};
            case T -> new int[][] {{0, 1}, {1, 0}, {1, 1}, {1, 2}};
            case S -> new int[][] {{0, 1}, {0, 2}, {1, 0}, {1, 1}};
            case Z -> new int[][] {{0, 0}, {0, 1}, {1, 1}, {1, 2}};
            case J -> new int[][] {{0, 0}, {1, 0}, {1, 1}, {1, 2}};
            case L -> new int[][] {{0, 2}, {1, 0}, {1, 1}, {1, 2}};
        };
        itemType = ItemType.NONE;
        itemRow = itemColumn = -1;
    }

    private Tetromino(PieceType type, int rotationSize, int[][] occupiedCells,
            ItemType itemType, int itemRow, int itemColumn) {
        this.type = type;
        this.rotationSize = rotationSize;
        this.occupiedCells = occupiedCells;
        this.itemType = itemType;
        this.itemRow = itemRow;
        this.itemColumn = itemColumn;
    }

    /** 블록이 차지하는 네 칸 중 지정한 한 칸에 아이템을 붙인 새 블록을 반환한다. */
    public Tetromino withItem(ItemType itemType, int cellIndex) {
        Objects.requireNonNull(itemType);
        if (itemType == ItemType.NONE || itemType.isStandalone() || this.itemType.isStandalone()
                || cellIndex < 0 || cellIndex >= occupiedCells.length) {
            throw new IllegalArgumentException("아이템 종류와 블록 안의 칸 번호가 필요합니다.");
        }
        int[] cell = occupiedCells[cellIndex];
        return new Tetromino(type, rotationSize, occupiedCells, itemType, cell[0], cell[1]);
    }

    /** 폭 4칸의 무게추. 위쪽 두 칸과 아래쪽 네 칸을 W로 표시한다. */
    public static Tetromino weight() {
        return new Tetromino(PieceType.I, 4,
                new int[][] {{0, 1}, {0, 2}, {1, 0}, {1, 1}, {1, 2}, {1, 3}},
                ItemType.WEIGHT, -1, -1);
    }

    /** B로 표시하는 한 칸 폭탄. Z의 색상을 사용하며 일반 블록 종류에는 추가하지 않는다. */
    public static Tetromino bomb() {
        return new Tetromino(PieceType.Z, 1, new int[][] {{0, 0}}, ItemType.BOMB, -1, -1);
    }

    public ItemType itemType() { return itemType; }
    public int itemRow() { return itemRow; }
    public int itemColumn() { return itemColumn; }

    public boolean hasItemAt(int row, int column) {
        if (itemType.isStandalone()) {
            for (int[] cell : occupiedCells) {
                if (cell[0] == row && cell[1] == column) return true;
            }
            return false;
        }
        return itemType != ItemType.NONE && itemRow == row && itemColumn == column;
    }

    /** O 블록도 아이템 위치가 달라지면 서로 다른 배치이다. */
    public boolean hasSameLayout(Tetromino other) {
        return type == other.type && Arrays.deepEquals(occupiedCells, other.occupiedCells)
                && itemType == other.itemType && itemRow == other.itemRow && itemColumn == other.itemColumn;
    }

    /** 블록의 종류를 반환한다. 회전해도 종류는 바뀌지 않는다. */
    public PieceType type() {
        return type;
    }

    /** 회전 기준 정사각형의 한 변에 포함된 칸 수를 반환한다. */
    public int rotationSize() {
        return rotationSize;
    }

    /** 차지하는 칸을 행·열 순서로 반환한다. 반환 배열을 수정해도 블록은 바뀌지 않는다. */
    public int[][] cells() {
        int[][] copiedCells = new int[occupiedCells.length][];
        for (int index = 0; index < occupiedCells.length; index++) {
            copiedCells[index] = occupiedCells[index].clone();
        }
        return copiedCells;
    }

    /** 원래 블록을 유지하고 시계방향으로 90도 회전한 새 블록을 반환한다. */
    public Tetromino rotateClockwise() {
        int[][] rotatedCells = new int[occupiedCells.length][2];
        for (int index = 0; index < occupiedCells.length; index++) {
            rotatedCells[index][0] = occupiedCells[index][1];
            rotatedCells[index][1] = rotationSize - 1 - occupiedCells[index][0];
        }
        Arrays.sort(rotatedCells, Comparator.comparingInt((int[] cell) -> cell[0])
                .thenComparingInt(cell -> cell[1]));
        boolean attached = itemType != ItemType.NONE && !itemType.isStandalone();
        return new Tetromino(type, rotationSize, rotatedCells, itemType,
                attached ? itemColumn : -1, attached ? rotationSize - 1 - itemRow : -1);
    }
}
