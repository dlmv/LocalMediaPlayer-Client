package com.dlmv.localplayer.client.util;

import android.content.Context;
import android.util.Log;

import com.dlmv.localmediaplayer.client.R;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

import org.w3c.dom.Element;

public class AbsFile {

	public static final String ROOT = "Files";
	public static final String DEVICE = "/";
	public static final String SAMBA = "smb://";


	public final String Path;
	public final String Size;
	public final boolean Readable;
	public final AbsFile.MediaType Type;

	public enum MediaType {
		AUDIO,
		IMAGE,
		OTHER,
		DIR,
		UP,
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof AbsFile)) {
			return false;
		}
		AbsFile a = (AbsFile) o;
		return Path.equals(a.Path);
	}

	public static ArrayList<AbsFile> rootList() {
		ArrayList<AbsFile> res = new ArrayList<>();
		res.add(new AbsFile(DEVICE, true, "", MediaType.DIR));
		res.add(new AbsFile(SAMBA, true, "", MediaType.DIR));
		return  res;
	}

	AbsFile(String path, boolean readable, String size, MediaType mt) {
		if (path.endsWith("/") && !path.equals(SAMBA) && !path.equals(DEVICE)) {
			Path = path.substring(0, path.length() - 1);
		} else {
			Path = path;
		}
		Readable = readable;
		Size  = size;
		Type = mt;
	}

	public String getName(Context c) {
		return name(Path, c);
	}

	public static String name(String path, Context c) {
		switch (path) {
			case ROOT:
				return c.getResources().getString(R.string.files);
			case DEVICE:
				return c.getResources().getString(R.string.device);
			case SAMBA:
				return c.getResources().getString(R.string.network);
			default:
				int divider = path.lastIndexOf("/");
				return path.substring(divider + 1);
		}
	}
	
	public String getPath() {
		if (Type.equals(MediaType.DIR) || Type.equals(MediaType.UP)) {
			return Path;
		} else {
			if (Path.endsWith("/")) {
				return Path.substring(0, Path.length() - 1);
			}
			return Path;
		}
	}

	public static String parent(String path) {
        Log.d("WTF", path);
		if (path.equals(ROOT)) {
			return null;
		} else if (path.equals(DEVICE) || path.equals(SAMBA)) {
			return ROOT;
		} else {
			path = path.substring(0, path.length() - 1);
			int divider = path.lastIndexOf("/");
			return path.substring(0, divider) + "/";
		}
	}

	public static AbsFile fromDom(Element e) throws UnsupportedEncodingException {
		String pt = URLDecoder.decode(e.getAttribute("path"), "UTF-8");
		String sz = e.getAttribute("size");
		Boolean rd = Boolean.parseBoolean(e.getAttribute("readable"));
		MediaType t = MediaType.valueOf(e.getAttribute("type"));
		return new AbsFile(pt, rd, sz, t);
	}

}