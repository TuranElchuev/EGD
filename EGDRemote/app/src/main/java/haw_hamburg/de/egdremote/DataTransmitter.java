package haw_hamburg.de.egdremote;

import android.os.AsyncTask;

import java.util.concurrent.TimeUnit;

public class DataTransmitter {

    public static final byte cmd_halt = (byte)0x01;
    public static final byte cmd_reboot = (byte)0x02;
    public static final byte cmd_go = (byte)0x03;
    public static final byte cmd_stop = (byte)0x04;
    public static final byte cmd_respond = (byte)0x05;
    public static final byte cmd_led = (byte)0x06;
    public static final byte cmd_video_on = (byte)0x07;
    public static final byte cmd_video_off = (byte)0x08;

    private Task task;

    private final long transmitIntervalMillis = 50;

    private final byte S = (byte)0x33;
    private final byte E = (byte)0x77;

    public static byte  joystick_X,
                        joystick_Y,
                        command;

    private static DataTransmitter dataTransmitter;
    public static DataTransmitter getInstance(){
        if(dataTransmitter == null){
            dataTransmitter = new DataTransmitter();
        }
        return dataTransmitter;
    }

    public DataTransmitter(){
        resetValues();
    }

    public void start(){
        stop();
        resetValues();
        task = new Task();
        task.execute();
    }

    public void resetValues(){
        resetJoystick();
        command = 0;
    }

    public void resetJoystick(){
        joystick_X = 64;
        joystick_Y = 64;
    }

    public void stop(){
        if(task != null && !task.isCancelled()){
            task.cancel(true);
        }
        task = null;
    }

    public void transmitData(){
        byte[] data = new byte[]{
                S,
                joystick_X,
                joystick_Y,
                command,
                E
        };

        BluetoothCommunicationHandler.getInstance().sendBytes(data);

        if(command != 0)
            command = 0;
    }

    private class Task extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                while (true){
                    TimeUnit.MILLISECONDS.sleep(transmitIntervalMillis);
                    publishProgress();
                }
            }catch (InterruptedException e){}
            return null;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);
            transmitData();
        }
    };
}
