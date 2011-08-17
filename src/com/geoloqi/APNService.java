package com.geoloqi;

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

import com.geoloqi.interfaces.GeoloqiConstants;

public class APNService extends Service {

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		notifier.start();
		return Service.START_FLAG_REDELIVERY;
	}

	@Override
	public void onDestroy() {

	}

	Thread notifier = new Thread() {

		Socket socket;

		@Override
		public void run() {
			try {
				socket = new Socket(GeoloqiConstants.downloadAddress, GeoloqiConstants.downloadPort);
				PrintWriter out = new PrintWriter(socket.getOutputStream());
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out.print(Installation.id(APNService.this));
				if (in.readLine().equals("ok")) {
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
			try {
				JSONObject obj = new JSONObject(json);
				if (obj.has("aps")) {
					String message = obj.getString("aps");
					notify(message);
				}
				if (!obj.has("aps") || obj.length() > 2) {
					forward(json);
				}
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
		}

		public void notify(String tickerText) {
			Notification notification = new Notification(R.drawable.ic_stat_notify, tickerText, System.currentTimeMillis());
			CharSequence contentTitle = "Geoloqi";
			CharSequence contentText = tickerText;
			Intent contentIntent = new Intent(APNService.this, MapAttackActivity.class);
			contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			PendingIntent notificationIntent = PendingIntent.getActivity(APNService.this, 0, contentIntent, 0);
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			notification.setLatestEventInfo(APNService.this, contentTitle, contentText, notificationIntent);
			((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, notification);
		}

		public void forward(String json) {
			Intent notifyPush = new Intent("PUSH");
			notifyPush.putExtra("json", json);
			sendBroadcast(notifyPush);
		}

	};

}
