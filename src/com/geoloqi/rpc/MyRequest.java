package com.geoloqi.rpc;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;

import com.geoloqi.ADB;

class MyRequest {
	static final int GET = 0;
	static final int POST = 1;

	private ArrayList<Header> headers = new ArrayList<Header>();
	private BasicHttpParams params = new BasicHttpParams();
	private ArrayList<BasicNameValuePair> entityParams = new ArrayList<BasicNameValuePair>();
	private AbstractHttpEntity entity = null;

	private final HttpRequestBase request;

	MyRequest(int requestType, String url) {
		switch (requestType) {
		case GET:
			request = new HttpGet(url);
			break;
		case POST:
			request = new HttpPost(url);
			break;
		default:
			throw new IllegalArgumentException("Request type must be one of the static types.");
		}
	}

	synchronized void authorize(OAuthToken token) {
		ADB.log("In authorize");
		// Remove the old authorization header.
		for (Header header : headers) {
			if (header.getName().equals("Authorization")) {
				headers.remove(header);
			}
		}
		// Insert the new authorization header.
		headers.add(new BasicHeader("Authorization", "OAuth " + token.accessToken));
	}

	synchronized void addHeaders(Header... headers) {
		ADB.log("In addHeaders");
		for (int i = 0; i < headers.length; i++) {
			this.headers.add(headers[i]);
		}
	}

	synchronized void addParams(BasicNameValuePair... pairs) {
		ADB.log("In addParams");
		for (int i = 0; i < pairs.length; i++) {
			params.setParameter(pairs[i].getName(), pairs[i].getValue());
		}
	}

	synchronized void addEntityParams(BasicNameValuePair... pairs) {
		ADB.log("In addEntityParams");
		if (request instanceof HttpEntityEnclosingRequestBase) {
			for (int i = 0; i < pairs.length; i++) {
				entityParams.add(pairs[i]);
			}
		} else {
			throw new RuntimeException("Request must be PUT or POST to enclose an entity.");
		}
	}

	synchronized void setEntity(AbstractHttpEntity entity) {
		ADB.log("In setEntity");
		if (request instanceof HttpEntityEnclosingRequestBase) {
			this.entity = entity;
		} else {
			throw new RuntimeException("Request must be PUT or POST to enclose an entity.");
		}
	}

	synchronized public HttpRequestBase getRequest() {
		ADB.log("------------------------------------------");
		ADB.log("URI: " + request.getURI());
		for (Header header : headers) {
			ADB.log("Header: " + header.getName() + "=" + header.getValue());
		}
		ADB.log("Parameters: " + params.toString());
		for (BasicNameValuePair param : entityParams) {
			ADB.log("Entity Parameter: " + param.getName() + "=" + param.getValue());
		}
		//Set headers
		if (headers.size() > 0) {
			request.setHeaders(headers.toArray(new Header[headers.size()]));
		}
		//Set params
		request.setParams(params);
		//Set entity
		if (request instanceof HttpEntityEnclosingRequestBase) {
			if (entityParams.size() > 0) {
				if (entity != null) {
					throw new RuntimeException("Entity conflict");
				} else {
					try {
						((HttpEntityEnclosingRequestBase) request).setEntity(new UrlEncodedFormEntity(entityParams));
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException(e.getMessage());
					}
				}
			} else if (entity != null) {
				((HttpEntityEnclosingRequestBase) request).setEntity(entity);
			}
		}
		return request;
	}
}
