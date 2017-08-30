package com.dlmv.localplayer.client.network;

import java.io.*;
import java.net.*;
import java.util.*;

public class NetworkManager {
	private static NetworkManager ourManager;

	//	private boolean myAuthorizationInProgress = false;

	public static NetworkManager Instance() {
		if (ourManager == null) {
			ourManager = new NetworkManager();
		}
		return ourManager;
	}

	private static final char PARAMETER_DELIMITER = '&';
	private static final char PARAMETER_EQUALS_CHAR = '=';

	private static String createQueryStringForParameters(Map<String, String> parameters) throws UnsupportedEncodingException {
		StringBuilder parametersAsQueryString = new StringBuilder();
		if (parameters != null) {
			boolean firstParameter = true;

			for (String parameterName : parameters.keySet()) {
				if (!firstParameter) {
					parametersAsQueryString.append(PARAMETER_DELIMITER);
				}
				parametersAsQueryString.append(parameterName)
				.append(PARAMETER_EQUALS_CHAR)
				.append(URLEncoder.encode(parameters.get(parameterName), "UTF-8"));

				firstParameter = false;
			}
		}
		return parametersAsQueryString.toString();
	}

	public void perform(NetworkRequest request) throws NetworkException {
		try {
			String us = request.URL + "?" + createQueryStringForParameters(request.PostParameters);
			URL url = new URL(us);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setReadTimeout(20000);
			urlConnection.setConnectTimeout(30000);
			try {
				int responseCode;
				try {
					responseCode = urlConnection.getResponseCode(); 
				} catch (IOException e) {
				    responseCode = urlConnection.getResponseCode(); 
				}
				if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
					throw new NetworkException();
				}
				InputStream stream =  urlConnection.getInputStream();
				if (stream != null) {
					try {
						request.handleStream(stream);
					} finally {
						stream.close();
					}
				}
			} finally {
				urlConnection.disconnect();
			}
		} catch (Exception e) {
			throw new NetworkException(e);
		}
	}


}
