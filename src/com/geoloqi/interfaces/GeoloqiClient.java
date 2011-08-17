package com.geoloqi.interfaces;

import org.json.JSONObject;

import com.geoloqi.data.Fix;
import com.geoloqi.data.Geonote;
import com.geoloqi.data.Layer;
import com.geoloqi.data.Place;
import com.geoloqi.data.SharingLink;

public interface GeoloqiClient {

	public static final int LOGGED_OUT = 0;
	public static final int ANONYMOUS = 1;
	public static final int LOGGED_IN = 2;

	public void registerAuthChangeListener(AuthChangeListener authChangeListener);

	public int getAuthLevel();

	public void createAnonymousAccount() throws RPCException;

	public void signUp(String email) throws RPCException;

	public JSONObject logIn(String email, String password) throws RPCException;

	public void logOut();

	public String getDisplayName();

	public void postLocationUpdate(Fix[] locations) throws RPCException;

	public SharingLink postSharingLink(Integer minutes, String message) throws RPCException;

	public String postGeonote(Geonote note) throws RPCException;

	public String postPlace(Place place) throws RPCException;

	public Geonote[] getSetGeonotes() throws RPCException;

	public Geonote[] getRecentGeonotes() throws RPCException;

	public Place[] getPlaces() throws RPCException;

	public Layer[][] getLayers() throws RPCException;

	public String getAccessToken() throws RPCException;

}
