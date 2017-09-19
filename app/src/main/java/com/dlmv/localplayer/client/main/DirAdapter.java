package com.dlmv.localplayer.client.main;

import java.util.List;

import android.content.Context;
import android.view.*;
import android.widget.*;

import com.dlmv.localmediaplayer.client.R;
import com.dlmv.localplayer.client.db.BookmarksDB;
import com.dlmv.localplayer.client.db.BookmarksDB.Bookmark;
import com.dlmv.localplayer.client.util.AbsFile;
import com.dlmv.localplayer.client.util.RootApplication;

class DirAdapter extends ArrayAdapter<AbsFile> {

	DirAdapter(Context context, List<AbsFile> objects) {
		super(context, R.layout.fileitem, objects);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.fileitem, parent, false);
		}

		TextView textView = convertView.findViewById(R.id.textView1);
		TextView sizeView = convertView.findViewById(R.id.size);
		ImageView imageView = convertView.findViewById(R.id.imageView1);
		AbsFile f = getItem(position);
		if (f == null) {
			textView.setText("");
			sizeView.setText("");
			return convertView;
		}
		textView.setText(f.getName(getContext()));
		sizeView.setText(f.Size);

		if (f.Type.equals(AbsFile.MediaType.DIR)) {
			if (f.Readable) {
				BookmarksDB db = ((RootApplication)getContext().getApplicationContext()).BookmarksDB();
				final Bookmark b = new Bookmark(f.Path, -1);
				if (db.exists(b)) {
					imageView.setImageResource(R.drawable.folder_starred);
				} else {
					imageView.setImageResource(R.drawable.folder);
				}
			} else {
				imageView.setImageResource(R.drawable.folder_closed);
			}
		} else if (f.Type.equals(AbsFile.MediaType.UP)) {
			imageView.setImageResource(R.drawable.up);
		} else {
			if (f.Readable) {
				if (f.Type.equals(AbsFile.MediaType.AUDIO)) {
					imageView.setImageResource(R.drawable.file_music);
				} else if (f.Type.equals(AbsFile.MediaType.IMAGE)) {
					imageView.setImageResource(R.drawable.file_image);
				} else {
					imageView.setImageResource(R.drawable.file);
				}
			} else {
				imageView.setImageResource(R.drawable.file_closed);
			} 
		}
		return convertView;
	}
}