package org.empyrn.darkknight.gamelogic;

import java.util.ArrayList;
import java.util.List;


/**
 * Used to get various search information during search
 */
public interface SearchListener {
    public void notifyDepth(int depth);
    public void notifyCurrMove(Position pos, Move m, int moveNr);
    public void notifyPV(Position pos, int depth, int score, int time, int nodes, int nps,
    		boolean isMate, boolean upperBound, boolean lowerBound, ArrayList<Move> pv);
    public void notifyStats(int nodes, int nps, int time);
	public void notifyBookInfo(String bookInfo, List<Move> moveList);
}
