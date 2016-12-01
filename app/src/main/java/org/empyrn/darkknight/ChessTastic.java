package org.empyrn.darkknight;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.empyrn.darkknight.bluetooth.BluetoothGameController;
import org.empyrn.darkknight.bluetooth.DeviceListActivity;
import org.empyrn.darkknight.gamelogic.ChessController;
import org.empyrn.darkknight.gamelogic.ChessParseError;
import org.empyrn.darkknight.gamelogic.GameTree.Node;
import org.empyrn.darkknight.gamelogic.Move;
import org.empyrn.darkknight.gamelogic.PgnToken;
import org.empyrn.darkknight.gamelogic.Position;
import org.empyrn.darkknight.gamelogic.TextIO;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.ClipboardManager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import com.nemesis.materialchess.R;

public class ChessTastic extends AppCompatActivity implements GUIInterface, OnShowcaseEventListener {
	// FIXME!!! Computer clock should stop if phone turned off (computer stops
	// thinking if unplugged)
	// FIXME!!! book.txt (and test classes) should not be included in apk
	// FIXME!!! Current position in game should be visible:
	// TextView.bringPointIntoView()

	// FIXME!!! PGN view option: game continuation (for training)
	// FIXME!!! PGN view option: Promote played variations to mainline (default
	// true)
	// FIXME!!! Implement "revert to mainline": Go back, set default to follow
	// mainline back/forward from point.
	// FIXME!!! Command to go to next/previous move in PGN export order.

	// FIXME!!! Remove invalid playerActions in PGN import (should be done in
	// verifyChildren)

	// FIXME!!! Implement "limit strength" option
	// FIXME!!! Implement PGN database support (and FEN?)
	// FIXME!!! Implement pondering (permanent brain)
	// FIXME!!! Implement multi-variation analysis mode
	// FIXME!!! Save analysis (analyze mode and computer thinking mode) as PGN
	// comments and/or variation
	// FIXME!!! Online play on FICS
	// FIXME!!! Make program translatable

	// FIXME!!! Add support for all time controls defined by the PGN standard
	// FIXME!!! How to handle hour-glass time control?
	// FIXME!!! What should happen if you change time controls in the middle of
	// a game?

	// FIXME!!! Bluetooth code needs cleanup
	// FIXME!!! No display of clock on Honeycomb

	private ChessBoard cb;
	private ChessController ctrl = null;
	private boolean mShowThinking;
	private boolean mShowBookHints;
	private int maxNumArrows;
	private GameMode gameMode;
	private boolean boardFlipped;
	private boolean autoSwapSides;
	private int maxDepth;

	private TextView status;
	private ScrollView moveListScroll;
	private TextView moveList;
	private TextView thinking;

	SharedPreferences settings;

	private float scrollSensitivity;
	private boolean invertScrollDirection;
	private boolean soundEnabled;
	private MediaPlayer moveSound;

	private final String bookDir = "ChessTastic";
	private final String pgnDir = "ChessTastic" + File.separator + "pgn";
	private String currentBookFile = "";
	private PGNOptions pgnOptions = new PGNOptions();

	private long lastVisibleMillis; // Time when GUI became invisible. 0 if
									// currently visible.
	private long lastComputationMillis; // Time when engine last showed that it
										// was computing.

	BluetoothGameController bGameCtrl = null;

	PgnScreenText gameTextListener;
	
	private DrawerLayout drawerLayoutt;
	private ListView listView;
	private ActionBarDrawerToggle actionBarDrawerToggle;
	private String[] navigationDrawerItems;
	private MyAdapter myadapter;

    int choice,theme;
    public static Context contextOfApplication;

    private boolean shouldGoInvisible;
    private static final float ALPHA_DIM_VALUE = 1f;

    ShowcaseView sv;
    private ViewTarget target;
    private LinearLayout homelayout;
    private SharedPreferences sharedPreferences;
    private Toolbar toolbar;

