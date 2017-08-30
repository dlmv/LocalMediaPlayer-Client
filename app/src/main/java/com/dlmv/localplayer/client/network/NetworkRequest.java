package com.dlmv.localplayer.client.network;

import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class NetworkRequest {
	String URL;
	public final Map<String,String> PostParameters = new HashMap<String,String>();

	protected NetworkRequest(String url) {
		URL = url;
	}

	public void addPostParameter(String name, String value) {
		PostParameters.put(name, value);
	}

	public abstract void handleStream(InputStream inputStream) throws NetworkException;

}
