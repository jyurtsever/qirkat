package qirkat;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Formatter;
import java.util.Observable;
import java.util.Observer;
import java.util.Stack;

import static qirkat.GameException.error;

import static qirkat.Move.*;
import static qirkat.PieceColor.*;

/**
 * A Qirkat board.   The squares are labeled by column (a char value between
 * 'a' and 'e') and row (a char value between '1' and '5'.
 *
 * For some purposes, it is useful to refer to squares using a single
 * integer, which we call its "linearized index".  This is simply the
 * number of the square in row-major order (with row 0 being the bottom row)
 * counting from 0).
 *
 * Moves on this board are denoted by Moves.
 *
 * @author Joshua Yurtsever
 */
class Board extends Observable {
    /**
     * Stores the setup version of a board.
     */
    private String spec = "w w w w w w w w w w b b - w w b b b b b b b b b b";

    /**
     * A new, cleared board at the start of the game.
     */
    Board() {
        _history = new Stack<>();
        clear();
        _history.add(new Board(this));

    }

    /**
     * A copy of B.
     */
    Board(Board b) {
        internalCopy(b);
    }

    /**
     * Return a constant view of me (allows any access method, but no
     * method that modifies it).
     */
    Board constantView() {
        return this.new ConstantBoard();
    }

    /**
     * Returns true iff the boards have the same contest and the same
     * whose move.
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof Board && toString().equals(o.toString())
                && whoseMove() == ((Board) o).whoseMove()
                && ((Board) o)._history.equals(_history);
    }
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Clear me to my starting state, with pieces in their initial
     * positions.
     */
    void clear() {
        _whoseMove = WHITE;
        _gameOver = false;
        draws = new Move[MAX_INDEX + 1];
        setPieces(spec, _whoseMove);

        setChanged();
        notifyObservers();
    }

    /**
     * Returns my _CONTENTS.
     */
    public PieceColor[] getContents() {
        return _contents;
    }

    /**
     * Copy B into me.
     */
    void copy(Board b) {
        internalCopy(b);
    }

    /**
     * Copy B into me.
     */
    private void internalCopy(Board b) {
        _contents = b.getContents().clone();
        _whoseMove = b.whoseMove();
        _history = b._history;
        draws = b.draws.clone();
    }

    /**
     * Set my contents as defined by STR.  STR consists of 25 characters,
     * each of which is b, w, or -, optionally interspersed with whitespace.
     * These give the contents of the Board in row-major order, starting
     * with the bottom row (row 1) and left column (column a). All squares
     * are initialized to allow horizontal movement in either direction.
     * NEXTMOVE indicates whose move it is.
     */
    void setPieces(String str, PieceColor nextMove) {
        if (nextMove == EMPTY || nextMove == null) {
            throw new IllegalArgumentException("bad player color");
        }
        str = str.replaceAll("\\s", "");
        if (!str.matches("[bw-]{25}")) {
            throw new IllegalArgumentException("bad board description");
        }
        _contents = new PieceColor[MAX_INDEX + 1];
        for (int k = 0; k < str.length(); k += 1) {
            switch (str.charAt(k)) {
            case '-':
                set(k, EMPTY);
                break;
            case 'b':
            case 'B':
                set(k, BLACK);
                break;
            case 'w':
            case 'W':
                set(k, WHITE);
                break;
            default:
                break;
            }
        }

        _whoseMove = nextMove;
        setChanged();
        notifyObservers();
    }

    /**
     * Return true iff the game is over: i.e., if the current player has
     * no moves.
     */
    boolean gameOver() {
        return _gameOver;
    }

    /**
     * Return the current contents of square C R, where 'a' <= C <= 'e',
     * and '1' <= R <= '5'.
     */
    PieceColor get(char c, char r) {
        assert validSquare(c, r);
        return get(index(c, r));
    }

    /**
     * Return the current contents of the square at linearized index K.
     */
    PieceColor get(int k) {
        assert validSquare(k);
        if (!validSquare(k)) {
            throw error("not valid square");
        }
        return _contents[k];
    }

