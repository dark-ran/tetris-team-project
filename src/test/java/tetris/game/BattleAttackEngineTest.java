package tetris.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tetris.game.BattleTestSupport.engine;
import static tetris.game.BattleTestSupport.line;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tetris.board.Board;
import tetris.piece.ItemType;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;

@DisplayName("대전 공격 줄 생성과 추가")
class BattleAttackEngineTest {
    private static final Tetromino O = new Tetromino(PieceType.O);
    private static final Tetromino I = new Tetromino(PieceType.I);

    @Test
    @DisplayName("2줄을 삭제하면 마지막 블록 칸을 뺀 모양 그대로 공격 줄을 만든다")
    void twoClearedRowsBecomeAttackLinesWithoutTheLastPiece() {
        GameEngine engine = engine(List.of(O), 0, 2, 4, 6, 8);
        engine.newGame();
        for (int count = 0; count < 4; count++) {
            assertTrue(engine.apply(GameAction.HARD_DROP).attackLines().isEmpty());
        }
        GameActionResult result = engine.apply(GameAction.HARD_DROP);
        assertEquals(2, result.clearedRows());
        assertEquals(List.of(line("########.."), line("########..")), result.attackLines());
    }

    @Test
    @DisplayName("1줄 삭제는 공격하지 않는다")
    void singleClearedRowDoesNotAttack() {
        GameEngine engine = engine(List.of(I, I, O), 0, 4, 8);
        engine.newGame();
        engine.apply(GameAction.HARD_DROP);
        engine.apply(GameAction.HARD_DROP);
        GameActionResult result = engine.apply(GameAction.HARD_DROP);
        assertEquals(1, result.clearedRows());
        assertTrue(result.attackLines().isEmpty());
    }

    @Test
    @DisplayName("황금 블록으로 지운 줄은 공격하지 않는다")
    void goldenPieceDoesNotAttack() {
        Tetromino golden = new Tetromino(PieceType.O).withItem(ItemType.GOLDEN);
        GameEngine engine = engine(List.of(O, O, O, O, golden), 0, 2, 4, 6, 8);
        engine.newGame();
        for (int count = 0; count < 4; count++) engine.apply(GameAction.HARD_DROP);
        GameActionResult result = engine.apply(GameAction.HARD_DROP);
        assertEquals(2, result.clearedRows());
        assertTrue(result.attackLines().isEmpty());
    }

    @Test
    @DisplayName("줄 삭제 아이템으로 지운 줄도 공격한다")
    void lineClearItemAttacks() {
        Tetromino marked = new Tetromino(PieceType.O).withItem(ItemType.LINE_CLEAR, 0);
        GameEngine engine = engine(List.of(O, O, O, O, marked), 0, 2, 4, 6, 8);
        engine.newGame();
        for (int count = 0; count < 4; count++) engine.apply(GameAction.HARD_DROP);
        GameActionResult result = engine.apply(GameAction.HARD_DROP);
        assertEquals(2, result.attackLines().size());
    }

    @Test
    @DisplayName("받은 공격 줄은 블록을 고정한 뒤 다음 블록 생성 전에 보드 아래에 회색 칸으로 추가된다")
    void receivedLinesAreAddedAfterTheNextLock() {
        GameEngine engine = engine(List.of(O), 0);
        engine.newGame();
        assertEquals(2, engine.receiveAttack(List.of(line("#########."), line(".#########"))));
        assertEquals(2, engine.state().pendingAttackLines().size());
        assertEquals(Board.EMPTY_CELL, engine.state().board().snapshot()[19][0]);

        GameActionResult result = engine.apply(GameAction.HARD_DROP);
        assertTrue(result.pieceSpawned());
        int[][] cells = engine.state().board().snapshot();
        assertEquals(Board.GARBAGE_CELL, cells[18][0]);
        assertEquals(Board.EMPTY_CELL, cells[18][9]);
        assertEquals(Board.EMPTY_CELL, cells[19][0]);
        assertEquals(PieceType.O.cellValue(), cells[17][0]);
        assertTrue(engine.state().pendingAttackLines().isEmpty());
    }

    @Test
    @DisplayName("공격 줄 때문에 쌓인 블록이 보드 위로 밀려나면 게임오버")
    void overflowFromAttackLinesEndsTheGame() {
        GameEngine engine = engine(List.of(O), 0);
        engine.newGame();
        for (int count = 0; count < 9; count++) engine.apply(GameAction.HARD_DROP);
        engine.receiveAttack(List.of(line("#........."), line("#........."), line("#.........")));
        GameActionResult result = engine.apply(GameAction.HARD_DROP);
        assertTrue(result.becameGameOver());
        assertEquals(GamePhase.GAME_OVER, engine.state().phase());
        assertNull(engine.state().currentPiece());
    }

    @Test
    @DisplayName("새 게임을 시작하면 대기 중인 공격 줄을 비운다")
    void newGameClearsPendingAttacks() {
        GameEngine engine = engine(List.of(O), 0);
        engine.newGame();
        engine.receiveAttack(List.of(line("#.........")));
        engine.newGame();
        assertTrue(engine.state().pendingAttackLines().isEmpty());
    }
}
