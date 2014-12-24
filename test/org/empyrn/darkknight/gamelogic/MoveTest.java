/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.empyrn.darkknight.gamelogic;

import org.empyrn.darkknight.gamelogic.Move;
import org.empyrn.darkknight.gamelogic.Piece;
import org.empyrn.darkknight.gamelogic.Position;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


import static org.junit.Assert.*;

/**
 *
 * @author petero
 */
public class MoveTest {

    public MoveTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }
    
    /**
     * Test of move constructor, of class Move.
     */
    @Test
    public void testMoveConstructor() {
        int f = Position.getSquare(4, 1);
        int t = Position.getSquare(4, 3);
        int p = Piece.WROOK;
        Move move = new Move(f, t, p);
        assertEquals(move.from, f);
        assertEquals(move.to,t);
        assertEquals(move.promoteTo, p);
    }
    
    /**
     * Test of equals, of class Move.
     */
    @Test
    public void testEquals() {
        Move m1 = new Move(Position.getSquare(0, 6), Position.getSquare(1, 7), Piece.WROOK);
        Move m2 = new Move(Position.getSquare(0, 6), Position.getSquare(0, 7), Piece.WROOK);
        Move m3 = new Move(Position.getSquare(1, 6), Position.getSquare(1, 7), Piece.WROOK);
        Move m4 = new Move(Position.getSquare(0, 6), Position.getSquare(1, 7), Piece.WKNIGHT);
        Move m5 = new Move(Position.getSquare(0, 6), Position.getSquare(1, 7), Piece.WROOK);
        assertTrue(!m1.equals(m2));
        assertTrue(!m1.equals(m3));
        assertTrue(!m1.equals(m4));
        assertTrue(m1.equals(m5));
    }
}