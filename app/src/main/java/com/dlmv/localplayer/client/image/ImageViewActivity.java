package com.dlmv.localplayer.client.image;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.dlmv.localmediaplayer.client.R;

import android.app.Activity;
import android.content.Intent;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;

public class ImageViewActivity extends Activity {
	
	public final static String PATH = "PATH";

	private Bitmap myBitmap;

	private boolean myTooLate = false;

	private AsyncUploadImage myTask;

	public void setImageBitmap(Bitmap b) {
		myBitmap = b;
		if (myTooLate) {
			myBitmap.recycle();
			return;
		}
		setContentView(R.layout.image_loaded);
		SomeImageView v = findViewById(R.id.imageView);
		v.setBitmap(myBitmap);
	}

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		getWindow().setFlags(
				WindowManager.LayoutParams.FLAG_FULLSCREEN,
				 WindowManager.LayoutParams.FLAG_FULLSCREEN
				);

		setContentView(R.layout.image_loading);

		final Intent intent = getIntent();
		final String uri = intent.getStringExtra(PATH);
		myTask = new AsyncUploadImage(this);
		myTask.execute(uri);
		try {
			String uri1 = URLDecoder.decode(uri, "UTF-8");
			int divider = uri1.lastIndexOf("/");
			String title = uri1.substring(divider + 1);
			setTitle(title);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

	}

	protected void onPause() {
		super.onPause();
		if (myBitmap != null) {
			myBitmap.recycle();
		} else {
			myTooLate = true;
		}
		myBitmap = null;
		myTask.cancel(true);
		finish();
	}
}
