package haw_hamburg.de.egdremote.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import haw_hamburg.de.egdremote.utils.IRxFrame;
import haw_hamburg.de.egdremote.utils.RxFrameString;

/*
Author: Turan Elchuev, turan.elchuev@haw-hamburg.de, 02/2019

This class handles Bluetooth communication.
Given a bluetooth device object, performs connections.
The class provides methods for sending bytes, receiving frames (IRxFrame) as well as
provides callbacks for connect/disconnect/failed connection and frame received events.
*/

public class BluetoothCommunicationHandler {

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private BluetoothSocket bluetoothSocket;

    private final int HANDLER_FRAME_RECEIVED = 0;
    private final int HANDLER_CONNECTED = 1;
    private final int HANDLER_DISCONNECTED = 2;
    private final int HANDLER_FAILED_TO_CONNECT = 3;

    public interface BTCallbacks {
        void onBluetoothFrameReceived(IRxFrame frame);

        void onBluetoothConnected();

        void onBluetoothDisconnected();

        void onBluetoothConnectionFailed(String message);
    }

    private static BluetoothCommunicationHandler instance;

    public static BluetoothCommunicationHandler getInstance() {
        if (instance == null) {
            instance = new BluetoothCommunicationHandler();
        }
        return instance;
    }

    private ArrayList<BTCallbacks> listeners = new ArrayList<BTCallbacks>();

    private Handler messageHandler = new Handler() {

        public void handleMessage(Message msg) {

            switch (msg.what) {

                case HANDLER_FRAME_RECEIVED:
                    for (BTCallbacks listener : listeners) {
                        if (listener != null) {
                            listener.onBluetoothFrameReceived((IRxFrame) msg.obj);
                        }
                    }
                    break;

                case HANDLER_CONNECTED:
                    onConnected();
                    break;

                case HANDLER_DISCONNECTED:
                    onDisconnected();
                    break;

                case HANDLER_FAILED_TO_CONNECT:
                    onConnectionFailed("");
                    break;

                default:
                    break;
            }
        }
    };

    // pass a listener to receive callbacks
    public void registerListener(BTCallbacks listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    // remove listener when no longer needed
    public void deregisterListener(BTCallbacks listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    // send plain byte array
    public void sendBytes(byte[] bytes) {
        if (!isConnected()) {
            return;
        }
        connectedThread.write(bytes);
    }

    public void connect(BluetoothDevice device) {

        if (isConnected()) {
            disconnect();
            return;
        }

        if (device == null) {
            onConnectionFailed("INVALID DEVICE");
            return;
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            onConnectionFailed("ENABLE BLUETOOTH");
            return;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();
    }

    public void disconnect() {
        stopThreads();
    }

    public boolean isConnected() {
        return (connectedThread != null
                && bluetoothSocket != null
                && bluetoothSocket.isConnected());
    }

    private void onConnected() {
        for (BTCallbacks listener : listeners) {
            if (listener != null) {
                listener.onBluetoothConnected();
            }
        }
    }

    private void onDisconnected() {
        for (BTCallbacks listener : listeners) {
            if (listener != null) {
                listener.onBluetoothDisconnected();
            }
        }
        stopThreads();
    }

    private void onConnectionFailed(String message) {
        for (BTCallbacks listener : listeners) {
            if (listener != null) {
                listener.onBluetoothConnectionFailed(message);
            }
        }
        stopThreads();
    }

    private void stopThreads() {
        if (connectThread != null) {
            try {
                connectThread.cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (connectedThread != null) {
            try {
                connectedThread.cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // A thread to handle the process of connecting. A separate thread needed since the
    // process of connecting is blocking GUI and can take some time
    private class ConnectThread extends Thread {

        public ConnectThread(BluetoothDevice device) {
            try {
                bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {

            bluetoothAdapter.cancelDiscovery();

            try {

                bluetoothSocket.connect();
                messageHandler.obtainMessage(HANDLER_CONNECTED, 0, -1, null).sendToTarget();

                connectedThread = new ConnectedThread();
                connectedThread.start();

            } catch (IOException connectException) {

                try {
                    bluetoothSocket.close();
                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }

                messageHandler.obtainMessage(HANDLER_FAILED_TO_CONNECT, 0, -1, null).sendToTarget();

                return;
            }
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // A thread to handle running connection (after connection was established).
    // Sends bytes and catches IRxFrame frames
    private class ConnectedThread<T extends IRxFrame> extends Thread {

        private InputStream inputStream;
        private OutputStream outputStream;

        public ConnectedThread() {
            try {
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
            }
        }

        public void run() {

            while (true) {
                try {
                    IRxFrame frame = new RxFrameString();
                    frame.catchNextFrame(inputStream);
                    messageHandler.obtainMessage(HANDLER_FRAME_RECEIVED, 0, 0, frame).sendToTarget();
                } catch (IOException e) {
                    messageHandler.obtainMessage(HANDLER_DISCONNECTED, 0, 0, null).sendToTarget();
                    e.printStackTrace();
                    break;
                }
            }
        }

        // send byte array
        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}