package tetris.scoreboard;

import java.util.List;

/** 점수 기록 저장 경계. 저장 형식, 경로 및 파일 오류 정책은 미정이다. */
public class ScoreRepository {
    public List<ScoreEntry> load() {
        // TODO(Req1): 점수 기록 읽기
        throw new UnsupportedOperationException("TODO: 점수 기록 읽기");
    }

    public void save(List<ScoreEntry> entries) {
        // TODO(Req1): 점수 기록 저장
        throw new UnsupportedOperationException("TODO: 점수 기록 저장");
    }

    public void clear() {
        // TODO(Req1): 저장된 점수 기록 삭제
        throw new UnsupportedOperationException("TODO: 저장된 점수 기록 삭제");
    }
}