    /** Called when the activity is first created. */
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ChessTastic.this);
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

		settings = PreferenceManager.getDefaultSharedPreferences(this);
		settings.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreferences, String key) {
				readPrefs();
				ctrl.setGameMode(gameMode);
				ctrl.setMaxDepth(maxDepth);
			}
		});

        contextOfApplication = this;

		initUI(true);

		gameTextListener = new PgnScreenText(pgnOptions);
		ctrl = new ChessController(this, gameTextListener, pgnOptions);
		ctrl.newGame(new GameMode(GameMode.TWO_PLAYERS));

		readPrefs();
		ctrl.setMaxDepth(maxDepth);
		ctrl.newGame(gameMode);
		{
			byte[] data = null;
			if (savedInstanceState != null) {
				data = savedInstanceState.getByteArray("gameState");
			} else {
				String dataStr = settings.getString("gameState", null);
				if (dataStr != null)
					data = strToByteArr(dataStr);
			}
			if (data != null)
				ctrl.fromByteArray(data);
		}

		ctrl.setGuiPaused(true);
		ctrl.setGuiPaused(false);
		ctrl.startGame();


	}

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void dimView(LinearLayout view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            view.setAlpha(ALPHA_DIM_VALUE);
        }
    }

    @Override
    public void onShowcaseViewHide(ShowcaseView showcaseView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            homelayout.setAlpha(1f);
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
        }
    }

    @Override
    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {

    }

    @Override
    public void onShowcaseViewShow(ShowcaseView showcaseView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getResources().getColor(R.color.darkgrey));}
        dimView(homelayout);
    }

    @Override
    public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public static Context getContextOfApplication(){
        return contextOfApplication;
    }

    protected void saveTheme(int str)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ChessTastic.this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("Theme", str);
        editor.commit();
    }
    protected void loadTheme()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ChessTastic.this);
        theme = sharedPreferences.getInt("Theme",R.style.AppThemeOrange);
        super.setTheme(theme);


    }

	private final byte[] strToByteArr(String str) {
		int nBytes = str.length() / 2;
		byte[] ret = new byte[nBytes];
		for (int i = 0; i < nBytes; i++) {
			int c1 = str.charAt(i * 2) - 'A';
			int c2 = str.charAt(i * 2 + 1) - 'A';
			ret[i] = (byte) (c1 * 16 + c2);
		}
		return ret;
	}

	private final String byteArrToString(byte[] data) {
		StringBuilder ret = new StringBuilder(32768);
		int nBytes = data.length;
		for (int i = 0; i < nBytes; i++) {
			int b = data[i];
			if (b < 0)
				b += 256;
			char c1 = (char) ('A' + (b / 16));
			char c2 = (char) ('A' + (b & 15));
			ret.append(c1);
			ret.append(c2);
		}
		return ret.toString();
	}
	
	@Override
	public void onBackPressed() {
	    if(drawerLayoutt.isDrawerOpen(Gravity.START|Gravity.LEFT)){
	    	drawerLayoutt.closeDrawers();
	        return;
	    }
	    super.onBackPressed();
	}

    private void displayShowcaseViewOne() {
        target = new ViewTarget(R.id.chessboard, this);
        sv = new ShowcaseView.Builder(this, true)
                .setTarget(target)
                .setContentTitle("Welcome")
                .setContentText("To play, tap a Chess Piece and then tap the destination square to move.")
                .setStyle(R.style.CustomShowcaseTheme2)
                .setShowcaseEventListener(new OnShowcaseEventListener() {

                    @Override
                    public void onShowcaseViewShow(final ShowcaseView scv) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                            getWindow().setStatusBarColor(getResources().getColor(R.color.darkgrey));
                        }
                        dimView(homelayout);
                    }

                    @Override
                    public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {

                    }

                    @Override
                    public void onShowcaseViewHide(final ShowcaseView scv) {
                        scv.setVisibility(View.GONE);
                        showOverlayTutorialTwo();
                    }

                    @Override
                    public void onShowcaseViewDidHide(final ShowcaseView scv) {
                    }

                })
                .build();
    }

    public void showOverlayTutorialTwo() {
        target = new ViewTarget(R.id.moveList, this);
        sv = new ShowcaseView.Builder(this, true)
                .setTarget(target)
                .setContentTitle("Move List")
                .setContentText("This box shows the details of the moves in the game. The text size can be adjusted in settings.")
                .setStyle(R.style.CustomShowcaseTheme2)
                .setShowcaseEventListener(new OnShowcaseEventListener() {

                    @Override
                    public void onShowcaseViewShow(final ShowcaseView scv) {
                    }

                    @Override
                    public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {

                    }

                    @Override
                    public void onShowcaseViewHide(final ShowcaseView scv) {
                        showOverlayTutorialThree();
                        scv.setVisibility(View.GONE);
                    }

                    @Override
                    public void onShowcaseViewDidHide(final ShowcaseView scv) {
                    }

                })
                .build();


    }

    public void showOverlayTutorialThree() {
        //target = new ViewTarget(R.id.moveList, this);
        Target viewTarget = new Target() {
            @Override
            public Point getPoint() {
                return new ViewTarget(toolbar.findViewById(R.id.item_redo)).getPoint();
            }
        };
        sv = new ShowcaseView.Builder(this, true)
                .setTarget(viewTarget)
                .setContentTitle("Undo and Redo")
                .setContentText("Use the Undo and Redo Buttons to go back and forth between moves.")
                .setStyle(R.style.CustomShowcaseTheme2)
                .setShowcaseEventListener(new OnShowcaseEventListener() {

                    @Override
                    public void onShowcaseViewShow(final ShowcaseView scv) { }

                    @Override
                    public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {

                    }

                    @Override
                    public void onShowcaseViewHide(final ShowcaseView scv) {
                        drawerLayoutt.openDrawer(Gravity.LEFT);
                        showOverlayTutorialFour();
                        scv.setVisibility(View.GONE);
                    }

                    @Override
                    public void onShowcaseViewDidHide(final ShowcaseView scv) { }

                })
                .build();


    }

    public void showOverlayTutorialFour() {
        //target = new ViewTarget(R.id.moveList, this);
        /*Target viewTarget = new Target() {
            @Override
            public Point getPoint() {
                return new ViewTarget(toolbar.findViewById(android.R.id.home)).getPoint();
            }
        };*/
        sv = new ShowcaseView.Builder(this, true)
                .setTarget(new ViewTarget( ((ViewGroup)findViewById(R.id.my_awesome_toolbar2)).getChildAt(0) ) )
                .setContentTitle("More Options")
                .setContentText("Slide from the left to reveal more options.")
                .setStyle(R.style.CustomShowcaseTheme3)
                .setShowcaseEventListener(new OnShowcaseEventListener() {

                    @Override
                    public void onShowcaseViewShow(final ShowcaseView scv) {
                        }

                    @Override
                    public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {

                    }

                    @Override
                    public void onShowcaseViewHide(final ShowcaseView scv) {
                        drawerLayoutt.closeDrawers();
                        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
                        if (currentapiVersion >= Build.VERSION_CODES.HONEYCOMB) {
                            homelayout.setAlpha(1f);
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
                        }
                        scv.setVisibility(View.GONE);
                    }

                    @Override
                    public void onShowcaseViewDidHide(final ShowcaseView scv) { }

                })
                .build();


    }


    @Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		actionBarDrawerToggle.onConfigurationChanged(newConfig);
		ChessBoard oldCB = cb;
		String statusStr = status.getText().toString();
		initUI(false);
		readPrefs();
		ctrl.setMaxDepth(maxDepth);
		cb.cursorX = oldCB.cursorX;
		cb.cursorY = oldCB.cursorY;
		cb.cursorVisible = oldCB.cursorVisible;
		cb.setPosition(oldCB.pos);
		cb.setFlipped(oldCB.flipped);
		cb.oneTouchMoves = oldCB.oneTouchMoves;
		setSelection(oldCB.selectedSquare);
		setStatusString(statusStr);
		moveListUpdated();
		updateThinkingInfo();
	}

	private final void initUI(boolean initTitle) {
		setContentView(R.layout.main);
		
		toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar2);
		navigationDrawerItems = getResources().getStringArray(R.array.navigation_drawer_items);
		drawerLayoutt = (DrawerLayout) findViewById(R.id.drawer_layout);
		myadapter=new MyAdapter(this);
		listView = (ListView) findViewById(R.id.left_drawer);
		SpannableString s = new SpannableString("CHESSTASTIC");
		s.setSpan(new TypefaceSpan(this, "KlinicSlabBold.otf"), 0, s.length(),
		        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		TextView txt1 = (TextView) findViewById(R.id.status); 
		TextView txt2 = (TextView) findViewById(R.id.moveList); 
		TextView txt3 = (TextView) findViewById(R.id.thinking); 
		Typeface font = Typeface.createFromAsset(getAssets(), "fonts/KlinicSlabMedium.otf"); 
		Typeface font1 = Typeface.createFromAsset(getAssets(), "fonts/KlinicSlabBold.otf"); 
		txt1.setTypeface(font1); 
		txt2.setTypeface(font); 
		txt3.setTypeface(font); 
		
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle(s);


            
            //drawerLayoutt.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
            // set up the drawer's list view with items and click listener
            listView.setAdapter(myadapter);
            listView.setOnItemClickListener(new DrawerItemClickListener());
            //actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayoutt, toolbar, R.string.app_name, R.string.app_name);

            actionBarDrawerToggle=new ActionBarDrawerToggle(this, drawerLayoutt, toolbar, R.string.app_name, R.string.app_name){

                float mPreviousOffset = 0f;

                @Override
                public void onDrawerClosed(View arg0) {
                    super.onDrawerClosed(arg0);
                    shouldGoInvisible = false;
                    invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }

                @Override
                public void onDrawerOpened(View arg0) {
                    super.onDrawerOpened(arg0);
                    shouldGoInvisible = true;
                    invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }

                @Override
                public void onDrawerSlide(View arg0, float slideOffset) {
                    super.onDrawerSlide(arg0, slideOffset);
                    if(slideOffset > mPreviousOffset && !shouldGoInvisible){
                        shouldGoInvisible = true;
                        invalidateOptionsMenu();
                    }else if(mPreviousOffset > slideOffset && slideOffset < 0.5f && shouldGoInvisible){
                        shouldGoInvisible = false;
                        invalidateOptionsMenu();
                    }
                    mPreviousOffset = slideOffset;


                }

                @Override
                public void onDrawerStateChanged(int arg0) {
                    // or use states of the drawer to hide/show the items

                }};
            drawerLayoutt.setDrawerListener(actionBarDrawerToggle);
            // enable ActionBar app icon to behave as action to toggle nav drawer
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            /*if (savedInstanceState == null) {
            selectItem(0);
            }*/

            final String PREFS_NAME = "MyPrefsFile";

            SharedPreferences settings2 = getSharedPreferences(PREFS_NAME, 0);

            if (settings2.getBoolean("my_first_time", true)) {
                //the app is being launched for first time, do something
                homelayout=(LinearLayout)findViewById(R.id.my_home);

                displayShowcaseViewOne();

                // first time task

                // record the fact that the app has been started at least once
                settings2.edit().putBoolean("my_first_time", false).commit();
            }



        }



		
		
		
		
		status = (TextView) findViewById(R.id.status);
		moveListScroll = (ScrollView) findViewById(R.id.scrollView);
		moveList = (TextView) findViewById(R.id.moveList);
		thinking = (TextView) findViewById(R.id.thinking);
		status.setFocusable(false);
		moveListScroll.setFocusable(false);
		moveList.setFocusable(false);
		thinking.setFocusable(false);

		cb = (ChessBoard) findViewById(R.id.chessboard);
		cb.setFocusable(true);
		cb.requestFocus();
		cb.setClickable(true);

		final GestureDetector gd = new GestureDetector(
				new GestureDetector.SimpleOnGestureListener() {
					private float scrollX = 0;
					private float scrollY = 0;

					public boolean onDown(MotionEvent e) {
						scrollX = 0;
						scrollY = 0;
						return false;
					}

					public boolean onScroll(MotionEvent e1, MotionEvent e2,
							float distanceX, float distanceY) {
						cb.cancelLongPress();
						if (invertScrollDirection) {
							distanceX = -distanceX;
							distanceY = -distanceY;
						}
						if (scrollSensitivity > 0) {
							scrollX += distanceX;
							scrollY += distanceY;
							float scrollUnit = cb.sqSize * scrollSensitivity;
							if (Math.abs(scrollX) >= Math.abs(scrollY)) {
								// Undo/redo
								int nRedo = 0, nUndo = 0;
								while (scrollX > scrollUnit) {
									nRedo++;
									scrollX -= scrollUnit;
								}
								while (scrollX < -scrollUnit) {
									nUndo++;
									scrollX += scrollUnit;
								}
								if (nUndo + nRedo > 0)
									scrollY = 0;
								if (nRedo + nUndo > 1) {
									boolean analysis = gameMode.analysisMode();
									boolean human = gameMode.playerWhite()
											|| gameMode.playerBlack();
									if (analysis || !human)
										ctrl.setGameMode(new GameMode(
												GameMode.TWO_PLAYERS));
								}
								for (int i = 0; i < nRedo; i++)
									ctrl.redoMove();
								for (int i = 0; i < nUndo; i++)
									ctrl.undoMove();
								ctrl.setGameMode(gameMode);
							} else {
								// Next/previous variation
								int varDelta = 0;
								while (scrollY > scrollUnit) {
									varDelta++;
									scrollY -= scrollUnit;
								}
								while (scrollY < -scrollUnit) {
									varDelta--;
									scrollY += scrollUnit;
								}
								if (varDelta != 0)
									scrollX = 0;
								ctrl.changeVariation(varDelta);
							}
						}
						return true;
					}

					public boolean onSingleTapUp(MotionEvent e) {
						cb.cancelLongPress();
						handleClick(e);
						return true;
					}

					public boolean onDoubleTapEvent(MotionEvent e) {
						if (e.getAction() == MotionEvent.ACTION_UP)
							handleClick(e);
						return true;
					}

					private final void handleClick(MotionEvent e) {
						if (ctrl.humansTurn()) {
							int sq = cb.eventToSquare(e);
							Move m = cb.mousePressed(sq);
							if (m != null)
								ctrl.makeHumanMove(m);
						}
					}
				});
		cb.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				return gd.onTouchEvent(event);
			}
		});
		cb.setOnTrackballListener(new ChessBoard.OnTrackballListener() {
			public void onTrackballEvent(MotionEvent event) {
				if (ctrl.humansTurn()) {
					Move m = cb.handleTrackballEvent(event);
					if (m != null) {
						ctrl.makeHumanMove(m);
					}
				}
			}
		});
		cb.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View v) {
				removeDialog(CLIPBOARD_DIALOG);
				showDialog(CLIPBOARD_DIALOG);
				return true;
			}
		});
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (ctrl != null) {
			byte[] data = ctrl.toByteArray();
			outState.putByteArray("gameState", data);
		}
	}

	@Override
	protected void onResume() {
		lastVisibleMillis = 0;
		if (ctrl != null && !gameMode.bluetoothMode()) {
			ctrl.setGuiPaused(false);
		}
		updateNotification();
		super.onResume();
        loadTheme();
	}

	@Override
	protected void onPause() {
		if (ctrl != null && !gameMode.bluetoothMode()) {
			ctrl.setGuiPaused(true);
			byte[] data = ctrl.toByteArray();
			Editor editor = settings.edit();
			String dataStr = byteArrToString(data);
			editor.putString("gameState", dataStr);
			editor.commit();
		}

		lastVisibleMillis = System.currentTimeMillis();
		updateNotification();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		if (ctrl != null) {
			ctrl.shutdownEngine();
		}
		setNotification(false);

		if (bGameCtrl != null) {
			bGameCtrl.stopBluetoothService();
			bGameCtrl = null;
		}

		super.onDestroy();
	}



	private final void readPrefs() {
		String tmp = settings.getString("gameMode", "1");
		int modeNr = Integer.parseInt(tmp);
		gameMode = new GameMode(modeNr);
		maxDepth = Integer
				.parseInt(settings.getString("difficultyDepth", "2"));
		boardFlipped = settings.getBoolean("boardFlipped", false);
		autoSwapSides = settings.getBoolean("autoSwapSides", false);
		setBoardFlip();
		cb.oneTouchMoves = settings.getBoolean("oneTouchMoves", false);

		mShowThinking = settings.getBoolean("showThinking", false);
		tmp = settings.getString("thinkingArrows", "2");
		maxNumArrows = Integer.parseInt(tmp);
		mShowBookHints = settings.getBoolean("bookHints", false);

		tmp = settings.getString("timeControl", "15000");
		int timeControl = Integer.parseInt(tmp);
		tmp = settings.getString("movesPerSession", "60");
		int movesPerSession = Integer.parseInt(tmp);
		tmp = settings.getString("timeIncrement", "0");
		int timeIncrement = Integer.parseInt(tmp);
		ctrl.setTimeLimit(timeControl, movesPerSession, timeIncrement);

		tmp = settings.getString("scrollSensitivity", "0");
		scrollSensitivity = Float.parseFloat(tmp);
		invertScrollDirection = settings.getBoolean("invertScrollDirection",
				false);

		tmp = settings.getString("fontSize", "20");
		int fontSize = Integer.parseInt(tmp);
		status.setTextSize(fontSize);
		moveList.setTextSize(fontSize);
		thinking.setTextSize(fontSize);
		thinking.setPadding(24, 2, 24, 2);
		status.setPadding(24, 2, 24, 2);
		moveList.setPadding(24, 2, 24, 2);
		soundEnabled = settings.getBoolean("soundEnabled", false);

		String bookFile = settings.getString("bookFile", "");
		setBookFile(bookFile);
		updateThinkingInfo();

		pgnOptions.view.variations = settings
				.getBoolean("viewVariations", true);
		pgnOptions.view.comments = settings.getBoolean("viewComments", true);
		pgnOptions.view.nag = settings.getBoolean("viewNAG", true);
		pgnOptions.view.headers = settings.getBoolean("viewHeaders", false);
		pgnOptions.imp.variations = settings.getBoolean("importVariations",
				true);
		pgnOptions.imp.comments = settings.getBoolean("importComments", true);
		pgnOptions.imp.nag = settings.getBoolean("importNAG", true);
		pgnOptions.exp.variations = settings.getBoolean("exportVariations",
				true);
		pgnOptions.exp.comments = settings.getBoolean("exportComments", true);
		pgnOptions.exp.nag = settings.getBoolean("exportNAG", true);
		pgnOptions.exp.playerAction = settings.getBoolean("exportPlayerAction",
				false);
		pgnOptions.exp.clockInfo = settings.getBoolean("exportTime", false);

		cb.setColors();

		// if Bluetooth was on but has been changed, disable it
		if (!gameMode.bluetoothMode()) {
			if (bGameCtrl != null) {
				bGameCtrl.stopBluetoothService();
				bGameCtrl = null;
			}
		}

		// otherwise, if it has been activated, enable it
		else {
			if (bGameCtrl == null) {
				bGameCtrl = new BluetoothGameController(this, ctrl, gameMode);
				bGameCtrl.setupBluetoothService();
			}
		}

		gameTextListener.clear();
		ctrl.prefsChanged();
	}

	private final void setBookFile(String bookFile) {
		currentBookFile = bookFile;
		if (bookFile.length() > 0) {
			File extDir = Environment.getExternalStorageDirectory();
			String sep = File.separator;
			bookFile = extDir.getAbsolutePath() + sep + bookDir + sep
					+ bookFile;
		}
		ctrl.setBookFileName(bookFile);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.options_menu, menu);
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		//final MenuItem bluetoothSubmenuItem = (MenuItem) menu
			//	.findItem(R.id.bluetooth_submenu);
		final MenuItem undoMenuItem = (MenuItem) menu.findItem(R.id.item_undo);
		final MenuItem redoMenuItem = (MenuItem) menu.findItem(R.id.item_redo);
		/*final MenuItem gotoMoveMenuItem = (MenuItem) menu
				.findItem(R.id.item_goto_move);
		final MenuItem loadPGNMenuItem = (MenuItem) menu
				.findItem(R.id.item_load_pgn_file);
		final MenuItem editBoardMenuItem = (MenuItem) menu
				.findItem(R.id.item_editboard);
		final MenuItem forceMoveMenuItem = (MenuItem) menu
				.findItem(R.id.item_force_move);
		final MenuItem resignMenuItem = (MenuItem) menu
				.findItem(R.id.item_resign);
		// final MenuItem drawMenuItem = (MenuItem)
		// menu.findItem(R.id.item_draw);
		if (gameMode.bluetoothMode()) {
			bluetoothSubmenuItem.setEnabled(true);
			undoMenuItem.setEnabled(false);
			redoMenuItem.setEnabled(false);
			gotoMoveMenuItem.setEnabled(false);
			loadPGNMenuItem.setEnabled(false);
			editBoardMenuItem.setEnabled(false);
			forceMoveMenuItem.setEnabled(false);
			resignMenuItem.setEnabled(false);
		} else {
			bluetoothSubmenuItem.setEnabled(false);
			undoMenuItem.setEnabled(true);
			redoMenuItem.setEnabled(true);
			gotoMoveMenuItem.setEnabled(true);
			loadPGNMenuItem.setEnabled(true);
			editBoardMenuItem.setEnabled(true);
			forceMoveMenuItem.setEnabled(true);
			resignMenuItem.setEnabled(true);
		}

		if (ctrl != null && ctrl.computerBusy()) {
			forceMoveMenuItem.setEnabled(true);
		} else {
			forceMoveMenuItem.setEnabled(false);
		}*/
        boolean drawerOpen = shouldGoInvisible;
        hideMenuItems(menu, !drawerOpen);
        return super.onPrepareOptionsMenu(menu);
	}

    private void hideMenuItems(Menu menu, boolean visible)
    {

        for(int i = 0; i < menu.size(); i++){

            menu.getItem(i).setVisible(visible);

        }
    }

	static private final int RESULT_EDITBOARD = 0;
	static private final int RESULT_SETTINGS = 1;
	static private final int RESULT_LOAD_PGN = 2;
	static private final int REQUEST_CONNECT_DEVICE = 3;
	static private final int REQUEST_ENABLE_BT = 4;
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
	    super.onPostCreate(savedInstanceState);
	    try{
	    actionBarDrawerToggle.syncState();}
	    catch(Exception e){};
	}



	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		try{
			if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
		        return true;
		    }
			}
		    catch(Exception e){};
		
		switch (item.getItemId()) {
		case R.id.item_undo:
			ctrl.undoMove();
			return true;
		case R.id.item_redo:
			ctrl.redoMove();
			return true;
		/*case R.id.item_new_game:
			if (gameMode.bluetoothMode()) {
				Intent serverIntent = new Intent(this, DeviceListActivity.class);
				startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
				return true;
			}

			if (autoSwapSides
					&& (gameMode.playerWhite() != gameMode.playerBlack())) {
				int gameModeType;
				if (gameMode.playerWhite()) {
					gameModeType = GameMode.PLAYER_BLACK;
				} else {
					gameModeType = GameMode.PLAYER_WHITE;
				}
				Editor editor = settings.edit();
				String gameModeStr = String.format("%d", gameModeType);
				editor.putString("gameMode", gameModeStr);
				editor.commit();
				gameMode = new GameMode(gameModeType);
			}

			ctrl.newGame(gameMode);
			ctrl.startGame();
			return true;
		case R.id.item_editboard: {
			Intent i = new Intent(ChessTastic.this, EditBoard.class);
			i.setAction(ctrl.getFEN());
			startActivityForResult(i, RESULT_EDITBOARD);
			return true;
		}
		case R.id.item_flipboard: {
			this.flipBoard();
			return true;
		}
		case R.id.item_settings: {
			Intent i = new Intent(ChessTastic.this, Preferences.class);
			startActivityForResult(i, RESULT_SETTINGS);
			return true;
		}
		
		case R.id.item_goto_move: {
			showDialog(SELECT_MOVE_DIALOG);
			return true;
		}
		case R.id.item_force_move: {
			ctrl.stopSearch();
			return true;
		}
			/*
			 * case R.id.item_draw: { if (ctrl.humansTurn()) { if
			 * (!ctrl.claimDrawIfPossible()) {
			 * Toast.makeText(getApplicationContext(), R.string.offer_draw,
			 * Toast.LENGTH_SHORT).show(); } } return true; }
			 /
		case R.id.item_resign: {
			if (ctrl.humansTurn()) {
				removeDialog(CONFIRM_RESIGN_DIALOG);
				showDialog(CONFIRM_RESIGN_DIALOG);
			}
			return true;
		}
		case R.id.select_book:
			removeDialog(SELECT_BOOK_DIALOG);
			showDialog(SELECT_BOOK_DIALOG);
			return true;
		case R.id.item_load_pgn_file:
			removeDialog(SELECT_PGN_FILE_DIALOG);
			showDialog(SELECT_PGN_FILE_DIALOG);
			return true;
		case R.id.bluetooth_create:
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			return true;
		case R.id.bluetooth_set_discoverable:
			if (bGameCtrl == null) {
				Toast.makeText(getApplicationContext(),
						"Bluetooth mode is not enabled", Toast.LENGTH_SHORT)
						.show();
				return true;
			}

			if (bGameCtrl.getmBluetoothAdapter().getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
				Intent discoverableIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				discoverableIntent.putExtra(
						BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
				startActivity(discoverableIntent);
			} else {
				Toast.makeText(getApplicationContext(),
						"This device is already discoverable",
						Toast.LENGTH_SHORT).show();
			}

			return true;
		case R.id.bluetooth_reset:
			if (bGameCtrl != null) {
				bGameCtrl.reset();
			}
			return true;
		case R.id.item_about:
			showDialog(ABOUT_DIALOG);
			return true;*/
		}

		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case RESULT_SETTINGS:
			readPrefs();
			ctrl.setGameMode(gameMode);
			ctrl.setMaxDepth(maxDepth);
			break;
		case RESULT_EDITBOARD:
			if (resultCode == RESULT_OK) {
				try {
					String fen = data.getAction();
					ctrl.setFENOrPGN(fen);
				} catch (ChessParseError e) {
				}
			}
			break;
		case RESULT_LOAD_PGN:
			if (resultCode == RESULT_OK) {
				try {
					String pgn = data.getAction();
					ctrl.setFENOrPGN(pgn);
				} catch (ChessParseError e) {
					Toast.makeText(getApplicationContext(), e.getMessage(),
							Toast.LENGTH_SHORT).show();
				}
			}
			break;
		case REQUEST_CONNECT_DEVICE:
			if (bGameCtrl == null) {
				if (bGameCtrl == null) {
					Toast.makeText(getApplicationContext(),
							"Bluetooth mode is not enabled", Toast.LENGTH_SHORT)
							.show();
					return;
				}
			}

			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BluetoothDevice object
				BluetoothDevice device = bGameCtrl.getmBluetoothAdapter()
						.getRemoteDevice(address);

				if (device != null) {
					// Attempt to connect to the device
					bGameCtrl.connectToDevice(device);
				} else {
					showDialog(R.string.bt_not_enabled_leaving);
				}
			}
			break;
		case REQUEST_ENABLE_BT:
			// when the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				if (bGameCtrl == null)
					bGameCtrl = new BluetoothGameController(this, ctrl,
							gameMode);

				// Bluetooth is now enabled, so set up a chat session
				bGameCtrl.setupBluetoothService();
			} else {
				// Bluetooth not enabled or an error occurred
				showDialog(R.string.bt_not_enabled_leaving);
			}
			break;
		}
	}

	private final void flipBoard() {
		boardFlipped = !boardFlipped;

		Editor editor = settings.edit();
		editor.putBoolean("boardFlipped", boardFlipped);
		editor.apply();

		setBoardFlip();
	}

	private final void setBoardFlip() {
		boolean flipped = boardFlipped;
		if (autoSwapSides) {
			if (gameMode.analysisMode()) {
				flipped = !cb.pos.whiteMove;
			} else if (gameMode.playerWhite() && gameMode.playerBlack()) {
				flipped = !cb.pos.whiteMove;
			} else if (gameMode.playerWhite()) {
				flipped = false;
			} else if (gameMode.playerBlack()) {
				flipped = true;
			} else { // two computers
				flipped = !cb.pos.whiteMove;
			}
		}
		cb.setFlipped(flipped);
	}

	@Override
	public void setSelection(int sq) {
		cb.setSelection(sq);
	}

	@Override
	public void setStatusString(String str) {
		status.setText(str);
	}

	@Override
	public void moveListUpdated() {
		moveList.setText("\n"+gameTextListener.getSpannableData());
		if (gameTextListener.atEnd())
			moveListScroll.fullScroll(ScrollView.FOCUS_DOWN);
	}

	@Override
	public void setPosition(Position pos, String variantInfo,
			List<Move> variantMoves) {
		variantStr = variantInfo;
		this.variantMoves = variantMoves;
		cb.setPosition(pos);
		setBoardFlip();
		updateThinkingInfo();
	}

	private String thinkingStr = "";
	private String bookInfoStr = "";
	private String variantStr = "";
	private List<Move> pvMoves = null;
	private List<Move> bookMoves = null;
	private List<Move> variantMoves = null;

	@Override
	public void setThinkingInfo(String pvStr, String bookInfo,
			List<Move> pvMoves, List<Move> bookMoves) {
		thinkingStr = pvStr;
		bookInfoStr = bookInfo;
		this.pvMoves = pvMoves;
		this.bookMoves = bookMoves;

		updateThinkingInfo();

		if (ctrl.computerBusy())
			lastComputationMillis = System.currentTimeMillis();
		else
			lastComputationMillis = 0;

		updateNotification();
	}

	/*
	 * private double parseEvaluationFromThinkingString(String thinkingStr) {
	 * double eval;
	 * 
	 * try { StringTokenizer tokenizer = new StringTokenizer(thinkingStr);
	 * tokenizer.nextToken(); // the first token is move-depth number
	 * 
	 * String evalStr = tokenizer.nextToken(); if (evalStr.startsWith(">=") ||
	 * evalStr.startsWith("<=")) evalStr = evalStr.substring(2); else if
	 * (evalStr.startsWith("=")) evalStr = evalStr.substring(1);
	 * 
	 * eval = Double.parseDouble(evalStr); } catch (Exception e) {
	 * System.out.println("An error occurred parsing the thinking string " +
	 * thinkingStr); eval = 0.00; }
	 * 
	 * return eval; }
	 * 
	 * private int parseDepthFromThinkingString(String thinkingStr) { int
	 * depthValIndex = thinkingStr.indexOf("d:");
	 * 
	 * if (depthValIndex == -1) return -1;
	 * 
	 * depthValIndex += 2;
	 * 
	 * int depthValEnd = thinkingStr.indexOf(' ', depthValIndex); int depth =
	 * Integer.parseInt(thinkingStr.substring(depthValIndex, depthValEnd));
	 * 
	 * return depth; }
	 * 
	 * private int parseMateNumFromThinkingString(String thinkingStr) { int
	 * mateNum;
	 * 
	 * try { StringTokenizer tokenizer = new StringTokenizer(thinkingStr);
	 * tokenizer.nextToken(); // the first token is move-depth number
	 * 
	 * String mateStr = tokenizer.nextToken(); if (!mateStr.startsWith("m"))
	 * mateNum = 0; else mateNum = Integer.parseInt(mateStr.substring(1)); }
	 * catch (Exception e) { mateNum = 0; }
	 * 
	 * return mateNum; }
	 */

	private final void updateThinkingInfo() {
		boolean thinkingEmpty = true;
		{
			String s = "";
			if (mShowThinking || gameMode.analysisMode()) {
				s = thinkingStr;
			}
			thinking.setText(s, TextView.BufferType.SPANNABLE);
			if (s.length() > 0)
				thinkingEmpty = false;
		}
		if (mShowBookHints && (bookInfoStr.length() > 0)) {
			String s = "";
			if (!thinkingEmpty)
				s += "<br>";
			s += "<b>Book:</b>" + bookInfoStr;
			thinking.append(Html.fromHtml(s));
			thinkingEmpty = false;
		}
		if (variantStr.indexOf(' ') >= 0) {
			String s = "";
			if (!thinkingEmpty)
				s += "<br>";
			s += "<b>Var:</b> " + variantStr;
			thinking.append(Html.fromHtml(s));
		}

		List<Move> hints = null;
		if (mShowThinking || gameMode.analysisMode())
			hints = pvMoves;
		if ((hints == null) && mShowBookHints)
			hints = bookMoves;
		if ((variantMoves != null) && variantMoves.size() > 1) {
			hints = variantMoves;
		}
		if ((hints != null) && (hints.size() > maxNumArrows)) {
			hints = hints.subList(0, maxNumArrows);
		}
		cb.setMoveHints(hints);
	}

	static final int PROMOTE_DIALOG = 0;
	static final int CLIPBOARD_DIALOG = 1;
	static final int ABOUT_DIALOG = 2;
	static final int SELECT_MOVE_DIALOG = 3;
	static final int SELECT_BOOK_DIALOG = 4;
	static final int SELECT_PGN_FILE_DIALOG = 5;
	static final int SET_COLOR_THEME_DIALOG = 6;
	static final int CONFIRM_RESIGN_DIALOG = 7;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROMOTE_DIALOG: {
			final CharSequence[] items = { getString(R.string.queen),
					getString(R.string.rook), getString(R.string.bishop),
					getString(R.string.knight) };
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.promote_pawn_to);
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					ctrl.reportPromotePiece(item);
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}
		case CLIPBOARD_DIALOG: {
			final int COPY_GAME = 0;
			final int COPY_POSITION = 1;
			final int PASTE = 2;
			final int LOAD_GAME = 3;
			final int REMOVE_VARIATION = 4;

			List<CharSequence> lst = new ArrayList<CharSequence>();
			List<Integer> actions = new ArrayList<Integer>();
			lst.add(getString(R.string.copy_game));
			actions.add(COPY_GAME);
			lst.add(getString(R.string.copy_position));
			actions.add(COPY_POSITION);
			lst.add(getString(R.string.paste));
			actions.add(PASTE);
			lst.add(getString(R.string.load_game));
			actions.add(LOAD_GAME);
			if (ctrl.humansTurn() && (ctrl.numVariations() > 1)) {
				lst.add(getString(R.string.remove_variation));
				actions.add(REMOVE_VARIATION);
			}
			final List<Integer> finalActions = actions;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.tools_menu);
			builder.setItems(lst.toArray(new CharSequence[4]),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							switch (finalActions.get(item)) {
							case COPY_GAME: {
								String pgn = ctrl.getPGN();
								ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
								clipboard.setText(pgn);
								break;
							}
							case COPY_POSITION: {
								String fen = ctrl.getFEN() + "\n";
								ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
								clipboard.setText(fen);
								break;
							}
							case PASTE: {
								ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
								if (clipboard.hasText()) {
									String fenPgn = clipboard.getText()
											.toString();
									try {
										ctrl.setFENOrPGN(fenPgn);
									} catch (ChessParseError e) {
										Toast.makeText(getApplicationContext(),
												e.getMessage(),
												Toast.LENGTH_SHORT).show();
									}
								}
								break;
							}
							case LOAD_GAME:
								removeDialog(SELECT_PGN_FILE_DIALOG);
								showDialog(SELECT_PGN_FILE_DIALOG);
								break;
							case REMOVE_VARIATION:
								ctrl.removeVariation();
								break;
							}
						}
					});
			AlertDialog alert = builder.create();
			return alert;
		}
		case ABOUT_DIALOG: {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.app_name_pop).setMessage(R.string.about_info);
			AlertDialog alert = builder.create();
            //alert.getWindow().setLayout(600, 400);
			return alert;
		}
		case SELECT_MOVE_DIALOG: {
			//final Dialog dialog = new Dialog(this);
            final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.select_move_number, null);
            dialog.setView(dialogView);
			//dialog.setContentView(R.layout.select_move_number);
			dialog.setTitle(R.string.goto_move);
			final EditText moveNrView = (EditText) dialogView.findViewById(R.id.selmove_number);
			Button ok = (Button) dialogView.findViewById(R.id.selmove_ok);
			Button cancel = (Button) dialogView.findViewById(R.id.selmove_cancel);
            final AlertDialog alertDialog = dialog.create();
			moveNrView.setText("1");
			final Runnable gotoMove = new Runnable() {
				public void run() {
					try {
						int moveNr = Integer.parseInt(moveNrView.getText()
								.toString());
						ctrl.gotoMove(moveNr);
						alertDialog.dismiss();
					} catch (NumberFormatException nfe) {
						Toast.makeText(getApplicationContext(),
								R.string.invalid_number_format,
								Toast.LENGTH_SHORT).show();
					}
				}
			};
			moveNrView.setOnKeyListener(new OnKeyListener() {
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if ((event.getAction() == KeyEvent.ACTION_DOWN)
							&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
						gotoMove.run();
						return true;
					}
					return false;
				}
			});
			ok.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					gotoMove.run();
				}
			});
			cancel.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					alertDialog.dismiss();
				}
			});

            alertDialog.show();
			return null;
		}
		case SELECT_BOOK_DIALOG: {
			String[] fileNames = findFilesInDirectory(bookDir);
			final int numFiles = fileNames.length;
			CharSequence[] items = new CharSequence[numFiles + 1];
			for (int i = 0; i < numFiles; i++)
				items[i] = fileNames[i];
			items[numFiles] = getString(R.string.internal_book);
			final CharSequence[] finalItems = items;
			int defaultItem = numFiles;
			for (int i = 0; i < numFiles; i++) {
				if (currentBookFile.equals(items[i])) {
					defaultItem = i;
					break;
				}
			}
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.select_opening_book_file);
			builder.setSingleChoiceItems(items, defaultItem,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							Editor editor = settings.edit();
							String bookFile = "";
							if (item < numFiles)
								bookFile = finalItems[item].toString();
							editor.putString("bookFile", bookFile);
							editor.commit();
							setBookFile(bookFile);
							dialog.dismiss();
						}
					});
			AlertDialog alert = builder.create();
			return alert;
		}
		case SELECT_PGN_FILE_DIALOG: {
			final String[] fileNames = findFilesInDirectory(pgnDir);
			final int numFiles = fileNames.length;
			if (numFiles == 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.app_name).setMessage(
						R.string.no_pgn_files);
				AlertDialog alert = builder.create();
				return alert;
			}
			int defaultItem = 0;
			String currentPGNFile = settings.getString("currentPGNFile", "");
			for (int i = 0; i < numFiles; i++) {
				if (currentPGNFile.equals(fileNames[i])) {
					defaultItem = i;
					break;
				}
			}
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.select_pgn_file);
			builder.setSingleChoiceItems(fileNames, defaultItem,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							Editor editor = settings.edit();
							String pgnFile = fileNames[item].toString();
							editor.putString("currentPGNFile", pgnFile);
							editor.commit();
							String sep = File.separator;
							String pathName = Environment
									.getExternalStorageDirectory()
									+ sep
									+ pgnDir + sep + pgnFile;
							Intent i = new Intent(ChessTastic.this,
									LoadPGN.class);
							i.setAction(pathName);
							startActivityForResult(i, RESULT_LOAD_PGN);
							dialog.dismiss();
						}
					});
			AlertDialog alert = builder.create();
            Toast.makeText(getApplicationContext(), "After you select a game from the pgn, use the undo and redo buttons to go back and forth in the game.",
                    Toast.LENGTH_LONG).show();
			return alert;
		}
		case CONFIRM_RESIGN_DIALOG: {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Are you sure you want to resign?")
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									if (ctrl.humansTurn()) {
										ctrl.resignGame();
									}
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog alert = builder.create();
			return alert;
		}
		}
		return null;
	}

	private final String[] findFilesInDirectory(String dirName) {
		File extDir = Environment.getExternalStorageDirectory();
		String sep = File.separator;
		File dir = new File(extDir.getAbsolutePath() + sep + dirName);
		File[] files = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isFile();
			}
		});
		if (files == null)
			files = new File[0];
		final int numFiles = files.length;
		String[] fileNames = new String[numFiles];
		for (int i = 0; i < files.length; i++)
			fileNames[i] = files[i].getName();
		Arrays.sort(fileNames, String.CASE_INSENSITIVE_ORDER);
		return fileNames;
	}

	@Override
	public void requestPromotePiece() {
		runOnUIThread(new Runnable() {
			public void run() {
				showDialog(PROMOTE_DIALOG);
			}
		});
	}

	@Override
	public void reportInvalidMove(Move m) {
		String msg = String.format("Invalid move %s-%s",
				TextIO.squareToString(m.from), TextIO.squareToString(m.to));
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void computerMoveMade() {
		if (soundEnabled) {
			if (moveSound != null)
				moveSound.release();
			moveSound = MediaPlayer.create(this, R.raw.movesound);
			moveSound.start();
		}
	}

	@Override
	public void runOnUIThread(Runnable runnable) {
		runOnUiThread(runnable);
	}

	/** Decide if user should be warned about heavy CPU usage. */
	private final void updateNotification() {
		boolean warn = false;
		if (lastVisibleMillis != 0) { // GUI not visible
			warn = lastComputationMillis >= lastVisibleMillis + 30000;
		}

		setNotification(warn);
	}

	private boolean notificationActive = false;

	/** Set/clear the "heavy CPU usage" notification. */
	private final void setNotification(boolean show) {
		if (notificationActive == show)
			return;
		notificationActive = show;
		final int cpuUsage = 1;
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		if (show) {
			int icon = R.drawable.ic_launcher;
			CharSequence tickerText = "Heavy CPU usage";
			long when = System.currentTimeMillis();
			Notification notification = new Notification(icon, tickerText, when);
			notification.flags |= Notification.FLAG_ONGOING_EVENT;

			Context context = getApplicationContext();
			CharSequence contentTitle = "Background processing";
			CharSequence contentText = "ChessTastic is using a lot of CPU power";
			Intent notificationIntent = new Intent(this, CPUWarning.class);

			PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					notificationIntent, 0);
//			notification.setLatestEventInfo(context, contentTitle, contentText,
//					contentIntent);

			mNotificationManager.notify(cpuUsage, notification);
		} else {
			mNotificationManager.cancel(cpuUsage);
		}
	}

	/*
	 * private final String timeToString(long time) { int secs = (int)
	 * Math.floor((time + 999) / 1000.0); boolean neg = false; if (secs < 0) {
	 * neg = true; secs = -secs; } int mins = secs / 60; secs -= mins * 60;
	 * StringBuilder ret = new StringBuilder(); if (neg) ret.append('-');
	 * ret.append(mins); ret.append(':'); if (secs < 10) ret.append('0');
	 * ret.append(secs); return ret.toString(); }
	 */

	/*
	 * private Handler handlerTimer = new Handler(); private Runnable r = new
	 * Runnable() { public void run() { ctrl.updateRemainingTime(); } };
	 */

	/* not yet optimized for Honeycomb */
	public void setRemainingTime(long wTime, long bTime, long nextUpdate) {
		// whiteClock.setText("White: " + timeToString(wTime));
		// blackClock.setText("Black: " + timeToString(bTime));
		// handlerTimer.removeCallbacks(r); //if (nextUpdate > 0) { //
		// handlerTimer.postDelayed(r, nextUpdate); //}
	}



    /**
	 * PngTokenReceiver implementation that renders PGN data for screen display.
	 */
	static class PgnScreenText implements PgnToken.PgnTokenReceiver {
		private SpannableStringBuilder sb = new SpannableStringBuilder();
		private int prevType = PgnToken.EOF;
		int nestLevel = 0;
		boolean col0 = true;
		Node currNode = null;
		final int indentStep = 15;
		int currPos = 0, endPos = 0;
		boolean upToDate = false;
		PGNOptions options;

		private static class NodeInfo {
			int l0, l1;

			NodeInfo(int ls, int le) {
				l0 = ls;
				l1 = le;
			}
		}

		HashMap<Node, NodeInfo> nodeToCharPos;

		PgnScreenText(PGNOptions options) {
			nodeToCharPos = new HashMap<Node, NodeInfo>();
			this.options = options;
		}

		public final SpannableStringBuilder getSpannableData() {
			return sb;
		}

		public final boolean atEnd() {
			return currPos >= endPos - 10;
		}

		public boolean isUpToDate() {
			return upToDate;
		}

		int paraStart = 0;
		int paraIndent = 0;
		boolean paraBold = false;

		private final void newLine() {
			if (!col0) {
				if (paraIndent > 0) {
					int paraEnd = sb.length();
					int indent = paraIndent * indentStep;
					sb.setSpan(new LeadingMarginSpan.Standard(indent),
							paraStart, paraEnd,
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				if (paraBold) {
					int paraEnd = sb.length();
					sb.setSpan(new StyleSpan(Typeface.BOLD), paraStart,
							paraEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				sb.append('\n');
				paraStart = sb.length();
				paraIndent = nestLevel;
				paraBold = false;
			}
			col0 = true;
		}

		boolean pendingNewLine = false;

		public void processToken(Node node, int type, String token) {
			if ((prevType == PgnToken.RIGHT_BRACKET)
					&& (type != PgnToken.LEFT_BRACKET)) {
				if (options.view.headers) {
					col0 = false;
					newLine();
				} else {
					sb.clear();
					paraBold = false;
				}
			}
			if (pendingNewLine) {
				if (type != PgnToken.RIGHT_PAREN) {
					newLine();
					pendingNewLine = false;
				}
			}
			switch (type) {
			case PgnToken.STRING:
				sb.append(" \"");
				sb.append(token);
				sb.append('"');
				break;
			case PgnToken.INTEGER:
				if ((prevType != PgnToken.LEFT_PAREN)
						&& (prevType != PgnToken.RIGHT_BRACKET) && !col0)
					sb.append(' ');
				sb.append(token);
				col0 = false;
				break;
			case PgnToken.PERIOD:
				sb.append('.');
				col0 = false;
				break;
			case PgnToken.ASTERISK:
				sb.append(" *");
				col0 = false;
				break;
			case PgnToken.LEFT_BRACKET:
				sb.append('[');
				col0 = false;
				break;
			case PgnToken.RIGHT_BRACKET:
				sb.append("]\n");
				col0 = false;
				break;
			case PgnToken.LEFT_PAREN:
				nestLevel++;
				if (col0)
					paraIndent++;
				newLine();
				sb.append('(');
				col0 = false;
				break;
			case PgnToken.RIGHT_PAREN:
				sb.append(')');
				nestLevel--;
				pendingNewLine = true;
				break;
			case PgnToken.NAG:
				sb.append(Node.nagStr(Integer.parseInt(token)));
				col0 = false;
				break;
			case PgnToken.SYMBOL: {
				if ((prevType != PgnToken.RIGHT_BRACKET)
						&& (prevType != PgnToken.LEFT_BRACKET) && !col0)
					sb.append(' ');
				int l0 = sb.length();
				sb.append(token);
				int l1 = sb.length();
				nodeToCharPos.put(node, new NodeInfo(l0, l1));
				if (endPos < l0)
					endPos = l0;
				col0 = false;
				if (nestLevel == 0)
					paraBold = true;
				break;
			}
			case PgnToken.COMMENT:
				if (prevType == PgnToken.RIGHT_BRACKET) {
				} else if (nestLevel == 0) {
					nestLevel++;
					newLine();
					nestLevel--;
				} else {
					if ((prevType != PgnToken.LEFT_PAREN) && !col0) {
						sb.append(' ');
					}
				}
				sb.append(token.replaceAll("[ \t\r\n]+", " ").trim());
				col0 = false;
				if (nestLevel == 0)
					newLine();
				break;
			case PgnToken.EOF:
				newLine();
				upToDate = true;
				break;
			}
			prevType = type;
		}

		@Override
		public void clear() {
			sb.clear();
			prevType = PgnToken.EOF;
			nestLevel = 0;
			col0 = true;
			currNode = null;
			currPos = 0;
			endPos = 0;
			nodeToCharPos.clear();
			paraStart = 0;
			paraIndent = 0;
			paraBold = false;
			pendingNewLine = false;

			upToDate = false;
		}

		BackgroundColorSpan bgSpan = new BackgroundColorSpan(0xff888888);

		@Override
		public void setCurrent(Node node) {
			sb.removeSpan(bgSpan);
			NodeInfo ni = nodeToCharPos.get(node);
			if (ni != null) {
				sb.setSpan(bgSpan, ni.l0, ni.l1,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				currPos = ni.l0;
			}
			currNode = node;
		}
	}

	@Override
	public void humanMoveMade(Move m) {
		if (bGameCtrl != null)
			bGameCtrl.sendMove(m);
	}
	
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@SuppressWarnings("deprecation")
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		//selectItem(position);
			switch(position)
			{
			case 0:
				drawerLayoutt.closeDrawers();
				if (gameMode.bluetoothMode()) {
					Intent serverIntent = new Intent(ChessTastic.this, DeviceListActivity.class);
					startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
					break;
				}

				if (autoSwapSides
						&& (gameMode.playerWhite() != gameMode.playerBlack())) {
					int gameModeType;
					if (gameMode.playerWhite()) {
						gameModeType = GameMode.PLAYER_BLACK;
					} else {
						gameModeType = GameMode.PLAYER_WHITE;
					}
					Editor editor = settings.edit();
					String gameModeStr = String.format("%d", gameModeType);
					editor.putString("gameMode", gameModeStr);
					editor.commit();
					gameMode = new GameMode(gameModeType);
				}

				ctrl.newGame(gameMode);
				ctrl.startGame();
				break;
			case 1: {
				Intent i = new Intent(ChessTastic.this, EditBoard.class);
				i.setAction(ctrl.getFEN());
				startActivityForResult(i, RESULT_EDITBOARD);
				break;
			}
			case 2: {
				drawerLayoutt.closeDrawers();
				ChessTastic.this.flipBoard();
				break;
			}
			case 5: {
				Intent i = new Intent(ChessTastic.this, Preferences.class);
				startActivityForResult(i, RESULT_SETTINGS);
				break;
			}
			
			case 7: {
				drawerLayoutt.closeDrawers();
				showDialog(SELECT_MOVE_DIALOG);
				break;
			}
			case 8: {
				drawerLayoutt.closeDrawers();
				if (ctrl != null && ctrl.computerBusy())
				ctrl.stopSearch();
				else
					Toast.makeText(getApplicationContext(), "Busy",
							   Toast.LENGTH_SHORT).show();
				break;
			}
				/*
				 * case R.id.item_draw: { if (ctrl.humansTurn()) { if
				 * (!ctrl.claimDrawIfPossible()) {
				 * Toast.makeText(getApplicationContext(), R.string.offer_draw,
				 * Toast.LENGTH_SHORT).show(); } } return true; }
				 */
			case 6: {
				
				if (ctrl.humansTurn()) {
					removeDialog(CONFIRM_RESIGN_DIALOG);
					showDialog(CONFIRM_RESIGN_DIALOG);
				}
				break;
			}
			case 9:
				removeDialog(SELECT_BOOK_DIALOG);
				showDialog(SELECT_BOOK_DIALOG);
				break;
			case 4:
				removeDialog(SELECT_PGN_FILE_DIALOG);
				showDialog(SELECT_PGN_FILE_DIALOG);
				break;
			case 3:
				Toast.makeText(getApplicationContext(), "Bluetooth is currently a work in progress.",
						   Toast.LENGTH_LONG).show();
				break;
			/*case R.id.bluetooth_create:
				Intent serverIntent = new Intent(this, DeviceListActivity.class);
				startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
				return true;
			case R.id.bluetooth_set_discoverable:
				if (bGameCtrl == null) {
					Toast.makeText(getApplicationContext(),
							"Bluetooth mode is not enabled", Toast.LENGTH_SHORT)
							.show();
					return true;
				}

				if (bGameCtrl.getmBluetoothAdapter().getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
					Intent discoverableIntent = new Intent(
							BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
					discoverableIntent.putExtra(
							BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
					startActivity(discoverableIntent);
				} else {
					Toast.makeText(getApplicationContext(),
							"This device is already discoverable",
							Toast.LENGTH_SHORT).show();
				}

				return true;
			case R.id.bluetooth_reset:
				if (bGameCtrl != null) {
					bGameCtrl.reset();
				}
				return true;*/

            case 10:
                //rate
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        ChessTastic.this);

                // set title
                alertDialogBuilder.setTitle("Rating and Feedback");

                // set dialog message
                alertDialogBuilder
                        .setMessage("Do you like this app? Press Rate to add a good rating and some kind words on the Play Store. Are you facing any problems? Press the Feedback button below to send me an email!")
                        .setCancelable(true)
                        .setPositiveButton("Rate",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                // if this button is clicked, close
                                // current activity
                                final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
                                try {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                                } catch (android.content.ActivityNotFoundException anfe) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                                }
                            }
                        })
                        .setNegativeButton("Feedback",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                // if this button is clicked, just close
                                // the dialog box and do nothing
                                Toast.makeText(getApplicationContext(), "Opening email. Please tell me your problems. ", Toast.LENGTH_LONG).show();
                                /*Intent i = new Intent(android.content.Intent.ACTION_SEND);
                                i.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] {"avijitg22@gmail.com"});
                                i.putExtra(android.content.Intent.EXTRA_SUBJECT, "ChessTastic™ Beta");
                                //i.putExtra(Intent.EXTRA_TEXT, "\n\n\n\n"+"-----------------------------------------------------------------------"+"\n"+raw);
                                i.setType("plain/text");
                                startActivity(i);*/
                                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                                        "mailto","avijitg22@gmail.com", null));
                                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "ChessTastic™ Beta");
                                startActivity(Intent.createChooser(emailIntent, "Send email..."));
                            }
                        });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();
                break;
			case 11:
				showDialog(ABOUT_DIALOG);
				break;
			}
			}
		}
	
}
		
		
		
