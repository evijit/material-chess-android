package org.empyrn.darkknight.gamelogic;

/**
 * Constants for different piece types.
 * @author petero
 */
public class Piece {
    public static final int EMPTY = 0;

    public static final int WKING = 1;
    public static final int WQUEEN = 2;
    public static final int WROOK = 3;
    public static final int WBISHOP = 4;
    public static final int WKNIGHT = 5;
    public static final int WPAWN = 6;

    public static final int BKING = 7;
    public static final int BQUEEN = 8;
    public static final int BROOK = 9;
    public static final int BBISHOP = 10;
    public static final int BKNIGHT = 11;
    public static final int BPAWN = 12;

    public static final int nPieceTypes = 13;

    /**
     * Return true if p is a white piece, false otherwise.
     * Note that if p is EMPTY, an unspecified value is returned.
     */
    public static boolean isWhite(int pType) {
        return pType < BKING;
    }
    public static int makeWhite(int pType) {
    	return pType < BKING ? pType : pType - (BKING - WKING);
    }
    public static int makeBlack(int pType) {
    	return ((pType >= WKING) && (pType <= WPAWN)) ? pType + (BKING - WKING) : pType;
    }
}
