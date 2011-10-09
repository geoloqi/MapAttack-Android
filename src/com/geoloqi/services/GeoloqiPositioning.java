package com.geoloqi.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import com.geoloqi.ADB;
import com.geoloqi.data.Fix;
import com.geoloqi.interfaces.GeoloqiFixSocket;
import com.geoloqi.mapattack.UDPClient;

public class GeoloqiPositioning extends Service implements LocationListener {
	public static final String TAG = "GeoloqiPositioning";

	private int batteryLevel = 0;

	GeoloqiFixSocket fixSocket;

	@Override
	public void onCreate() {
		if (isConnected()) {
			fixSocket = UDPClient.getApplicationClient(this);
		} else {
			// TODO: This is a crude check. Should probably be rolled into UDPClient class directly.
			Log.w(TAG, "Network unavailable! Stopping positioning service.");
			stopSelf();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onStart(Intent intent, int startid) {
		registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		for (String provider : ((LocationManager) getSystemService(LOCATION_SERVICE)).getAllProviders()) {
			if (!provider.equals("passive")) {
				ADB.log("Registering for updates with " + provider);
				((LocationManager) getSystemService(LOCATION_SERVICE)).requestLocationUpdates(provider, 0, 0, this);
			}
		}
	}

	public void onStop() {
		unregisterReceiver(batteryReceiver);
		((LocationManager) getSystemService(LOCATION_SERVICE)).removeUpdates(this);
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(batteryReceiver);
		((LocationManager) getSystemService(LOCATION_SERVICE)).removeUpdates(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startid) {
		onStart(intent, startid);
		return Service.START_REDELIVER_INTENT;
	}

	@Override
	public void onLocationChanged(Location location) {
		@SuppressWarnings("unchecked")
		Fix lqLocation = new Fix(location, new Pair<String, String>("battery", "" + batteryLevel));
		
		if (isConnected()) {
			fixSocket.pushFix(lqLocation);
		} else {
			// TODO: This is a crude check. Should probably be rolled into UDPClient class directly.
			Log.w(TAG, "Network unavailable, failed to push location fix!");
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	BroadcastReceiver batteryReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			batteryLevel = intent.getIntExtra("level", 0);
		}
	};
	
	/** Determine if the network is connected and available. */
	private boolean isConnected() {
		ConnectivityManager manager = (ConnectivityManager) getApplicationContext()
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
		
		return (activeNetwork != null && activeNetwork.isConnected());
	}
}
