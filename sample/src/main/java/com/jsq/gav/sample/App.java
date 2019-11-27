package com.jsq.gav.sample;

import android.app.Application;

import com.jsq.gav.sample.crash.CustomActivityOnCrash;

/**
 * Created by jiang on 2019/5/9
 */

public class App extends Application {

    private static App INSTANCE;

    public static App get() {
        return INSTANCE;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;

        CustomActivityOnCrash.install(this, System.currentTimeMillis());
    }
}
