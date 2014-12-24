package org.empyrn.darkknight.gamelogic;

import java.util.ArrayList;
import java.util.List;

import org.empyrn.darkknight.GUIInterface;
import org.empyrn.darkknight.GameMode;
import org.empyrn.darkknight.PGNOptions;
import org.empyrn.darkknight.engine.ComputerPlayer;
import org.empyrn.darkknight.gamelogic.Game.GameState;

/**
 * The glue between the chess engine and the GUI.
 * 
 * @author petero
 */
public class ChessController {
	private ComputerPlayer computerPlayer = null;
	private PgnToken.PgnTokenReceiver gameTextListener = null;
	private String bookFileName = "";
	private Game game;
	private GUIInterface gui;
	private GameMode gameMode;
	private PGNOptions pgnOptions;
	private Thread computerThread;
	private Thread analysisThread;

	private int timeControl;
	private int movesPerSession;
	private int timeIncrement;

	private int maxDepth;

	class SearchListener implements
			org.empyrn.darkknight.gamelogic.SearchListener {
		private int currDepth = 0;
		private int currMoveNr = 0;
		private String currMove = "";
		private int currNodes = 0;
		private int currNps = 0;
		private int currTime = 0;

		private int pvDepth = 0;
		private int pvScore = 0;
		private boolean pvIsMate = false;
		private boolean pvUpperBound = false;
		private boolean pvLowerBound = false;
		private String bookInfo = "";
		private String pvStr = "";
		private List<Move> pvMoves = null;
		private List<Move> bookMoves = null;

		public final void clearSearchInfo() {
			pvDepth = 0;
			currDepth = 0;
			bookInfo = "";
			pvMoves = null;
			bookMoves = null;
			setSearchInfo();
		}

		private final void setSearchInfo() {
			StringBuilder buf = new StringBuilder();
			if (pvDepth > 0) {
				buf.append(String.format("[%d] ", pvDepth));
				if (pvUpperBound) {
					buf.append("<=");
				} else if (pvLowerBound) {
					buf.append(">=");
				}
				if (pvIsMate) {
					buf.append(String.format("m%d", pvScore));
				} else {
					buf.append(String.format("%.2f", pvScore / 100.0));
				}
				buf.append(pvStr);
				buf.append("\n");
			}
			if (currDepth > 0) {
				buf.append(String.format("d:%d %d:%s t:%.2f n:%d nps:%d",
						currDepth, currMoveNr, currMove, currTime / 1000.0,
						currNodes, currNps));
			}
			final String newPV = buf.toString();
			final String newBookInfo = bookInfo;
			final SearchStatus localSS = ss;
			gui.runOnUIThread(new Runnable() {
				public void run() {
					if (!localSS.searchResultWanted)
						return;
					gui.setThinkingInfo(newPV, newBookInfo, pvMoves, bookMoves);
				}
			});
		}

		public void notifyDepth(int depth) {
			currDepth = depth;
			setSearchInfo();
		}

		public void notifyCurrMove(Position pos, Move m, int moveNr) {
			currMove = TextIO.moveToString(pos, m, false);
			currMoveNr = moveNr;
			setSearchInfo();
		}

		public void notifyPV(Position pos, int depth, int score, int time,
				int nodes, int nps, boolean isMate, boolean upperBound,
				boolean lowerBound, ArrayList<Move> pv) {
			pvDepth = depth;
			pvScore = score;
			currTime = time;
			currNodes = nodes;
			currNps = nps;
			pvIsMate = isMate;
			pvUpperBound = upperBound;
			pvLowerBound = lowerBound;

			StringBuilder buf = new StringBuilder();
			Position tmpPos = new Position(pos);
			UndoInfo ui = new UndoInfo();
			for (Move m : pv) {
				buf.append(String.format(" %s",
						TextIO.moveToString(tmpPos, m, false)));
				tmpPos.makeMove(m, ui);
			}
			pvStr = buf.toString();
			pvMoves = pv;
			setSearchInfo();
		}

		public void notifyStats(int nodes, int nps, int time) {
			currNodes = nodes;
			currNps = nps;
			currTime = time;
			setSearchInfo();
		}

		@Override
		public void notifyBookInfo(String bookInfo, List<Move> moveList) {
			this.bookInfo = bookInfo;
			bookMoves = moveList;
			setSearchInfo();
		}
	}

	SearchListener listener;

