package com.geoloqi.ui;

import java.util.regex.Pattern;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.geoloqi.mapattack.R;
import com.geoloqi.interfaces.GeoloqiConstants;
import com.geoloqi.interfaces.RPCException;
import com.geoloqi.rpc.MapAttackClient;

public class SignInActivity extends Activity implements OnClickListener {
	public static final String TAG = "SignInActivity";

	/** Validates an email address. */
	public static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w\\.-]+@([\\w\\-]+\\.)+[a-z]{2,4}$",
			Pattern.CASE_INSENSITIVE);

	/** The id of the game to launch when finished. */
	private String mGameId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sign_in_activity);

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mGameId = extras.getString(MapAttackActivity.PARAM_GAME_ID);
		}

		// Load saved user information
		final SharedPreferences sharedPreferences = getSharedPreferences(
				GeoloqiConstants.PREFERENCES_FILE, Context.MODE_PRIVATE);
		if (sharedPreferences != null) {
			final TextView initialsView = (TextView) findViewById(R.id.initials);
			final TextView emailView = (TextView) findViewById(R.id.email);
			
			initialsView.setText(sharedPreferences.getString("initials", ""));
			emailView.setText(sharedPreferences.getString("email", ""));
		}

		// Listen for form submission
		findViewById(R.id.submit_button).setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.submit_button:
			final EditText initialsField = (EditText) findViewById(R.id.initials);
			final EditText emailField = (EditText) findViewById(R.id.email);

			final String initials = initialsField.getText().toString();
			final String email = emailField.getText().toString().toLowerCase();

			// Validate input
			if (initials.length() == 2) {
				if (EMAIL_PATTERN.matcher(email).matches()) {
					new CreateAnonymousAccountTask(this, initials, email).execute();
				} else {
					Toast.makeText(this, R.string.error_email,
							Toast.LENGTH_LONG).show();
				}
			} else {
				Toast.makeText(this, R.string.error_initials,
						Toast.LENGTH_LONG).show();
			}
		}
	}

	/** Stub */
	private void finishLogin(boolean result) {
		if (result) {
			if (!TextUtils.isEmpty(mGameId)) {
				// Launch the map attack activity
				Intent intent = new Intent(this, MapAttackActivity.class);
				intent.putExtra(MapAttackActivity.PARAM_GAME_ID, mGameId);
				startActivity(intent);
			} else {
				Log.e(TAG, "Got an empty game ID when trying to finish login!");
				Toast.makeText(this, R.string.error_invalid_game_id, Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(this, R.string.error_join_game, Toast.LENGTH_LONG).show();
		}

		// Finish the login activity
		finish();
	}

	/** TODO: Move this to an external class file. */
	private static class CreateAnonymousAccountTask extends AsyncTask<String, Void, Boolean> {
		private final ProgressDialog mProgressDialog;
		private final Context mContext;
		private final String mInitials;
		private final String mEmail;

		public CreateAnonymousAccountTask(final Context context, final String initials,
				final String email) {
			mProgressDialog = new ProgressDialog(context);
			mProgressDialog.setTitle(null);
			mProgressDialog.setMessage(context.getString(R.string.sign_in_loading_text));
			
			mContext = context;
			mInitials = initials;
			mEmail = email;
		}

		@Override
		public void onPreExecute() {
			mProgressDialog.show();
		}

		@Override
		protected Boolean doInBackground(String... params) {
			// TODO: Use the default shared preferences here:
			//PreferenceManager.getDefaultSharedPreferences(this)
			Editor prefs = (Editor) mContext.getSharedPreferences(
					GeoloqiConstants.PREFERENCES_FILE, Context.MODE_PRIVATE).edit();
			prefs.putString("initials", mInitials);
			prefs.putString("email", mEmail);
			prefs.commit();
			
			try {
				// Start login.
				final MapAttackClient client = MapAttackClient.getApplicationClient(mContext);
				client.createAnonymousAccount();
			} catch (RPCException e) {
				Log.e(TAG, "Got an RPCException when trying to create an anonymous account.", e);
				return false;
			}
			
			return true;
		}

		@Override
		public void onPostExecute(Boolean result) {
			mProgressDialog.dismiss();
			
			try {
				((SignInActivity) mContext).finishLogin(result);
			} catch (ClassCastException e) {
				Log.w(TAG, "Got a ClassCastException when trying to finish login!", e);
			}
		}
	}
}