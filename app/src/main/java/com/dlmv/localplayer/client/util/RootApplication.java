package com.dlmv.localplayer.client.util;

import android.app.Application;
import com.dlmv.localplayer.client.db.*;

public class RootApplication extends Application {
    private BookmarksDB Bookmarks;
    private ServersDB Servers;

    @Override
    public void onCreate() {
        super.onCreate();
        Bookmarks = new BookmarksDB(this.getApplicationContext());
        Servers = new ServersDB(this.getApplicationContext());
    }

    public BookmarksDB BookmarksDB() {
        return Bookmarks;
    }

    public ServersDB ServersDB() {
        return Servers;
    }

}
