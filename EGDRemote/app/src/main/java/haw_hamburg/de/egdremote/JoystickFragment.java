package haw_hamburg.de.egdremote;

import android.os.Bundle;
import android.app.Fragment;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class JoystickFragment extends Fragment {

    private static JoystickFragment instance;
    public static JoystickFragment getInstance(){
        if(instance == null)
            instance = new JoystickFragment();
        return instance;
    }

    private View v, joystick, joystickPanel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_joystick, container, false);
        setupGUI();
        return v;
    }

    private void setupGUI(){

        joystick = v.findViewById(R.id.joystick);
        joystickPanel = v.findViewById(R.id.joystick_panel);

        joystickPanel.setOnTouchListener(new View.OnTouchListener() {

            private float maxMove, downX, downY, moveX, moveY, joystickW, joystickH;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        if(maxMove == 0){
                            maxMove = Math.min(joystickPanel.getWidth(), joystickPanel.getHeight()) / 4;
                            joystickW = joystick.getWidth();
                            joystickH = joystick.getHeight();
                        }
                        downX = event.getX();
                        downY = event.getY();

                        joystick.setX(downX - joystickW / 2);
                        joystick.setY(downY - joystickH / 2);
                        joystick.setVisibility(View.VISIBLE);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        moveX = Math.min(Math.max(event.getX() - downX, -maxMove), maxMove);
                        DataTransmitter.joystick_X = (byte)(Math.min(Math.max(moveX / maxMove * 65, -64), 63) + 64);

                        moveY = Math.min(Math.max(downY - event.getY(), -maxMove), maxMove);
                        DataTransmitter.joystick_Y = (byte)(Math.min(Math.max(moveY / maxMove * 65, -64), 63) + 64);

                        joystick.setX(downX + moveX - joystickW / 2);
                        joystick.setY(downY - moveY - joystickH / 2);
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        DataTransmitter.getInstance().resetJoystick();
                        joystick.setVisibility(View.GONE);
                        break;
                    case MotionEvent.ACTION_UP:
                        DataTransmitter.getInstance().resetJoystick();
                        joystick.setVisibility(View.GONE);
                        break;
                    default:
                        break;
                }
                return true;
            }
        });

    }
}

