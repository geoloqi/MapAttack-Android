package com.geoloqi.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.geoloqi.ADB;
import com.geoloqi.mapattack.R;
import com.geoloqi.interfaces.GeoloqiConstants;
import com.geoloqi.interfaces.RPCException;
import com.geoloqi.rpc.AccountMonitor;
import com.geoloqi.rpc.MapAttackClient;
import com.geoloqi.services.AndroidPushNotifications;

public class MapAttackActivity extends Activity {
	public static final String PARAM_GAME_ID = "game_id";

	String id, email, initials;
	Intent apnIntent;
	WebView webView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		ADB.logo();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		{// Initialize variables.
			SharedPreferences prefs = getSharedPreferences(GeoloqiConstants.PREFERENCES_FILE, Context.MODE_PRIVATE);
			id = getIntent().getExtras().getString(PARAM_GAME_ID);
			email = prefs.getString("email", null);
			initials = prefs.getString("initials", null);
			apnIntent = new Intent(this, AndroidPushNotifications.class);
			webView = (WebView) findViewById(R.id.webView);
		}
	}

	@Override
	public void onStart() {
		try {
			super.onStart();
			if (!MapAttackClient.getApplicationClient(this).hasToken()) {
				// TODO: Please don't do this! It breaks the activity lifecycle. Refactor!
				startActivityForResult(new Intent(this, SignInActivity.class), 77);
			} else {
				loadWebView();
			}
			{//Start services.
				try {
					unregisterReceiver(pushReceiver);
				} catch (IllegalArgumentException e) {
				}
				registerReceiver(pushReceiver, new IntentFilter("PUSH"));
				MapAttackClient.getApplicationClient(this).joinGame(id);
				stopService(apnIntent);
				startService(apnIntent);
			}
		} catch (RPCException e) {
			onStart();
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
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.share:
			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.setType("text/plain");
			shareIntent.putExtra(Intent.EXTRA_TEXT, "Map Attack!  http://mapattack.org/game/" + id + " #mapattack");
			startActivity(Intent.createChooser(shareIntent, "Share this map: "));
			return true;
		case R.id.quit:
			System.exit(0);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 77 && resultCode == RESULT_OK) {
			loadWebView();
		}
	}

	private void loadWebView() {
		webView.clearCache(false);
		webView.loadUrl("http://mapattack.org/game/" + id + "?id=" + AccountMonitor.getUserID(this));
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(webViewClient);
		webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
	}

	@Override
	public void onRestart() {
		super.onRestart();
		registerReceiver(pushReceiver, new IntentFilter("PUSH"));
		startService(apnIntent);
	}

	@Override
	public void onStop() {
		super.onStop();
		unregisterReceiver(pushReceiver);
		stopService(apnIntent);
	}

	private WebViewClient webViewClient = new WebViewClient() {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
	};

	private BroadcastReceiver pushReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context ctxt, Intent intent) {
			webView.loadUrl("javascript:LQHandlePushData(" + intent.getExtras().getString("json") + ")");
		}

	};
}