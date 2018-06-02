package com.example.liyongjian.mycamear;

import android.app.Application;
import android.util.Log;

/**
 * Created by user on 18-6-2.
 */

public class LYJApplication extends Application{

    private static final String TAG = "LYJApplication";

    @Override
    public void onCreate() {
        Log.d(TAG,"LYJApplication");
        super.onCreate();
    }
}
