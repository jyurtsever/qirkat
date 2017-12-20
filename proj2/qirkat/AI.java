package qirkat;

import java.util.ArrayList;
import java.util.Random;

import static qirkat.PieceColor.BLACK;
import static qirkat.PieceColor.WHITE;

/**
 * A Player that computes its own moves.
 *
 * @author Joshua Yurtsever
 */
class AI extends Player {

    /**
     * Maximum minimax search depth before going to static evaluation.
     */
    private static final int MAX_DEPTH = 8;
    /**
     * A position magnitude indicating a win (for white if positive, black
     * if negative).
     */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;
    /**
     * A magnitude greater than a normal value.
     */
    private static final int INFTY = Integer.MAX_VALUE;
    /**
     * The move found by the last call to one of the
     * ...FindMove methods
     * below.
     */
    private Move _lastFoundMove;

    /**
     * A new AI for GAME that will play MYCOLOR. Creates
     * a dumb AI iff DUMB true
     */
    AI(Game game, PieceColor myColor, boolean... dumb) {
        super(game, myColor);
        _dumb = dumb != null && dumb[0];
    }

    @Override
    Move myMove() {
        Main.startTiming();
        Move move = findMove();
        System.out.printf("%s moves %s. \n",
                myColor(), move);
        Main.endTiming();
        return move;
    }

    /**
     * Return a move for me from the current position, assuming there
     * is a move.
     */
    private Move findMove() {
        Board b = new Board(board());
        if (myColor() == WHITE) {
            findMove(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        } else {
            findMove(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /**
     * Find a move from position BOARD and return its value, recording
     * the move found in _lastFoundMove iff SAVEMOVE. The move
     * should have maximal value or have value > BETA if SENSE==1,
     * and minimal value or value < ALPHA if SENSE==-1. Searches up to
     * DEPTH levels.  Searching at level 0 simply returns a static estimate
     * of the board value and does not set _lastMoveFound.
     */
    private int findMove(Board board, int depth, boolean saveMove, int sense,
                         int alpha, int beta) {
        Move best;
        best = null;
        ArrayList<Move> moves = board.getMoves();
        if (_dumb) {
            Random rand = new Random();
            _lastFoundMove = moves.get(rand.nextInt(moves.size()));
            return 0;
        }
        if (depth == 0 || board.gameOver()) {
            return staticScore(board);
        }
        int bestScore = 0;
        for (Move mv : moves) {
            Board hypot = new Board(board);
            hypot.makeMove(mv);
            if (hypot.gameOver()) {
                if (saveMove) {
                    _lastFoundMove = mv;
                }
                return sense * INFTY;
            }
        }
        for (Move mv : moves) {
            Board nextBoard = new Board(board);
            nextBoard.makeMove(mv);
            int findMoveOp = findMove(nextBoard, depth - 1,
                    false, -sense, alpha, beta);
            if (best == null || findMoveOp * sense > sense * bestScore) {
                bestScore = findMoveOp;
                best = mv;
                if (sense == 1) {
                    alpha = Math.max(alpha, bestScore);
                } else if (sense == -1) {
                    beta = Math.min(beta, bestScore);
                }
                if (beta <= alpha) {
                    break;
                }
            }
        }
        if (saveMove) {
            _lastFoundMove = best;
        }
        return bestScore;
    }

    /**
     * Return a heuristic value for BOARD.
     */
    private int staticScore(Board board) {
        if (board.gameOver()) {
            if (board.whoseMove() == WHITE) {
                return -INFTY;
            } else {
                return INFTY;
            }
        }
        int whites = 0;
        int blacks = 0;
        for (PieceColor piece : board.getContents()) {
            if (piece == WHITE) {
                whites += 1;
            }
            if (piece == BLACK) {
                blacks += 1;
            }
        }
        return whites - blacks;
    }
    /** True iff dumb is true. */
    private boolean _dumb;
}
