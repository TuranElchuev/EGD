package haw_hamburg.de.egdremote.video;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/*
Author: Turan Elchuev, turan.elchuev@haw-hamburg.de, 02/2019
Based on TcpIpReader.java by Shawn Baker https://github.com/ShawnBaker/RPiCameraViewer
*/

public class TcpIpReader {

    public final static int IO_TIMEOUT = 1000;

    private final static int CONNECT_TIMEOUT = 5000;

    private Socket socket = null;
    private InputStream inputStream = null;

    public TcpIpReader(String IP, int port) {
        try {
            socket = getConnection(IP, port, CONNECT_TIMEOUT);
            socket.setSoTimeout(IO_TIMEOUT);
            inputStream = socket.getInputStream();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static Socket getConnection(String ip_address, int port, int timeout) {

        Socket socket;

        try {
            socket = new Socket();
            InetSocketAddress socketAddress = new InetSocketAddress(ip_address, port);
            socket.connect(socketAddress, timeout);
        } catch (Exception ex) {
            ex.printStackTrace();
            socket = null;
        }
        return socket;
    }

    public int read(byte[] buffer) {
        try {
            return (inputStream != null) ? inputStream.read(buffer) : 0;
        } catch (IOException ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    public boolean isConnected() {
        return (socket != null) && socket.isConnected();
    }

    public void close() {

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            inputStream = null;
        }

        if (socket != null) {
            try {
                socket.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            socket = null;
        }
    }
}