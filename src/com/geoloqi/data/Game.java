package com.geoloqi.data;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

public class Game implements Parcelable {
	public String id;
	public String name;
	public String description;

	public static final Parcelable.Creator<Game> CREATOR = new Parcelable.Creator<Game>() {
		public Game createFromParcel(Parcel in) {
			return new Game(in);
		}
		
		public Game[] newArray(int size) {
			return new Game[size];
		}
	};

	public Game(JSONObject data) {
		try {
			id = data.getString("layer_id");
			name = data.getString("name");
			description = data.getString("description");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	private Game(Parcel in) {
		id = in.readString();
		name = in.readString();
		description = in.readString();
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(id);
		out.writeString(name);
		out.writeString(description);
	}
}
