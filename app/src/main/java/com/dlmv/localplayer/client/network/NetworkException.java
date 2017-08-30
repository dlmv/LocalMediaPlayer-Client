package com.dlmv.localplayer.client.network;

import android.content.Context;

import com.dlmv.localmediaplayer.client.R;

public class NetworkException extends Exception {

    private boolean myUnauthorized = false;

    public NetworkException(Throwable cause) {
        super(cause);
        if (cause instanceof NetworkException) {
            myUnauthorized = ((NetworkException) cause).myUnauthorized;
        }
    }

    NetworkException() {
        super();
        myUnauthorized = true;
    }

    public String getLocalizedMessage(Context c) {
        if (isUnauthorized()) {
            return c.getResources().getString(R.string.unauthorized);
        }
        return super.getLocalizedMessage();
    }

    public boolean isUnauthorized() {
        return myUnauthorized;
    }
}