	public ChessController(GUIInterface gui,
			PgnToken.PgnTokenReceiver gameTextListener, PGNOptions options) {
		this.gui = gui;
		this.gameTextListener = gameTextListener;
		pgnOptions = options;
		listener = new SearchListener();
	}

	public int getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	public final void setBookFileName(String bookFileName) {
		if (!this.bookFileName.equals(bookFileName)) {
			this.bookFileName = bookFileName;
			if (computerPlayer != null) {
				computerPlayer.setBookFileName(bookFileName);
				if (analysisThread != null) {
					stopAnalysis();
					startAnalysis();
				}
				updateBookHints();
			}
		}
	}

	private final void updateBookHints() {
		if (gameMode != null) {
			boolean analysis = gameMode.analysisMode();
			if (!analysis && humansTurn()) {
				ss = new SearchStatus();
				Pair<String, ArrayList<Move>> bi = computerPlayer
						.getBookHints(game.currPos());
				listener.notifyBookInfo(bi.first, bi.second);
			}
		}
	}

	private final static class SearchStatus {
		boolean searchResultWanted = true;
	}

	SearchStatus ss = new SearchStatus();

	public final void newGame(GameMode gameMode) {
		ss.searchResultWanted = false;
		stopComputerThinking();
		stopAnalysis();
		this.gameMode = gameMode;
		if (computerPlayer == null) {
			computerPlayer = new ComputerPlayer();
			computerPlayer.setListener(listener);
			computerPlayer.setBookFileName(bookFileName);
		}
		game = new Game(computerPlayer, gameTextListener, timeControl,
				movesPerSession, timeIncrement);
		setPlayerNames(game);
		updateGamePaused();
	}

	public final void startGame() {
		updateComputeThreads(true);
		setSelection();
		updateGUI();
		updateGamePaused();
	}

	private boolean guiPaused = false;

	public final void setGuiPaused(boolean paused) {
		guiPaused = paused;
		updateGamePaused();
	}

	private final void updateGamePaused() {
		if (game != null) {
			boolean gamePaused = gameMode.analysisMode()
					|| (humansTurn() && guiPaused);
			game.setGamePaused(gamePaused);
			updateRemainingTime();
		}
	}

	private final void updateComputeThreads(boolean clearPV) {
		boolean analysis = gameMode.analysisMode();
		boolean computersTurn = !humansTurn() && !gameMode.bluetoothMode();
		if (!analysis)
			stopAnalysis();
		if (!computersTurn)
			stopComputerThinking();
		if (clearPV) {
			listener.clearSearchInfo();
			updateBookHints();
		}
		if (analysis)
			startAnalysis();
		if (computersTurn)
			startComputerThinking();
	}

	/** Set game mode. */
	public final void setGameMode(GameMode newMode) {
		if (!gameMode.equals(newMode)) {
			if (newMode.humansTurn(game.currPos().whiteMove))
				ss.searchResultWanted = false;
			gameMode = newMode;
			if (!gameMode.playerWhite() || !gameMode.playerBlack())
				setPlayerNames(game); // If computer player involved, set player
										// names
			updateGamePaused();
			updateComputeThreads(true);
			updateGUI();
		}
	}

	public final void prefsChanged() {
		updateBookHints();
		updateMoveList();
	}

	private final void setPlayerNames(Game game) {
		if (game != null) {
			String engine = ComputerPlayer.engineName;
			String white = gameMode.playerWhite() ? "Player" : engine;
			String black = gameMode.playerBlack() ? "Player" : engine;
			game.tree.setPlayerNames(white, black);
		}
	}

	public final void fromByteArray(byte[] data) {
		game.fromByteArray(data);
	}

	public final byte[] toByteArray() {
		return game.tree.toByteArray();
	}

	public final String getFEN() {
		return TextIO.toFEN(game.tree.currentPos);
	}

	/** Convert current game to PGN format. */
	public final String getPGN() {
		return game.tree.toPGN(pgnOptions);
	}

	public final void setFENOrPGN(String fenPgn) throws ChessParseError {
		Game newGame = new Game(null, gameTextListener, timeControl,
				movesPerSession, timeIncrement);
		try {
			Position pos = TextIO.readFEN(fenPgn);
			newGame.setPos(pos);
			setPlayerNames(newGame);
		} catch (ChessParseError e) {
			// Try read as PGN instead
			if (!newGame.readPGN(fenPgn, pgnOptions)) {
				throw e;
			}
		}
		ss.searchResultWanted = false;
		game = newGame;
		game.setComputerPlayer(computerPlayer);
		gameTextListener.clear();
		updateGamePaused();
		stopAnalysis();
		stopComputerThinking();
		computerPlayer.clearTT();
		updateComputeThreads(true);
		gui.setSelection(-1);
		updateGUI();
	}

