package qirkat;

import org.junit.Assert;
import org.junit.Test;

public class MoreBoardTests {
    private static final String[] GAME1 = {"c2-c3", "c4-c2", "c1-c3", "a3-c1"};

    private final char[][] boardRepr = new char[][]{
            {'w', 'w', 'b', 'w', 'w'},
            {'w', '-', '-', 'w', 'w'},
            {'-', 'b', 'w', 'w', 'w'},
            {'b', 'b', '-', 'b', 'b'},
            {'b', 'b', 'b', 'b', 'b'}
    };
    private final PieceColor currMove = PieceColor.WHITE;

    /**
     * @return the String representation of the initial state. This will
     * be a string in which we concatenate the values from the bottom of
     * board upwards, so we can pass it into setPieces. Read the comments
     * in Board#setPieces for more information.
     * For our current boardRepr, the String returned
     * by getInitialRepresentation is
     * "  w w w w w\n  w w w w w\n  b b - w w\n  b
     * b b b b\n  b b b b b"
     * We use a StringBuilder to avoid recreating
     * Strings (because Strings
     * are immutable).
     */
    private String getInitialRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("  ");
        for (int i = boardRepr.length - 1; i >= 0; i--) {
            for (int j = 0; j < boardRepr[0].length; j++) {
                sb.append(boardRepr[i][j] + " ");
            }
            sb.deleteCharAt(sb.length() - 1);
            if (i != 0) {
                sb.append("\n  ");
            }
        }
        return sb.toString();
    }


    private Board getBoard() {
        Board b = new Board();
        b.setPieces(getInitialRepresentation(), currMove);
        return b;
    }
    private void resetToInitialState(Board b) {

        b.setPieces(getInitialRepresentation(), currMove);
    }

    @Test
    public void testSomething() {
        Board b = new Board();
        for (String smv : GAME1) {
            b.makeMove(Move.parseMove(smv));
        }
        Assert.assertEquals(getInitialRepresentation(), b.toString());


    }
}
