package com.geoloqi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MapAttackActivity extends Activity {

	WebView webView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		webView = (WebView) findViewById(R.id.webView);
		webView.loadUrl(getString(R.string.webview_url));
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(webViewClient);
		webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

		this.registerReceiver(pushReceiver, new IntentFilter("PUSH"));
		ADB.log("Here!");
	}

	@Override
	public void onStart() {
		super.onStart();
		startActivity(new Intent(this, APNService.class));
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
			webView.loadUrl("javascript:function(" + intent.getExtras().getString("json") + ")");
		}

	};
}