package tetris.game;

import java.util.Objects;
import tetris.piece.ItemType;

/**
 * 대전 모드의 공격·승패·시간제한 규칙. 상태를 갖지 않는 계산만 제공한다.
 * P2P에서는 서버 쪽에서만 승패를 판정해야 양쪽 결과가 어긋나지 않는다.
 */
public final class BattleRules {
    /** 한 번에 이 줄 수 이상을 삭제해야 공격한다. */
    public static final int MIN_ROWS_FOR_ATTACK = 2;

    /** 시간제한 모드의 기본 제한 시간. */
    public static final long DEFAULT_TIME_LIMIT_MILLIS = 120_000;

    private BattleRules() { }

    /**
     * 고정한 블록과 삭제한 줄 수로 공격 여부를 정한다.
     * 아이템 중에서는 줄 삭제(L)만 공격으로 인정한다.
     * 1칸 블록은 한 번에 1줄까지만 채울 수 있어 공격 조건(2줄 이상)에 해당하지 않는다.
     */
    public static boolean sendsAttack(ItemType pieceItem, int clearedRows) {
        Objects.requireNonNull(pieceItem, "고정한 블록의 아이템 종류가 필요합니다.");
        if (clearedRows < MIN_ROWS_FOR_ATTACK) return false;
        return pieceItem == ItemType.NONE || pieceItem == ItemType.LINE_CLEAR;
    }

    /** 남은 시간. 0 미만으로 내려가지 않는다. */
    public static long remainingMillis(long limitMillis, long elapsedMillis) {
        if (limitMillis < 0 || elapsedMillis < 0) {
            throw new IllegalArgumentException("제한 시간과 경과 시간은 0 이상이어야 합니다.");
        }
        return Math.max(0, limitMillis - elapsedMillis);
    }

    /**
     * 승패 판정.
     * <ul>
     *   <li>둘 다 더 이상 쌓을 수 없으면 무승부</li>
     *   <li>한쪽만 쌓을 수 없으면 상대 승리</li>
     *   <li>시간이 끝났으면 점수가 높은 쪽 승리, 같으면 무승부</li>
     * </ul>
     */
    public static BattleOutcome decide(boolean player1GameOver, boolean player2GameOver,
            long player1Score, long player2Score, boolean timeUp) {
        if (player1GameOver && player2GameOver) return BattleOutcome.DRAW;
        if (player1GameOver) return BattleOutcome.PLAYER2_WIN;
        if (player2GameOver) return BattleOutcome.PLAYER1_WIN;
        if (!timeUp) return BattleOutcome.ONGOING;
        if (player1Score == player2Score) return BattleOutcome.DRAW;
        return player1Score > player2Score ? BattleOutcome.PLAYER1_WIN : BattleOutcome.PLAYER2_WIN;
    }

    public static BattleOutcome decide(PlayerSnapshot player1, PlayerSnapshot player2, boolean timeUp) {
        Objects.requireNonNull(player1);
        Objects.requireNonNull(player2);
        return decide(player1.isGameOver(), player2.isGameOver(), player1.score(), player2.score(), timeUp);
    }
}
