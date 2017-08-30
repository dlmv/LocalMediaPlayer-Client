package com.dlmv.localplayer.client.db;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.*;

public class BookmarksDB {

	public static class Bookmark {
		public final String Path;
		final int Id;
		
		public Bookmark(String p, int id) {
			if (p.endsWith("/")) {
				Path = p.substring(0, p.length() - 1);
			} else {
				Path = p;
			}
			Id = id;
		}
		
		public String getName() {
			int divider = Path.lastIndexOf("/");
			return Path.substring(divider + 1);
		}
		
		public String getPath() {
			if (!Path.endsWith("/")) {
				return Path + "/";
			}
			return Path;
		}
	}

	private SQLiteDatabase myDb;


	public BookmarksDB(Context c) {
		BookmarkHelper bookmarkHelper = new BookmarkHelper(c);
		myDb = bookmarkHelper.getWritableDatabase();
	}

	private static String TABLE_NAME = "bookmarks";
	private static String DATABASE_NAME = "Bookmarks.db";

	private static final String INSERT = "insert into " + TABLE_NAME + " (path) values (?)";
	private static final String DELETE = "delete from " + TABLE_NAME + " where id = ?";
	private static final String DELETEPATH = "delete from " + TABLE_NAME + " where path = ?";

	public void saveBookmark(Bookmark b) {
		if (exists(b)) {
			return;
		}
		SQLiteStatement insertStmt = myDb.compileStatement(INSERT);
		insertStmt.bindString(1, b.Path);
		insertStmt.execute();
	}

	ArrayList<Bookmark> getBookmarks() {
		ArrayList<Bookmark> res = new ArrayList<>();
		Cursor cursor = myDb.query(TABLE_NAME, new String[] { "id","path"}, null, null, null, null, "id asc");
		if (cursor.moveToFirst()) {
			do {
				int id = cursor.getInt(0);
				String path = cursor.getString(1);
				Bookmark b = new Bookmark(path, id);
				res.add(b);
			} while (cursor.moveToNext());
		}
		cursor.close();
		return res;
	}
	
	public boolean exists(Bookmark b) {//TODO: use sqlite statement
		ArrayList<Bookmark> bookmarks = getBookmarks();
		for (Bookmark c : bookmarks) {
			if (c.Path.equals(b.Path)) {
				return true;
			}
		}
		return false;
	}
	
	void deleteBookmark(int id) {
		SQLiteStatement deleteStmt = myDb.compileStatement(DELETE);
		deleteStmt.bindLong(1, id);
		deleteStmt.execute();
	}
	
	public void deleteBookmark(String path) {
		SQLiteStatement deleteStmt = myDb.compileStatement(DELETEPATH);
		deleteStmt.bindString(1, path);
		deleteStmt.execute();
	}

	private static class BookmarkHelper extends SQLiteOpenHelper {

		BookmarkHelper(Context context) {
			super(context, DATABASE_NAME, null, 1);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_NAME + " (id INTEGER PRIMARY KEY, path TEXT NOT NULL)");

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			onCreate(db);

		}

	}
}
