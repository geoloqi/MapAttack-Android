package com.geoloqi.rpc;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.geoloqi.ADB;
import com.geoloqi.Installation;
import com.geoloqi.data.Game;
import com.geoloqi.interfaces.GeoloqiConstants;
import com.geoloqi.interfaces.RPCException;

public class MapAttackClient implements GeoloqiConstants {
	private final String TAG = "MapAttackClient";

	private static final int TIMEOUT = 60000;

	private static final HttpParams httpParams = new BasicHttpParams();
	private static HttpClient client;
	private static MapAttackClient singleton = null;
	private Context context;

	public static MapAttackClient getApplicationClient(Context context) {
		if (singleton == null) {
			singleton = new MapAttackClient(context);
		}
		return singleton;
	}

	private MapAttackClient(Context context) {
		this.context = context;
		HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT);
		client = new DefaultHttpClient(httpParams);
	}

	public void createAnonymousAccount() throws RPCException {
		try {
			String name, deviceID, platform, hardware;
			{// Initialize variables.
				name = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE).getString("initials", null);
				deviceID = Installation.getIDAsString(context);
				platform = android.os.Build.VERSION.RELEASE;
				hardware = android.os.Build.MODEL;
			}

			MyRequest request;
			{
				request = new MyRequest(MyRequest.POST, URL_BASE + "user/create_anon");
				request.addHeaders(new BasicScheme().authenticate(new UsernamePasswordCredentials(GEOLOQI_ID, GEOLOQI_SECRET), request.getRequest()));
				request.addEntityParams(pair("name", name), pair("device_id", deviceID), pair("platform", platform), pair("hardware", hardware));
			}

			JSONObject response = send(request);

			{//Save Results
				saveToken(new OAuthToken(response));
				context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE).edit().putString("userID", response.getString("user_id")).commit();
			}
		} catch (JSONException e) {
			throw new RuntimeException(e.getMessage());
		} catch (AuthenticationException e) {
			throw new RuntimeException(e);
		}
	}

	protected void saveToken(OAuthToken token) {
		context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE).edit().putString("authToken", token.accessToken).commit();
	}

	public boolean hasToken() {
		return context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE).contains("authToken");
	}

	public String getToken() {
		return context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE).getString("authToken", null);
	}

	public List<Game> getGames(Double latitude, Double longitude) throws RPCException {
		MyRequest request = new MyRequest(MyRequest.GET,
				GAME_LIST_ADDRESS + "&latitude=" + latitude + "&longitude=" + longitude);
		Header authHeader;
		try {
			authHeader = new BasicScheme().authenticate(new UsernamePasswordCredentials(GEOLOQI_ID, GEOLOQI_SECRET), request.getRequest());
		} catch (AuthenticationException e) {
			throw new RPCException(e.getMessage());
		}
		request.addHeaders(authHeader);
		JSONObject response = send(request);
		try {
			JSONArray gamesArray = response.getJSONArray("nearby");
			List<Game> games = new LinkedList<Game>();
			for (int i = 0; i < gamesArray.length(); i++) {
				games.add(new Game(gamesArray.getJSONObject(i)));
			}
			return games;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Get the best guess intersection name for the given latitude and longitude.
	 * 
	 * @param latitude
	 * @param longitude
	 * @return The nearest intersection as a String or null.
	 */
	public String getNearestIntersection(final Double latitude, final Double longitude) {
		// Build the request
		final MyRequest request = new MyRequest(MyRequest.GET,
				String.format("%slocation/context?latitude=%s&longitude=%s", URL_BASE, latitude, longitude));

		try {
			// Sign the request
			Header authHeader = new BasicScheme().authenticate(
					new UsernamePasswordCredentials(GEOLOQI_ID, GEOLOQI_SECRET), request.getRequest());
			request.addHeaders(authHeader);

			try {
				// Get the response
				JSONObject response = send(request);
				return response.getString("best_name");
			} catch (RPCException e) {
				Log.e(TAG, "Got an RPCException when fetching the nearest intersection name!", e);
			} catch (JSONException e) {
				Log.e(TAG, "Got a JSONException when fetching the nearest intersection name!", e);
			}
		} catch (AuthenticationException e) {
			Log.e(TAG, "Got an AuthenticationException when fetching the nearest intersection name!", e);
		}

		return null;
	}

	public void joinGame(String id) throws RPCException {
		String token, email, initials;
		{// Initialize variables
			SharedPreferences prefs = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
			token = prefs.getString("authToken", null);
			email = prefs.getString("email", null);
			initials = prefs.getString("initials", null);
		}
		MyRequest request;
		{// Initialize the request.
			request = new MyRequest(MyRequest.POST, "http://mapattack.org/game/" + id + "/join");
			request.addEntityParams(pair("access_token", token), pair("email", email), pair("initials", initials));
		}

		try {// Send will throw a RuntimeException for the non-JSON return value.
			send(request);
		} catch (RuntimeException e) {
		}
	}

	protected synchronized JSONObject send(MyRequest request) throws RPCException {
		ADB.log("param " + request.getRequest().getURI());
		JSONObject response;
		try {
			response = new JSONObject(EntityUtils.toString(client.execute(request.getRequest()).getEntity()));
		} catch (ParseException e) {
			ADB.log("ParseException: " + e.getMessage());
			throw new RuntimeException(e.getMessage());
		} catch (JSONException e) {
			ADB.log("JSONException: " + e.getMessage());
			throw new RuntimeException(e.getMessage());
		} catch (ClientProtocolException e) {
			ADB.log("ClientProtocolException: " + e.getMessage());
			throw new RPCException(e.getMessage());
		} catch (IOException e) {
			ADB.log("IOException: " + e.getMessage());
			throw new RPCException(e.getMessage());
		}

		if (response.has("error")) {
			try {
				throw new RPCException(response.getString("error"));
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
		} else {
			return response;
		}
	}

	private static BasicNameValuePair pair(String key, String val) {
		return new BasicNameValuePair(key, val);
	}

}
