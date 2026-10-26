package tetris.piece;

/** 일반 블록에 붙는 아이템과 독립된 모양을 사용하는 아이템 종류. */
public enum ItemType {
    NONE("", ""),
    LINE_CLEAR("L", "줄 삭제 (L)"),
    WEIGHT("W", "무게추 (W)", true),
    BOMB("B", "폭탄 (B)", true);

    private final String symbol;
    private final String displayName;
    private final boolean standalone;

    ItemType(String symbol, String displayName) {
        this(symbol, displayName, false);
    }

    ItemType(String symbol, String displayName, boolean standalone) {
        this.symbol = symbol;
        this.displayName = displayName;
        this.standalone = standalone;
    }

    public String symbol() {
        return symbol;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isStandalone() { return standalone; }
}
