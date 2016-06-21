package org.empyrn.darkknight;


import com.nemesis.materialchess.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;

public class CPUWarning extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cpu_warning);
		showDialog(CPU_WARNING_DIALOG);
	}

	static final int CPU_WARNING_DIALOG = 1;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CPU_WARNING_DIALOG:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.app_name).setMessage(R.string.cpu_warning);
			AlertDialog alert = builder.create();
			alert.setOnDismissListener(new OnDismissListener() {
				public void onDismiss(DialogInterface dialog) {
					finish();
				}
			});
			return alert;
		}
		return null;
	}
}
