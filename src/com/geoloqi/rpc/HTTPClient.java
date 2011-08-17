package com.geoloqi.rpc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Stack;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.Header;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.geoloqi.ADB;
import com.geoloqi.data.Fix;
import com.geoloqi.data.Geonote;
import com.geoloqi.data.Layer;
import com.geoloqi.data.Place;
import com.geoloqi.data.SharingLink;
import com.geoloqi.interfaces.AuthChangeListener;
import com.geoloqi.interfaces.GeoloqiClient;
import com.geoloqi.interfaces.GeoloqiConstants;
import com.geoloqi.interfaces.RPCException;

public final class HTTPClient implements GeoloqiClient {

	private static final int TIMEOUT = 60000;

	private static final HttpParams httpParams = new BasicHttpParams();
	private static HttpClient client;

	private Context context;

	private static volatile GeoloqiClient geoloqiClient = null;

	private OAuthToken token = null;

	protected HTTPClient(Context context) {
		this.context = context;
		HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT);
		client = new DefaultHttpClient(httpParams);
		authNotifier.start();
	}

	public static synchronized GeoloqiClient getApplicationClient(Context context) {
		if (geoloqiClient == null) {
			geoloqiClient = new HTTPClient(context);
		}
		return geoloqiClient;
	}

	private static BasicNameValuePair pair(String key, String val) {
		return new BasicNameValuePair(key, val);
	}

	private static Header header(String name, String val) {
		return new BasicHeader(name, val);
	}

	//ACCOUNT METHODS

	public void createAnonymousAccount() throws RPCException {
		MyRequest request = new MyRequest(MyRequest.POST, GeoloqiConstants.URL_BASE + "user/create_anon");
		request.addEntityParams(pair("client_id", GeoloqiConstants.GEOLOQI_ID), pair("client_secret", GeoloqiConstants.GEOLOQI_SECRET));
		try {

			JSONObject response;
			response = send(request);
			saveToken(new OAuthToken(response));
		} catch (JSONException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public void signUp(String email) throws RPCException {
		MyRequest request = new MyRequest(MyRequest.POST, GeoloqiConstants.URL_BASE + "account/anonymous_set_email");
		request.authorize(getToken());
		request.addEntityParams(pair("client_id", GeoloqiConstants.GEOLOQI_ID), pair("client_secret", GeoloqiConstants.GEOLOQI_SECRET), pair("email", email));
		send(request);
		authChangeLock.lock();
		try {
			token.signedUp();
			authChanged.signalAll();
		} finally {
			authChangeLock.unlock();
		}
	}

	public JSONObject logIn(String username, String password) throws RPCException {
		MyRequest request = new MyRequest(MyRequest.POST, GeoloqiConstants.URL_BASE + "oauth/token");
		request.addHeaders(header("X-LQ-Request-Type", "login"));
		request.addEntityParams(pair("grant_type", "password"), pair("client_id", GeoloqiConstants.GEOLOQI_ID), pair("client_secret", GeoloqiConstants.GEOLOQI_SECRET), pair("username", username), pair("password", password));

		try {
			JSONObject response = send(request);
			saveToken(new OAuthToken(response));
			return response;
		} catch (JSONException e) {
			throw new RuntimeException(" JSON Exception: " + e.getMessage());
		}
	}

	public void logOut() {
		authChangeLock.lock();
		try {
			context.getSharedPreferences(GeoloqiConstants.PREFERENCES_FILE, Context.MODE_PRIVATE).edit().remove("refreshToken").remove("isAnonymous").commit();
			token = null;
			authChanged.signalAll();
		} finally {
			authChangeLock.unlock();
		}
	}

	//DATA METHODS

	public String getDisplayName() {
		return token == null ? "Anonymous" : token.displayName;
	}

	@Override
	public void postLocationUpdate(Fix[] locations) throws RPCException {
		if (locations.length == 0) {
			return;
		}

		MyRequest request;
		// Encode locations in a JSON array.
		String json = "[" + locations[0].castToJSONObject().toString();
		for (int i = 1; i < locations.length; i++) {
			json += "," + locations[i].castToJSONObject().toString();
		}
		json += "]";
		// Done encoding.

		request = new MyRequest(MyRequest.POST, GeoloqiConstants.URL_BASE + "location/update");
		request.authorize(getToken());
		request.addHeaders(header("Content-type", "application/json"));

		try {
			request.setEntity(new StringEntity(json, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		try {
			send(request);
		} catch (ExpiredTokenException e) {
			refreshToken();
		}
	}

	public SharingLink postSharingLink(Integer minutes, String message) throws RPCException {
		MyRequest request = new MyRequest(MyRequest.POST, GeoloqiConstants.URL_BASE + "link/create");
		request.authorize(getToken());
		request.addEntityParams(pair("description", message));
		if (minutes != null) {
			request.addEntityParams(pair("minutes", "" + minutes));
		}

		try {
			return new SharingLink(send(request));
		} catch (JSONException e) {
			throw new RuntimeException(e.getMessage());
		} catch (ExpiredTokenException e) {
			refreshToken();
		}

		return postSharingLink(minutes, message);
	}

	public String postGeonote(Geonote note) throws RPCException {
		MyRequest request = new MyRequest(MyRequest.POST, GeoloqiConstants.URL_BASE + "geonote/create");
		request.authorize(getToken());
		request.addEntityParams(pair("latitude", "" + (note.place.location.getLatitudeE6() / 1000000.)), pair("longitude", "" + (note.place.location.getLongitudeE6() / 1000000.)), pair("span_longitude", "" + note.place.longitudinalRadius), pair("text", note.text));
		if (note.extra != null) {
			request.addEntityParams(pair("evernote", note.extra));
		}
		if (!note.place.isAnonymous()) {
			request.addEntityParams(pair("place_id", note.place.id));
		}

		try {
			JSONObject response = send(request);
			if (note.place.id == null && !note.place.isAnonymous()) {
				try {
					return response.getString("place_id");
				} catch (JSONException e) {
					throw new RuntimeException(e);
				}
			} else {
				return null;
			}
		} catch (ExpiredTokenException e) {
			refreshToken();
		}

		return postGeonote(note);
	}

	public String postPlace(Place place) throws RPCException {
		MyRequest request = new MyRequest(MyRequest.POST, GeoloqiConstants.URL_BASE + "place/create");
		request.authorize(getToken());
		request.addEntityParams(pair("latitude", "" + place.location.getLatitudeE6() / 1000000.), pair("longitude", "" + place.location.getLongitudeE6() / 1000000.), pair("span_longitude", "" + (place.longitudinalRadius * 2)), pair("name", place.name));

		try {
			JSONObject response = send(request);
			try {
				return response.getString("place_id");
			} catch (JSONException e) {
				throw new RPCException(e.getMessage());
			}
		} catch (ExpiredTokenException e) {
			refreshToken();
		}

		return postPlace(place);
	}

	public Geonote[] getSetGeonotes() throws RPCException {
		MyRequest request = new MyRequest(MyRequest.GET, GeoloqiConstants.URL_BASE + "geonote/list_set");
		request.authorize(getToken());

		try {
			JSONObject response = send(request);
			JSONArray array = response.getJSONArray("geonotes");

			Geonote[] geonotes = new Geonote[array.length()];
			for (int i = 0; i < geonotes.length; i++) {
				geonotes[i] = new Geonote(array.getString(i));
			}

			return geonotes;
		} catch (ExpiredTokenException e) {
			refreshToken();
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}

		return getSetGeonotes();
	}

	public Geonote[] getRecentGeonotes() throws RPCException {
		MyRequest request = new MyRequest(MyRequest.GET, GeoloqiConstants.URL_BASE + "geonote/list_recent");
		request.authorize(getToken());

		try {
			JSONObject response = send(request);
			JSONArray array = response.getJSONArray("geonotes");

			Geonote[] geonotes = new Geonote[array.length()];
			for (int i = 0; i < geonotes.length; i++) {
				geonotes[i] = new Geonote(array.getString(i));
			}

			return geonotes;
		} catch (ExpiredTokenException e) {
			refreshToken();
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}

		return getRecentGeonotes();
	}

	public Place[] getPlaces() throws RPCException {
		MyRequest request = new MyRequest(MyRequest.POST, GeoloqiConstants.URL_BASE + "place/list");
		request.authorize(getToken());

		try {
			JSONObject response = send(request);
			JSONArray array = response.getJSONArray("places");
			Place[] places = new Place[array.length()];
			for (int i = 0; i < places.length; i++) {
				places[i] = new Place(array.getString(i));
			}
			return places;
		} catch (ExpiredTokenException e) {
			refreshToken();
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}

		return getPlaces();
	}

	public Layer[][] getLayers() throws RPCException {
		MyRequest request = new MyRequest(MyRequest.GET, GeoloqiConstants.URL_BASE + "layer/app_list");
		request.authorize(getToken());

		try {
			JSONObject response = send(request);
			JSONArray active = response.getJSONArray("active");
			JSONArray inactive = response.getJSONArray("inactive");
			JSONArray featured = response.getJSONArray("featured");

			Layer[][] array = new Layer[3][];
			array[0] = new Layer[active.length()];
			array[1] = new Layer[inactive.length()];
			array[2] = new Layer[featured.length()];

			for (int i = 0; i < array[0].length; i++) {
				array[0][i] = new Layer(active.getJSONObject(i));
			}
			for (int i = 0; i < array[1].length; i++) {
				array[1][i] = new Layer(inactive.getJSONObject(i));
			}
			for (int i = 0; i < array[2].length; i++) {
				array[2][i] = new Layer(featured.getJSONObject(i));
			}

			return array;
		} catch (ExpiredTokenException e) {
			refreshToken();
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}

		return getLayers();

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
				if (response.getString("error").equals("expired_token")) {
					refreshToken();
					request.authorize(token);
					return send(request);
				} else {
					throw new RPCException(response.getString("error"));
				}
			} catch (JSONException e) {
				throw new RuntimeException(e.getMessage());
			}
		} else {
			return response;
		}
	}

	//TOKEN HANDLING

	protected synchronized OAuthToken getToken() throws RPCException {
		if (token == null) {
			if (context.getSharedPreferences(GeoloqiConstants.PREFERENCES_FILE, Context.MODE_PRIVATE).contains("refreshToken")) {
				ADB.log("Refreshing token");
				refreshToken();
			} else {
				createAnonymousAccount();
			}
		}
		return token;
	}

	protected void refreshToken() {
		MyRequest request = new MyRequest(MyRequest.POST, GeoloqiConstants.URL_BASE + "oauth/token");
		String refreshToken;
		if (token != null) {
			refreshToken = token.refreshToken;
		} else if (context.getSharedPreferences(GeoloqiConstants.PREFERENCES_FILE, Context.MODE_PRIVATE).contains("refreshToken")) {
			refreshToken = context.getSharedPreferences(GeoloqiConstants.PREFERENCES_FILE, Context.MODE_PRIVATE).getString("refreshToken", null);
		} else {
			throw new RuntimeException("No refresh token");
		}
		request.addEntityParams(pair("grant_type", "refresh_token"), pair("client_id", GeoloqiConstants.GEOLOQI_ID), pair("client_secret", GeoloqiConstants.GEOLOQI_SECRET), pair("refresh_token", refreshToken));

		try {
			saveToken(new OAuthToken(send(request)));
		} catch (RPCException e) {
			ADB.log("RPCException: " + e.getMessage());
			ADB.log("Refreshing token.");
			refreshToken();
		} catch (JSONException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	protected void saveToken(OAuthToken token) {
		authChangeLock.lock();
		try {
			context.getSharedPreferences(GeoloqiConstants.PREFERENCES_FILE, Context.MODE_PRIVATE).edit().putString("refreshToken", token.refreshToken).putBoolean("isAnonymous", token.isAnonymous).commit();
			this.token = token;
			authChanged.signalAll();
		} finally {
			authChangeLock.unlock();
		}
	}

	//AUTH NOTIFICATIONS

	private static final ReentrantLock authChangeLock = new ReentrantLock(true);
	private static final Condition authChanged = authChangeLock.newCondition();
	private final Semaphore listenerLock = new Semaphore(1, true);
	private final Stack<AuthChangeListener> authChangeListeners = new Stack<AuthChangeListener>();

	public int getAuthLevel() {
		if (token != null) {
			return token.isAnonymous ? ANONYMOUS : LOGGED_IN;
		} else if (context.getSharedPreferences(GeoloqiConstants.PREFERENCES_FILE, Context.MODE_PRIVATE).contains("isAnonymous")) {
			return context.getSharedPreferences(GeoloqiConstants.PREFERENCES_FILE, Context.MODE_PRIVATE).getBoolean("isAnonymous", true) ? ANONYMOUS : LOGGED_IN;
		} else {
			return LOGGED_OUT;
		}
	}

	public void registerAuthChangeListener(AuthChangeListener authChangeListener) {
		listenerLock.acquireUninterruptibly();
		try {
			authChangeListeners.push(authChangeListener);
		} finally {
			listenerLock.release();
		}
	}

	protected final Thread authNotifier = new Thread() {
		@Override
		public void run() {
			authChangeLock.lock();
			try {
				int authLevel = getAuthLevel();
				ADB.log("Notifier: Authorization Level is " + authLevel + ".");
				listenerLock.acquireUninterruptibly();
				try {
					for (AuthChangeListener listener : authChangeListeners) {
						listener.onAuthChanged(authLevel);
					}
				} finally {
					listenerLock.release();
				}
				while (true) {
					authChanged.awaitUninterruptibly();
					ADB.log("Auth level changed.");
					authLevel = getAuthLevel();
					listenerLock.acquireUninterruptibly();
					try {
						for (AuthChangeListener listener : authChangeListeners) {
							listener.onAuthChanged(authLevel);
						}
					} finally {
						listenerLock.release();
					}
				}
			} finally {
				authChangeLock.unlock();
			}
		}
	};

	public String getAccessToken() throws RPCException {
		return getToken().accessToken;
	}
}