package com.geoloqi.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.geoloqi.mapattack.R;
import com.geoloqi.interfaces.RPCException;
import com.geoloqi.rpc.AccountMonitor;
import com.geoloqi.rpc.MapAttackClient;
import com.geoloqi.services.AndroidPushNotifications;

public class MapAttackActivity extends Activity {
	public static final String TAG = "MapAttackActivity";
	
	public static final String PARAM_GAME_ID = "game_id";
	
	private String mGameId;
	private String mGameUrl;
	private WebView mWebView;
	private Intent mPushNotificationIntent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mGameId = getIntent().getExtras().getString(PARAM_GAME_ID);
		}

		// Build game
		mGameUrl = String.format("http://mapattack.org/game/%s", mGameId);
		mWebView = (WebView) findViewById(R.id.webView);
		mPushNotificationIntent = new Intent(this, AndroidPushNotifications.class);
		
		// Prepare the web view
		mWebView.clearCache(false);
		mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setWebViewClient(mWebViewClient);
		
		// Show the loading indicator
		setLoading(true);
	}

	@Override
	public void onStart() {
		super.onStart();

		final MapAttackClient client = MapAttackClient.getApplicationClient(this);

		// Check for a valid account token
		if (!client.hasToken()) {
			// Kick user out to the sign in activity
			Intent intent = new Intent(this, SignInActivity.class);
			intent.putExtra(MapAttackActivity.PARAM_GAME_ID, mGameId);
			startActivity(intent);
			finish();
		} else {
			try {
				// Stop any previously started services and broadcast receivers
				unregisterReceiver(mPushReceiver);
				stopService(mPushNotificationIntent);
			} catch (IllegalArgumentException e) {
				Log.w(TAG, "Trying to unregister an inactive push receiver.");
			}

			// Start our services
			registerReceiver(mPushReceiver, new IntentFilter("PUSH"));
			startService(mPushNotificationIntent);

			try {
				// Join the game
				client.joinGame(mGameId);

				// Load the game into the WebView
				mWebView.loadUrl(String.format("%s?id=%s", mGameUrl,
						AccountMonitor.getUserID(this)));
			} catch (RPCException e) {
				Log.e(TAG, "Got an RPCException when trying to join the game!", e);
				Toast.makeText(this, R.string.error_join_game, Toast.LENGTH_LONG).show();
				finish();
			}
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		try {
			unregisterReceiver(mPushReceiver);
			stopService(mPushNotificationIntent);
		} catch (IllegalArgumentException e) {
			Log.w(TAG, "Trying to unregister an inactive push receiver.");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.game_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.share:
			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.setType("text/plain");
			shareIntent.putExtra(Intent.EXTRA_TEXT,
					String.format("Map Attack! %s #mapattack", mGameUrl));
			startActivity(Intent.createChooser(shareIntent, "Share this map: "));
			return true;
		case R.id.quit:
			finish();
			return true;
		}
		return false;
	}

	/** Show or hide the loading indicator. */
	private void setLoading(boolean loading) {
		ProgressBar spinner = (ProgressBar) findViewById(R.id.loading);

		if (loading) {
			spinner.setVisibility(View.VISIBLE);
			mWebView.setVisibility(View.GONE);
		} else {
			spinner.setVisibility(View.GONE);
			mWebView.setVisibility(View.VISIBLE);
		}
	}

	/** A reference to the WebViewClient that hosts the MapAttack game. */
	private WebViewClient mWebViewClient = new WebViewClient() {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
		
		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			
			// Make WebView visible and hide loading indicator
			setLoading(false);
		}
	};

	/** The broadcast receiver used to push game data to the server. */
	private BroadcastReceiver mPushReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context ctxt, Intent intent) {
			mWebView.loadUrl(String.format("javascript:LQHandlePushData(%s)",
					intent.getExtras().getString("json")));
		}
	};
}