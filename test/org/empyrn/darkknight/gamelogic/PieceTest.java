/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.empyrn.darkknight.gamelogic;

import org.empyrn.darkknight.gamelogic.Piece;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


import static org.junit.Assert.*;

/**
 *
 * @author petero
 */
public class PieceTest {

    public PieceTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test of isWhite method, of class Piece.
     */
    @Test
    public void testIsWhite() {
        assertEquals(false, Piece.isWhite(Piece.BBISHOP));
        assertEquals(true , Piece.isWhite(Piece.WBISHOP));
        assertEquals(true , Piece.isWhite(Piece.WKING));
        assertEquals(false, Piece.isWhite(Piece.BKING));
    }
}