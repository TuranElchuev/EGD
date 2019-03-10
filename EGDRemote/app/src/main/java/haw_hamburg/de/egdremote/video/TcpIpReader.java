package haw_hamburg.de.egdremote.video;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpIpReader {
    public final static int IO_TIMEOUT = 1000;

    private final static int CONNECT_TIMEOUT = 5000;

    private Socket socket = null;
    private InputStream inputStream = null;

    public TcpIpReader(String IP, int port)
    {
        try
        {
            socket = getConnection(IP, port, CONNECT_TIMEOUT);
            socket.setSoTimeout(IO_TIMEOUT);
            inputStream = socket.getInputStream();
        }
        catch (Exception ex) {}
    }

    public int read(byte[] buffer) {
        try{
            return (inputStream != null) ? inputStream.read(buffer) : 0;
        }catch (IOException ex){
            return 0;
        }
    }

    public boolean isConnected() {
        return (socket != null) && socket.isConnected();
    }

    public void close() {
        if (inputStream != null){
            try{
                inputStream.close();
            }catch (Exception ex) {}
            inputStream = null;
        }
        if (socket != null){
            try{
                socket.close();
            }
            catch (Exception ex) {}
            socket = null;
        }
    }

    public static Socket getConnection(String baseAddress, int port, int timeout) {
        Socket socket;
        try{
            socket = new Socket();
            InetSocketAddress socketAddress = new InetSocketAddress(baseAddress, port);
            socket.connect(socketAddress, timeout);
        }catch (Exception ex){
            socket = null;
        }
        return socket;
    }
}