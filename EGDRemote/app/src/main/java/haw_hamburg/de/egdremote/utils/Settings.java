package haw_hamburg.de.egdremote.utils;

import android.content.Context;
import android.content.SharedPreferences;

/*
Author: Turan Elchuev, turan.elchuev@haw-hamburg.de, 02/2019

This class stores settings as well as handles saving and retrieving them.
*/

public class Settings {

    // SETTINGS //////////////////////////////
    public static String RPi_IP = "0.0.0.0";
    public static int RPi_video_port = 8000;
    public static int RPi_video_fps = 25;
    public static int RPi_video_W = 1280;
    public static int RPi_video_H = 720;
    //////////////////////////////////////////

    private static final String PREF = "preferences";

    private static final String KEY_IP = "ip";
    private static final String KEY_PORT = "port";
    private static final String KEY_W = "w";
    private static final String KEY_H = "h";
    private static final String KEY_FPS = "fps";


    // Initializes settings from the settings file.
    // After initialization settings can be used and modified
    public static void init(Context context) {
        SharedPreferences sPref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (!sPref.contains(KEY_IP)) {
            save(context);
            return;
        }

        RPi_IP = sPref.getString(KEY_IP, "0.0.0.0");
        RPi_video_port = sPref.getInt(KEY_PORT, 8000);
        RPi_video_W = sPref.getInt(KEY_W, 1280);
        RPi_video_H = sPref.getInt(KEY_H, 720);
        RPi_video_fps = sPref.getInt(KEY_FPS, 25);
    }

    // Saves settings to the file
    public static void save(Context context) {
        SharedPreferences sPref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sPref.edit();
        editor.putString(KEY_IP, RPi_IP);
        editor.putInt(KEY_PORT, RPi_video_port);
        editor.putInt(KEY_W, RPi_video_W);
        editor.putInt(KEY_H, RPi_video_H);
        editor.putInt(KEY_FPS, RPi_video_fps);

        editor.commit();
    }

}
