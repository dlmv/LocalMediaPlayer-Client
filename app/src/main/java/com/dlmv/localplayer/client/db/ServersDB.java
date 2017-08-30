package com.dlmv.localplayer.client.db;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.*;

public class ServersDB {

	public static class ServerBookmark {
		public String server;
		public int port;
		public String name;
		public int id;
		@Override
		public String toString() {
			return server + ":" + port + "/";
		}
	}

	private SQLiteDatabase myDb;

	public ServersDB(Context c) {
		ServerHelper serverHelper = new ServerHelper(c);
		myDb = serverHelper.getWritableDatabase();
	}

	private static String TABLE_NAME = "servers";
	private static String DATABASE_NAME = "Servers.db";

	private static final String INSERT = "insert into " + TABLE_NAME + " (name,server,port) values (?,?,?)";
	private static final String DELETE = "delete from " + TABLE_NAME + " where id = ?";

	public void saveServer(ServerBookmark b) {
		SQLiteStatement insertStmt = myDb.compileStatement(INSERT);
		insertStmt.bindString(1, b.name);
		insertStmt.bindString(2, b.server);
		insertStmt.bindLong(3, b.port);
		insertStmt.execute();
	}

	ArrayList<ServerBookmark> getServers() {
		ArrayList<ServerBookmark> res = new ArrayList<>();
		Cursor cursor = myDb.query(TABLE_NAME, new String[] { "id","name","server","port" }, null, null, null, null, "id asc");
		if (cursor.moveToFirst()) {
			do {
				ServerBookmark b = new ServerBookmark();
				b.id = cursor.getInt(0);
				b.name = cursor.getString(1);
				b.server = cursor.getString(2);
				b.port = cursor.getInt(3);
				res.add(b);
			} while (cursor.moveToNext());
		}
		cursor.close();
		return res;
	}
	
	public boolean exists(ServerBookmark b) {//TODO: use sqlite statement
		ArrayList<ServerBookmark> bookmarks = getServers();
		for (ServerBookmark c : bookmarks) {
			if (c.server.equals(b.server) && b.port == c.port) {
				return true;
			}
		}
		return false;
	}
	
	void deleteServer(int id) {
		SQLiteStatement deleteStmt = myDb.compileStatement(DELETE);
		deleteStmt.bindLong(1, id);
		deleteStmt.execute();
	}

	private static class ServerHelper extends SQLiteOpenHelper {

		ServerHelper(Context context) {
			super(context, DATABASE_NAME, null, 1);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_NAME + " (id INTEGER PRIMARY KEY, name TEXT NOT NULL, server TEXT NOT NULL, port INTEGER NOT NULL)");

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			onCreate(db);

		}

	}
}
