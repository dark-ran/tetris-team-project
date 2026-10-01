package tetris.piece;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * 네 칸으로 이루어진 블록의 종류와 모양을 보관한다.
 * 좌표는 회전 기준 정사각형 안의 [행, 열]이며, 생성 후에는 모양이 바뀌지 않는다.
 * 보드 위의 위치와 회전 가능 여부는 게임 엔진과 보드에서 관리한다.
 */
public final class Tetromino {
    private final PieceType type;
    private final int rotationSize;
    private final int[][] occupiedCells;

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
    }

    private Tetromino(PieceType type, int rotationSize, int[][] occupiedCells) {
        this.type = type;
        this.rotationSize = rotationSize;
        this.occupiedCells = occupiedCells;
    }

    /** 블록의 종류를 반환한다. 회전해도 종류는 바뀌지 않는다. */
    public PieceType type() {
        return type;
    }

    /** 회전 기준 정사각형의 한 변에 포함된 칸 수를 반환한다. */
    public int rotationSize() {
        return rotationSize;
    }

    /** 차지하는 네 칸을 행·열 순서로 반환한다. 반환 배열을 수정해도 블록은 바뀌지 않는다. */
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
        return new Tetromino(type, rotationSize, rotatedCells);
    }
}
