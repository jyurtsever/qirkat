package qirkat;

import ucb.gui2.LayoutSpec;
import ucb.gui2.TopLevel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

/**
 * The GUI for the Qirkat game.
 *
 * @author Joshua Yurtsever
 */
class GUI extends TopLevel implements Observer, Reporter {

    /* The implementation strategy applied here is to make it as
     * unnecessary as possible for the rest of the program to know that it
     * is interacting with a GUI as opposed to a terminal.
     *
     * To this end, we first have made Board observable, so that the
     * GUI gets notified of changes to a Game's board and can interrogate
     * it as needed, while the Game and Board themselves need not be aware
     * that it is being watched.
     *
     * Second, instead of creating a new API by which the GUI communicates
     * with a Game, we instead simply arrange to make the GUI's input look
     * like that from a terminal, so that we can reuse all the machinery
     * in the rest of the program to interpret and execute commands.  The
     * GUI simply composes commands (such as "start" or "clear") and
     * writes them to a Writer that (using the Java library's PipedReader
     * and PipedWriter classes) provides input to the Game using exactly the
     * same API as would be used to read from a terminal. Thus, a simple
     * Manual player can handle all commands and moves from the GUI.
     *
     * See also Main.java for how this might get set up.
     */

    /**
     * Minimum size of board in pixels.
     */
    private static final int MIN_SIZE = 300;
    /**
     * Contains the drawing logic for the Qirkat model.
     */
    private BoardWidget _widget;
    /**
     * The model of the game.
     */
    private Board _model;
    /**
     * Output sink for sending commands to a game.
     */
    private PrintWriter _out;
    /**
     * Move selected by clicking.
     */
    private Move _selectedMove;
    /**
     * The index of the from square.
     */
    private String from;

    /**
     * A new display observing MODEL, with TITLE as its window title.
     * It uses OUTCOMMANDS to send commands to a game instance, using the
     * same commands as the text format for Qirkat.
     */
    GUI(String title, Board model, Writer outCommands) {
        super(title, true);
        addMenuButton("Game->Quit", this::quit);
        addMenuButton("Options->Seed...", this::setSeed);
        addMenuButton("Game->New Game Auto White", this::autoWhite);
        addMenuButton("Game->New Game Auto Black", this::autoBlack);
        addMenuButton("Options->Seed...", this::setSeed);
        addMenuButton("Game->New Game Easy Auto White", this::easyAutoWhite);
        addMenuButton("Game->New Game Easy Auto Black", this::easyAutoBlack);
        addMenuButton("Game->New Game 2 Players", this::twoPlayer);
        _model = model;
        _widget = new BoardWidget(model);
        _out = new PrintWriter(outCommands, true);
        add(_widget,
                new LayoutSpec("height", "1",
                        "width", "REMAINDER",
                        "ileft", 5, "itop", 5, "iright", 5,
                        "ibottom", 5));
        setMinimumSize(MIN_SIZE, MIN_SIZE);
        _widget.addObserver(this);
        _model.addObserver(this);
    }

    /**
     * Execute the "Quit" button function.
     */
    private synchronized void quit(String unused) {
        _out.printf("quit%n");
    }

    /**
     * Execute the "Auto White" button function.
     */
    private synchronized void autoWhite(String unused) {
        _out.println("clear");
        _out.println("manual black");
        _out.println("auto white");
        _out.printf("start%n");
    }

    /**
     * Execute the "Auto Black" button function.
     */
    private synchronized void autoBlack(String unused) {
        _out.println("clear");
        _out.printf("start%n");
    }

    /**
     * Execute the "Easy Auto Black" button function.
     */
    private synchronized void easyAutoBlack(String unused) {
        _out.println("clear");
        _out.println("auto dumbblack");
        _out.printf("start%n");
    }

    /**
     * Execute the "Easy Auto White" button function.
     */
    private synchronized void easyAutoWhite(String unused) {
        _out.println("clear");
        _out.println("manual black");
        _out.println("auto dumbwhite");
        _out.printf("start%n");
    }

    /**
     * Execute the "Auto White" button function.
     */
    private synchronized void twoPlayer(String unused) {
        _out.println("clear");
        _out.println("manual black");
        _out.printf("start%n");
    }

    /**
     * Execute Seed... command.
     */
    private synchronized void setSeed(String unused) {
        String resp =
                getTextInput("Random Seed", "Get Seed", "question", "");
        if (resp == null) {
            return;
        }
        try {
            long s = Long.parseLong(resp);
            _out.printf("seed %d%n", s);
        } catch (NumberFormatException excp) {
            return;
        }
    }

    /**
     * Display text in file NAME in a box titled TITLE.
     */
    private void displayText(String name, String title) {
        InputStream input =
                Game.class.getClassLoader().getResourceAsStream(name);
        if (input != null) {
            try {
                BufferedReader r
                        = new BufferedReader(new InputStreamReader(input));
                char[] buffer = new char[1 << 15];
                int len = r.read(buffer);
                showMessage(new String(buffer, 0, len), title, "plain");
                r.close();
            } catch (IOException e) {
                /* Ignore IOException */
            }
        }
    }

    @Override
    public void errMsg(String format, Object... args) {
        displayText(String.format(format, args), "Error: ");
    }

    @Override
    public void outcomeMsg(String format, Object... args) {
        displayText(String.format(format, args), "Qirkat: ");
    }

    @Override
    public void moveMsg(String format, Object... args) {
        displayText(String.format(format, args), "Move: ");
    }

    @Override
    public void update(Observable obs, Object arg) {
        if (obs == _model) {
            _widget.update(obs, arg);
        } else if (obs == _widget) {
            ArrayList<Move> moves = new ArrayList<>();
            int k = Move.index(((String) arg).charAt(0),
                    ((String) arg).charAt(1));
            if (_model.jumpPossible()) {
                _model.getJumps(moves, k);
            } else {
                _model.getMoves(moves, k);
            }
            if (from == null) {
                if (moves.size() == 0) {
                    return;
                }
                from = (String) arg;
                _widget.indicateMoves(moves);
                return;
            } else {
                from += "-" + arg;
            }
            for (Move mv : _model.getMoves()) {
                if (mv.toString().equals(from)) {
                    movePiece((String) arg);
                    return;
                } else if (mv.toString().contains(from)) {
                    return;
                }
            }
            from = null;
            update(_model, null);
        }
    }

    /**
     * Respond to a click on SQ.
     */
    private void movePiece(String sq) {
        selectMove(Move.parseMove(from));
        from = null;
    }

    /**
     * Make MOV the user-selected move (no move if null).
     */
    private void selectMove(Move mov) {
        _selectedMove = mov;
        _widget.indicateMove(mov);
        System.out.println(mov);
        _out.println(mov.toString());
        update(_model, null);
    }
}
