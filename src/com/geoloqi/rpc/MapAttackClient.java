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
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.geoloqi.ADB;
import com.geoloqi.Installation;
import com.geoloqi.data.Game;
import com.geoloqi.interfaces.GeoloqiConstants;
import com.geoloqi.interfaces.RPCException;

public class MapAttackClient {

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

	public void createAnonymousAccount(String name) throws RPCException {
		try {
			MyRequest request = new MyRequest(MyRequest.POST, GeoloqiConstants.URL_BASE + "user/create_anon");
			Header authHeader = new BasicScheme().authenticate(new UsernamePasswordCredentials(GeoloqiConstants.GEOLOQI_ID, GeoloqiConstants.GEOLOQI_SECRET), request.getRequest());
			request.addHeaders(authHeader);
			String deviceID = Installation.getIDAsString(context);
			String platform = android.os.Build.VERSION.RELEASE;
			String hardware = android.os.Build.MODEL;
			request.addEntityParams(pair("name", name), pair("device_id", deviceID), pair("platform", platform), pair("hardware", hardware));
			JSONObject response = send(request);
			saveToken(new OAuthToken(response));
		} catch (JSONException e) {
			throw new RuntimeException(e.getMessage());
		} catch (AuthenticationException e) {
			throw new RuntimeException(e);
		}
	}

	protected void saveToken(OAuthToken token) {
		context.getSharedPreferences(GeoloqiConstants.PREFERENCES_FILE, Context.MODE_PRIVATE).edit().putString("authToken", token.accessToken).commit();
	}

	public boolean hasToken() {
		return context.getSharedPreferences(GeoloqiConstants.PREFERENCES_FILE, Context.MODE_PRIVATE).contains("authToken");
	}

	public String getToken() {
		return context.getSharedPreferences(GeoloqiConstants.PREFERENCES_FILE, Context.MODE_PRIVATE).getString("authToken", null);
	}

	public List<Game> getGames(Double latitude, Double longitude) throws RPCException {
		MyRequest request = new MyRequest(MyRequest.GET, GeoloqiConstants.gameListAddress + "&latitude=" + latitude + "&longitude=" + longitude);
		Header authHeader;
		try {
			authHeader = new BasicScheme().authenticate(new UsernamePasswordCredentials(GeoloqiConstants.GEOLOQI_ID, GeoloqiConstants.GEOLOQI_SECRET), request.getRequest());
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

	public void joinGame(String id, String email, String initials) throws RPCException {
		String token = context.getSharedPreferences(GeoloqiConstants.PREFERENCES_FILE, Context.MODE_PRIVATE).getString("authToken", null);
		MyRequest request = new MyRequest(MyRequest.POST, "http://mapattack.org/game/" + id);
		request.addEntityParams(pair("access_token", token), pair("email", email), pair("initials", initials));
		try {
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

	private static Header header(String name, String val) {
		return new BasicHeader(name, val);
	}

	private static BasicNameValuePair pair(String key, String val) {
		return new BasicNameValuePair(key, val);
	}

}
