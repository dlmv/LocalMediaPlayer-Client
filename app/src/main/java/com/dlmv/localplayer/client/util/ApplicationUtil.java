package com.dlmv.localplayer.client.util;

import java.util.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.dlmv.localmediaplayer.client.R;
import com.dlmv.localplayer.client.main.BrowseActivity;
import com.dlmv.localplayer.client.db.BookmarksActivity;

public abstract class ApplicationUtil {

	public static class Location {

		public final String Path;
		public final String Request;
		
		public Location(String p, String r) {
			Path = p;
			Request = r;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((Path == null) ? 0 : Path.hashCode());
			result = prime * result
					+ ((Request == null) ? 0 : Request.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Location other = (Location) obj;
			if (Path == null) {
				if (other.Path != null)
					return false;
			} else if (!Path.equals(other.Path))
				return false;
			if (Request == null) {
				if (other.Request != null)
					return false;
			} else if (!Request.equals(other.Request))
				return false;
			return true;
		}
	}
	
	
	
	 public static class DataHolder {
		public Location lastLocation = new Location(AbsFile.ROOT, "");

		public Stack<Location> history = new Stack<>();
		public HashMap<Location, ArrayList<AbsFile>> cache = new HashMap<>();
		
		public String serverUri = null;

        public void setUri(String uri) {
            lastLocation = new Location(AbsFile.ROOT, "");
            history.clear();
            cache.clear();
            serverUri = uri;
        }
	}
	
	public static final DataHolder Data = new DataHolder();
	
	
	public static final String LOCATION_PATH = "PATH";
	public static final String LOCATION_REQUEST = "REQUEST";
	public static final String PREFS_NAME = "MpPrefs";
	public static final String LAST_URI = "lasturi";
	
	public static final int CONNECT_CODE = 0;
	public static final int BOOKMARKS_CODE = 1;
	public static final int BROWSE_CODE = 2;

	public static void browse(Activity c) {
		Intent i = new Intent();
		i.setClass(c, BrowseActivity.class);
		i.putExtra(LOCATION_PATH, Data.lastLocation.Path);
		i.putExtra(LOCATION_REQUEST, Data.lastLocation.Request);
		c.startActivityForResult(i, BROWSE_CODE);
	}
	
	public static void open(Activity c, String uri) {
		if (!uri.endsWith("/")) {
			uri += "/";
		}
		Intent i = new Intent();
		i.setClass(c, BrowseActivity.class);
		i.putExtra(LOCATION_PATH, uri);
		i.putExtra(LOCATION_REQUEST, "");
		c.startActivityForResult(i, BROWSE_CODE);
	}
	
	public static void showBookmarks(Activity c) {
		Intent i = new Intent(c, BookmarksActivity.class);
		c.startActivityForResult(i, BOOKMARKS_CODE);
	}
	
	public static String timeFormat(int s) {
		int hours = s / 3600;
		int minutes = s / 60 - hours * 60;
		int seconds = s - minutes * 60;
		String res = "";
		if (hours > 0) {
			res += (Integer.toString(hours) + ":");
		}
		String min = Integer.toString(minutes);
		if (min.length() == 1 && hours > 0) {
			min = "0" + min;
		}
		String sec = Integer.toString(seconds);
		if (sec.length() == 1) {
			sec = "0" + sec;
		}
		return res + min + ":" + sec;
	}

	public interface LoginRunnable {
		void run(String login, String password);
	}

	public static void showLoginDialog(Context c, final String share, final ApplicationUtil.LoginRunnable runnable) {
		View dialogView = View.inflate(c, R.layout.login_dialog, null);
		((TextView) dialogView.findViewById(R.id.loginText)).setText(c.getResources().getString(R.string.login));
		((TextView) dialogView.findViewById(R.id.passwordText)).setText(c.getResources().getString(R.string.password));
		TextView info = dialogView.findViewById(R.id.infoText);
		info.setText(c.getResources().getString(R.string.shareLoginRequired).replaceAll("%s", share));
		info.setVisibility(View.VISIBLE);
		final EditText inputL = dialogView.findViewById(R.id.login);
		final EditText inputP = dialogView.findViewById(R.id.password);
		inputL.setText("");
		inputP.setText("");
		final AlertDialog.Builder d = new AlertDialog.Builder(c)
				.setPositiveButton(c.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog1, int which) {
						final String login = inputL.getText().toString();
						final String password = inputP.getText().toString();
						dialog1.dismiss();
						runnable.run(login, password);
					}
				}).setNegativeButton(c.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog1, int which) {
						dialog1.dismiss();
					}
				});
		d.setView(dialogView);
		d.show();
	}
	
}
