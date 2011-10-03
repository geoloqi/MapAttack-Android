package com.geoloqi.data;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import com.geoloqi.ADB;
import static com.geoloqi.interfaces.GeoloqiConstants.VERSION;;

public class Fix extends Location implements Serializable, Parcelable, Comparable<Fix> {

	private static final long serialVersionUID = 3L;
	List<Pair<String, String>> rawData;

	public Fix(Uri uri) {
		super("Geoloqi");
		String[] data = uri.getPath().split("/");
		this.setLatitude(Float.parseFloat(data[1]));
		this.setLongitude(Float.parseFloat(data[2]));
		this.setAltitude(Float.parseFloat(data[3]));
		this.setBearing(Float.parseFloat(data[4]));
		this.setSpeed(Float.parseFloat(data[5]));
		this.setTime(Long.parseLong(data[6]));
		this.setAccuracy(Float.parseFloat(data[7]));

		rawData = new LinkedList<Pair<String, String>>();
		for (int i = 0; 9 + 2 * i + 1 < data.length; i++) {
			rawData.add(new Pair<String, String>(data[9 + 2 * i], data[9 + 2 * i + 1]));
		}
	}

	public Fix(JSONObject json, long timestamp, float bearing) {
		super("");
		try {
			this.setProvider(json.getJSONObject("client").getString("name"));

			JSONObject position = json.getJSONObject("location").getJSONObject("position");
			this.setAccuracy((float) position.getDouble("horizontal_accuracy"));
			this.setAltitude(position.getDouble("altitude"));
			this.setLatitude(position.getDouble("latitude"));
			this.setLongitude(position.getDouble("longitude"));
			this.setSpeed((float) position.getDouble("speed"));

			rawData = new LinkedList<Pair<String, String>>();
			JSONObject raw = json.getJSONObject("raw");
			@SuppressWarnings("unchecked")
			Iterator<String> keys = raw.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				rawData.add(new Pair<String, String>(key, raw.getString(key)));
			}
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		this.setBearing(bearing);
		this.setTime(timestamp);
	}

	public Fix(Location l, Pair<String, String>... rawData) {
		super(l);
		this.rawData = Arrays.asList(rawData);
	}

	public JSONObject castToJSONObject() {
		try {
			String version = VERSION;
			String platform = "2.1";
			String hardware = "unknown";
			JSONObject point = new JSONObject();

			point.put("latitude", getLatitude());
			point.put("longitude", getLongitude());
			point.put("speed", getSpeed());
			point.put("altitude", getAltitude());
			point.put("horizontal_accuracy", getAccuracy());

			JSONObject location = new JSONObject();
			JSONObject raw = new JSONObject();
			JSONObject client = new JSONObject();

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
			Date d = new Date(getTime());

			location.put("type", "point");
			location.put("position", point);

			for (Pair<String, String> datum : rawData) {
				raw.put(datum.first, datum.second);
			}

			client.put("name", "Geoloqi");
			client.put("version", version);
			client.put("platform", platform);
			client.put("hardware", hardware);

			JSONObject json = new JSONObject();

			json.put("date", sdf.format(d));
			json.put("location", location);
			json.put("raw", raw);
			json.put("client", client);

			return json;
		} catch (JSONException e) {
			ADB.log("JSON Exception in toJSON: " + e.getMessage());
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {

	}

	@Override
	public int compareTo(Fix arg) {
		return (int) (this.getTime() - arg.getTime());
	}

}
