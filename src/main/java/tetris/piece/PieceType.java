package tetris.piece;

/** 테트로미노의 일곱 종류와 보드에서 사용하는 고정 칸 저장값. */
public enum PieceType {
    I(1),
    O(2),
    T(3),
    S(4),
    Z(5),
    J(6),
    L(7);

    private final int cellValue;

    PieceType(int cellValue) {
        this.cellValue = cellValue;
    }

    /** 보드에 저장하는 블록 종류의 값. 실제 색상과 무늬는 화면에서 관리한다. */
    public int cellValue() {
        return cellValue;
    }

    /** 빈 칸을 제외한 칸 저장값을 블록 종류로 변환한다. */
    public static PieceType fromCellValue(int cellValue) {
        for (PieceType type : values()) {
            if (type.cellValue == cellValue) {
                return type;
            }
        }
        throw new IllegalArgumentException("블록 칸 저장값은 1부터 7까지여야 합니다: " + cellValue);
    }
}
