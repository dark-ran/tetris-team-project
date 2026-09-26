package tetris.game;

import java.util.List;
import tetris.board.Board;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;

/** 게임 상태의 단일 원본. UI는 이 상태를 표시하며 별도의 게임 상태 원본을 만들지 않는다. */
public class GameState {
    public Board board() {
        // TODO(Req1): 보드 조회
        throw new UnsupportedOperationException("TODO: 보드 조회");
    }

    public Tetromino currentPiece() {
        // TODO(Req1): 현재 블록 조회
        throw new UnsupportedOperationException("TODO: 현재 블록 조회");
    }

    public long score() {
        // TODO(Req1): 현재 점수 조회
        throw new UnsupportedOperationException("TODO: 현재 점수 조회");
    }

    public GamePhase phase() {
        // TODO(Req1): 게임 진행 상태 조회
        throw new UnsupportedOperationException("TODO: 게임 진행 상태 조회");
    }

    public List<PieceType> nextPieces() {
        // TODO(Req1): 다음 블록 목록 조회
        throw new UnsupportedOperationException("TODO: 다음 블록 목록 조회");
    }
}
