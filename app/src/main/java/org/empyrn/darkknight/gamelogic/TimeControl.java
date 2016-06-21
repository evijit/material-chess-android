package org.empyrn.darkknight.gamelogic;

public class TimeControl {
	private long timeControl;
	private int movesPerSession;
	private long increment;

	private long whiteBaseTime;
	private long blackBaseTime;

	int currentMove;
	boolean whiteToMove;

	long elapsed; // Accumulated elapsed time for this move.
	long timerT0; // Time when timer started. 0 if timer is stopped.


	/** Constructor. Sets time control to "game in 5min". */
	public TimeControl() {
		setTimeControl(5 * 60 * 1000, 0, 0);
		reset();
	}

	public final void reset() {
		currentMove = 1;
		whiteToMove = true;
		elapsed = 0;
		timerT0 = 0;
	}

	/** Set time control to "moves" moves in "time" milliseconds, + inc milliseconds per move. */
	public final void setTimeControl(long time, int moves, long inc) {
		timeControl = time;
		movesPerSession = moves;
		increment = inc;
	}

	public final void setCurrentMove(int move, boolean whiteToMove, long whiteBaseTime, long blackBaseTime) {
		currentMove = move;
		this.whiteToMove = whiteToMove;
		this.whiteBaseTime = whiteBaseTime;
		this.blackBaseTime = blackBaseTime;
		timerT0 = 0;
		elapsed = 0;
	}

	public final boolean clockRunning() {
		return timerT0 != 0;
	}

	public final void startTimer(long now) {
		if (!clockRunning()) {
			timerT0 = now;
		}
	}

	public final void stopTimer(long now) {
		if (clockRunning()) {
			long timerT1 = now;
			long currElapsed = timerT1 - timerT0;
			timerT0 = 0;
			if (currElapsed > 0) {
				elapsed += currElapsed;
			}
		}
	}

	/** Compute new remaining time after a move is made. */
	public final int moveMade(long now) {
		stopTimer(now);
		long remaining = getRemainingTime(whiteToMove, now);
		remaining += increment;
		if (getMovesToTC() == 1) {
			remaining += timeControl;
		}
		elapsed = 0;
		return (int)remaining;
	}

	/** Get remaining time */
	public final int getRemainingTime(boolean whiteToMove, long now) {
		long remaining = whiteToMove ? whiteBaseTime : blackBaseTime;
		if (whiteToMove == this.whiteToMove) { 
			remaining -= elapsed;
			if (timerT0 != 0) {
				remaining -= now - timerT0;
			}
		}
		return (int)remaining;
	}

	public final int getInitialTime() {
		return (int)timeControl;
	}

	public final int getIncrement() {
		return (int)increment;
	}
	
	public final int getMovesToTC() {
		if (movesPerSession <= 0)
			return 0;
		int nextTC = 1;
		while (nextTC <= currentMove)
			nextTC += movesPerSession;
		return nextTC - currentMove;
	}
}
