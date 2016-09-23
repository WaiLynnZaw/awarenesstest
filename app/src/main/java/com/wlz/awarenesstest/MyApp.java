package com.wlz.awarenesstest;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

/**
 * Created by WaiLynnZaw on 9/23/16.
 */

public class MyApp extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
