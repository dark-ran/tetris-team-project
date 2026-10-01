package tetris.game;

import tetris.piece.Tetromino;

/** 시작 위치를 행동 처리와 분리한다. 변경할 생성 규칙은 이 인터페이스로 전달한다. */
public interface PieceSpawnPolicy {
    /** 회전 기준 정사각형의 왼쪽 위가 놓이는 보드 행을 반환한다. */
    int startingRow(Tetromino piece);

    /** 회전 기준 정사각형의 왼쪽 위가 놓이는 보드 열을 반환한다. */
    int startingColumn(Tetromino piece);
}
