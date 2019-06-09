package haw_hamburg.de.egdremote.activities;

import android.app.FragmentManager;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;

import haw_hamburg.de.egdremote.R;

/*
Author: Turan Elchuev, turan.elchuev@haw-hamburg.de, 02/2019

This class is the entry point of the App.
When the App is opened, Main Activity pops up. and displays its content.
 */

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "EGD";

    private final String FRAGMENT_CONTROLS_LEFT_TAG = "1";
    private final String FRAGMENT_CONTROLS_RIGHT_TAG = "2";
    private final String FRAGMENT_SERVICE_TAG = "3";

    public static TextureView video;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        video = (TextureView) findViewById(R.id.video);

        // Add fragment into the corresponding containers on the screen (can be rearranged if needed)
        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getFragmentManager();

            // Add the controls fragment into the "frame_control" container (the one at the left-hand side of the screen)
            fragmentManager.beginTransaction().add(R.id.frame_controls, ControlsFragment.getInstance(), FRAGMENT_CONTROLS_LEFT_TAG).commit();

            // Add the joystick fragment into the "frame_joystick" container (the one at the right-hand side of the screen)
            fragmentManager.beginTransaction().add(R.id.frame_joystick, JoystickFragment.getInstance(), FRAGMENT_CONTROLS_RIGHT_TAG).commit();

            // Add the service fragment into the "frame_service" container (the one in the middle of the screen)
            fragmentManager.beginTransaction().add(R.id.frame_service, ServiceFragment.getInstance(), FRAGMENT_SERVICE_TAG).commit();
        }

        video.setSurfaceTextureListener(ServiceFragment.getInstance());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
