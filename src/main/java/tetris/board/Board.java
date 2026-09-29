package tetris.board;

import tetris.piece.Tetromino;

/** 20행 10열 보드의 배치 검사와 블록 고정을 담당한다. */
public class Board {
    public static final int ROWS = 20;
    public static final int COLS = 10;

    public void reset() {
        // TODO(Req1): 보드 초기화
        throw new UnsupportedOperationException("TODO: 보드 초기화");
    }

    public boolean canPlace(Tetromino piece, int row, int col) {
        // TODO(Req1): 블록 배치 가능 여부 검사
        throw new UnsupportedOperationException("TODO: 블록 배치 가능 여부 검사");
    }

    public void lock(Tetromino piece, int row, int col) {
        // TODO(Req1): 블록 고정
        throw new UnsupportedOperationException("TODO: 블록 고정");
    }

    public int clearFullRows() {
        // TODO(Req1): 완성된 행 삭제
        throw new UnsupportedOperationException("TODO: 완성된 행 삭제");
    }

    public int[][] snapshot() {
        // TODO(Req1): 보드 상태 조회; 셀 표현 및 복사 정책은 팀 리뷰 필요
        throw new UnsupportedOperationException("TODO: 보드 상태 조회; 셀 표현 및 복사 정책은 팀 리뷰 필요");
    }
}
