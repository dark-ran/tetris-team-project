package tetris.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tetris.game.BattleTestSupport.engine;
import static tetris.game.BattleTestSupport.line;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tetris.board.Board;
import tetris.piece.ItemType;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;
import tetris.rule.SpeedSystem;

@DisplayName("플레이어 세션과 화면 사본")
class PlayerSessionTest {
    private static final Tetromino O = new Tetromino(PieceType.O);

    @Test
    @DisplayName("2줄을 지우면 일반 게임과 같은 점수를 얻고 상대에게 공격 줄이 넘어간다")
    void clearingTwoRowsScoresAndAttacksTheOpponent() {
        PlayerSession attacker = new PlayerSession(engine(List.of(O), 0, 2, 4, 6, 8));
        PlayerSession defender = new PlayerSession(engine(List.of(O), 4));
        attacker.setAttackListener(defender::receiveAttack);
        attacker.start(BattleMode.NORMAL, Difficulty.NORMAL);
        defender.start(BattleMode.NORMAL, Difficulty.NORMAL);

        long dropScore = 0;
        for (int count = 0; count < 5; count++) {
            dropScore += attacker.apply(GameAction.HARD_DROP).droppedRows();
        }
        assertEquals(dropScore + 300, attacker.score());
        assertEquals(List.of(line("########.."), line("########..")),
                defender.snapshot().pendingAttackLines());
    }

    @Test
    @DisplayName("공격 리스너가 없거나 공격이 없으면 아무것도 전달하지 않는다")
    void noListenerOrNoAttackIsQuiet() {
        PlayerSession session = new PlayerSession(engine(List.of(O), 0, 2, 4, 6, 8));
        session.start(BattleMode.ITEM, Difficulty.EASY);
        for (int count = 0; count < 5; count++) session.apply(GameAction.HARD_DROP);
        assertEquals(BattleMode.ITEM, session.mode());
        assertEquals(SpeedSystem.INITIAL_INTERVAL_MILLIS, session.gravityIntervalMillis());
        assertFalse(session.isGameOver());
        assertThrows(NullPointerException.class, () -> session.apply(null));
        assertThrows(NullPointerException.class, () -> session.start(null, Difficulty.NORMAL));
        assertThrows(NullPointerException.class, () -> new PlayerSession(null));
    }

    @Test
    @DisplayName("두 플레이어가 각자 스레드에서 서로 공격해도 멈추지 않는다")
    void concurrentPlayersDoNotDeadlock() {
        PlayerSession first = new PlayerSession();
        PlayerSession second = new PlayerSession();
        first.setAttackListener(second::receiveAttack);
        second.setAttackListener(first::receiveAttack);
        first.start(BattleMode.NORMAL, Difficulty.NORMAL);
        second.start(BattleMode.NORMAL, Difficulty.NORMAL);
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            CountDownLatch ready = new CountDownLatch(1);
            Thread one = new Thread(() -> play(first, ready));
            Thread two = new Thread(() -> play(second, ready));
            one.start();
            two.start();
            ready.countDown();
            one.join();
            two.join();
        });
        assertNotNull(first.snapshot());
        assertNotNull(second.snapshot());
    }

    private static void play(PlayerSession session, CountDownLatch ready) {
        try {
            ready.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return;
        }
        GameAction[] actions = {GameAction.LEFT, GameAction.ROTATE_CLOCKWISE, GameAction.RIGHT,
                GameAction.DOWN, GameAction.HARD_DROP};
        for (int count = 0; count < 2_000 && !session.isGameOver(); count++) {
            session.apply(actions[count % actions.length]);
            session.snapshot();
        }
    }

    @Test
    @DisplayName("화면 사본은 보드·현재 블록·다음 블록·점수를 담고 직렬화할 수 있다")
    void snapshotCarriesDrawingDataAndSerializes() throws Exception {
        Tetromino marked = new Tetromino(PieceType.T).withItem(ItemType.LINE_CLEAR, 1);
        PlayerSession session = new PlayerSession(engine(List.of(marked, O), 3, 0));
        session.start(BattleMode.ITEM, Difficulty.NORMAL);
        session.receiveAttack(List.of(line("#########.")));
        PlayerSnapshot snapshot = session.snapshot();

        assertEquals(BattleMode.ITEM, snapshot.mode());
        assertEquals(GamePhase.RUNNING, snapshot.phase());
        assertFalse(snapshot.isGameOver());
        assertEquals(0, snapshot.score());
        assertEquals(1, snapshot.level());
        assertEquals(0, snapshot.totalClearedRows());
        assertEquals(18, snapshot.landingRow());
        assertEquals(1, snapshot.pendingAttackLines().size());
        assertEquals(Board.EMPTY_CELL, snapshot.cellAt(19, 0));
        assertEquals(ItemType.NONE, snapshot.itemAt(19, 0));

        PlayerSnapshot.PieceView current = snapshot.currentPiece();
        assertEquals(0, current.row());
        assertEquals(3, current.column());
        assertEquals(PieceType.T.cellValue(), current.cellValue());
        assertEquals(ItemType.LINE_CLEAR, current.itemType());
        assertArrayEquals(new boolean[] {false, true, false, false}, current.itemCells());
        assertEquals(4, current.cells().length);
        assertEquals(PieceType.O.cellValue(), snapshot.nextPiece().cellValue());

        int[][] cells = snapshot.boardCells();
        cells[0][0] = 7;
        assertEquals(Board.EMPTY_CELL, snapshot.cellAt(0, 0));
        current.cells()[0][0] = 9;
        assertEquals(0, current.cells()[0][0]);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(snapshot);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            PlayerSnapshot copy = (PlayerSnapshot) input.readObject();
            assertEquals(snapshot.pendingAttackLines(), copy.pendingAttackLines());
            assertEquals(PieceType.T.cellValue(), copy.currentPiece().cellValue());
            assertEquals(snapshot.landingRow(), copy.landingRow());
        }
    }

    @Test
    @DisplayName("게임오버 사본에는 현재 블록이 없다")
    void gameOverSnapshotHasNoCurrentPiece() {
        PlayerSession session = new PlayerSession(engine(List.of(O), 0));
        session.start(BattleMode.NORMAL, Difficulty.NORMAL);
        for (int count = 0; count < 12 && !session.isGameOver(); count++) session.apply(GameAction.HARD_DROP);
        assertTrue(session.isGameOver());
        assertTrue(session.snapshot().isGameOver());
        assertNull(session.snapshot().currentPiece());
        assertThrows(IllegalArgumentException.class, () -> new PlayerSnapshot.PieceView(0, 0, 1,
                ItemType.NONE, new int[][] {{0, 0}}, new boolean[2]));
        assertThrows(NullPointerException.class, () -> PlayerSnapshot.of(null, BattleMode.NORMAL));
    }
}
