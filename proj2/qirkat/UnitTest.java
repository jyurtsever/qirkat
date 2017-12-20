package qirkat;

import org.junit.Test;
import ucb.junit.textui;

import static org.junit.Assert.assertEquals;

/**
 * The suite of all JUnit tests for the qirkat package.
 *
 * @author
 */
public class UnitTest {

    /**
     * Run the JUnit tests in this package. Add xxxTest.class entries to
     * the arguments of runClasses to run other JUnit tests.
     */
    public static void main(String[] ignored) {
        System.exit(textui.runClasses(MoveTest.class, BoardTest.class,
                CommandTest.class));
    }

    @Test
    public void indexTest() {
        System.out.println(Move.index('A', '2'));
    }

    @Test
    public void testToString() {
        String initial = "w w w w w w w w w w b b - w w b b b b b b b b b b";
        Board b = new Board();
        b.setPieces(initial, PieceColor.WHITE);
        System.out.println(b.toString(true));
    }

    @Test
    public void testJumpedRow() {
        Move mv = Move.move('a', '1', 'c', '3');
        Move mv2 = Move.move('a', '1', 'a', '2');
        Move mv3 = Move.move('c', '3', 'e', '1');
        Move mv4 = Move.move('c', '3', 'd', '3');
        System.out.println(Move.col(24));
        System.out.println(Move.index('a', '1'));

        assertEquals('b', mv.jumpedCol());
        assertEquals('a', mv2.jumpedCol());
        assertEquals('2', mv.jumpedRow());
        assertEquals('2', mv2.jumpedRow());
        assertEquals('2', mv3.jumpedRow());
        assertEquals(false, mv3.isLeftMove());
        assertEquals(false, mv4.isLeftMove());
        assertEquals(true, mv4.isRightMove());

    }

    @Test
    public void testLegalMove() {
        Move mv = Move.move('a', '1', 'c', '3');
        Move mv2 = Move.move('a', '1', 'a', '2');
        Move mv3 = Move.move('c', '3', 'e', '1');
        Move mv4 = Move.move('c', '3', 'd', '3');
        String initial =
                  "w w w w w "
                + "w w - w w "
                + "b b - - w "
                + "b - b b b "
                + "b b b b b";
        Board b = new Board();
        b.setPieces(initial, PieceColor.WHITE);
        assertEquals(false, b.legalMove(Move.move('a', '1', 'b', '1')));
        assertEquals(false, b.legalMove(Move.move('b', '1', 'c', '2')));
        assertEquals(true, b.legalMove(Move.move('c', '1', 'c', '2')));
        assertEquals(true, b.legalMove(Move.move('d', '2', 'c', '3')));

    }

    @Test
    public void testParseMove() {
        Move a = Move.parseMove("b2-b4-d2-d4");
        System.out.println(a.jumpTail().row0());
    }

    @Test
    public void testDraw() {
        Board board = new Board();
        board.clear();
        board.setPieces("--bbb w---- ----- ----- -----", PieceColor.BLACK);
        board.setDraws(Move.move(2, 1), 2);
        board.checkGameOver();
        assertEquals(true, board.gameOver());

    }
    @Test
    public void testMoveEquals() {
        Move m1 = Move.move(1, 2);
        Move m2 = Move.move(1, 2);
        System.out.println(m1.equals(m2));
    }

}


