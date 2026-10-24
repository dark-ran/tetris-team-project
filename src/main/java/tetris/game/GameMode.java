package tetris.game;

/** 게임 시작부터 재시작과 점수 저장까지 유지하는 모드. */
public enum GameMode {
    NORMAL("일반 모드"), ITEM("아이템 모드");

    private final String label;
    GameMode(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
