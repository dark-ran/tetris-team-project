package tetris.piece;

/** 블록의 모양과 별도로 부착되는 아이템 종류. */
public enum ItemType {
    NONE("", ""),
    LINE_CLEAR("L", "줄 삭제 (L)");

    private final String symbol;
    private final String displayName;

    ItemType(String symbol, String displayName) {
        this.symbol = symbol;
        this.displayName = displayName;
    }

    public String symbol() {
        return symbol;
    }

    public String displayName() {
        return displayName;
    }
}
