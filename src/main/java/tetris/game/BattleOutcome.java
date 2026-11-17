package tetris.game;

/** 대전 판정 결과. */
public enum BattleOutcome {
    ONGOING,
    PLAYER1_WIN,
    PLAYER2_WIN,
    DRAW;

    public boolean isFinished() {
        return this != ONGOING;
    }
}
