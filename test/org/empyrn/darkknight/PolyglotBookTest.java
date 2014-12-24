package org.empyrn.darkknight;

import static org.junit.Assert.*;

import org.empyrn.darkknight.engine.PolyglotBook;
import org.empyrn.darkknight.gamelogic.ChessParseError;
import org.empyrn.darkknight.gamelogic.Position;
import org.empyrn.darkknight.gamelogic.TextIO;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;



public class PolyglotBookTest {
    public PolyglotBookTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test of getBookMove method, of class Book.
     */
    @Test
    public void testGetHashKey() throws ChessParseError {
    	// starting position
    	Position pos = TextIO.readFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    	long key = 0x463b96181691fc9cL;
    	assertEquals(key, PolyglotBook.getHashKey(pos));

    	// position after e2e4
    	pos = TextIO.readFEN("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
    	key = 0x823c9b50fd114196L;
    	assertEquals(key, PolyglotBook.getHashKey(pos));

    	// position after e2e4 d75
    	pos = TextIO.readFEN("rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2");
    	key = 0x0756b94461c50fb0L;
    	assertEquals(key, PolyglotBook.getHashKey(pos));

    	// position after e2e4 d7d5 e4e5
    	pos = TextIO.readFEN("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 2");
    	key = 0x662fafb965db29d4L;
    	assertEquals(key, PolyglotBook.getHashKey(pos));

    	// position after e2e4 d7d5 e4e5 f7f5
    	pos = TextIO.readFEN("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3");
    	key = 0x22a48b5a8e47ff78L;
    	assertEquals(key, PolyglotBook.getHashKey(pos));

    	// position after e2e4 d7d5 e4e5 f7f5 e1e2
    	pos = TextIO.readFEN("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPPKPPP/RNBQ1BNR b kq - 0 3");
    	key = 0x652a607ca3f242c1L;
    	assertEquals(key, PolyglotBook.getHashKey(pos));

    	// position after e2e4 d7d5 e4e5 f7f5 e1e2 e8f7
    	pos = TextIO.readFEN("rnbq1bnr/ppp1pkpp/8/3pPp2/8/8/PPPPKPPP/RNBQ1BNR w - - 0 4");
    	key = 0x00fdd303c946bdd9L;
    	assertEquals(key, PolyglotBook.getHashKey(pos));

    	// position after a2a4 b7b5 h2h4 b5b4 c2c4
    	pos = TextIO.readFEN("rnbqkbnr/p1pppppp/8/8/PpP4P/8/1P1PPPP1/RNBQKBNR b KQkq c3 0 3");
    	key = 0x3c8123ea7b067637L;
    	assertEquals(key, PolyglotBook.getHashKey(pos));

    	// position after a2a4 b7b5 h2h4 b5b4 c2c4 b4c3 a1a3
    	pos = TextIO.readFEN("rnbqkbnr/p1pppppp/8/8/P6P/R1p5/1P1PPPP1/1NBQKBNR b Kkq - 0 4");
    	key = 0x5c3f9b829b279560L;
    	assertEquals(key, PolyglotBook.getHashKey(pos));
    }
}
