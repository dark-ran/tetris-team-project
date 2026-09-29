package tetris.settings;

import java.util.Map;

/** 설정 데이터 API. 화면 프리셋, 키 이름과 중복 키 정책은 팀 리뷰가 필요하다. */
public class GameSettings {
    public int windowSizePreset() {
        // TODO(Req1): 화면 크기 프리셋 조회
        throw new UnsupportedOperationException("TODO: 화면 크기 프리셋 조회");
    }

    public boolean colorBlindMode() {
        // TODO(Req1): 색각이상 모드 조회
        throw new UnsupportedOperationException("TODO: 색각이상 모드 조회");
    }

    public Map<String, Integer> keyBindings() {
        // TODO(Req1): 게임 조작 키 설정 조회
        throw new UnsupportedOperationException("TODO: 게임 조작 키 설정 조회");
    }
}
