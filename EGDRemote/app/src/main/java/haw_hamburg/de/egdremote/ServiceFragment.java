package haw_hamburg.de.egdremote;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

public class ServiceFragment extends Fragment implements View.OnClickListener, BluetoothCommunicationHandler.BTCallbacks {

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

    private View v;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        bluetoothHelper = new BluetoothHelper(getActivity());
        BluetoothCommunicationHandler.getInstance().registerListener(this);
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
        BluetoothCommunicationHandler.getInstance().deregisterListener(this);
        BluetoothCommunicationHandler.getInstance().disconnect();
    }

    private void setupGUI(){
        v.findViewById(R.id.btn_connect).setOnClickListener(this);
        v.findViewById(R.id.btn_autoscroll).setOnClickListener(this);
        v.findViewById(R.id.btn_clear_log).setOnClickListener(this);
        v.findViewById(R.id.btn_share).setOnClickListener(this);
        v.findViewById(R.id.btn_video).setOnClickListener(this);
        v.findViewById(R.id.btn_power).setOnClickListener(this);
        v.findViewById(R.id.btn_log).setOnClickListener(this);

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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_connect:
                bluetoothHelper.pickBTDeviceAndConnect();
                break;
            case R.id.btn_autoscroll:
                if(logAdapter.toggleAutoScroll()){
                    ((ImageButton)v.findViewById(R.id.btn_autoscroll)).setBackgroundResource(R.drawable.selector_switch_on);
                }else {
                    ((ImageButton)v.findViewById(R.id.btn_autoscroll)).setBackgroundResource(R.drawable.selector_switch_off);
                }
                break;
            case R.id.btn_clear_log:
                logAdapter.clear();
                break;
            case R.id.btn_share:
                shareLog();
                break;
            case R.id.btn_video:
                toggleVideo();
                break;
            case R.id.btn_log:
                toggleLog();
                break;
            case R.id.btn_power:
                powerBtnPressed();
                break;
            default:
                break;
        }
    }

    private void toggleLog(){
        if(v.findViewById(R.id.log_list).getVisibility() == View.GONE){
            v.findViewById(R.id.log_list).setVisibility(View.VISIBLE);
            ((ImageButton)v.findViewById(R.id.btn_log)).setBackgroundResource(R.drawable.selector_switch_on);
        }
        else {
            v.findViewById(R.id.log_list).setVisibility(View.GONE);
            ((ImageButton)v.findViewById(R.id.btn_log)).setBackgroundResource(R.drawable.selector_switch_off);
        }
    }

    private void toggleVideo(){

        if(v.findViewById(R.id.video).getVisibility() == View.GONE){

            DataTransmitter.command = DataTransmitter.cmd_video_on;

            v.findViewById(R.id.video).setVisibility(View.VISIBLE);
            ((ImageButton)v.findViewById(R.id.btn_video)).setImageResource(R.drawable.ic_videocam_white_48dp);
            ((ImageButton)v.findViewById(R.id.btn_video)).setBackgroundResource(R.drawable.selector_switch_on);

            videoStreamDecoder = new VideoStreamDecoder();
            videoStreamDecoder.setSurface(surface);
            videoStreamDecoder.start();

        }else{

            DataTransmitter.command = DataTransmitter.cmd_video_off;

            if (videoStreamDecoder != null){
                videoStreamDecoder.interrupt();
                try{
                    videoStreamDecoder.join(TcpIpReader.IO_TIMEOUT * 2);
                }catch (Exception ex) {}
                videoStreamDecoder = null;
            }

            v.findViewById(R.id.video).setVisibility(View.GONE);
            ((ImageButton)v.findViewById(R.id.btn_video)).setImageResource(R.drawable.ic_videocam_off_white_48dp);
            ((ImageButton)v.findViewById(R.id.btn_video)).setBackgroundResource(R.drawable.selector_switch_off);
        }
    }

    private void powerBtnPressed(){

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Choose an action");

        String[] actions = {"HALT", "REBOOT", "CLOSE APP"};
        builder.setItems(actions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        DataTransmitter.command = DataTransmitter.cmd_halt;
                        break;
                    case 1:
                        DataTransmitter.command = DataTransmitter.cmd_reboot;
                        break;
                    case 2:
                        getActivity().finishAffinity();
                        System.exit(0);
                        break;
                }
            }
        });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if(getActivity() instanceof MainActivity){
                    ((MainActivity) getActivity()).hideSystemUI();
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_BTSETTINGS) {
            bluetoothHelper.pickBTDeviceAndConnect();
        }
    }

    @Override
    public void frameReceived(IRxFrame frame) {
        logAdapter.addFrame(frame);
    }

    @Override
    public void onConnected() {
        toast("CONNETCED");
        ((ImageButton)v.findViewById(R.id.btn_connect)).setImageResource(R.drawable.ic_bluetooth_connected_white_48dp);
        ((ImageButton)v.findViewById(R.id.btn_connect)).setBackgroundResource(R.drawable.selector_switch_on);
        bluetoothHelper.hideWaitingDialog();
        DataTransmitter.getInstance().start();
    }

    @Override
    public void onDisconnected() {
        toast("DISCONNETCED");
        ((ImageButton)v.findViewById(R.id.btn_connect)).setImageResource(R.drawable.ic_bluetooth_white_48dp);
        ((ImageButton)v.findViewById(R.id.btn_connect)).setBackgroundResource(R.drawable.selector_switch_off);
        DataTransmitter.getInstance().stop();
    }

    @Override
    public void onConnectionFailed(String message) {
        toast("FAILED TO CONNECT: " + message);
        bluetoothHelper.hideWaitingDialog();
    }

    private void toast(String text){
        Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
    }

    public void shareLog(){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, logAdapter.dataToString());
        getActivity().startActivityForResult(Intent.createChooser(intent, "Share via"), REQUEST_CODE_SHARE_LOG);
    }
}
