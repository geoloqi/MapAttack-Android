package com.geoloqi.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.EditText;
import android.widget.Toast;

import com.geoloqi.ADB;
import com.geoloqi.R;
import com.geoloqi.interfaces.GeoloqiConstants;
import com.geoloqi.interfaces.RPCException;
import com.geoloqi.rpc.MapAttackClient;

public class SignInActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sign_in_activity);
		findViewById(R.id.submit_button).setOnTouchListener(submitButtonListener);
	}

	OnTouchListener submitButtonListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View view, MotionEvent evt) {
			if (evt.getAction() == MotionEvent.ACTION_UP) {
				try {
					String initials = ((EditText) findViewById(R.id.initials)).getText().toString();
					String email = ((EditText) findViewById(R.id.email)).getText().toString();
					MapAttackClient.getApplicationClient(SignInActivity.this).createAnonymousAccount(initials);
					getSharedPreferences(GeoloqiConstants.PREFERENCES_FILE, Context.MODE_PRIVATE).edit().putString("initials", initials).putString("email", email).commit();
					setResult(RESULT_OK);
					finish();
				} catch (RPCException e) {
					ADB.makeToast(SignInActivity.this, "Signup failed", Toast.LENGTH_LONG);
				}
			}
			return false;
		}
	};

}
