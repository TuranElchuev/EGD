package haw_hamburg.de.egdremote.bluetooth;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import haw_hamburg.de.egdremote.R;
import haw_hamburg.de.egdremote.utils.WaitingDialog;
import haw_hamburg.de.egdremote.activities.ServiceFragment;

/*
Author: Turan Elchuev, turan.elchuev@haw-hamburg.de, 02/2019

This is a helper class to deal with BluetoothCommunicationHandler.class.

Provides some useful methods, e.g. pickBTDeviceAndConnect().

*/

public class BluetoothHelper {

    private Activity activity;

    public BluetoothHelper(Activity activity) {
        this.activity = activity;
    }

    /*
     This method pops up a dialog with a list of paired Bluetooth devices.
     Clicking on a device from the list will initiate connection with that particular device.
     If a particular device is not in the list, then it is not paired yet. In this case
     by clicking the button "Manage Devices" under the list, one can pair with new devices.

     After establishing connection with the device, the connection can be accessed using
     BluetoothCommunicationHandler.getInstance() e.g. to transmit bytes
     or to attach a listener in order to receive IRxFrame frames and connection callbacks.
     */
    public void pickBTDeviceAndConnect() {

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!btAdapter.isEnabled()) {
            Toast.makeText(activity, "ENABLE BLUETOOTH", Toast.LENGTH_SHORT).show();
            return;
        }

        if (BluetoothCommunicationHandler.getInstance().isConnected()) {
            BluetoothCommunicationHandler.getInstance().disconnect();
            return;
        }

        final ArrayList<BluetoothDevice> pairedDevices = new ArrayList<>(btAdapter.getBondedDevices());

        final ArrayAdapter<BluetoothDevice> adapter = new ArrayAdapter<BluetoothDevice>(activity, 0, pairedDevices) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.bt_device_item, parent, false);
                }

                ((TextView) convertView.findViewById(R.id.name)).
                        setText(pairedDevices.get(position).getName());

                ((TextView) convertView.findViewById(R.id.mac)).
                        setText(pairedDevices.get(position).getAddress());

                return convertView;
            }
        };

        final Dialog dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_bt_device_picker);

        ListView list = dialog.findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dialog.cancel();
                WaitingDialog.show(activity);
                BluetoothCommunicationHandler.getInstance().connect(pairedDevices.get(position));
            }
        });

        ((Button) dialog.findViewById(R.id.btn_new)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
                addNewBTDevice();
            }
        });

        dialog.show();
    }

    private void addNewBTDevice() {
        activity.startActivityForResult(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS), ServiceFragment.REQUEST_CODE_BTSETTINGS);
    }
}
