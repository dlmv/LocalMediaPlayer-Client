package com.dlmv.localplayer.client.db;

import java.util.List;

import com.dlmv.localplayer.client.util.RootApplication;
import com.dlmv.localplayer.client.db.ServersDB.ServerBookmark;

import android.content.Intent;
import android.view.*;
import android.widget.*;

public class ServerActivity extends DBListActivity<ServerBookmark> {
	
	@Override
	protected View setAdapterView(ServerBookmark b, View convertView, ViewGroup parent, LayoutInflater inflater) {
		if (convertView == null) {
			convertView = inflater.inflate(android.R.layout.simple_expandable_list_item_2, parent, false);
		}
		TextView title = convertView.findViewById(android.R.id.text1);
		TextView subtitle = convertView.findViewById(android.R.id.text2);
		title.setText(b.name);
		subtitle.setText(b.toString());
		return convertView;
	}

	@Override
	protected List<ServerBookmark> getList() {
		return ((RootApplication)getApplication()).ServersDB().getServers();
	}

	@Override
	protected void fillResultIntent(ServerBookmark b, Intent data) {
		data.putExtra("SERVER", b.server);
		data.putExtra("PORT", b.port);
	}

	@Override
	protected void delete(ServerBookmark b) {
		((RootApplication)getApplication()).ServersDB().deleteServer(b.id);
	}

	@Override
	protected boolean openable() {
		return true;
	}

}