	/** True if human's turn to make a move. (True in analysis mode.) */
	public final boolean humansTurn() {
		return gameMode.humansTurn(game.currPos().whiteMove);
	}

	/** Return true if computer player is using CPU power. */
	public final boolean computerBusy() {
		if (game.getGameState() != GameState.ALIVE)
			return false;
		return gameMode.analysisMode() || !humansTurn();
	}

	private final void undoMoveNoUpdate() {
		if (game.getLastMove() != null) {
			ss.searchResultWanted = false;
			game.undoMove();
			if (!humansTurn()) {
				if (game.getLastMove() != null) {
					game.undoMove();
					if (!humansTurn()) {
						game.redoMove();
					}
				} else {
					// Don't undo first white move if playing black vs computer,
					// because that would cause computer to immediately make
					// a new move and the whole redo history will be lost.
					if (gameMode.playerWhite() || gameMode.playerBlack())
						game.redoMove();
				}
			}
		}
	}

	public final void undoMove() {
		if (game.getLastMove() != null) {
			ss.searchResultWanted = false;
			stopAnalysis();
			stopComputerThinking();
			undoMoveNoUpdate();
			updateComputeThreads(true);
			setSelection();
			updateGUI();
		}
	}

	private final void redoMoveNoUpdate() {
		if (game.canRedoMove()) {
			ss.searchResultWanted = false;
			game.redoMove();
			if (!humansTurn() && game.canRedoMove()) {
				game.redoMove();
				if (!humansTurn())
					game.undoMove();
			}
		}
	}

	public final boolean canRedoMove() {
		return game.canRedoMove();
	}

	public final void redoMove() {
		if (canRedoMove()) {
			ss.searchResultWanted = false;
			stopAnalysis();
			stopComputerThinking();
			redoMoveNoUpdate();
			updateComputeThreads(true);
			setSelection();
			updateGUI();
		}
	}

	public final int numVariations() {
		return game.numVariations();
	}

	public final void changeVariation(int delta) {
		if (game.numVariations() > 1) {
			ss.searchResultWanted = false;
			stopAnalysis();
			stopComputerThinking();
			game.changeVariation(delta);
			updateComputeThreads(true);
			setSelection();
			updateGUI();
		}
	}

	public final void removeVariation() {
		if (game.numVariations() > 1) {
			ss.searchResultWanted = false;
			stopAnalysis();
			stopComputerThinking();
			game.removeVariation();
			updateComputeThreads(true);
			setSelection();
			updateGUI();
		}
	}

	public final void gotoMove(int moveNr) {
		boolean needUpdate = false;
		while (game.currPos().fullMoveCounter > moveNr) { // Go backward
			int before = game.currPos().fullMoveCounter * 2
					+ (game.currPos().whiteMove ? 0 : 1);
			undoMoveNoUpdate();
			int after = game.currPos().fullMoveCounter * 2
					+ (game.currPos().whiteMove ? 0 : 1);
			if (after >= before)
				break;
			needUpdate = true;
		}
		while (game.currPos().fullMoveCounter < moveNr) { // Go forward
			int before = game.currPos().fullMoveCounter * 2
					+ (game.currPos().whiteMove ? 0 : 1);
			redoMoveNoUpdate();
			int after = game.currPos().fullMoveCounter * 2
					+ (game.currPos().whiteMove ? 0 : 1);
			if (after <= before)
				break;
			needUpdate = true;
		}
		if (needUpdate) {
			stopAnalysis();
			stopComputerThinking();
			updateComputeThreads(true);
			setSelection();
			updateGUI();
		}
	}

	public final void makeHumanMove(Move m) {
		if (humansTurn()) {
			if (doMove(m)) {
				ss.searchResultWanted = false;
				stopAnalysis();
				stopComputerThinking();
				updateComputeThreads(true);
				gui.humanMoveMade(m);
				updateGUI();
			} else {
				gui.setSelection(-1);
			}
		}
	}

	public final void makeBluetoothMove(Move m) {
		if (!humansTurn()) {
			if (doMove(m)) {
				setSelection();
				updateGUI();
			} else {
				gui.setSelection(-1);
			}
		}
	}

