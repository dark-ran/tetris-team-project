package tetris.piece;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class QueuedPieceTest {
    @Test void previewAndSpawnUseTheSameImmutablePiece() {
        Tetromino next = new Tetromino(PieceType.T).rotateClockwise();
        PieceQueue queue = PieceQueue.generatedBy(() -> next, 1);
        var preview = queue.previewPieces();
        assertThrows(UnsupportedOperationException.class, preview::clear);
        assertSame(next, preview.getFirst());
        assertSame(preview.getFirst(), queue.nextPiece());
        assertSame(next, preview.getFirst());
    }
}
