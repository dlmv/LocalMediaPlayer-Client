package com.dlmv.localplayer.client.db;

import java.util.List;

import com.dlmv.localmediaplayer.client.R;
import com.dlmv.localplayer.client.util.RootApplication;
import com.dlmv.localplayer.client.db.BookmarksDB.Bookmark;

import android.content.Intent;
import android.view.*;
import android.widget.*;

public class BookmarksActivity extends DBListActivity<Bookmark> {
	
	@Override
	protected View setAdapterView(Bookmark b, View convertView, ViewGroup parent, LayoutInflater inflater) {
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.bookmarks_item, parent, false);
		}
		TextView title = convertView.findViewById(R.id.text1);
		TextView subtitle = convertView.findViewById(R.id.text2);
		title.setText(b.getName());
		subtitle.setText(b.Path);
		return convertView;
	}

	@Override
	protected List<Bookmark> getList() {
		return ((RootApplication)getApplication()).BookmarksDB().getBookmarks();
	}

	@Override
	protected void fillResultIntent(Bookmark b, Intent data) {
		data.putExtra("BOOKMARK", b.getPath());
	}

	@Override
	protected void delete(Bookmark b) {
		((RootApplication)getApplication()).BookmarksDB().deleteBookmark(b.Id);
	}

	@Override
	protected boolean openable() {
		return true;
	}

}
