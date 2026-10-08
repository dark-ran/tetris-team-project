package tetris.input;

import java.util.Map;
import tetris.game.GameAction;

/** 키 코드를 게임 행동에 매핑한다. 미등록 키와 중복 키의 처리 정책은 미정이다. */
public class KeyMapper {
    // TODO(2번): 키 이름과 중복 검증 정책을 확정해 실제 매핑에 반영한다.
    public void updateBindings(Map<String, Integer> bindings) {
        throw new UnsupportedOperationException("TODO: 사용자 키 설정 적용");
    }

    public GameAction map(int keyCode) {
        // TODO(Req1): 사용자 키 설정에 따른 게임 행동 조회
        throw new UnsupportedOperationException("TODO: 사용자 키 설정에 따른 게임 행동 조회");
    }
}
