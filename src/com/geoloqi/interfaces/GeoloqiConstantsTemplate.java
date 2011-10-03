package com.geoloqi.interfaces;

/**
 * This is a template file. Rename this class to GeoloqiConstants
 * and update GEOLOQI_ID and GEOLOQI_SECRET with your API keys to get started.
 * 
 * @author tristanw
 */
public abstract interface GeoloqiConstantsTemplate {
	public static final String GEOLOQI_ID = "";
	public static final String GEOLOQI_SECRET = "";
	
	public static final String URL_BASE = "https://api.geoloqi.com/1/";
	public static final String GAME_LIST_ADDRESS = URL_BASE + "layer/nearby?application_id=?";
	public static final String GAME_REQUEST_ADDRESS = "http://mapattack.org/join/";
	
	public static final String UPLOAD_ADDRESS = "loki.geoloqi.com";
	public static final String DOWNLOAD_ADDRESS = "loki.geoloqi.com";
	
	public static final int UPLOAD_PORT = 40000;
	public static final int DOWNLOAD_PORT = 40001;

	public static final String PREFERENCES_FILE = "GEOLOQIHTTPCLIENT";
	public static final String VERSION = "1";
}
