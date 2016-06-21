package org.empyrn.darkknight.engine;

import java.util.ArrayList;

import org.empyrn.darkknight.gamelogic.Move;
import org.empyrn.darkknight.gamelogic.MoveGen;
import org.empyrn.darkknight.gamelogic.Pair;
import org.empyrn.darkknight.gamelogic.Position;
import org.empyrn.darkknight.gamelogic.SearchListener;
import org.empyrn.darkknight.gamelogic.TextIO;
import org.empyrn.darkknight.gamelogic.UndoInfo;


/**
 * A computer algorithm player.
 */
public class ComputerPlayer {
    public static String engineName = "";

    static NativePipedProcess npp = null;
	SearchListener listener;
	int timeLimit;
	Book book;
	private boolean newGame = false;

    public ComputerPlayer() {
    	if (npp == null) {
    		npp = new NativePipedProcess();
    		npp.initialize();
    		npp.writeLineToProcess("uci");
    		readUCIOptions();
    		npp.writeLineToProcess("setoption name Hash value 16");
    		npp.writeLineToProcess("setoption name Ponder value false");
    		npp.writeLineToProcess("setoption name Aggressiveness value 185");
    		npp.writeLineToProcess("ucinewgame");
    		syncReady();
    	}
    	
    	listener = null;
    	timeLimit = 0;
    	book = new Book(false);
    }

	public final void setListener(SearchListener listener) {
        this.listener = listener;
    }

	public final void setBookFileName(String bookFileName) {
		book.setBookFileName(bookFileName);
	}
	
    private void readUCIOptions() {
    	int timeout = 1000;
    	while (true) {
    		String s = npp.readLineFromProcess(timeout);
    		String[] tokens = tokenize(s);
    		if (tokens[0].equals("uciok"))
    			break;
    		else if (tokens[0].equals("id")) {
    			if (tokens[1].equals("name")) {
    				engineName = "";
    				for (int i = 2; i < tokens.length; i++) {
    					if (engineName.length() > 0)
    						engineName += " ";
    					engineName += tokens[i];
    				}
    			}
    		}
    	}
    }

    /** Convert a string to tokens by splitting at whitespace characters. */
    private final String[] tokenize(String cmdLine) {
        cmdLine = cmdLine.trim();
        return cmdLine.split("\\s+");
    }

    private final void syncReady() {
    	npp.writeLineToProcess("isready");
    	while (true) {
    		String s = npp.readLineFromProcess(1000);
    		if (s.equals("readyok"))
    			break;
    	}
    }

    /** Clear transposition table. */
	public final void clearTT() {
		newGame = true;
	}

	public final void maybeNewGame() {
		if (newGame) {
			newGame = false;
			npp.writeLineToProcess("ucinewgame");
			syncReady();
		}
	}
	
	/** Stop the engine process. */
    public final void shutdownEngine() {
    	if (npp != null) {
    		npp.shutDown();
    		npp = null;
    	}
	}

    /**
     * Do a search and return a command from the computer player.
     * The command can be a valid move string, in which case the move is played
     * and the turn goes over to the other player. The command can also be a special
     * command, such as "draw" and "resign".
     * @param pos  An earlier position from the game
     * @param mList List of moves to go from the earlier position to the current position.
     *              This list makes it possible for the computer to correctly handle draw
     *              by repetition/50 moves.
     */
    public final String doSearch(Position prevPos, ArrayList<Move> mList, Position currPos,
    							 boolean drawOffer,
    							 int wTime, int bTime, int inc, int movesToGo, int maxDepth) {
    	if (listener != null) 
    		listener.notifyBookInfo("", null);

    	// Set up for draw detection
        long[] posHashList = new long[mList.size()+1];
        int posHashListSize = 0;
        Position p = new Position(prevPos);
        UndoInfo ui = new UndoInfo();
        for (int i = 0; i < mList.size(); i++) {
            posHashList[posHashListSize++] = p.zobristHash();
        	p.makeMove(mList.get(i), ui);
        }

    	// If we have a book move, play it.
    	Move bookMove = book.getBookMove(currPos);
    	if (bookMove != null) {
        	if (canClaimDraw(currPos, posHashList, posHashListSize, bookMove) == "") {
        		return TextIO.moveToString(currPos, bookMove, false);
        	}
        }

    	// If only one legal move, play it without searching
        ArrayList<Move> moves = new MoveGen().pseudoLegalMoves(currPos);
        moves = MoveGen.removeIllegal(currPos, moves);
        if (moves.size() == 0) {
        	return ""; // User set up a position where computer has no valid moves.
        }
        if (moves.size() == 1) {
        	Move bestMove = moves.get(0);
        	if (canClaimDraw(currPos, posHashList, posHashListSize, bestMove) == "") {
        		return TextIO.moveToUCIString(bestMove);
        	}
        }

    	StringBuilder posStr = new StringBuilder();
    	posStr.append("position fen ");
    	posStr.append(TextIO.toFEN(prevPos));
    	int nMoves = mList.size();
    	if (nMoves > 0) {
    		posStr.append(" moves");
    		for (int i = 0; i < nMoves; i++) {
    			posStr.append(" ");
    			posStr.append(TextIO.moveToUCIString(mList.get(i)));
    		}
    	}
    	maybeNewGame();
    	npp.writeLineToProcess(posStr.toString());
    	if (wTime < 1) wTime = 1;
    	if (bTime < 1) bTime = 1;
    	String goStr = String.format("go wtime %d btime %d", wTime, bTime);
    	
    	if (maxDepth > 1)
    		goStr += String.format(" depth %d", maxDepth);
    	if (inc > 0)
    		goStr += String.format(" winc %d binc %d", inc, inc);
    	if (movesToGo > 0)
    		goStr += String.format(" movestogo %d", movesToGo);
    	
    	npp.writeLineToProcess(goStr);

    	String bestMove = monitorEngine(currPos);

        // Claim draw if appropriate
        if (statScore <= 0) {
        	String drawClaim = canClaimDraw(currPos, posHashList, posHashListSize, TextIO.UCIstringToMove(bestMove));
        	if (drawClaim != "")
        		bestMove = drawClaim;
        }
        // Accept draw offer if engine is losing
        if (drawOffer && !statIsMate && (statScore <= -300)) {
        	bestMove = "draw accept";
        }
        return bestMove;
    }

