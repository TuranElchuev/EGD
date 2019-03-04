package haw_hamburg.de.egdremote;

import android.app.FragmentManager;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "EGD";

    private final String FRAGMENT_CONTROLS_LEFT_TAG = "1";
    private final String FRAGMENT_CONTROLS_RIGHT_TAG = "2";
    private final String FRAGMENT_SERVICE_TAG = "3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(savedInstanceState == null){
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().add(R.id.frame_controls, ControlsFragment.getInstance(), FRAGMENT_CONTROLS_LEFT_TAG).commit();
            fragmentManager.beginTransaction().add(R.id.frame_joystick, JoystickFragment.getInstance(), FRAGMENT_CONTROLS_RIGHT_TAG).commit();
            fragmentManager.beginTransaction().add(R.id.frame_service, ServiceFragment.getInstance(), FRAGMENT_SERVICE_TAG).commit();
        }

        hideSystemUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        hideSystemUI();
    }

    public void hideSystemUI(){
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
