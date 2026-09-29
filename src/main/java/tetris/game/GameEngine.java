package tetris.game;

/** UI와 독립적으로 게임 행동과 상태 전이를 처리한다. */
public class GameEngine {
    public void newGame() {
        // TODO(Req1): 새 게임 초기화
        throw new UnsupportedOperationException("TODO: 새 게임 초기화");
    }

    public void apply(GameAction action) {
        // TODO(Req1): 게임 행동 적용
        throw new UnsupportedOperationException("TODO: 게임 행동 적용");
    }

    public GameState state() {
        // TODO(Req1): 게임 상태 조회
        throw new UnsupportedOperationException("TODO: 게임 상태 조회");
    }

    public boolean isGameOver() {
        // TODO(Req1): 게임 종료 여부 확인
        throw new UnsupportedOperationException("TODO: 게임 종료 여부 확인");
    }
}
