package com.moyinoluwa.hellolagos;

import android.app.Application;

import com.firebase.client.Firebase;

/**
 * Created by moyinoluwa on 10/26/15.
 */
public class ChatApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Firebase.setAndroidContext(this);
    }
}
