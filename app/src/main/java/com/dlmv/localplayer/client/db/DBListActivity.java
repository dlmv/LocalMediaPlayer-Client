package com.dlmv.localplayer.client.db;

import java.util.ArrayList;
import java.util.List;

import com.dlmv.localmediaplayer.client.R;

import android.os.Bundle;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.*;

public abstract class DBListActivity<T> extends ListActivity {

	private Adapter myAdapter;
	private List<T> myObjects = new ArrayList<T>();
	
	protected abstract View setAdapterView(T b, View convertView, ViewGroup parent, LayoutInflater inflater);
	protected abstract List<T> getList();
	protected abstract void fillResultIntent(T b, Intent data);
	protected abstract void delete(T b);
	protected abstract boolean openable();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bookmark);
		myObjects = getList();
		myAdapter = new Adapter(this, myObjects);
		getListView().setAdapter(myAdapter);
		if (openable()) {
			getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> p, View v, int position, long id) {
					T b = (T) p.getItemAtPosition(position);
					Intent data = new Intent();
					fillResultIntent(b, data);
					setResult(RESULT_OK, data);
					finish();
				}
			});
		}
		registerForContextMenu(getListView());
		((TextView)findViewById(android.R.id.empty)).setText(getResources().getString(R.string.empty));
	}
	
	private class Adapter extends ArrayAdapter<T>{

		Adapter(Context context, List<T> objects) {
			super(context, android.R.layout.simple_expandable_list_item_2, objects);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			T b = getItem(position);
			return setAdapterView(b, convertView, parent, inflater);
		}

	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (openable()) {
			menu.add(Menu.NONE, 1, Menu.NONE, getResources().getString(R.string.open));
		}
		menu.add(Menu.NONE, 2, Menu.NONE, getResources().getString(R.string.remove));
	}
	
	
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		int pos = info.position;
		T b = myAdapter.getItem(pos);
		if (item.getItemId() == 1) {
			Intent data = new Intent();
			fillResultIntent(b, data);
			setResult(RESULT_OK, data);
			finish();
		}
		if (item.getItemId() == 2) {
			delete(b);
			myObjects.remove(b);
			myAdapter.notifyDataSetChanged();
		}
		return true;
	}

}
