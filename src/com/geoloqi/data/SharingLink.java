package com.geoloqi.data;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

public class SharingLink {

	public Uri link;
	public Uri shortLink;
	public String token;

	public SharingLink(JSONObject json) throws JSONException {
		this.link = Uri.parse(json.getString("link"));
		this.shortLink = Uri.parse(json.getString("shortlink"));
		this.token = json.getString("token");
	}

}
