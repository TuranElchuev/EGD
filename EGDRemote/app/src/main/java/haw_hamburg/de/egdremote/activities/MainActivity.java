package haw_hamburg.de.egdremote.activities;

import android.app.FragmentManager;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;

import haw_hamburg.de.egdremote.R;

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
        video = (TextureView)findViewById(R.id.video);

        if(savedInstanceState == null){
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().add(R.id.frame_controls, ControlsFragment.getInstance(), FRAGMENT_CONTROLS_LEFT_TAG).commit();
            fragmentManager.beginTransaction().add(R.id.frame_joystick, JoystickFragment.getInstance(), FRAGMENT_CONTROLS_RIGHT_TAG).commit();
            fragmentManager.beginTransaction().add(R.id.frame_service, ServiceFragment.getInstance(), FRAGMENT_SERVICE_TAG).commit();
        }

        video.setSurfaceTextureListener(ServiceFragment.getInstance());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
