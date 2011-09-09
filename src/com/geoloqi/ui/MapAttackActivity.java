package com.geoloqi.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.geoloqi.ADB;
import com.geoloqi.R;
import com.geoloqi.services.AndroidPushNotifications;
import com.geoloqi.services.GeoloqiPositioning;

public class MapAttackActivity extends Activity {

	WebView webView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		ADB.logo();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		webView = (WebView) findViewById(R.id.webView);
		webView.loadUrl("http://mapattack.org/game/" + this.getIntent().getExtras().getString("id"));
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(webViewClient);
		webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

		this.registerReceiver(pushReceiver, new IntentFilter("PUSH"));
		startService(new Intent(this, AndroidPushNotifications.class));
		startService(new Intent(this, GeoloqiPositioning.class));
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