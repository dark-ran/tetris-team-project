package tetris.game;

/** 사용자 입력과 자동 낙하가 전달할 게임 행동. 종료 요청은 앱 제어에서 처리한다. */
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
