package org.empyrn.darkknight;

import com.nemesis.materialchess.R;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
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

    @SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
	        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
	        getWindow().setStatusBarColor(getResources().getColor(R.color.darkred));
	    }
        
        

        getFragmentManager().beginTransaction().replace(R.id.content_frame, new MyPreferenceFragment()).commit();
        
        
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
   
}


