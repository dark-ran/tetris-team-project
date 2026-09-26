package tetris.scoreboard;

import java.util.List;

/** 점수 등록과 순위 조회를 담당한다. 동률 및 기록 진입 정책은 미정이다. */
public class ScoreBoardService {
    public boolean qualifies(long score) {
        // TODO(Req1): 스코어보드 진입 여부 확인
        throw new UnsupportedOperationException("TODO: 스코어보드 진입 여부 확인");
    }

    public void register(String name, long score) {
        // TODO(Req1): 점수 기록 등록
        throw new UnsupportedOperationException("TODO: 점수 기록 등록");
    }

    public List<ScoreEntry> top() {
        // TODO(Req1): 상위 점수 기록 조회
        throw new UnsupportedOperationException("TODO: 상위 점수 기록 조회");
    }

    public void reset() {
        // TODO(Req1): 점수 기록 초기화
        throw new UnsupportedOperationException("TODO: 점수 기록 초기화");
    }
}
