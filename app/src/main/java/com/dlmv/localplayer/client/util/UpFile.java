package com.dlmv.localplayer.client.util;

public class UpFile extends AbsFile {

	@Override
	public String getName() {
		return "..";
	}

	public UpFile(String path) {
		super(path, true, "0", MediaType.UP);
	}

}
