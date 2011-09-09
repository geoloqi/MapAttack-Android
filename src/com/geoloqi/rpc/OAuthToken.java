package com.geoloqi.rpc;

import org.json.JSONException;
import org.json.JSONObject;

class OAuthToken {
	public final String accessToken;

	public OAuthToken(JSONObject json) throws JSONException {
		accessToken = json.get("access_token").toString();
	}
}
