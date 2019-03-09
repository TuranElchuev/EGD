package haw_hamburg.de.egdremote;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class BluetoothHelper {

    private Activity activity;

    public BluetoothHelper(Activity activity){
        this.activity = activity;
    }

    public void pickBTDeviceAndConnect(){

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!btAdapter.isEnabled()) {
            Toast.makeText(activity,"ENABLE BLUETOOTH", Toast.LENGTH_SHORT).show();
            return;
        }

        if(BluetoothCommunicationHandler.getInstance().isConnected()){
            BluetoothCommunicationHandler.getInstance().disconnect();
            return;
        }

        final ArrayList<BluetoothDevice> pairedDevices = new ArrayList<>(btAdapter.getBondedDevices());

        final ArrayAdapter<BluetoothDevice> adapter = new ArrayAdapter<BluetoothDevice>(activity, 0, pairedDevices){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.bt_device_item, parent, false);
                }

                ((TextView)convertView.findViewById(R.id.name)).
                        setText(pairedDevices.get(position).getName());

                ((TextView)convertView.findViewById(R.id.mac)).
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

        ((Button)dialog.findViewById(R.id.btn_new)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
                addNewBTDevice();
            }
        });

        dialog.show();
    }

    private void addNewBTDevice(){
        activity.startActivityForResult(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS), ServiceFragment.REQUEST_CODE_BTSETTINGS);
    }
}
