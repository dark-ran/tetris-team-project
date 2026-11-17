package tetris.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tetris.game.BattleTestSupport.line;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("공격 줄 대기열의 10줄 제한")
class AttackQueueTest {
    private static List<AttackLine> lines(int count, String pattern) {
        return Collections.nCopies(count, line(pattern));
    }

    @Test
    @DisplayName("여러 번 받은 줄은 기존 줄의 아래쪽에 쌓인다")
    void laterAttacksGoBelowEarlierOnes() {
        AttackQueue queue = new AttackQueue();
        queue.offer(lines(2, "#########."));
        queue.offer(lines(1, ".#########"));
        assertEquals(List.of(line("#########."), line("#########."), line(".#########")), queue.pending());
    }

    @Test
    @DisplayName("합쳐서 10줄을 넘으면 새 줄의 아래쪽을 잘라낸다")
    void overflowCutsTheBottomOfTheNewLines() {
        AttackQueue queue = new AttackQueue();
        assertEquals(9, queue.offer(lines(9, "#########.")));
        assertEquals(1, queue.offer(List.of(line("##########"), line("#........."))));
        assertEquals(10, queue.size());
        assertEquals(line("##########"), queue.pending().getLast());
    }

    @Test
    @DisplayName("이미 10줄이면 새 공격은 무시한다")
    void fullQueueIgnoresNewAttacks() {
        AttackQueue queue = new AttackQueue();
        queue.offer(lines(10, "#########."));
        assertEquals(0, queue.offer(lines(2, ".#########")));
        assertEquals(10, queue.size());
    }

    @Test
    @DisplayName("꺼내면 위에서 아래 순서로 반환하고 비운다")
    void drainReturnsInOrderAndEmpties() {
        AttackQueue queue = new AttackQueue();
        queue.offer(List.of(line("#........."), line(".#........")));
        assertEquals(List.of(line("#........."), line(".#........")), queue.drain());
        assertEquals(0, queue.size());
        queue.offer(lines(1, "#........."));
        queue.clear();
        assertTrue(queue.pending().isEmpty());
    }

    @Test
    @DisplayName("잘못된 입력은 거부한다")
    void rejectsInvalidInput() {
        AttackQueue queue = new AttackQueue();
        assertThrows(NullPointerException.class, () -> queue.offer(null));
        List<AttackLine> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(NullPointerException.class, () -> queue.offer(withNull));
        assertThrows(IllegalArgumentException.class, () -> new AttackLine(new boolean[3]));
    }

    @Test
    @DisplayName("공격 줄은 사본을 보관하고 값으로 비교한다")
    void attackLineCopiesAndComparesByValue() {
        boolean[] filled = new boolean[10];
        filled[0] = true;
        AttackLine attack = new AttackLine(filled);
        filled[1] = true;
        assertFalse(attack.isFilled(1));
        attack.filled()[2] = true;
        assertFalse(attack.isFilled(2));
        assertEquals(line("#........."), attack);
        assertEquals(line("#.........").hashCode(), attack.hashCode());
        assertFalse(attack.equals("#"));
        assertEquals("AttackLine[#.........]", attack.toString());
        assertTrue(Arrays.equals(attack.filled(), line("#.........").filled()));
    }
}
