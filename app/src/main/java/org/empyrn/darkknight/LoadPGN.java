package org.empyrn.darkknight;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;


import com.nemesis.materialchess.R;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.WindowManager;

public class LoadPGN extends ActionBarActivity {
    private int theme;
    private int choice;

    private static final class GameInfo {
		String event = "";
		String site = "";
		String date = "";
		String round = "";
		String white = "";
		String black = "";
		String result = "";
		long startPos;
		long endPos;
	}

	static Vector<GameInfo> gamesInFile = new Vector<GameInfo>();
	String fileName;
	ProgressDialog progress;
	static int defaultItem = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(LoadPGN.this);
        theme = sharedPreferences.getInt("Theme",R.style.AppThemeOrange);
        loadTheme();
        super.setTheme(theme);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            choice = sharedPreferences.getInt("Choice",4);
            switch(choice)
            {
                case 0: getWindow().setStatusBarColor(getResources().getColor(R.color.darkred));break;
                case 1:	getWindow().setStatusBarColor(getResources().getColor(R.color.darkblue));break;
                case 2:	getWindow().setStatusBarColor(getResources().getColor(R.color.darkpurple));break;
                case 3:	getWindow().setStatusBarColor(getResources().getColor(R.color.darkgreen));break;
                case 4:	getWindow().setStatusBarColor(getResources().getColor(R.color.darkorange));break;
                case 5:	getWindow().setStatusBarColor(getResources().getColor(R.color.darkgrey));break;
            }
        }

		Intent i = getIntent();
		fileName = i.getAction();
		showDialog(PROGRESS_DIALOG);
		new Thread(new Runnable() {
			public void run() {
				readFile();
				runOnUiThread(new Runnable() {
					public void run() {
						progress.dismiss();
						removeDialog(SELECT_GAME_DIALOG);
						showDialog(SELECT_GAME_DIALOG);
					}
				});
			}
		}).start();
	}

    protected void saveTheme(int str)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(LoadPGN.this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("Theme", str);
        editor.commit();
    }
    protected void loadTheme()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(LoadPGN.this);
        theme = sharedPreferences.getInt("Theme",R.style.AppThemeOrange);
        super.setTheme(theme);


    }
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	final static int PROGRESS_DIALOG = 0;
	final static int SELECT_GAME_DIALOG = 1;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROGRESS_DIALOG:
			progress = new ProgressDialog(this);
			progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progress.setTitle(R.string.reading_pgn_file);
			progress.setMessage(getString(R.string.please_wait));
			progress.setCancelable(false);
			return progress;
		case SELECT_GAME_DIALOG:
	    	final String[] items = new String[gamesInFile.size()];
	    	for (int i = 0; i < items.length; i++) {
	    		GameInfo gi = gamesInFile.get(i);
	    		StringBuilder info = new StringBuilder(128);
	    		info.append(gi.white);
	    		info.append(" - ");
	    		info.append(gi.black);
	    		if (gi.date.length() > 0) {
	    			info.append(' ');
	    			info.append(gi.date);
	    		}
	    		if (gi.round.length() > 0) {
	    			info.append(' ');
		    		info.append(gi.round);
	    		}
	    		if (gi.event.length() > 0) {
	    			info.append(' ');
	    			info.append(gi.event);
	    		}
	    		if (gi.site.length() > 0) {
	    			info.append(' ');
	    			info.append(gi.site);
	    		}
	    		info.append(' ');
	    		info.append(gi.result);
	    		items[i] = info.toString();
	    	}
	    	if (defaultItem >= items.length) {
	    		defaultItem = 0;
	    	}
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.select_pgn_game);
			builder.setSingleChoiceItems(items, defaultItem, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					defaultItem = item;
					sendBackResult(item);
				}
			});
			AlertDialog alert = builder.create();
			alert.setOnDismissListener(new OnDismissListener() {
				public void onDismiss(DialogInterface dialog) {
					sendBackResult(-1);
				}
			});
			return alert;
		default:
			return null;
		}
	}

	static private final class BufferedRandomAccessFileReader {
		RandomAccessFile f;
		byte[] buffer = new byte[8192];
		long bufStartFilePos = 0;
		int bufLen = 0;
		int bufPos = 0;

		BufferedRandomAccessFileReader(String fileName) throws FileNotFoundException {
			f = new RandomAccessFile(fileName, "r");
		}
		final long length() throws IOException {
			return f.length();
		}
		final long getFilePointer() throws IOException {
			return bufStartFilePos + bufPos;
		}
		final void close() throws IOException {
			f.close();
		}

		private final int EOF = -1024;

		final String readLine() throws IOException {
			// First handle the common case where the next line is entirely 
			// contained in the buffer
			for (int i = bufPos; i < bufLen; i++) {
				byte b = buffer[i];
				if ((b == '\n') || (b == '\r')) {
					String line = new String(buffer, bufPos, i - bufPos);
					for ( ; i < bufLen; i++) {
						b = buffer[i];
						if ((b != '\n') && (b != '\r')) {
							bufPos = i;
							return line;
						}
					}
					break;
				}
			}

			// Generic case
			byte[] lineBuf = new byte[8192];
			int lineLen = 0;
			int b;
			while (true) {
				b = getByte();
				if (b == '\n' || b == '\r' || b == EOF)
					break;
				lineBuf[lineLen++] = (byte)b;
				if (lineLen >= lineBuf.length)
					break;
			}
			while (true) {
				b = getByte();
				if ((b != '\n') && (b != '\r')) {
					if (b != EOF)
						bufPos--;
					break;
				}
			}
			if ((b == EOF) && (lineLen == 0))
				return null;
			else
				return new String(lineBuf, 0, lineLen);
		}
		
		private final int getByte() throws IOException {
			if (bufPos >= bufLen) {
				bufStartFilePos = f.getFilePointer();
				bufLen = f.read(buffer);
				bufPos = 0;
				if (bufLen <= 0)
					return EOF;
			}
			return buffer[bufPos++];
		}
	}
	
	static long lastModTime = -1;
	static String lastFileName = "";
	
	private final void readFile() {
		if (!fileName.equals(lastFileName))
			defaultItem = 0;
		long modTime = new File(fileName).lastModified();
		if ((modTime == lastModTime) && fileName.equals(lastFileName))
			return;
		lastModTime = modTime;
		lastFileName = fileName;
		try {
			int percent = -1;
			gamesInFile.clear();
			BufferedRandomAccessFileReader f = new BufferedRandomAccessFileReader(fileName);
			long fileLen = f.length();
			GameInfo gi = null;
			boolean inHeader = false;
			long filePos = 0;
			while (true) {
				filePos = f.getFilePointer();
				String line = f.readLine();
				if (line == null)
					break; // EOF
				int len = line.length();
				if (len == 0)
					continue;
				boolean isHeader = line.charAt(0) == '[';
				if (isHeader) {
					if (!line.contains("\"")) // Try to avoid some false positives
						isHeader = false;
				}
				if (isHeader) {
					if (!inHeader) { // Start of game
						inHeader = true;
						if (gi != null) {
							gi.endPos = filePos;
							gamesInFile.add(gi);
							final int newPercent = (int)(filePos * 100 / fileLen);
							if (newPercent > percent) {
								percent =  newPercent;
								runOnUiThread(new Runnable() {
									public void run() {
										progress.setProgress(newPercent);
									}
								});
							}
						}
						gi = new GameInfo();
						gi.startPos = filePos;
						gi.endPos = -1;
					}
					if (line.startsWith("[Event ")) {
						gi.event = line.substring(8, len - 2);
						if (gi.event.equals("?")) gi.event = "";
					} else if (line.startsWith("[Site ")) {
						gi.site = line.substring(7, len - 2);
						if (gi.site.equals("?")) gi.site= "";
					} else if (line.startsWith("[Date ")) {
						gi.date = line.substring(7, len - 2);
						if (gi.date.equals("?")) gi.date= "";
					} else if (line.startsWith("[Round ")) {
						gi.round = line.substring(8, len - 2);
						if (gi.round.equals("?")) gi.round= "";
					} else if (line.startsWith("[White ")) {
						gi.white = line.substring(8, len - 2);
					} else if (line.startsWith("[Black ")) {
						gi.black = line.substring(8, len - 2);
					} else if (line.startsWith("[Result ")) {
						gi.result = line.substring(9, len - 2);
					}
				} else {
					inHeader = false;
				}
			}
			if (gi != null) {
				gi.endPos = filePos;
				gamesInFile.add(gi);
			}
			f.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	private final void sendBackResult(int gameNo) {
		try {
			if ((gameNo >= 0) && (gameNo < gamesInFile.size())) {
				GameInfo gi = gamesInFile.get(gameNo);
				RandomAccessFile f;
				f = new RandomAccessFile(fileName, "r");
				byte[] pgnData = new byte[(int) (gi.endPos - gi.startPos)];
				f.seek(gi.startPos);
				f.readFully(pgnData);
				f.close();
				String result = new String(pgnData);
				setResult(RESULT_OK, (new Intent()).setAction(result));
				finish();
			}
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
		setResult(RESULT_CANCELED);
		finish();
	}
}
