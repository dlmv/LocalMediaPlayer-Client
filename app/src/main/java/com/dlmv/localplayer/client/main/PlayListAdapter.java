package com.dlmv.localplayer.client.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.dlmv.localmediaplayer.client.R;
import com.dlmv.localplayer.PlayerStatus;

import java.util.List;

class PlayListAdapter extends ArrayAdapter<PlayerStatus.PlaylistItem> {

    private MainActivity myActivity;

    PlayListAdapter(MainActivity activity, List<PlayerStatus.PlaylistItem> objects) {
        super(activity, R.layout.playlistitem, objects);
        myActivity = activity;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.playlistitem, parent, false);
        }
        TextView textView = convertView.findViewById(R.id.textView1);
        ImageView imageView = convertView.findViewById(R.id.imageView1);
        PlayerStatus.PlaylistItem f = getItem(position);
        if (f == null) {
            textView.setText("");
            imageView.setImageResource(R.drawable.play_transparent);
            return convertView;
        }
            textView.setText(f.getName());
            if (myActivity.getStatus().myCurrentTrackNo == position && myActivity.getStatus().myState.equals(PlayerStatus.State.PLAYING)) {
                if (position == myActivity.getStatus().myStopAfter) {
                    if (myActivity.getStatus().myStopAfterType == PlayerStatus.PAUSE) {
                        imageView.setImageResource(R.drawable.play_pause_after);
                    } else {
                        imageView.setImageResource(R.drawable.play_stop_after);
                    }
                } else {
                    imageView.setImageResource(R.drawable.play_white);
                }
            } else if (myActivity.getStatus().myCurrentTrackNo == position && myActivity.getStatus().myState.equals(PlayerStatus.State.PAUSED)) {
                if (position == myActivity.getStatus().myStopAfter) {
                    if (myActivity.getStatus().myStopAfterType == PlayerStatus.PAUSE) {
                        imageView.setImageResource(R.drawable.pause_pause_after);
                    } else {
                        imageView.setImageResource(R.drawable.pause_stop_after);
                    }
                } else {
                    imageView.setImageResource(R.drawable.pause_white);
                }
            } else {
                if (position == myActivity.getStatus().myStopAfter) {
                    if (myActivity.getStatus().myStopAfterType == PlayerStatus.PAUSE) {
                        imageView.setImageResource(R.drawable.pause_after);
                    } else {
                        imageView.setImageResource(R.drawable.stop_after);
                    }
                } else {
                    imageView.setImageResource(R.drawable.play_transparent);
                }
            }

        return convertView;
    }
}