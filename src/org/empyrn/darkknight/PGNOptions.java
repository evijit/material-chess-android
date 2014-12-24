package org.empyrn.darkknight;

/** Settings controlling PGN import/export */
public class PGNOptions {
	public static class Viewer {
		public boolean variations;
		public boolean comments;
		public boolean nag;
		public boolean headers;
	}
	public static class Import {
		public boolean variations;
		public boolean comments;
		public boolean nag;
	}
	public static class Export {
		public boolean variations;
		public boolean comments;
		public boolean nag;
		public boolean playerAction;
		public boolean clockInfo;
		public boolean pgnPromotions;
		public boolean moveNrAfterNag;
	}

	public Viewer view;
	public Import imp;
	public Export exp;

	public PGNOptions() {
		view = new Viewer();
		imp = new Import();
		exp = new Export();
		exp.moveNrAfterNag = true;
	}
}
