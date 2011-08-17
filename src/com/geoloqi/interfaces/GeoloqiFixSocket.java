package com.geoloqi.interfaces;

import com.geoloqi.data.Fix;

public interface GeoloqiFixSocket {

	public void pushFixes(Fix[] fixes);

}
