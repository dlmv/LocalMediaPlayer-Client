package com.dlmv.localplayer.client.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.w3c.dom.Element;

public class AbsFile {

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

	AbsFile(String path, boolean readable, String size, MediaType mt) {
		if (path.endsWith("/") && !path.equals("smb://") && !path.equals("/")) {
			Path = path.substring(0, path.length() - 1);
		} else {
			Path = path;
		}
		Readable = readable;
		Size  = size;
		Type = mt;
	}

	public String getName() {
		if (Path.equals("/") || Path.equals("smb://")) {
			return Path;
		}
		int divider = Path.lastIndexOf("/");
		return Path.substring(divider + 1);
	}
	
	public String getPath() {
		if (Type.equals(MediaType.DIR) || Type.equals(MediaType.UP)) {
			if (!Path.endsWith("/")) {
				return Path + "/";
			}
			return Path;
		} else {
			if (Path.endsWith("/")) {
				return Path.substring(0, Path.length() - 1);
			}
			return Path;
		}
	}

	public static String parent(String Path) {
		if (Path.equals("/") || Path.equals("smb://")) {
			return null;
		} else {
			String path = Path.substring(0, Path.length() - 1);
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