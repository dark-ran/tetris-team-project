package tetris.game;

/** 사용자 입력과 자동 Tick이 전달할 게임 행동. */
public enum GameAction {
    LEFT,
    RIGHT,
    DOWN,
    ROTATE_CLOCKWISE,
    HARD_DROP,
    TICK,
    TOGGLE_PAUSE,
    QUIT
}
