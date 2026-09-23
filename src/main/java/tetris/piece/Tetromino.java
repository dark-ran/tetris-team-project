package tetris.piece;

/** 테트로미노 타입과 좌표, 회전 API. 좌표 표현과 회전 정책은 팀 리뷰가 필요하다. */
public class Tetromino {
    public PieceType type() {
        // TODO(Req1): 블록 종류 조회
        throw new UnsupportedOperationException("TODO: 블록 종류 조회");
    }

    public int[][] cells() {
        // TODO(Req1): 블록 셀 좌표 조회
        throw new UnsupportedOperationException("TODO: 블록 셀 좌표 조회");
    }

    public Tetromino rotateClockwise() {
        // TODO(Req1): 시계방향 회전; 새 객체 반환 여부 및 벽 근처 정책은 미정
        throw new UnsupportedOperationException("TODO: 시계방향 회전; 새 객체 반환 여부 및 벽 근처 정책은 미정");
    }
}