	Move promoteMove;

	public final void reportPromotePiece(int choice) {
		final boolean white = game.currPos().whiteMove;
		int promoteTo;
		switch (choice) {
		case 1:
			promoteTo = white ? Piece.WROOK : Piece.BROOK;
			break;
		case 2:
			promoteTo = white ? Piece.WBISHOP : Piece.BBISHOP;
			break;
		case 3:
			promoteTo = white ? Piece.WKNIGHT : Piece.BKNIGHT;
			break;
		default:
			promoteTo = white ? Piece.WQUEEN : Piece.BQUEEN;
			break;
		}
		promoteMove.promoteTo = promoteTo;
		Move m = promoteMove;
		promoteMove = null;
		makeHumanMove(m);
	}

	/**
	 * Move a piece from one square to another.
	 * 
	 * @return True if the move was legal, false otherwise.
	 */
	final private boolean doMove(Move move) {
		Position pos = game.currPos();
		ArrayList<Move> moves = new MoveGen().pseudoLegalMoves(pos);
		moves = MoveGen.removeIllegal(pos, moves);
		int promoteTo = move.promoteTo;
		for (Move m : moves) {
			if ((m.from == move.from) && (m.to == move.to)) {
				if ((m.promoteTo != Piece.EMPTY) && (promoteTo == Piece.EMPTY)) {
					promoteMove = m;
					gui.requestPromotePiece();
					return false;
				}
				if (m.promoteTo == promoteTo) {
					String strMove = TextIO.moveToString(pos, m, false);
					game.processString(strMove);
					return true;
				}
			}
		}
		gui.reportInvalidMove(move);
		return false;
	}

	final private void updateGUI() {
		String str = new Integer(game.currPos().fullMoveCounter).toString();
		str += game.currPos().whiteMove ? ". White's move" : "... Black's move";
		if (computerThread != null)
			str += " (thinking)";
		if (analysisThread != null)
			str += " (analyzing)";
		if (game.getGameState() != Game.GameState.ALIVE) {
			str = game.getGameStateString();
		}
		gui.setStatusString(str);
		updateMoveList();

		StringBuilder sb = new StringBuilder();
		if (game.tree.currentNode != game.tree.rootNode) {
			game.tree.goBack();
			Position pos = game.currPos();
			List<Move> prevVarList = game.tree.variations();
			for (int i = 0; i < prevVarList.size(); i++) {
				if (i > 0)
					sb.append(' ');
				if (i == game.tree.currentNode.defaultChild)
					sb.append("<b>");
				sb.append(TextIO.moveToString(pos, prevVarList.get(i), false));
				if (i == game.tree.currentNode.defaultChild)
					sb.append("</b>");
			}
			game.tree.goForward(-1);
		}
		gui.setPosition(game.currPos(), sb.toString(), game.tree.variations());

		updateRemainingTime();
	}

	final private void updateMoveList() {
		if (!gameTextListener.isUpToDate()) {
			PGNOptions tmpOptions = new PGNOptions();
			tmpOptions.exp.variations = pgnOptions.view.variations;
			tmpOptions.exp.comments = pgnOptions.view.comments;
			tmpOptions.exp.nag = pgnOptions.view.nag;
			tmpOptions.exp.playerAction = false;
			tmpOptions.exp.clockInfo = false;
			tmpOptions.exp.moveNrAfterNag = false;
			gameTextListener.clear();
			game.tree.pgnTreeWalker(tmpOptions, gameTextListener);
		}
		gameTextListener.setCurrent(game.tree.currentNode);
		gui.moveListUpdated();
	}

	final public void updateRemainingTime() {
		// Update remaining time
		long now = System.currentTimeMillis();
		long wTime = game.timeController.getRemainingTime(true, now);
		long bTime = game.timeController.getRemainingTime(false, now);
		long nextUpdate = 0;
		if (game.timeController.clockRunning()) {
			long t = game.currPos().whiteMove ? wTime : bTime;
			nextUpdate = (t % 1000);
			if (nextUpdate < 0)
				nextUpdate += 1000;
			nextUpdate += 1;
		}
		gui.setRemainingTime(wTime, bTime, nextUpdate);
	}

	final private void setSelection() {
		Move m = game.getLastMove();
		int sq = (m != null) ? m.to : -1;
		gui.setSelection(sq);
	}

