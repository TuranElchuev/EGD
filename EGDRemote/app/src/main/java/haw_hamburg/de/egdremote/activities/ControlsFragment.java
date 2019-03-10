package haw_hamburg.de.egdremote.activities;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import haw_hamburg.de.egdremote.bluetooth.DataTransmitter;
import haw_hamburg.de.egdremote.R;

public class ControlsFragment extends Fragment implements View.OnClickListener{

    private static ControlsFragment instance;
    public static ControlsFragment getInstance(){
        if(instance == null)
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

    private void setupGUI(){
        v.findViewById(R.id.btn_go).setOnClickListener(this);
        v.findViewById(R.id.btn_stop).setOnClickListener(this);
        v.findViewById(R.id.btn_respond).setOnClickListener(this);
        v.findViewById(R.id.btn_led).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
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
