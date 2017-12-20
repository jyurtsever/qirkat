package qirkat;

/* Author: P. N. Hilfinger */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;

import static qirkat.Command.Type.*;
import static qirkat.Game.State.PLAYING;
import static qirkat.Game.State.SETUP;
import static qirkat.GameException.error;
import static qirkat.PieceColor.BLACK;
import static qirkat.PieceColor.WHITE;

/**
 * Controls the play of the game.
 *
 * @author Joshua Yurtsever
 */
class Game {

    /**
     * States of play.
     */
    static enum State {
        SETUP, PLAYING;
    }

    /**
     * A new Game, using BOARD to play on, reading initially from
     * BASESOURCE and using REPORTER for error and informational messages.
     */
    Game(Board board, CommandSource baseSource, Reporter reporter) {
        _inputs.addSource(baseSource);
        _board = board;
        _constBoard = _board.constantView();
        _reporter = reporter;
        _state = SETUP;
    }

    /**
     * Stores the two players.
     */
    private Player white, black, turn;

    /**
     * Run a session of Qirkat gaming.
     */
    void process() {
        white = black = null;
        doClear(null);

        while (true) {
            while (_state == SETUP) {
                doCommand();
            }

            System.out.println();
            _board.checkGameOver();
            turn = _board.whoseMove() == WHITE ? white : black;
            while (_state != SETUP && !_board.gameOver()) {
                Move move;
                move = turn.myMove();
                if (move == null) {
                    doClear(null);
                }
                if (_state == PLAYING) {
                    try {
                        _board.makeMove(move);
                        _board.checkGameOver();
                        turn = opposite(turn);
                    } catch (GameException excp) {
                        if (turn instanceof AI) {
                            throw error("something wrong with AI: "
                                    + excp.getMessage());
                        }
                        _reporter.errMsg(excp.getMessage());
                    }
                }
            }
            if (_state == PLAYING) {
                reportWinner();
            }
            _state = SETUP;
        }

    }

    /**
     * Returns the opposite player of TRN.
     */
    private Player opposite(Player trn) {
        if (trn == white) {
            return black;
        } else if (trn == black) {
            return white;
        } else {
            throw error("error in line 86 of Game");
        }
    }

    /**
     * Makes a move for MV whether its a jump or not.
     * Returns a move.
     */
    private Move moveMaker(String mv) {
        return Move.parseMove(mv);
    }

    /**
     * Return a read-only view of my game board.
     */
    Board board() {
        return _constBoard;
    }

    /**
     * Perform the next command from our input source.
     */
    void doCommand() {
        try {
            Command cmnd =
                    Command.parseCommand(_inputs.getLine("qirkat: "));
            _commands.get(cmnd.commandType()).accept(cmnd.operands());
        } catch (GameException excp) {
            _reporter.errMsg(excp.getMessage());
        }
    }

    /**
     * Read and execute commands until encountering a move or until
     * the game leaves playing state due to one of the commands. Return
     * the terminating move command, or null if the game first drops out
     * of playing mode. If appropriate to the current input source, use
     * PROMPT to prompt for input.
     */
    Command getMoveCmnd(String prompt) {
        while (_state == PLAYING) {
            try {
                Command cmnd = Command.parseCommand(_inputs.getLine(prompt));
                switch (cmnd.commandType()) {
                case PIECEMOVE:
                    return cmnd;
                default:
                    _commands.get(
                            cmnd.commandType()).accept(cmnd.operands());
                }
            } catch (GameException excp) {
                _reporter.errMsg(excp.getMessage());
            }
        }
        return null;
    }

    /**
     * Return random integer between 0 (inclusive) and MAX>0 (exclusive).
     */
    int nextRandom(int max) {
        return _randoms.nextInt(max);
    }

    /**
     * Report a move, using a message formed from FORMAT and ARGS as
     * for String.format.
     */
    void reportMove(String format, Object... args) {
        _reporter.moveMsg(format, args);
    }

    /**
     * Report an error, using a message formed from FORMAT and ARGS as
     * for String.format.
     */
    void reportError(String format, Object... args) {
        _reporter.errMsg(format, args);
    }

    /* Command Processors */

    /**
     * Perform the command 'auto OPERANDS[0]'.
     */
    void doAuto(String[] operands) {
        _state = SETUP;
        String str = operands[0];
        boolean dumb = false;
        if (str.equals("dumbwhite")) {
            dumb = true;
            str = "white";
        } else if (str.equals("dumbblack")) {
            dumb = true;
            str = "black";
        }
        if (str.equals("White") || str.equals("white")) {
            white = new AI(this, WHITE, dumb);
        } else if (str.equals("Black") || str.equals("black")) {
            black = new AI(this, BLACK, dumb);
        }
    }



