package com.geoloqi.ui;

import java.util.regex.Pattern;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.EditText;
import android.widget.Toast;

import com.geoloqi.ADB;
import com.geoloqi.mapattack.R;
import com.geoloqi.interfaces.GeoloqiConstants;
import com.geoloqi.rpc.AccountMonitor;

public class SignInActivity extends Activity {

	Pattern emailPattern = Pattern.compile("^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$", Pattern.CASE_INSENSITIVE);

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
				String initials, email;
				{// Grep input.
					initials = ((EditText) findViewById(R.id.initials)).getText().toString();
					email = ((EditText) findViewById(R.id.email)).getText().toString().toUpperCase();
				}
				{// Test validity
					boolean twoInitials = initials.length() == 2;
					boolean validEmail = emailPattern.matcher(email).matches();
					if (!twoInitials) {
						ADB.makeToast(SignInActivity.this, "Two initials please.", Toast.LENGTH_LONG);
						return false;
					} else if (!validEmail) {
						ADB.makeToast(SignInActivity.this, "Invalid e-mail", Toast.LENGTH_LONG);
						return false;
					}
				}
				new SignIn().execute();
				{// Write to preferences.
					Editor prefs = getSharedPreferences(GeoloqiConstants.PREFERENCES_FILE, Context.MODE_PRIVATE).edit();
					prefs.putString("initials", initials);
					prefs.putString("email", email).commit();
				}
				// Start login.
				AccountMonitor.createUserID(SignInActivity.this);
				// Finish.
				setResult(RESULT_OK);
				finish();
			}
			return false;
		}
	};

	class SignIn extends AsyncTask<Void, Void, Void> {

		ProgressDialog progressDialog;

		@Override
		public void onPreExecute() {
			progressDialog = ProgressDialog.show(SignInActivity.this, "Map Attack!", "Signing in.");
		}

		@Override
		protected Void doInBackground(Void... params) {
			AccountMonitor.createUserID(SignInActivity.this);
			return null;
		}

		@Override
		public void onPostExecute(Void result) {
			progressDialog.dismiss();
		}
	}

}
