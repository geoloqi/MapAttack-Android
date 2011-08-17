package com.geoloqi.rpc;

import org.json.JSONException;
import org.json.JSONObject;

class OAuthToken {
	public final String displayName;
	public final String username;
	public final String accessToken;
	public final String refreshToken;
	public final Long expiresIn;
	public final Long expiresAt;
	public final String scope;
	public boolean isAnonymous;

	public OAuthToken(JSONObject json) throws JSONException {
		displayName = json.getString("display_name");
		username = json.getString("username");
		accessToken = json.get("access_token").toString();
		refreshToken = json.get("refresh_token").toString();
		expiresIn = Long.parseLong(json.get("expires_in").toString()) * 1000l;
		expiresAt = System.currentTimeMillis() + expiresIn;
		scope = json.get("scope").toString();
		isAnonymous = json.getInt("is_anonymous") == 1 ? true : false;
	}

	public void signedUp() {
		isAnonymous = false;
	}

	@Override
	public String toString() {
		throw new RuntimeException();
	}
}