    /**
     * Perform a 'help' command.
     */
    void doHelp(String[] unused) {
        InputStream helpIn =
                Game.class.getClassLoader().
                        getResourceAsStream("qirkat/help.txt");
        if (helpIn == null) {
            System.err.println("No help available.");
        } else {
            try {
                BufferedReader r
                        = new BufferedReader(new InputStreamReader(helpIn));
                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    System.out.println(line);
                }
                r.close();
            } catch (IOException e) {
                /* Ignore IOException */
            }
        }
    }

    /**
     * Perform the command 'load OPERANDS[0]'.
     */
    void doLoad(String[] operands) {
        try {
            FileReader reader = new FileReader(operands[0]);
            int curr = reader.read();
            int i = 0;
            String toDo = "";
            while (i < 1000) {
                if (curr == 10 || curr == -1) {
                    try {
                        Command cmnd =
                                Command.parseCommand(toDo);
                        _commands.get(cmnd.commandType()).
                                accept(cmnd.operands());
                    } catch (GameException excp) {
                        _reporter.errMsg(excp.getMessage());
                    }
                    toDo = "";
                    if (curr == -1) {
                        break;
                    }
                } else {
                    toDo += (char) curr;
                }
                curr = reader.read();
                i++;
            }
            if (reader.read() != -1) {
                throw error("To many characters %s", operands[0]);
            }
        } catch (IOException e) {
            throw error("Cannot open file %s", operands[0]);
        }
    }

    /**
     * Perform the command 'manual OPERANDS[0]'.
     */
    void doManual(String[] operands) {
        _state = SETUP;
        if (operands[0].equals("White") || operands[0].equals("white")) {
            white = new Manual(this, WHITE);
        } else if (operands[0].equals("Black") || operands[0].equals("black")) {
            black = new Manual(this, BLACK);
        }
    }

    /**
     * Exit the program.
     */
    void doQuit(String[] unused) {
        Main.reportTotalTimes();
        System.exit(0);
    }

    /**
     * Perform the command 'start'.
     */
    void doStart(String[] unused) {
        _state = PLAYING;
    }

    /**
     * Perform the move OPERANDS[0].
     */
    void doMove(String[] operands) {
        try {
            Move move = moveMaker(operands[0]);
            _board.makeMove(move);
        } catch (GameException excp) {
            _reporter.errMsg(excp.getMessage());
        }
    }

    /**
     * Perform the command 'clear'.
     */
    void doClear(String[] unused) {
        _state = SETUP;
        _board.clear();
        white = new Manual(this, WHITE);
        black = new AI(this, BLACK, false);
        turn = white;
    }

    /**
     * Perform the command 'set OPERANDS[0] OPERANDS[1]'.
     */
    void doSet(String[] operands) {
        PieceColor player;
        if (operands[0].toLowerCase().equals("white")) {
            player = WHITE;
        } else if (operands[0].toLowerCase().equals("black")) {
            player = BLACK;
        } else {
            throw error("not a valid piece color");
        }
        _board.clear();
        _board.setPieces(operands[1], player);

    }

    /**
     * Perform the command 'dump'.
     */
    void doDump(String[] unused) {
        System.out.println("===");
        System.out.print(_board.toString());
        System.out.println("\n===");
    }

    /**
     * Execute 'seed OPERANDS[0]' command, where the operand is a string
     * of decimal digits. Silently substitutes another value if
     * too large.
     */
    void doSeed(String[] operands) {
        try {
            _randoms.setSeed(Long.parseLong(operands[0]));
        } catch (NumberFormatException e) {
            _randoms.setSeed(Long.MAX_VALUE);
        }
    }

    /**
     * Execute the artificial 'error' command.
     */
    void doError(String[] unused) {
        throw error("Command not understood");
    }

    /**
     * Report the outcome of the current game.
     */
    void reportWinner() {
        String msg;
        msg = String.format("Game over: %s wins.",
                _board.whoseMove().opposite());
        _reporter.outcomeMsg(msg);
    }

    /**
     * Mapping of command types to methods that process them.
     */
    private final HashMap<Command.Type, Consumer<String[]>> _commands =
            new HashMap<>();

    {
        _commands.put(AUTO, this::doAuto);
        _commands.put(CLEAR, this::doClear);
        _commands.put(DUMP, this::doDump);
        _commands.put(HELP, this::doHelp);
        _commands.put(MANUAL, this::doManual);
        _commands.put(PIECEMOVE, this::doMove);
        _commands.put(SEED, this::doSeed);
        _commands.put(SETBOARD, this::doSet);
        _commands.put(START, this::doStart);
        _commands.put(LOAD, this::doLoad);
        _commands.put(QUIT, this::doQuit);
        _commands.put(ERROR, this::doError);
        _commands.put(EOF, this::doQuit);
    }


    /**
     * Input source.
     */
    private final CommandSources _inputs = new CommandSources();

    /**
     * My board and its read-only view.
     */
    private Board _board, _constBoard;
    /**
     * Indicate which players are manual players (as opposed to AIs).
     */
    private boolean _whiteIsManual, _blackIsManual;
    /**
     * Current game state.
     */
    private State _state;
    /**
     * Used to send messages to the user.
     */
    private Reporter _reporter;
    /**
     * Source of pseudo-random numbers (used by AIs).
     */
    private Random _randoms = new Random();
}