class MyAdapter extends BaseAdapter{
	
	String[] options;
	int[] images={R.drawable.newg,R.drawable.edit,R.drawable.flip,R.drawable.bt,R.drawable.pgn,R.drawable.settings,R.drawable.resign,R.drawable.goton,R.drawable.force,R.drawable.book,R.drawable.thumb,R.drawable.about};
	private Context context;


	
	MyAdapter(Context context)
	{
		this.context=context;
		options=context.getResources().getStringArray(R.array.navigation_drawer_items);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return options.length;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return options[position];
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return arg0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		View row=null;


		if(convertView==null)
		{
			LayoutInflater inflater=(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		    row=inflater.inflate(R.layout.drawer_list_item, parent, false);
		}
		else
		{
			row=convertView;
		}
		TextView tv1=(TextView) row.findViewById(R.id.text1);
		ImageView iv1=(ImageView) row.findViewById(R.id.image1);
        RelativeLayout lLayout = (RelativeLayout) row.findViewById(R.id.parentLayout);
        if(position==3)
        {
            tv1.setText(options[position]);
            tv1.setTextColor(ChessTastic.getContextOfApplication().getResources().getColor(R.color.disabled));
		    iv1.setImageResource(images[position]);
            //row.setVisibility(View.GONE);
            lLayout.setVisibility(View.GONE);

        }
        else{
            tv1.setText(options[position]);
            iv1.setImageResource(images[position]);
            lLayout.setVisibility(View.VISIBLE);
        }
		
		return row;
	}
	
}


