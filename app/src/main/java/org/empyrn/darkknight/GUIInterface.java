package org.empyrn.darkknight;

import java.util.List;

import org.empyrn.darkknight.gamelogic.Move;
import org.empyrn.darkknight.gamelogic.Position;



/** Interface between the gui and the ChessController. */
public interface GUIInterface {

	/** Update the displayed board position. */
	public void setPosition(Position pos, String variantInfo, List<Move> variantMoves);

	/** Mark square i as selected. Set to -1 to clear selection. */
	public void setSelection(int sq);

	/** Set the status text. */
	public void setStatusString(String str);

	/** Update the list of moves. */
	public void moveListUpdated();

	/** Update the computer thinking information. */
	public void setThinkingInfo(String pvStr, String bookInfo, List<Move> pvMoves, List<Move> bookMoves);
	
	/** Ask what to promote a pawn to. Should call reportPromotePiece() when done. */
	public void requestPromotePiece();

	/** Run code on the GUI thread. */
	public void runOnUIThread(Runnable runnable);

	/** Report that user attempted to make an invalid move. */
	public void reportInvalidMove(Move m);

	/** Called when computer made a move. GUI can notify user, for example by playing a sound. */
	public void computerMoveMade();
	public void humanMoveMade(Move m);

	/** Report remaining thinking time to GUI. */
	public void setRemainingTime(long wTime, long bTime, long nextUpdate);
}
