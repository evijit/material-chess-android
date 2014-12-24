package org.empyrn.darkknight;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import com.nemesis.materialchess.R;

public class MyPreferenceFragment extends PreferenceFragment 
{
    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
    
    
}
