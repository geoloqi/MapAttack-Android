package com.geoloqi.data;

import org.json.JSONException;
import org.json.JSONObject;

public class Game {

	public final String name, description, id;

	public Game(JSONObject data) {
		try {
			name = data.getString("name");
			description = data.getString("description");
			id = data.getString("layer_id");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

}