    /**
     * Set get(C, R) to V, where 'a' <= C <= 'e', and
     * '1' <= R <= '5'.
     */
    private void set(char c, char r, PieceColor v) {
        assert validSquare(c, r);
        set(index(c, r), v);
    }

    /**
     * Set get(K) to V, where K is the linearized index of a square.
     */
    private void set(int k, PieceColor v) {
        assert validSquare(k);
        _contents[k] = v;
    }

    /**
     * Return true iff MOV is legal on the current board.
     */
    boolean legalMove(Move mov) {
        if (checkDraw(mov)) {
            return false;
        }
        boolean cond;
        cond = (get(mov.fromIndex()) == whoseMove());
        cond = cond && get(mov.toIndex()) == EMPTY;
        char base = get(mov.fromIndex()) == WHITE ? '1' : '5';
        if (!mov.isJump() && cond
                && (mov.row0() - base) * (mov.row0() - base)
                <= (mov.row1() - base) * (mov.row1() - base)) {
            if (whoseMove() == WHITE && mov.fromIndex() / 5 == 4) {
                return false;
            }
            if (whoseMove() == BLACK && mov.fromIndex() / 5 == 0) {
                return false;
            }
            int deltaRowSquare = (mov.row1() - mov.row0())
                    * (mov.row1() - mov.row0());
            int deltaColSquare = (mov.col1() - mov.col0())
                    * (mov.col1() - mov.col0());
            int modulusSqaure = deltaColSquare + deltaRowSquare;
            if (jumpPossible()) {
                throw error("invalid move: jump possible");
            }
            if (mov.fromIndex() % 2 == 0
                    && deltaColSquare <= 1 && deltaRowSquare <= 1) {
                return true;
            }
            if (mov.fromIndex() % 2 == 1 && modulusSqaure
                    < 2 && modulusSqaure > 0) {
                return true;
            }
        } else if (mov.isJump() && cond) {
            return checkJump(mov, false);
        }
        return false;
    }

