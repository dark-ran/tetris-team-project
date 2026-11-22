package tetris.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.piece.ItemType;

@DisplayName("대전 공격·승패·시간 규칙")
class BattleRulesTest {
    @ParameterizedTest
    @EnumSource(ItemType.class)
    @DisplayName("아이템 중 줄 삭제만 공격으로 인정")
    void onlyLineClearItemAttacks(ItemType item) {
        boolean expected = item == ItemType.NONE || item == ItemType.LINE_CLEAR;
        assertEquals(expected, BattleRules.sendsAttack(item, 2));
        assertFalse(BattleRules.sendsAttack(item, 1));
    }

    @Test
    @DisplayName("남은 시간은 0 아래로 내려가지 않는다")
    void remainingTimeStopsAtZero() {
        assertEquals(30_000, BattleRules.remainingMillis(120_000, 90_000));
        assertEquals(0, BattleRules.remainingMillis(120_000, 130_000));
        assertThrows(IllegalArgumentException.class, () -> BattleRules.remainingMillis(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> BattleRules.remainingMillis(0, -1));
        assertThrows(NullPointerException.class, () -> BattleRules.sendsAttack(null, 2));
    }

    @Test
    @DisplayName("더 이상 쌓을 수 없는 쪽이 지고, 둘 다면 무승부")
    void gameOverDecidesTheWinner() {
        assertEquals(BattleOutcome.PLAYER2_WIN, BattleRules.decide(true, false, 900, 0, false));
        assertEquals(BattleOutcome.PLAYER1_WIN, BattleRules.decide(false, true, 0, 900, true));
        assertEquals(BattleOutcome.DRAW, BattleRules.decide(true, true, 10, 0, false));
        assertEquals(BattleOutcome.ONGOING, BattleRules.decide(false, false, 10, 0, false));
        assertFalse(BattleOutcome.ONGOING.isFinished());
        assertTrue(BattleOutcome.DRAW.isFinished());
    }

    @Test
    @DisplayName("시간이 끝나면 점수가 높은 쪽이 이기고 같으면 무승부")
    void timeUpComparesScores() {
        assertEquals(BattleOutcome.PLAYER1_WIN, BattleRules.decide(false, false, 500, 300, true));
        assertEquals(BattleOutcome.PLAYER2_WIN, BattleRules.decide(false, false, 300, 500, true));
        assertEquals(BattleOutcome.DRAW, BattleRules.decide(false, false, 500, 500, true));
    }

    @Test
    @DisplayName("플레이어 사본으로도 판정한다")
    void decidesFromSnapshots() {
        PlayerSession first = new PlayerSession();
        PlayerSession second = new PlayerSession();
        first.start(BattleMode.TIME_LIMIT, Difficulty.NORMAL);
        second.start(BattleMode.TIME_LIMIT, Difficulty.NORMAL);
        assertEquals(BattleOutcome.ONGOING, BattleRules.decide(first.snapshot(), second.snapshot(), false));
        assertEquals(BattleOutcome.DRAW, BattleRules.decide(first.snapshot(), second.snapshot(), true));
        assertThrows(NullPointerException.class, () -> BattleRules.decide(null, second.snapshot(), true));
    }

    @Test
    @DisplayName("대전 모드는 엔진 모드와 시간제한 여부를 알려준다")
    void battleModesMapToEngineModes() {
        assertEquals(GameMode.NORMAL, BattleMode.NORMAL.gameMode());
        assertEquals(GameMode.ITEM, BattleMode.ITEM.gameMode());
        assertEquals(GameMode.NORMAL, BattleMode.TIME_LIMIT.gameMode());
        assertTrue(BattleMode.TIME_LIMIT.isTimeLimited());
        assertFalse(BattleMode.ITEM.isTimeLimited());
        assertEquals("시간제한 대전", BattleMode.TIME_LIMIT.toString());
    }
}
