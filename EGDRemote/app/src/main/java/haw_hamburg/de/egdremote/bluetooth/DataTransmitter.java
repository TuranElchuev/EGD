package haw_hamburg.de.egdremote.bluetooth;

import android.os.AsyncTask;

import java.util.concurrent.TimeUnit;

/*
Author: Turan Elchuev, turan.elchuev@haw-hamburg.de, 02/2019

This class handles data transmission via Bluetooth.

The concept is as follows:

There is a thread that sends Frames to RPi with time interval defined by 'transmitIntervalMillis'.

Frame format is as follows:

byte 1: S - start of sequence
byte 2: joystick_X - joystick X value
byte 3: joystick_Y - joystick Y value
byte 4: command - some command, e.g. buttons or halt/reboot RPi
byte 5: E - end of sequence

in order to transmit a new value of joystick or command,
required values (e.g. command 'GO') have to be assigned to corresponding fields (e.g. field 'command'):

DataTransmitter.command = DataTransmitter.cmd_go;
DataTransmitter.joystick_X = 0x64;

The last assigned values will be transmitted within the next transmission.
The value of the 'command' field will be reset after each transmission in order
to avoid continuous transmission of the same command. This does not hold for
joystick values - they have to be reset explicitely (e.g. by setting them
0x64 - the value of neutral position of the joystick axis).

Transmission rate can be changed by 'transmitIntervalMillis'.
Frame format can be changed in any manner, however it has to be compliant with the
receiver side.

Transmission can start/stop using corresponding methods.
A necessary condition for transmission is established Bluetooth connection.

*/

public class DataTransmitter {

    // Commands to send to the RPi (must match those expected at the RPi side)
    // New commands can be added up tp 0x7F ideally excluding values defined as 'S' and 'E'
    public static final byte cmd_halt = (byte) 0x01;
    public static final byte cmd_reboot = (byte) 0x02;
    public static final byte cmd_go = (byte) 0x03;
    public static final byte cmd_stop = (byte) 0x04;
    public static final byte cmd_respond = (byte) 0x05;
    public static final byte cmd_led = (byte) 0x06;
    public static final byte cmd_video_on = (byte) 0x07;
    public static final byte cmd_video_off = (byte) 0x08;

    private Task task;

    private final long transmitIntervalMillis = 50;

    private final byte S = (byte) 0x33; // Start of the frame
    public static byte joystick_X; // allowed value range [0x0 : 0x7F]
    public static byte joystick_Y; // allowed value range [0x0 : 0x7F]
    public static byte command;
    private final byte E = (byte) 0x77; // End of the frame

    private static DataTransmitter dataTransmitter;

    public static DataTransmitter getInstance() {
        if (dataTransmitter == null) {
            dataTransmitter = new DataTransmitter();
        }
        return dataTransmitter;
    }

    public DataTransmitter() {
        resetValues();
    }

    // reset all values to default
    public void resetValues() {
        resetJoystick();
        command = 0;
    }

    // reset joystick values to neutral, e.g. when joystick is released
    public void resetJoystick() {
        joystick_X = 64;
        joystick_Y = 64;
    }

    // start transmission
    public void start() {
        stop();
        resetValues();
        task = new Task();
        task.execute();
    }

    // stop transmission
    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel(true);
        }
        task = null;
    }

    // actual transmission
    public void transmitData() {

        // bytes in the Frame format
        byte[] data = new byte[]{
                S,
                joystick_X,
                joystick_Y,
                command,
                E
        };

        // send bytes
        BluetoothCommunicationHandler.getInstance().sendBytes(data);

        // reset command in order to avoid repeated transmission of the same command
        if (command != 0)
            command = 0;
    }

    // The thread that transmits data every "transmitIntervalMillis" period of time
    private class Task extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                while (true) {
                    TimeUnit.MILLISECONDS.sleep(transmitIntervalMillis);
                    publishProgress();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);
            transmitData();
        }
    }

    ;
}
