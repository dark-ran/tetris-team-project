package tetris.game;

/** 대전 모드 3종. 시간제한은 아이템 없이 일반 블록으로 진행한다. */
public enum BattleMode {
    NORMAL("일반 대전", GameMode.NORMAL, false),
    ITEM("아이템 대전", GameMode.ITEM, false),
    TIME_LIMIT("시간제한 대전", GameMode.NORMAL, true);

    private final String label;
    private final GameMode gameMode;
    private final boolean timeLimited;

    BattleMode(String label, GameMode gameMode, boolean timeLimited) {
        this.label = label;
        this.gameMode = gameMode;
        this.timeLimited = timeLimited;
    }

    /** 각 플레이어의 엔진을 시작할 때 사용하는 게임 모드. */
    public GameMode gameMode() { return gameMode; }

    public boolean isTimeLimited() { return timeLimited; }

    @Override public String toString() { return label; }
}