	private final synchronized void startComputerThinking() {
		if (analysisThread != null)
			return;
		if (game.getGameState() != GameState.ALIVE)
			return;
		if (computerThread == null) {
			ss = new SearchStatus();
			final Pair<Position, ArrayList<Move>> ph = game.getUCIHistory();
			final Game g = game;
			final boolean haveDrawOffer = g.haveDrawOffer();
			final Position currPos = new Position(g.currPos());
			long now = System.currentTimeMillis();
			final int wTime = game.timeController.getRemainingTime(true, now);
			final int bTime = game.timeController.getRemainingTime(false, now);
			final int inc = game.timeController.getIncrement();
			final int movesToGo = game.timeController.getMovesToTC();
			computerThread = new Thread(new Runnable() {
				public void run() {
					final String cmd = computerPlayer.doSearch(ph.first,
							ph.second, currPos, haveDrawOffer, wTime, bTime,
							inc, movesToGo, maxDepth);
					final SearchStatus localSS = ss;
					gui.runOnUIThread(new Runnable() {
						public void run() {
							if (!localSS.searchResultWanted)
								return;
							g.processString(cmd);
							updateGamePaused();
							gui.computerMoveMade();
							listener.clearSearchInfo();
							stopComputerThinking();
							stopAnalysis(); // To force analysis to restart for
											// new position
							updateComputeThreads(true);
							setSelection();
							updateGUI();
						}
					});
				}
			});
			listener.clearSearchInfo();
			computerPlayer.shouldStop = false;
			computerThread.start();
			updateGUI();
		}
	}

	private final synchronized void stopComputerThinking() {
		if (computerThread != null) {
			computerPlayer.stopSearch();
			try {
				computerThread.join();
			} catch (InterruptedException ex) {
				System.out.printf("Could not stop computer thread%n");
			}
			computerThread = null;
			updateGUI();
		}
	}

	private final synchronized void startAnalysis() {
		if (gameMode.analysisMode()) {
			if (computerThread != null)
				return;
			if (analysisThread == null) {
				ss = new SearchStatus();
				final Pair<Position, ArrayList<Move>> ph = game.getUCIHistory();
				final boolean haveDrawOffer = game.haveDrawOffer();
				final Position currPos = new Position(game.currPos());
				final boolean alive = game.tree.getGameState() == GameState.ALIVE;
				analysisThread = new Thread(new Runnable() {
					public void run() {
						if (alive)
							computerPlayer.analyze(ph.first, ph.second,
									currPos, haveDrawOffer);
					}
				});
				listener.clearSearchInfo();
				computerPlayer.shouldStop = false;
				analysisThread.start();
				updateGUI();
			}
		}
	}

	private final synchronized void stopAnalysis() {
		if (analysisThread != null) {
			computerPlayer.stopSearch();
			try {
				analysisThread.join();
			} catch (InterruptedException ex) {
				System.out.printf("Could not stop analysis thread%n");
			}
			analysisThread = null;
			listener.clearSearchInfo();
			updateGUI();
		}
	}

	public final synchronized void setTimeLimit(int time, int moves, int inc) {
		timeControl = time;
		movesPerSession = moves;
		timeIncrement = inc;
		if (game != null)
			game.timeController.setTimeControl(timeControl, movesPerSession,
					timeIncrement);
	}

	public final void stopSearch() {
		if (computerThread != null) {
			computerPlayer.stopSearch();
		}
	}

	public final void shutdownEngine() {
		gameMode = new GameMode(GameMode.TWO_PLAYERS);
		stopComputerThinking();
		stopAnalysis();
		computerPlayer.shutdownEngine();
	}

	/**
	 * Help human to claim a draw by trying to find and execute a valid draw
	 * claim.
	 */
	public final boolean claimDrawIfPossible() {
		if (!findValidDrawClaim())
			return false;
		updateGUI();
		return true;
	}

	private final boolean findValidDrawClaim() {
		if (game.getGameState() != GameState.ALIVE)
			return true;
		game.processString("draw accept");
		if (game.getGameState() != GameState.ALIVE)
			return true;
		game.processString("draw rep");
		if (game.getGameState() != GameState.ALIVE)
			return true;
		game.processString("draw 50");
		if (game.getGameState() != GameState.ALIVE)
			return true;
		return false;
	}

	public final void resignGame() {
		if (game.getGameState() == GameState.ALIVE) {
			game.processString("resign");
			updateGUI();
		}
	}
}
