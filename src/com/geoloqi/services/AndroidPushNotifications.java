package com.geoloqi.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Hashtable;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.IBinder;

import com.geoloqi.ADB;
import com.geoloqi.Installation;
import com.geoloqi.mapattack.R;
import com.geoloqi.interfaces.GeoloqiConstants;
import com.geoloqi.ui.MapAttackActivity;

public class AndroidPushNotifications extends Service {

	Notifier notifier;
	boolean running = true;

	private SoundPool soundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
	Hashtable<String, Integer> sounds = new Hashtable<String, Integer>();

	private void initializeSounds() {
		sounds.put("pop", soundPool.load(this, R.raw.pop, 1));
	}

	private void playSound(String name) {
		if (sounds.containsKey(name)) {
			soundPool.play(sounds.get(name), 1f, 1f, 0, 0, 1f);
		} else {
			ADB.log("Could not play sound: " + name);
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		initializeSounds();
		notifier = new Notifier();
		notifier.start();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		onStart(intent, startId);
		return Service.START_REDELIVER_INTENT;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		running = false;

	}

	class Notifier extends Thread {

		Socket socket;
		String userID;

		@Override
		public void run() {
			try {
				{//Initialize variables.
					userID = AndroidPushNotifications.this.getSharedPreferences(GeoloqiConstants.PREFERENCES_FILE, Context.MODE_PRIVATE).getString("userID", null);
					socket = new Socket(GeoloqiConstants.downloadAddress, GeoloqiConstants.downloadPort);
				}
				while (running) {
					PrintWriter out = new PrintWriter(socket.getOutputStream());
					out.print(Installation.getIDAsString(AndroidPushNotifications.this));
					out.flush();
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					if (in != null && in.readLine().equals("ok")) {
						while (running) {
							handleJSON(in.readLine());
						}
					}
				}
			} catch (IOException e) {
				if (running) {
					run();
				}
			}
		}

		public void handleJSON(String json) {
			try {
				if (json != null) {
					JSONObject obj = new JSONObject(json);
					if (obj.has("aps")) {
						JSONObject aps = obj.getJSONObject("aps");
						if (aps.has("alert")) {
							notify(aps.getJSONObject("alert").getString("body"));
						}
					} else if (obj.has("mapattack")) {
						JSONObject subObj = obj.getJSONObject("mapattack");
						if (subObj.has("triggered_user_id")) {
							String id = subObj.getString("triggered_user_id");
							if (id.equals(userID)) {
								playSound("pop");
							}
						}
					}
					if (!obj.has("aps") || obj.length() > 2) {
						forward(json);
					}
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
			contentIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
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
