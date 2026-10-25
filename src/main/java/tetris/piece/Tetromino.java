
package tetris.piece;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * 블록의 종류와 실제 점유 모양을 보관한다.
 *
 * 일반 블록은 네 칸이고, 1칸 특수 블록은 한 칸이다.
 * 좌표는 회전 기준 정사각형 안의 [행, 열]이다.
 */
public final class Tetromino {
    private final PieceType type;
    private final int rotationSize;
    private final int[][] occupiedCells;

    /**
     * 기존 일곱 종류의 일반 블록을 생성한다.
     */
    public Tetromino(PieceType type) {
        this.type = Objects.requireNonNull(
                type, "블록 종류가 필요합니다.");

        this.rotationSize = switch (type) {
            case I -> 4;
            case O -> 2;
            case SINGLE -> 1;
            default -> 3;
        };

        occupiedCells = switch (type) {
            case I -> new int[][] {
                    {1, 0}, {1, 1}, {1, 2}, {1, 3}
            };
            case O -> new int[][] {
                    {0, 0}, {0, 1}, {1, 0}, {1, 1}
            };
            case T -> new int[][] {
                    {0, 1}, {1, 0}, {1, 1}, {1, 2}
            };
            case S -> new int[][] {
                    {0, 1}, {0, 2}, {1, 0}, {1, 1}
            };
            case Z -> new int[][] {
                    {0, 0}, {0, 1}, {1, 1}, {1, 2}
            };
            case J -> new int[][] {
                    {0, 0}, {1, 0}, {1, 1}, {1, 2}
            };
            case L -> new int[][] {
                    {0, 2}, {1, 0}, {1, 1}, {1, 2}
            };
            case SINGLE -> new int[][] {{0, 0}};
        };
    }

    private Tetromino(
            PieceType type,
            int rotationSize,
            int[][] occupiedCells) {
        this.type = Objects.requireNonNull(type);
        this.rotationSize = rotationSize;

        this.occupiedCells = new int[occupiedCells.length][];
        for (int index = 0; index < occupiedCells.length; index++) {
            this.occupiedCells[index] = occupiedCells[index].clone();
        }
    }

    /**
     * P 아이템의 실제 한 칸 모양.
     *
     * PieceType.O는 기존 보드 색상 저장 형식과의 호환을 위한
     * 내부 타입일 뿐이며, P 아이템을 일반 O 블록으로
     * 취급해야 한다는 뜻은 아니다.
     */
public static Tetromino singleCell() {
    return new Tetromino(PieceType.SINGLE);
}

    public boolean isSingleCell() {
        return occupiedCells.length == 1;
    }

    /**
     * 블록의 종류를 반환한다.
     */
    public PieceType type() {
        return type;
    }

    /**
     * 회전 기준 정사각형의 크기를 반환한다.
     */
    public int rotationSize() {
        return rotationSize;
    }

    /**
     * 실제 점유 칸의 방어적 복사본을 반환한다.
     */
    public int[][] cells() {
        int[][] copiedCells = new int[occupiedCells.length][];

        for (int index = 0; index < occupiedCells.length; index++) {
            copiedCells[index] = occupiedCells[index].clone();
        }

        return copiedCells;
    }

    /**
     * 기존 모양을 변경하지 않고 90도 회전한 새 블록을 만든다.
     * 1칸 블록은 회전해도 같은 좌표를 유지한다.
     */
    public Tetromino rotateClockwise() {
        int[][] rotatedCells = new int[occupiedCells.length][2];

        for (int index = 0; index < occupiedCells.length; index++) {
            rotatedCells[index][0] = occupiedCells[index][1];
            rotatedCells[index][1] =
                    rotationSize - 1 - occupiedCells[index][0];
        }

        Arrays.sort(
                rotatedCells,
                Comparator.comparingInt(
                        (int[] cell) -> cell[0])
                        .thenComparingInt(cell -> cell[1]));

        return new Tetromino(
                type, rotationSize, rotatedCells);
    }
}
