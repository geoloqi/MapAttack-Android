package com.geoloqi.data;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.maps.GeoPoint;

public class Place implements Parcelable {

	public String name;
	public String id = null;
	public GeoPoint location;
	public double longitudinalRadius;

	public Place(String name, GeoPoint location, double longitudinalRadius) {
		this.name = name;
		this.location = location;
		this.longitudinalRadius = longitudinalRadius;
	}

	public Place(String json) throws JSONException {
		JSONObject obj = new JSONObject(json);
		name = obj.getString("name");
		location = new GeoPoint((int) (obj.getDouble("latitude") * 1000000.), (int) (obj.getDouble("longitude") * 1000000.));
		longitudinalRadius = obj.getDouble("radius");
		if (obj.has("place_id")) {
			id = obj.getString("place_id");
		}
	}

	public boolean isAnonymous() {
		return name.equals("");
	}

	public JSONObject castToJSONObject() {
		try {
			JSONObject obj = new JSONObject();
			obj.put("name", name);
			obj.put("layer_type", "geonotes");
			obj.put("latitude", location.getLatitudeE6() / 1000000.);
			obj.put("longitude", location.getLongitudeE6() / 1000000.);
			obj.put("radius", longitudinalRadius);
			if (id != null) {
				obj.put("place_id", id);
			}
			return obj;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(name);
		out.writeInt(location.getLatitudeE6());
		out.writeInt(location.getLongitudeE6());
		out.writeDouble(longitudinalRadius);
		if (id == null) {
			out.writeInt(0);
		} else {
			out.writeInt(1);
			out.writeString(id);
		}
	}

	public static final Parcelable.Creator<Place> CREATOR = new Parcelable.Creator<Place>() {
		@Override
		public Place createFromParcel(Parcel in) {
			Place place = new Place(in.readString(), new GeoPoint(in.readInt(), in.readInt()), in.readDouble());
			if (in.readInt() == 1) {
				place.id = in.readString();
			}
			return place;
		}

		@Override
		public Place[] newArray(int size) {
			return new Place[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}
}
