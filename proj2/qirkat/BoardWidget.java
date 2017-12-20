package qirkat;

import ucb.gui2.Pad;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import static qirkat.PieceColor.*;

/**
 * Widget for displaying a Qirkat board.
 *
 * @author Joshua Yurtsever
 */
class BoardWidget extends Pad implements Observer {

    /**
     * Length of side of one square, in pixels.
     */
    static final int SQDIM = 50;
    /**
     * Number of squares on a side.
     */
    static final int SIDE = Move.SIDE;
    /**
     * Radius of circle representing a piece.
     */
    static final int PIECE_RADIUS = 15;

    /**
     * Color of white pieces.
     */
    private static final Color WHITE_COLOR = Color.WHITE;
    /** Color of "phantom" white pieces. */
    /**
     * Color of black pieces.
     */
    private static final Color BLACK_COLOR = Color.BLACK;
    /**
     * Color of painted lines.
     */
    private static final Color LINE_COLOR = Color.BLACK;
    /**
     * Color of blank squares.
     */
    private static final Color BLANK_COLOR = new Color(100, 100, 100);

    /**
     * Stroke for lines..
     */
    private static final BasicStroke LINE_STROKE = new BasicStroke(1.0f);

    /**
     * Stroke for outlining pieces.
     */
    private static final BasicStroke OUTLINE_STROKE = LINE_STROKE;

    /**
     * Model being displayed.
     */
    private static Board _model;
    /**
     * Dimension of current drawing surface in pixels.
     */
    private int _dim;
    /**
     * A partial Move indicating selected squares.
     */
    private Move _selectedMove;

    /**
     * list containing the possible moves from a square.
     */
    private ArrayList<Move> _possibleMoves;

    /**
     * A new widget displaying MODEL.
     */
    BoardWidget(Board model) {
        _model = model;
        setMouseHandler("click", this::readMove);
        _model.addObserver(this);
        _dim = SQDIM * SIDE;
        setPreferredSize(_dim, _dim);
    }

    /**
     * Indicate that the squares indicated by MOV are the currently selected
     * squares for a pending move.
     */
    void indicateMove(Move mov) {
        _selectedMove = mov;
        repaint();
    }

    /**
     * Indicates the moves possible from a square give MOVES.
     */
    void indicateMoves(ArrayList<Move> moves) {
        _possibleMoves = moves;
        repaint();
    }

    @Override
    public synchronized void paintComponent(Graphics2D g) {
        g.setColor(BLANK_COLOR);
        g.fillRect(0, 0, _dim, _dim);
        g.setColor(LINE_COLOR);
        g.setStroke(LINE_STROKE);
        int off = _dim / (2 * (SIDE));
        int d = _dim - off;
        g.drawLine(off, off, d, d);
        g.drawLine(d, off, off, d);
        g.drawLine((d + off) / 2, off, off, (d + off) / 2);
        g.drawLine(d, (d + off) / 2, (d + off) / 2, off);
        g.drawLine(off, (d + off) / 2, (d + off) / 2, d);
        g.drawLine((d + off) / 2, d, d, (d + off) / 2);
        for (int i = off; i <= d; i += (d - off) / (SIDE - 1)) {
            g.drawLine(off, i, d, i);
            g.drawLine(i, off, i, d);
        }
        for (int i = 0; i <= Move.MAX_INDEX; i++) {
            PieceColor curr = _model.get(i);
            Color piece = null;
            if (curr == EMPTY) {
                continue;
            } else if (curr == WHITE) {
                piece = WHITE_COLOR;
            } else if (curr == BLACK) {
                piece = BLACK_COLOR;
            }
            g.setColor(piece);
            fillIndex(i, g);
        }
        if (_possibleMoves != null) {
            for (Move mv : _possibleMoves) {
                g.setColor(Color.blue);
                while (mv.jumpTail() != null) {
                    fillIndex(mv.toIndex(), g);
                    mv = mv.jumpTail();
                }
                g.setColor(Color.green);
                fillIndex(mv.toIndex(), g);
            }
            _possibleMoves = null;
        }
        if (_model.gameOver()) {
            Font font = new Font("Serif", Font.PLAIN, 11 * 4);
            g.setFont(font);
            g.setColor(Color.red);
            g.drawString("Game over !!!", 0, _dim / 2);
        }
    }

    /**
     * Fills the square of linear index K give graphics G.
     */
    private void fillIndex(int k, Graphics2D g) {
        int y = Move.MAX_INDEX - k;
        int offset = (_dim / SIDE - 2 * PIECE_RADIUS) / 2;
        g.fillOval(k % SIDE * _dim / SIDE + offset,
                offset + y / SIDE * _dim / SIDE, PIECE_RADIUS * 2,
                PIECE_RADIUS * 2);
    }

    /**
     * Notify observers of mouse's current position from click event WHERE.
     */
    private void readMove(String unused, MouseEvent where) {
        if (_possibleMoves != null) {
            _possibleMoves = null;
        }
        int x = where.getX(), y = where.getY();
        char mouseCol, mouseRow;
        if (where.getButton() == MouseEvent.BUTTON1) {
            mouseCol = (char) (x / SQDIM + 'a');
            mouseRow = (char) ((SQDIM * SIDE - y) / SQDIM + '1');
            if (mouseCol >= 'a' && mouseCol <= 'g'
                    && mouseRow >= '1' && mouseRow <= '7') {
                setChanged();
                notifyObservers("" + mouseCol + mouseRow);
            }
        }
    }

    @Override
    public synchronized void update(Observable model, Object arg) {
        repaint();
    }
}
