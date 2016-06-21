package org.empyrn.darkknight;

import com.nemesis.materialchess.R;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

/*public class Preferences extends PreferenceActivity {

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
	        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
	        getWindow().setStatusBarColor(getResources().getColor(R.color.darkred));
	    }
	}
}*/

public class Preferences extends ActionBarActivity {
	
	public static Context contextOfApplication;
	int choice,theme;




	
    @SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Preferences.this);
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
    	
        setContentView(R.layout.pref_with_xml);

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
        SpannableString s = new SpannableString("CHESS SETTINGS");
		s.setSpan(new TypefaceSpan(this, "KlinicSlabBold.otf"), 0, s.length(),
		        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle(s);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            
        }
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS); 
        
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
	        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
	        getWindow().setStatusBarColor(getResources().getColor(R.color.darkred));
	    }*/
        
        contextOfApplication = this;

        getFragmentManager().beginTransaction().replace(R.id.content_frame, new MyPreferenceFragment()).commit();
        
        
    }
    
    public static Context getContextOfApplication(){
        return contextOfApplication;
    }
    
    @Override
	protected void onRestart()
	{
		super.onRestart();
		Intent intent = new Intent(Preferences.this, Preferences.class);
		startActivity(intent);
		finish();
	}

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        // Respond to the action bar's Up/Home button
        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        NavUtils.navigateUpFromSameTask(this);
    }
    
    private void convertPreferenceToUseCustomFont(Preference somePreference) {
        TypefaceSpan customTypefaceSpan = new TypefaceSpan(this, "KlinicSlabMedium.otf");

        SpannableStringBuilder ss;
        if (somePreference.getTitle() != null) {
            ss = new SpannableStringBuilder(somePreference.getTitle().toString());
            ss.setSpan(customTypefaceSpan, 0, ss.length(),Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            somePreference.setTitle(ss);
        }

        if (somePreference.getSummary() != null) {
            ss = new SpannableStringBuilder(somePreference.getSummary().toString());
            ss.setSpan(customTypefaceSpan, 0, ss.length(),Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            somePreference.setSummary(ss);
        }
    }
    
    protected void saveTheme(int str)
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Preferences.this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("Theme", str);
        editor.commit();
	}
	protected void loadTheme()
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Preferences.this);
		theme = sharedPreferences.getInt("Theme",R.style.AppThemeOrange);
		super.setTheme(theme);
		
		
	}


}