    /** Wait for engine to respond with "bestmove". While waiting, monitor and report search info. */
	private final String monitorEngine(Position pos) {
		// Monitor engine response
    	clearInfo();
    	boolean stopSent = false;
    	while (true) {
			int timeout = 2000;
    		while (true) {
    			if (shouldStop && !stopSent) {
    		    	npp.writeLineToProcess("stop");
    		    	stopSent = true;
    			}
    			String s = npp.readLineFromProcess(timeout);
    			if (s.length() == 0)
    				break;
    			String[] tokens = tokenize(s);
    			if (tokens[0].equals("info")) {
    				parseInfoCmd(tokens);
    			} else if (tokens[0].equals("bestmove")) {
    				return tokens[1];
    			}
    			timeout = 0;
    		}
    		notifyGUI(pos);
			try {
				Thread.sleep(100); // 10 GUI updates per second is enough
			} catch (InterruptedException e) {
			}
    	}
	}

	public final Pair<String, ArrayList<Move>> getBookHints(Position pos) {
		Pair<String, ArrayList<Move>> bi = book.getAllBookMoves(pos);
		return new Pair<String, ArrayList<Move>>(bi.first, bi.second);
	}

	public boolean shouldStop = false;

	public final void analyze(Position prevPos, ArrayList<Move> mList, Position currPos, boolean drawOffer) {
    	if (shouldStop)
    		return;
    	if (listener != null) {
    		Pair<String, ArrayList<Move>> bi = getBookHints(currPos);
    		listener.notifyBookInfo(bi.first, bi.second);
    	}

    	// If no legal moves, there is nothing to analyze
        ArrayList<Move> moves = new MoveGen().pseudoLegalMoves(currPos);
        moves = MoveGen.removeIllegal(currPos, moves);
        if (moves.size() == 0)
        	return;

    	StringBuilder posStr = new StringBuilder();
    	posStr.append("position fen ");
    	posStr.append(TextIO.toFEN(prevPos));
    	int nMoves = mList.size();
    	if (nMoves > 0) {
    		posStr.append(" moves");
    		for (int i = 0; i < nMoves; i++) {
    			posStr.append(" ");
    			posStr.append(TextIO.moveToUCIString(mList.get(i)));
    		}
    	}
    	maybeNewGame();
    	npp.writeLineToProcess(posStr.toString());
    	String goStr = String.format("go infinite");
    	npp.writeLineToProcess(goStr);

    	monitorEngine(currPos);
    }

    /** Check if a draw claim is allowed, possibly after playing "move".
     * @param move The move that may have to be made before claiming draw.
     * @return The draw string that claims the draw, or empty string if draw claim not valid.
     */
    private String canClaimDraw(Position pos, long[] posHashList, int posHashListSize, Move move) {
    	String drawStr = "";
        if (canClaimDraw50(pos)) {
            drawStr = "draw 50";
        } else if (canClaimDrawRep(pos, posHashList, posHashListSize, posHashListSize)) {
            drawStr = "draw rep";
        } else {
            String strMove = TextIO.moveToString(pos, move, false);
            posHashList[posHashListSize++] = pos.zobristHash();
            UndoInfo ui = new UndoInfo();
            pos.makeMove(move, ui);
            if (canClaimDraw50(pos)) {
                drawStr = "draw 50 " + strMove;
            } else if (canClaimDrawRep(pos, posHashList, posHashListSize, posHashListSize)) {
                drawStr = "draw rep " + strMove;
            }
            pos.unMakeMove(move, ui);
        }
        return drawStr;
    }
    
