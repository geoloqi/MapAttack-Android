package com.geoloqi.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.geoloqi.ADB;
import com.geoloqi.R;
import com.geoloqi.interfaces.GeoloqiConstants;
import com.geoloqi.ui.MapAttackActivity;

public class AndroidPushNotifications extends Service {

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		new Notifier().start();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		ADB.log("Spinning up the APN");
		new Notifier().start();
		return Service.START_REDELIVER_INTENT;
	}

	@Override
	public void onDestroy() {

	}

	class Notifier extends Thread {

		Socket socket;

		@Override
		public void run() {
			try {
				socket = new Socket(GeoloqiConstants.downloadAddress, GeoloqiConstants.downloadPort);
				ADB.log("Socket connected? -> " + socket.isConnected());
				PrintWriter out = new PrintWriter(socket.getOutputStream());
				out.print("0000");
				out.flush();
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				//out.print(Installation.id(APNService.this));
				if (in != null && in.readLine().equals("ok")) {
					while (true) {
						handleJSON(in.readLine());
					}
				} else {
					throw new RuntimeException("It's not okay.");
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		public void handleJSON(String json) {
			ADB.log("Got json: " + json);
			try {
				JSONObject obj = new JSONObject(json);
				if (obj.has("aps")) {
					JSONObject aps = obj.getJSONObject("aps");
					if (aps.has("alert")) {
						notify(aps.getString("alert"));
					}
				}
				if (!obj.has("aps") || obj.length() > 2) {
					forward(json);
				}
			} catch (JSONException e) {
				ADB.log("Could not parse string: " + json);
				return;
			}
		}

		public void notify(String tickerText) {
			Notification notification = new Notification(R.drawable.ic_stat_notify, tickerText, System.currentTimeMillis());
			CharSequence contentTitle = "Geoloqi";
			CharSequence contentText = tickerText;
			Intent contentIntent = new Intent(AndroidPushNotifications.this, MapAttackActivity.class);
			contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			PendingIntent notificationIntent = PendingIntent.getActivity(AndroidPushNotifications.this, 0, contentIntent, 0);
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			notification.setLatestEventInfo(AndroidPushNotifications.this, contentTitle, contentText, notificationIntent);
			((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, notification);
		}

		public void forward(String json) {
			Intent notifyPush = new Intent("PUSH");
			notifyPush.putExtra("json", json);
			sendBroadcast(notifyPush);
		}

	};

}
