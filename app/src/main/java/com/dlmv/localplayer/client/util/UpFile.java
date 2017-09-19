package com.dlmv.localplayer.client.util;

import android.content.Context;

public class UpFile extends AbsFile {

	@Override
	public String getName(Context c) {
		return "..";
	}

	public UpFile(String path) {
		super(path, true, "0", MediaType.UP);
	}

}