    private final static boolean canClaimDraw50(Position pos) {
        return (pos.halfMoveClock >= 100);
    }
    
    private final static boolean canClaimDrawRep(Position pos, long[] posHashList, int posHashListSize, int posHashFirstNew) {
        int reps = 0;
        for (int i = posHashListSize - 4; i >= 0; i -= 2) {
            if (pos.zobristHash() == posHashList[i]) {
                reps++;
                if (i >= posHashFirstNew) {
                    reps++;
                    break;
                }
            }
        }
        return (reps >= 2);
    }
    

	private int statCurrDepth = 0;
	private int statPVDepth = 0;
    private int statScore = 0;
    private boolean statIsMate = false;
    private boolean statUpperBound = false;
    private boolean statLowerBound = false;
    private int statTime = 0;
    private int statNodes = 0;
    private int statNps = 0;
    private ArrayList<String> statPV = new ArrayList<String>();
    private String statCurrMove = "";
    private int statCurrMoveNr = 0;

    private boolean depthModified = false;
    private boolean currMoveModified = false;
    private boolean pvModified = false;
    private boolean statsModified = false;

    private final void clearInfo() {
    	depthModified = false;
        currMoveModified = false;
        pvModified = false;
        statsModified = false;
    }

    private final void parseInfoCmd(String[] tokens) {
    	try {
    		int nTokens = tokens.length;
    		int i = 1;
    		while (i < nTokens - 1) {
    			String is = tokens[i++];
    			if (is.equals("depth")) {
    				statCurrDepth = Integer.parseInt(tokens[i++]);
    				depthModified = true;
    			} else if (is.equals("currmove")) {
    				statCurrMove = tokens[i++];
    				currMoveModified = true;
    			} else if (is.equals("currmovenumber")) {
    				statCurrMoveNr = Integer.parseInt(tokens[i++]);
    				currMoveModified = true;
    			} else if (is.equals("time")) {
    				statTime = Integer.parseInt(tokens[i++]);
    				statsModified = true;
    			} else if (is.equals("nodes")) {
    				statNodes = Integer.parseInt(tokens[i++]);
    				statsModified = true;
    			} else if (is.equals("nps")) {
    				statNps = Integer.parseInt(tokens[i++]);
    				statsModified = true;
    			} else if (is.equals("pv")) {
    				statPV.clear();
    				while (i < nTokens)
    					statPV.add(tokens[i++]);
    				pvModified = true;
    				statPVDepth = statCurrDepth;
    			} else if (is.equals("score")) {
					statIsMate = tokens[i++].equals("mate");
					statScore = Integer.parseInt(tokens[i++]);
					statUpperBound = false;
					statLowerBound = false;
					if (tokens[i].equals("upperbound")) {
						statUpperBound = true;
						i++;
					} else if (tokens[i].equals("lowerbound")) {
						statLowerBound = true;
						i++;
					}
    				pvModified = true;
    			}
    		}
    	} catch (NumberFormatException nfe) {
    		// Ignore
    	} catch (ArrayIndexOutOfBoundsException aioob) {
    		// Ignore
    	}
	}

    /** Notify GUI about search statistics. */
    private final void notifyGUI(Position pos) {
        if (listener == null)
    		return;
    	if (depthModified) {
    		listener.notifyDepth(statCurrDepth);
    		depthModified = false;
    	}
        if (currMoveModified) {
        	Move m = TextIO.UCIstringToMove(statCurrMove);
            listener.notifyCurrMove(pos, m, statCurrMoveNr);
        	currMoveModified = false;
        }
        if (pvModified) {
        	ArrayList<Move> moves = new ArrayList<Move>();
        	int nMoves = statPV.size();
        	for (int i = 0; i < nMoves; i++)
        		moves.add(TextIO.UCIstringToMove(statPV.get(i)));
            listener.notifyPV(pos, statPVDepth, statScore, statTime, statNodes, statNps,
            				  statIsMate, statUpperBound, statLowerBound, moves);
        	pvModified = false;
        }
        if (statsModified) {
            listener.notifyStats(statNodes, statNps, statTime);
        	statsModified = false;
        }
    }

    public final void stopSearch() {
    	shouldStop = true;
    	npp.writeLineToProcess("stop");
    }
}
