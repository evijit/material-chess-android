package org.empyrn.darkknight;

public class GameMode {
	private final boolean playerWhite;
	private final boolean playerBlack;
	private final boolean analysisMode;
	private final boolean bluetoothMode;

	public static final int PLAYER_WHITE			= 1;
	public static final int PLAYER_BLACK			= 2;
	public static final int BLUETOOTH_GAME_WHITE	= 3;
	public static final int BLUETOOTH_GAME_BLACK	= 4;
	public static final int TWO_PLAYERS				= 5;
	public static final int ANALYSIS				= 6;
	public static final int TWO_COMPUTERS			= 7;
	
	
	public GameMode(int modeNr) {
		switch (modeNr) {
		case PLAYER_WHITE: default:
			playerWhite = true;
			playerBlack = false;
			analysisMode = false;
			bluetoothMode = false;
			break;
		case PLAYER_BLACK:
			playerWhite = false;
			playerBlack = true;
			analysisMode = false;
			bluetoothMode = false;
			break;
		case BLUETOOTH_GAME_WHITE:
			playerWhite = true;
			playerBlack = false;
			analysisMode = false;
			bluetoothMode = true;
			break;
		case BLUETOOTH_GAME_BLACK:
			playerWhite = false;
			playerBlack = true;
			analysisMode = false;
			bluetoothMode = true;
			break;
		case TWO_PLAYERS:
			playerWhite = true;
			playerBlack = true;
			analysisMode = false;
			bluetoothMode = false;
			break;
		case ANALYSIS:
			playerWhite = true;
			playerBlack = true;
			analysisMode = true;
			bluetoothMode = false;
			break;
		case TWO_COMPUTERS:
			playerWhite = false;
			playerBlack = false;
			analysisMode = false;
			bluetoothMode = false;
		}
	}

	public final boolean playerWhite() {
		return playerWhite;
	}
	public final boolean playerBlack() {
		return playerBlack;
	}
	public final boolean analysisMode() {
		return analysisMode;
	}
	public final boolean bluetoothMode() {
		return bluetoothMode;
	}
	public final boolean humansTurn(boolean whiteMove) {
        return (whiteMove ? playerWhite : playerBlack) || analysisMode;
	}

	@Override
	public boolean equals(Object o) {
		if ((o == null) || (o.getClass() != this.getClass()))
			return false;
		GameMode other = (GameMode)o;
		if (playerWhite != other.playerWhite)
			return false;
		if (playerBlack != other.playerBlack)
			return false;
		if (analysisMode != other.analysisMode)
			return false;
		return true;
	}
}
