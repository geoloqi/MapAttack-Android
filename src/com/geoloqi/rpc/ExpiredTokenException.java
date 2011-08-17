package com.geoloqi.rpc;

import com.geoloqi.interfaces.RPCException;

@SuppressWarnings("serial")
class ExpiredTokenException extends RPCException {
	ExpiredTokenException() {
		super("Expired token.");
	}
}
