package com.geoloqi.data;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.maps.GeoPoint;

public class Geonote implements Parcelable {

	public Place place;
	public String text;
	public long date_created_ts;
	public String extra;

	public Geonote(Place place, String text, long timeInSeconds, String extra) {
		this.place = place;
		this.text = text;
		this.date_created_ts = timeInSeconds;
		this.extra = extra;
	}

	public Geonote(String json) throws JSONException {

		JSONObject obj = new JSONObject(json);
		this.date_created_ts = obj.getLong("date_created_ts");
		text = obj.getString("text");
		int latitude = (int) (obj.getDouble("latitude") * 1000000.);
		int longitude = (int) (obj.getDouble("longitude") * 1000000.);
		double radius = obj.getDouble("radius");
		place = new Place(obj.getString("place_name"), new GeoPoint(latitude, longitude), radius);
	}

	public JSONObject castToJSONObject() {
		try {
			JSONObject obj = new JSONObject();
			obj.put("text", text);
			obj.put("latitude", place.location.getLatitudeE6() / 1000000.);
			obj.put("longitude", place.location.getLongitudeE6() / 1000000.);
			obj.put("radius", place.longitudinalRadius);
			obj.put("date_created_ts", date_created_ts);
			obj.put("place_name", place.name);
			if (extra != null) {
				obj.put("extra", extra);
			}
			return obj;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeParcelable(place, flags);
		out.writeString(text);
		out.writeLong(date_created_ts);
		if (extra == null) {
			out.writeInt(0);
		} else {
			out.writeInt(1);
			out.writeString(extra.toString());
		}
	}

	public static final Parcelable.Creator<Geonote> CREATOR = new Parcelable.Creator<Geonote>() {
		@Override
		public Geonote createFromParcel(Parcel in) {
			Geonote note = new Geonote((Place) in.readParcelable(Place.class.getClassLoader()), in.readString(), in.readLong(), in.readInt() == 1 ? in.readString() : null);
			return note;
		}

		@Override
		public Geonote[] newArray(int size) {
			return new Geonote[size];
		}
	};

}
