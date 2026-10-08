package tetris.loop;

import java.util.Objects;

/** 게임 Tick의 수명주기를 담당한다. 타이머와 입력 처리 순서는 후속 구현에서 결정한다. */
public class GameLoop {
    private Runnable tickListener;

    // 연결 계약 제안: 실제 타이머는 이 콜백을 EDT에서 호출한다.
    public void setTickListener(Runnable listener) {
        tickListener = Objects.requireNonNull(listener);
    }

    // TODO(Req1): 낙하 간격 변경
    public void setIntervalMillis(long millis) {
        throw new UnsupportedOperationException("TODO: 낙하 간격 변경");
    }

    public void start() {
        // TODO(Req1): 게임 루프 시작
        throw new UnsupportedOperationException("TODO: 게임 루프 시작");
    }

    public void stop() {
        // TODO(Req1): 게임 루프 종료
        throw new UnsupportedOperationException("TODO: 게임 루프 종료");
    }

    public void pause() {
        // TODO(Req1): 자동 낙하 일시정지
        throw new UnsupportedOperationException("TODO: 자동 낙하 일시정지");
    }

    public void resume() {
        // TODO(Req1): 자동 낙하 재개
        throw new UnsupportedOperationException("TODO: 자동 낙하 재개");
    }
}
