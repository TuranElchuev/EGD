package haw_hamburg.de.egdremote.activities;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import haw_hamburg.de.egdremote.bluetooth.BluetoothCommunicationHandler;
import haw_hamburg.de.egdremote.bluetooth.BluetoothHelper;
import haw_hamburg.de.egdremote.bluetooth.DataTransmitter;
import haw_hamburg.de.egdremote.utils.FrameLogAdapter;
import haw_hamburg.de.egdremote.utils.IRxFrame;
import haw_hamburg.de.egdremote.R;
import haw_hamburg.de.egdremote.utils.Settings;
import haw_hamburg.de.egdremote.video.TcpIpReader;
import haw_hamburg.de.egdremote.video.VideoStreamDecoder;
import haw_hamburg.de.egdremote.utils.WaitingDialog;

public class ServiceFragment extends Fragment implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener,
        BluetoothCommunicationHandler.BTCallbacks,
        VideoStreamDecoder.VideoStreamDecoderCallbacks {

    private static ServiceFragment instance;
    public static ServiceFragment getInstance(){
        if(instance == null)
            instance = new ServiceFragment();
        return instance;
    }

    public static final int REQUEST_CODE_BTSETTINGS = 9000;
    public static final int REQUEST_CODE_SHARE_LOG = 9001;

    private BluetoothHelper bluetoothHelper;
    public FrameLogAdapter logAdapter;
    private VideoStreamDecoder videoStreamDecoder;
    private Surface surface;

    private Dialog settingsActionsDialog = null;

    private View v;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        bluetoothHelper = new BluetoothHelper(getActivity());
        BluetoothCommunicationHandler.getInstance().registerListener(this);
        Settings.init(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_service, container, false);
        setupGUI();
        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setEnableVideo(false);
        DataTransmitter.getInstance().transmitData();
        BluetoothCommunicationHandler.getInstance().deregisterListener(this);
        BluetoothCommunicationHandler.getInstance().disconnect();
    }

    private void setupGUI(){
        v.findViewById(R.id.btn_settings).setOnClickListener(this);

        logAdapter = new FrameLogAdapter((ListView)(v.findViewById(R.id.log_list)), getActivity(), null);

        TextureView video = (TextureView)v.findViewById(R.id.video);
        video.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface_, int width, int height) {
                surface = new Surface(surface_);
                if(videoStreamDecoder != null)
                    videoStreamDecoder.setSurface(surface);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
        });
    }

    private void showSettingsAndActionsDialog(){
        settingsActionsDialog = new Dialog(getActivity());
        settingsActionsDialog.setContentView(R.layout.dialog_settings_actions);
        settingsActionsDialog.findViewById(R.id.btn_bt_connect).setOnClickListener(this);
        settingsActionsDialog.findViewById(R.id.btn_bt_disconnect).setOnClickListener(this);
        settingsActionsDialog.findViewById(R.id.btn_reboot).setOnClickListener(this);
        settingsActionsDialog.findViewById(R.id.btn_halt).setOnClickListener(this);
        settingsActionsDialog.findViewById(R.id.btn_clear_log).setOnClickListener(this);
        settingsActionsDialog.findViewById(R.id.btn_share_log).setOnClickListener(this);

        ((Switch)settingsActionsDialog.findViewById(R.id.sw_autoscroll)).setChecked(logAdapter.isAutoscroll());
        ((Switch)settingsActionsDialog.findViewById(R.id.sw_autoscroll)).setOnCheckedChangeListener(this);

        ((Switch)settingsActionsDialog.findViewById(R.id.sw_enable_log)).setChecked(v.findViewById(R.id.log_list).getVisibility() == View.VISIBLE);
        ((Switch)settingsActionsDialog.findViewById(R.id.sw_enable_log)).setOnCheckedChangeListener(this);

        ((Switch)settingsActionsDialog.findViewById(R.id.sw_enable_video)).setChecked(v.findViewById(R.id.video).getVisibility() == View.VISIBLE);
        ((Switch)settingsActionsDialog.findViewById(R.id.sw_enable_video)).setOnCheckedChangeListener(this);

        ((EditText)settingsActionsDialog.findViewById(R.id.et_ip)).setText(Settings.RPi_IP);
        ((EditText)settingsActionsDialog.findViewById(R.id.et_port)).setText(String.valueOf(Settings.RPi_video_port));
        ((EditText)settingsActionsDialog.findViewById(R.id.et_video_w)).setText(String.valueOf(Settings.RPi_video_W));
        ((EditText)settingsActionsDialog.findViewById(R.id.et_video_h)).setText(String.valueOf(Settings.RPi_video_H));
        ((EditText)settingsActionsDialog.findViewById(R.id.et_video_fps)).setText(String.valueOf(Settings.RPi_video_fps));

        settingsActionsDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Settings.RPi_IP = ((EditText)settingsActionsDialog.findViewById(R.id.et_ip)).getText().toString();
                Settings.RPi_video_port = Integer.valueOf(((EditText)settingsActionsDialog.findViewById(R.id.et_port)).getText().toString().trim());
                Settings.RPi_video_W = Integer.valueOf(((EditText)settingsActionsDialog.findViewById(R.id.et_video_w)).getText().toString());
                Settings.RPi_video_H = Integer.valueOf(((EditText)settingsActionsDialog.findViewById(R.id.et_video_h)).getText().toString());
                Settings.RPi_video_fps = Integer.valueOf(((EditText)settingsActionsDialog.findViewById(R.id.et_video_fps)).getText().toString());

                Settings.save(getActivity());
            }
        });

        settingsActionsDialog.show();
    }

    @Override
    public void onClick(View v) {

        if(settingsActionsDialog != null && settingsActionsDialog.isShowing())
            settingsActionsDialog.dismiss();

        switch (v.getId()){
            case R.id.btn_bt_connect:
                bluetoothHelper.pickBTDeviceAndConnect();
                break;
            case R.id.btn_bt_disconnect:
                BluetoothCommunicationHandler.getInstance().disconnect();
                break;
            case R.id.btn_reboot:
                DataTransmitter.command = DataTransmitter.cmd_reboot;
                break;
            case R.id.btn_halt:
                DataTransmitter.command = DataTransmitter.cmd_halt;
                break;
            case R.id.btn_clear_log:
                logAdapter.clear();
                break;
            case R.id.btn_share_log:
                shareLog();
                break;
            case R.id.btn_settings:
                showSettingsAndActionsDialog();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()){
            case R.id.sw_autoscroll:
                logAdapter.setAutoscroll(isChecked);
                break;
            case R.id.sw_enable_log:
                setEnableLog(isChecked);
                break;
            case R.id.sw_enable_video:
                if(settingsActionsDialog != null && settingsActionsDialog.isShowing())
                    settingsActionsDialog.dismiss();
                setEnableVideo(isChecked);
                break;
        }
    }

    private void setEnableLog(boolean enable){
        if(enable && v.findViewById(R.id.log_list).getVisibility() == View.GONE){
            v.findViewById(R.id.log_list).setVisibility(View.VISIBLE);
        }
        else if(!enable && v.findViewById(R.id.log_list).getVisibility() == View.VISIBLE){
            v.findViewById(R.id.log_list).setVisibility(View.GONE);
        }
    }

    private void setEnableVideo(boolean enable){

        if (videoStreamDecoder != null){
            videoStreamDecoder.interrupt();
            try{
                videoStreamDecoder.join(TcpIpReader.IO_TIMEOUT * 2);
            }catch (Exception ex) {}
            videoStreamDecoder = null;
        }

        if(enable){

            DataTransmitter.command = DataTransmitter.cmd_video_on;

            v.findViewById(R.id.video).setVisibility(View.VISIBLE);

            videoStreamDecoder = new VideoStreamDecoder(this);
            videoStreamDecoder.setSurface(surface);
            videoStreamDecoder.start();

        }else{
            DataTransmitter.command = DataTransmitter.cmd_video_off;
            v.findViewById(R.id.video).setVisibility(View.GONE);
        }
    }

    private void toast(String text){
        try {
            Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
        }catch (Exception e){}

    }

    private void shareLog(){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, logAdapter.dataToString());
        getActivity().startActivityForResult(Intent.createChooser(intent, "Share via"), REQUEST_CODE_SHARE_LOG);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_BTSETTINGS) {
            bluetoothHelper.pickBTDeviceAndConnect();
        }
    }

    @Override
    public void onBluetoothFrameReceived(IRxFrame frame) {
        logAdapter.addFrame(frame);
    }

    @Override
    public void onBluetoothConnected() {
        toast("CONNETCED");
        v.findViewById(R.id.indicator_bt_connected).setVisibility(View.VISIBLE);
        WaitingDialog.hide();
        DataTransmitter.getInstance().start();
    }

    @Override
    public void onBluetoothDisconnected() {
        toast("DISCONNETCED");
        v.findViewById(R.id.indicator_bt_connected).setVisibility(View.GONE);
        DataTransmitter.getInstance().stop();
    }

    @Override
    public void onBluetoothConnectionFailed(String message) {
        toast("FAILED TO CONNECT: " + message);
        WaitingDialog.hide();
    }

    @Override
    public void onVideoConnecting() {
        WaitingDialog.show(getActivity());
        toast("Attempting to connect to video source " + Settings.RPi_IP + ":" + Settings.RPi_video_port);
    }

    @Override
    public void onVideoConnected() {
        WaitingDialog.hide();
        toast("Connected to video source " + Settings.RPi_IP + ":" + Settings.RPi_video_port);
    }

    @Override
    public void onVideoFailedToConnect() {
        setEnableVideo(false);
        WaitingDialog.hide();
        toast("Failed to connect to video source " + Settings.RPi_IP + ":" + Settings.RPi_video_port);
    }

    @Override
    public void onVideoDisconnected() {
        setEnableVideo(false);
        WaitingDialog.hide();
        toast("Disconnected from video source " + Settings.RPi_IP + ":" + Settings.RPi_video_port);
    }
}
