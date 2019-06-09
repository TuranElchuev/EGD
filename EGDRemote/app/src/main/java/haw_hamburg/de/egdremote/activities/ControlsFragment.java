package haw_hamburg.de.egdremote.activities;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import haw_hamburg.de.egdremote.bluetooth.DataTransmitter;
import haw_hamburg.de.egdremote.R;

/*
Author: Turan Elchuev, turan.elchuev@haw-hamburg.de, 02/2019

This fragment handles controls which send commands via Bluetooth,
e.g. command Buttons: "GO", "STOP", "LED", etc.

Additional controls (Buttons or other controls) can be added following steps:

1. Add a control into the view (file res/layout/fragment_controls.xml), e.g. a Button, and assing it an ID
2. Attach click listener (in case of Button), or any other listener if needed
    in a similar way as it is done inside the setupGUI() method of this class.
3. Handle click (in case of Button) in the onClick() method of this class in a similar way.

When the button is clicked, the corresponding command should be assigned to the
'DataTransmitter.command' such that the command is sent via Bluetooth within the
next transmitted frame. Example is provided in the onClick() method of this class.
 */

public class ControlsFragment extends Fragment implements View.OnClickListener {

    private static ControlsFragment instance;

    public static ControlsFragment getInstance() {
        if (instance == null)
            instance = new ControlsFragment();
        return instance;
    }

    private View v;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_controls, container, false);
        setupGUI();
        return v;
    }

    // Set handlers, e.g. OnClickListener in this method
    private void setupGUI() {
        v.findViewById(R.id.btn_go).setOnClickListener(this);
        v.findViewById(R.id.btn_stop).setOnClickListener(this);
        v.findViewById(R.id.btn_respond).setOnClickListener(this);
        v.findViewById(R.id.btn_led).setOnClickListener(this);
    }

    // Handle buttons clicks
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_go:
                DataTransmitter.command = DataTransmitter.cmd_go;
                break;
            case R.id.btn_stop:
                DataTransmitter.command = DataTransmitter.cmd_stop;
                break;
            case R.id.btn_respond:
                DataTransmitter.command = DataTransmitter.cmd_respond;
                break;
            case R.id.btn_led:
                DataTransmitter.command = DataTransmitter.cmd_led;
                break;
        }
    }
}
