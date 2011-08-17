package com.geoloqi.data;

import org.json.JSONException;
import org.json.JSONObject;

public class Layer {

	public String layerId, userId, type, name, icon, description, url;
	boolean isPublic, isSubscribed;

	public Layer(JSONObject json) throws JSONException {
		layerId = json.getString("layer_id");
		userId = json.getString("user_id");
		type = json.getString("type");
		name = json.getString("name");
		icon = json.getString("icon");
		description = json.getString("description");
		url = json.getString("url");
		isPublic = json.getBoolean("subscribed");
		isSubscribed = json.getBoolean("subscribed");
	}

	public JSONObject castToJSONObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("layer_id", layerId);
			obj.put("user_id", userId);
			obj.put("type", type);
			obj.put("name", name);
			obj.put("public", isPublic);
			obj.put("icon", icon);
			obj.put("description", description);
			obj.put("subscribed", isSubscribed);
			obj.put("url", url);
			return obj;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

}
