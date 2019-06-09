package haw_hamburg.de.egdremote.activities;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import haw_hamburg.de.egdremote.bluetooth.DataTransmitter;
import haw_hamburg.de.egdremote.R;

/*
Author: Turan Elchuev, turan.elchuev@haw-hamburg.de, 02/2019

This fragment handles X-Y joystick (further the term "joystick" is used to refer
to the "tip" of the joystick - the blue circle that pops up when the user touches
the joystick area).

Please check the setupGUI() method of this class for the details.
 */

public class JoystickFragment extends Fragment {

    private static JoystickFragment instance;

    public static JoystickFragment getInstance() {
        if (instance == null)
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

    private void setupGUI() {

        joystick = v.findViewById(R.id.joystick);
        joystickPanel = v.findViewById(R.id.joystick_panel);

        // Set touch listener to the joystick panel in order to handle touch events (e.g. finger down/move/up)
        joystickPanel.setOnTouchListener(new View.OnTouchListener() {

            private float maxDelta, downX, downY, deltaX, deltaY, joystickW, joystickH;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN: // occurres when joystick panel is touched (finger down)

                        // check if maxDelta is already initialized in order to avoid unnecessary
                        // repeated calculations every time the joystick panel is touched
                        if (maxDelta == 0) {

                            // max distance that the joystick can move from the "down" point
                            maxDelta = Math.min(joystickPanel.getWidth(), joystickPanel.getHeight()) / 3;

                            // store joystick dimensions
                            joystickW = joystick.getWidth();
                            joystickH = joystick.getHeight();
                        }

                        // store the coordinates of the finger "down" event
                        downX = event.getX();
                        downY = event.getY();

                        // allign the center of the joystick with the point of touching
                        joystick.setX(downX - joystickW / 2);
                        joystick.setY(downY - joystickH / 2);

                        // joystick pop up
                        joystick.setVisibility(View.VISIBLE);
                        break;

                    case MotionEvent.ACTION_MOVE: // occurres after finger down during finger move before finger up

                        // bound deltaX by [downX - maxDelta, downX + maxDelta]
                        deltaX = Math.min(Math.max(event.getX() - downX, -maxDelta), maxDelta);

                        // convert deltaX to corresponding command in the range [0:127] and assign it to the DataTransmitter.joystick_X
                        // in order to send it during the next data transmission
                        DataTransmitter.joystick_X = (byte) (Math.min(Math.max(deltaX / maxDelta * 64.1f, -64), 63) + 64);

                        // similar calculations for the Y axis
                        deltaY = Math.min(Math.max(downY - event.getY(), -maxDelta), maxDelta);
                        DataTransmitter.joystick_Y = (byte) (Math.min(Math.max(deltaY / maxDelta * 64.1f, -64), 63) + 64);

                        // Update location of the joystick
                        joystick.setX(downX + deltaX - joystickW / 2);
                        joystick.setY(downY - deltaY - joystickH / 2);
                        break;

                    case MotionEvent.ACTION_CANCEL: // something went wrong
                    case MotionEvent.ACTION_UP: // finger up

                        // reset values of the joystick to neutral in order to send corresponding commands via Bluetooth
                        DataTransmitter.getInstance().resetJoystick();

                        joystick.setVisibility(View.GONE); // hide joystick
                        break;

                    default:
                        break;
                }
                return true;
            }
        });

    }
}

