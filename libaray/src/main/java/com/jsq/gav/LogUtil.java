package com.jsq.gav;

import android.util.Log;

import java.util.Locale;

/**
 * Created by jiang on 2019/4/30
 */

public class LogUtil {

    private static final String TAG = "video";


    private static String getTag() {
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[4];
        String callerClazzName = stackTraceElement.getClassName();
        callerClazzName = callerClazzName.substring(callerClazzName.lastIndexOf(".") + 1);
        return String.format(Locale.getDefault(), "%s %s.%s(L:%d)", TAG, callerClazzName,
                stackTraceElement.getMethodName(), stackTraceElement.getLineNumber());
    }


    public static void e(String msg) {
        Log.e(getTag(), msg);
    }

    public static void w(String msg) {
        Log.w(getTag(), msg);
    }

    public static void v(String msg) {
        Log.v(getTag(), msg);
    }

    public static void d(String msg) {
        Log.d(getTag(), msg);
    }


    public static void i(String msg) {
        Log.i(getTag(), msg);
    }


}