    /**
     * Checks if a draw occurs as a result of MOV. Returns true
     * or false
     */
    boolean checkDraw(Move mov) {
        if (mov.isJump() || (!mov.isLeftMove() && !mov.isRightMove())) {
            return false;
        }
        if (draws[mov.fromIndex()] != null) {
            if (mov.equals(draws[mov.fromIndex()])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return TRUE iff there is a legal move from K.
     */
    boolean movePossible(int k) {
        int dir;
        if (_contents[k] != whoseMove()) {
            return false;
        } else if (whoseMove() == WHITE && k / 5 == 4) {
            return false;
        } else if (whoseMove() == BLACK && k / 5 == 0) {
            return false;
        }
        dir = _contents[k] == WHITE ? 1 : -1;
        int above = k + dir * 5;
        return checkValidDest(above) || (movePossibleHelper(k, k)
                || (k % 2 == 0 && movePossibleHelper(k, above)));
    }

    /**
     * Return TRUE iff there is a legal move in the row of R from
     * index O.
     */
    private boolean movePossibleHelper(int o, int r) {
        for (int i = -1; i <= 1; i += 2) {
            if (o % 5 == 0 && i == -1) {
                continue;
            }
            if (o % 5 == 4 && i == 1) {
                return false;
            }
            if (checkValidDest(r + i) && validSquare(o)) {
                if (!checkDraw(move(o, r + i))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return TRUE iff there is a legal move at D.
     */
    private boolean checkValidDest(int d) {
        return d < MAX_INDEX + 1 && d >= 0 && _contents[d].equals(EMPTY);
    }

    /**
     * Return a list of all legal moves from the current position.
     */
    ArrayList<Move> getMoves() {
        ArrayList<Move> result = new ArrayList<>();
        getMoves(result);
        return result;
    }

    /**
     * Add all legal moves from the current position to MOVES.
     */
    void getMoves(ArrayList<Move> moves) {
        if (gameOver()) {
            System.out.println("242 game over");
            return;
        }
        if (jumpPossible()) {
            for (int k = 0; k <= MAX_INDEX; k += 1) {
                if (get(k) != whoseMove()) {
                    continue;
                }
                getJumps(moves, k);
            }
        } else {
            for (int k = 0; k <= MAX_INDEX; k += 1) {
                if (get(k) != whoseMove()) {
                    continue;
                } else if (whoseMove() == WHITE && k / 5 == 4) {
                    continue;
                } else if (whoseMove() == BLACK && k / 5 == 0) {
                    continue;
                }
                getMoves(moves, k);
            }
        }
    }

    /**
     * Add all legal non-capturing moves from the position
     * with linearized index K to MOVES.
     */
    public void getMoves(ArrayList<Move> moves, int k) {
        int dir;
        if (_contents[k] != whoseMove()) {
            return;
        }
        dir = whoseMove() == WHITE ? 1 : -1;
        int above = k + dir * 5;
        getMovesHelper(moves, k, k);
        moveAddIf(moves, k, above);
        if (k % 2 == 0) {
            getMovesHelper(moves, k, above);
        }
    }

    /**
     * Checks the left and right of position ABOVE if the left or right
     * exists and is valid then adds to MOVES if move valid from K.
     */
    private void getMovesHelper(ArrayList<Move> moves, int k, int above) {
        for (int i = -1; i <= 1; i += 2) {
            if (k % 5 == 0 && i == -1) {
                continue;
            }
            if (k % 5 == 4 && i == 1) {
                break;
            }
            moveAddIf(moves, k, above + i);
        }
    }

    /**
     * Checkes if CAND satisfies conditions to be a valid move
     * from O and adds it to MOVES if it is. Assumes cand is adjacent.
     */
    private void moveAddIf(ArrayList<Move> moves, int o, int cand) {
        if (cand < 0 || cand > MAX_INDEX) {
            return;
        }
        if (_contents[cand] == EMPTY) {
            Move mv = move(o, cand);
            if (!checkDraw(mv)) {
                moves.add(move(o, cand));
            }
        }
    }

    /**
     * Add all legal captures from the position with linearized index K
     * to MOVES.
     */
    public void getJumps(ArrayList<Move> moves, int k) {
        Board hypot = new Board(this);
        if (!hypot.get(k).equals(whoseMove())) {
            return;
        }
        ArrayList<Move> res = getJumpsHelper(k, hypot);
        if (res != null) {
            moves.addAll(res);
        }

    }
    /**
     * Takes in K and HYPOT to get jumps recursively.
     * Returns a list of moves.
     */
    private ArrayList<Move> getJumpsHelper(int k, Board hypot) {
        ArrayList<Move> result = new ArrayList<>();
        boolean changed = false;
        for (int s = -10; s <= 10; s += 10) {
            for (int i = -2; i <= 2; i += 2) {
                int toInd = s + i + k;
                if ((i == s && s == 0) || !validSquare(toInd)
                        || (k % 2 == 1 && i * i == 4 && s != 0)) {
                    continue;
                }
                if (i == 2 && k % 5 >= 3) {
                    continue;
                }
                if (i == -2 && k % 5 <= 1) {
                    continue;
                }
                int jumpInd = k + s / 2 + i / 2;
                if (hypot.get(jumpInd) == hypot.get(k).opposite()
                        && hypot.get(toInd) == EMPTY) {
                    Board branch = new Board(hypot);
                    branch.set(jumpInd, EMPTY);
                    branch.set(toInd, branch.get(k));
                    branch.set(k, EMPTY);
                    result.addAll(concatTails(move(k, toInd),
                            getJumpsHelper(toInd, branch)));
                    changed = true;
                }

            }
        }
        if (changed) {
            return result;
        }
        return null;

    }
    /** Concatonates tails into one move given HEAD, TAILS. Returns
     * a list of moves.
     */
    private ArrayList<Move> concatTails(Move head, ArrayList<Move> tails) {
        if (tails == null) {
            ArrayList<Move> res = new ArrayList<>();
            res.add(head);
            return res;
        }
        for (int i = 0; i < tails.size(); i++) {
            tails.set(i, move(head, tails.get(i)));
        }
        return tails;
    }

    /**
     * Return true iff MOV is a valid jump sequence on the current board.
     * MOV must be a jump or null.  If ALLOWPARTIAL, allow jumps that
     * could be continued and are valid as far as they go.
     */
    boolean checkJump(Move mov, boolean allowPartial) {
        if (mov == null) {
            return true;
        }
        if (get(mov.fromIndex()) == EMPTY) {
            return false;
        }
        hypo = new Board(this);
        return checkJumpHelper(mov, allowPartial, hypo);
    }

    /**
     * Make one jump MOV given BRD assumes that jump is valid.
     */
    private void oneJump(Move mov, Board brd) {
        brd.set(mov.toIndex(), brd.get(mov.fromIndex()));
        brd.set(mov.fromIndex(), EMPTY);
        brd.set(mov.jumpedIndex(), EMPTY);
    }

    /**
     * Return true iff MOV is a valid jump sequence on the current board.
     * MOV must be a jump or null and give HYPOT.
     * If ALLOWPARTIAL, allow jumps that
     * could be continued and are valid as far as they go.
     */
    private boolean checkJumpHelper(Move mov, boolean allowPartial,
                                    Board hypot) {
        if (mov == null) {
            return true;
        }
        if (hypot.get(mov.toIndex()) != EMPTY
                || hypot.get(mov.fromIndex()) == EMPTY) {
            return false;
        }
        PieceColor color = hypot.get(mov.fromIndex());
        int rowSq = (mov.row1() - mov.row0())
                * (mov.row1() - mov.row0());
        int colSq = (mov.col1() - mov.col0())
                * (mov.col1() - mov.col0());
        if ((rowSq == colSq && rowSq == 4) || rowSq + colSq == 4) {
            int jumped = mov.jumpedIndex();
            if (hypot.get(jumped) == color.opposite()) {
                oneJump(mov, hypot);
                if (mov.jumpTail() == null) {
                    if (allowPartial) {
                        return true;
                    } else {
                        return !hypot.jumpPossible(mov.toIndex());
                    }
                }
                return checkJumpHelper(mov.jumpTail(), allowPartial, hypot);
            }
        }
        return false;
    }

    /**
     * Return true iff a jump is possible for a piece at position C R.
     */
    boolean jumpPossible(char c, char r) {
        return jumpPossible(index(c, r));
    }

    /**
     * Return true iff a jump is possible for a piece at position with
     * linearized index K.
     */
    boolean jumpPossible(int k) {
        if (get(k) != whoseMove()) {
            return false;
        }
        PieceColor color;
        color = get(k);
        int to;
        for (int r = -10; r <= 10; r += 10) {
            for (int c = -2; c <= 2; c += 2) {
                to = k + r + c;
                if (c == 2 && k % 5 >= 3) {
                    continue;
                }
                if (c == -2 && k % 5 <= 1) {
                    continue;
                }
                if (0 <= to && to <= MAX_INDEX && !(r == 0 && c == 0)
                        && !(r * r == 100 && c * c == 4 && k % 2 == 1)
                        && get(to) == EMPTY && color != EMPTY) {
                    int jumped = index((char) ((col(k) + col(to)) / 2),
                            (char) ((row(k) + row(to)) / 2));
                    if (get(jumped) == color.opposite()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    /** Return true iff a jump is possible for a piece at position with
     *  linearized index K. */

    /**
     * Return true iff a jump is possible from the current board.
     */
    boolean jumpPossible() {
        for (int k = 0; k <= MAX_INDEX; k += 1) {
            if (jumpPossible(k)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the color of the player who has the next move.  The
     * value is arbitrary if gameOver().
     */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /**
     * Perform the move C0R0-C1R1, or pass if C0 is '-'.  For moves
     * other than pass, assumes that legalMove(C0, R0, C1, R1).
     */
    void makeMove(char c0, char r0, char c1, char r1) {
        makeMove(move(c0, r0, c1, r1, null));
    }

    /**
     * Make the multi-jump C0 R0-C1 R1..., where NEXT is C1R1....
     * Assumes the result is legal.
     */
    void makeMove(char c0, char r0, char c1, char r1, Move next) {
        makeMove(move(c0, r0, c1, r1, next));
    }

    /**
     * Make the Move MOV on this Board, assuming it is legal.
     */
    void makeMove(Move mov) {
        if (_contents[mov.fromIndex()] != whoseMove()) {
            throw error("invalid move: %s's move", whoseMove());
        }
        if (!legalMove(mov)) {
            throw error("invalid move: enter another");
        }

        _history.add(new Board(this));
        if (draws[mov.fromIndex()] != null) {
            draws[mov.fromIndex()] = null;
        }
        if (!mov.isJump()) {
            if (jumpPossible(mov.fromIndex())) {
                throw error("invalid move: jump possible");
            }
            _contents[mov.toIndex()] = _contents[mov.fromIndex()];
            _contents[mov.fromIndex()] = EMPTY;

            if (mov.isRightMove() || mov.isLeftMove()) {
                draws[mov.toIndex()]
                        = move(mov.toIndex() , mov.fromIndex());
            }
        } else {
            hypo.draws = this.draws;
            copy(hypo);
        }
        switchPlayer();
        checkGameOver();
        setChanged();
        notifyObservers();
    }

    /**
     * Undo the last move, if any.
     */
    void undo() {
        try {
            copy(_history.pop());
            setChanged();
            notifyObservers();
        } catch (EmptyStackException exp) {
            throw error("Cannot undo anymore");
        }
    }

    @Override
    public String toString() {
        return toString(false);
    }

    /**
     * Return a text depiction of the board.  If LEGEND, supply row and
     * column numbers around the edges.
     */
    String toString(boolean legend) {
        Formatter out = new Formatter();
        String[] row;
        int dim = _contents.length / 5;
        for (int r = dim - 1; r >= 0; r--) {
            if (legend) {
                row = new String[dim + 1];
                row[0] = Integer.toString(r + 1);
            } else {
                row = new String[dim];
            }
            for (int c = 0; c < dim; c++) {
                int ind = c;
                if (legend) {
                    ind += 1;
                }
                row[ind] = get(r * 5 + c).shortName();
            }
            if (legend) {
                out.format("  %s %s %s %s %s %s", (Object[]) row);
            } else {
                out.format("  %s %s %s %s %s", (Object[]) row);
            }
            if (r != 0) {
                out.format("\n");
            }
        }
        if (legend) {
            out.format("\n    a b c d e \n");
        }
        return out.toString();
    }

    /**
     * Return true iff there is a move for the current player.
     */
    private boolean isMove() {
        for (int i = 0; i < _contents.length; i++) {
            if (movePossible(i) || jumpPossible(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stores the hypothetical board.
     */
    private Board hypo;

    /**
     * Switches player.
     */
    public void switchPlayer() {
        _whoseMove = whoseMove().opposite();
    }

    /**
     * Player that is on move.
     */
    private PieceColor _whoseMove;

    /**
     * Set true when game ends.
     */
    private boolean _gameOver;

    /**
     * Contents of the board in a linear fashion.
     */
    private PieceColor[] _contents;

    /**
     * Stores the contents of the board's history.
     */
    private Stack<Board> _history;

    /**
     * Convenience value giving values of pieces at each ordinal position.
     */
    static final PieceColor[] PIECE_VALUES = PieceColor.values();

    /**
     * Sets GAMEOVER to true if there are no moves.
     */
    public void checkGameOver() {
        _gameOver = !isMove();
    }

    /** Sets the variable at index IND of draws to MV.
     */
    public void setDraws(Move mv, int ind) {
        draws[ind] = mv;
    }
    /**
     * Keeps track of draws.
     */
    private Move[] draws;


    /**
     * One cannot create arrays of ArrayList<Move>, so we introduce
     * a specialized private list type for this purpose.
     */
    static class MoveList extends ArrayList<Move> {
    }

    /**
     * A read-only view of a Board.
     */
    private class ConstantBoard extends Board implements Observer {
        /**
         * A constant view of this Board.
         */
        ConstantBoard() {
            super(Board.this);
            Board.this.addObserver(this);
        }

        @Override
        void copy(Board b) {
            assert false;
        }

        @Override
        void clear() {
            assert false;
        }

        @Override
        void makeMove(Move move) {
            assert false;
        }

        /**
         * Undo the last move.
         */
        @Override
        void undo() {
            assert false;
        }

        @Override
        public void update(Observable obs, Object arg) {
            super.copy((Board) obs);
            setChanged();
            notifyObservers(arg);
        }
    }
}
