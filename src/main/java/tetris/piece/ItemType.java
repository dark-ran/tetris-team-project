package tetris.piece;

/** 일반 블록에 붙는 아이템과 독립된 모양을 사용하는 아이템 종류. */
public enum ItemType {
    NONE("", ""),
    LINE_CLEAR("L", "줄 삭제 (L)"),
    WEIGHT("W", "무게추 (W)", true, true),
    BOMB("B", "폭탄 (B)", true, false),
    GOLDEN("G", "황금 블록 (G)", false, true, 5);

    private final String symbol;
    private final String displayName;
    private final boolean standalone;
    private final boolean marksAllCells;
    private final int lineScoreMultiplier;

    ItemType(String symbol, String displayName) {
        this(symbol, displayName, false, false);
    }

    ItemType(String symbol, String displayName, boolean standalone, boolean marksAllCells) {
        this(symbol, displayName, standalone, marksAllCells, 1);
    }

    ItemType(String symbol, String displayName, boolean standalone, boolean marksAllCells,
            int lineScoreMultiplier) {
        this.symbol = symbol;
        this.displayName = displayName;
        this.standalone = standalone;
        this.marksAllCells = marksAllCells;
        this.lineScoreMultiplier = lineScoreMultiplier;
    }

    public String symbol() {
        return symbol;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isStandalone() { return standalone; }
    public boolean marksAllCells() { return marksAllCells; }
    public int lineScoreMultiplier() { return lineScoreMultiplier; }
}
