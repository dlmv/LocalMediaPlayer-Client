package com.dlmv.localplayer.client.image;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.dlmv.localmediaplayer.client.R;
import com.dlmv.localplayer.client.util.ApplicationUtil;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

class AsyncUploadImage extends AsyncTask<Object, Object, Object> {
	
	@Override
	protected void onCancelled() {
		freeMem();
		if (bitmap != null) {
			bitmap.recycle();
			bitmap = null;
		}
	}
	
	private static final String TAG = "AsyncUploadImage ";
	private ImageViewActivity myActivity;
	private HttpURLConnection connection;
	private InputStream is;
	private Bitmap bitmap;
	AsyncUploadImage(ImageViewActivity a) {
		myActivity = a;
	}
	
	private void freeMem() {
		try {
			if (is != null) {
				is.close();
				is = null;
			}
			if (connection != null) {
				connection.disconnect();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected Object doInBackground(Object... params) {
		URL url;
		try {
			String sb = ApplicationUtil.Data.serverUri + "image" +
					"?" +
					"path=" +
					params[0];
			url = new URL(sb);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();
			is = connection.getInputStream();
			bitmap = BitmapFactory.decodeStream(is);
			connection.disconnect();
			is.close();
			connection = null;
			is = null;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			freeMem();
		}
		return bitmap;
	}
	@Override
	protected void onPostExecute(Object result) {
		super.onPostExecute(result);
		if (null != result) {
			myActivity.setImageBitmap((Bitmap) result);
			Log.i(TAG, "image download ok！！！");
		}else {
			Bitmap b = BitmapFactory.decodeResource(myActivity.getResources(), R.drawable.image_error);
			myActivity.setImageBitmap(b);
			Log.i(TAG, "image download false！！！");
		}
	}

}