package org.empyrn.darkknight;

import android.graphics.Color;

public class Appearance {
	final static int DARK_SQUARE = 0;
	final static int BRIGHT_SQUARE = 1;
	final static int SELECTED_SQUARE = 2;
	final static int CURSOR_SQUARE = 3;
	final static int ARROW_0 = 4;
	final static int ARROW_1 = 5;
	final static int ARROW_2 = 6;
	final static int ARROW_3 = 7;
	final static int ARROW_4 = 8;
	final static int ARROW_5 = 9;

	private final static String colorTable[] = { "#D95452", "#C5A79D",
			"#A43C3B", "#FF00FF00", "#A01F1FFF", "#A0FF1F1F", "#501F1FFF",
			"#50FF1F1F", "#1E1F1FFF", "#28FF1F1F" };

	final static int getColor(int colorType) {
		return Color.parseColor(colorTable[colorType]);
	}
}

//private final static String colorTable[] = { "#E5CB98", "#FEEAC9",
//	"#FFFF0000", "#FF00FF00", "#A01F1FFF", "#A0FF1F1F", "#501F1FFF",
//	"#50FF1F1F", "#1E1F1FFF", "#28FF1F1F" };