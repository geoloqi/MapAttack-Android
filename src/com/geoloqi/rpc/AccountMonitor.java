package com.geoloqi.rpc;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;

import com.geoloqi.interfaces.GeoloqiConstants;
import com.geoloqi.interfaces.RPCException;

public class AccountMonitor {

	private static ReentrantLock lock = new ReentrantLock();
	private static Condition userIDReceived = lock.newCondition();

	public static void createUserID(final Context context) {
		new Thread() {
			@Override
			public void run() {
				lock.lock();
				try {
					MapAttackClient.getApplicationClient(context).createAnonymousAccount();
					userIDReceived.signalAll();
				} catch (RPCException e) {
					throw new RuntimeException(e);
				} finally {
					lock.unlock();
				}
			}
		}.start();
	}

	public static String getUserID(Context context) {
		lock.lock();
		try {
			if (!context.getSharedPreferences(GeoloqiConstants.PREFERENCES_FILE, Context.MODE_PRIVATE).contains("userID")) {
				userIDReceived.awaitUninterruptibly();
			}
			return context.getSharedPreferences(GeoloqiConstants.PREFERENCES_FILE, Context.MODE_PRIVATE).getString("userID", null);
		} finally {
			lock.unlock();
		}
	}

}
