package haw_hamburg.de.egdremote.video;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Message;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import haw_hamburg.de.egdremote.utils.Settings;

public class VideoStreamDecoder extends Thread {

    private final int HANDLER_CONNECTING = 0;
    private final int HANDLER_CONNECTED = 1;
    private final int HANDLER_DISCONNECTED = 2;
    private final int HANDLER_FAILED_TO_CONNECT = 3;

    public interface VideoStreamDecoderCallbacks{
        void onVideoConnecting();
        void onVideoConnected();
        void onVideoDisconnected();
        void onVideoFailedToConnect();
    }

    VideoStreamDecoderCallbacks listener;

    private Handler messageHandler = new Handler(){

        public void handleMessage(Message msg) {

            switch (msg.what) {

                case HANDLER_CONNECTING:
                    if(listener != null)
                        listener.onVideoConnecting();
                    break;
                case HANDLER_CONNECTED:
                    if(listener != null)
                        listener.onVideoConnected();
                    break;
                case HANDLER_DISCONNECTED:
                    if(listener != null)
                        listener.onVideoDisconnected();
                    break;
                case HANDLER_FAILED_TO_CONNECT:
                    if(listener != null)
                        listener.onVideoFailedToConnect();
                    break;
            }
        }
    };

    private final static int BUFFER_SIZE = 16384;
    private final static int NAL_SIZE_INC = 4096;
    private final static int MAX_READ_ERRORS = 300;

    private MediaCodec decoder = null;
    private MediaFormat format;
    private boolean decoding = false;
    private Surface surface;
    private byte[] buffer = null;
    private ByteBuffer[] inputBuffers = null;
    private long presentationTime;
    private long presentationTimeInc = 1000000 / Settings.RPi_video_fps;
    private TcpIpReader reader = null;

    public VideoStreamDecoder(VideoStreamDecoderCallbacks listener){
        this.listener = listener;
    }

    public void setSurface(Surface surface){

        this.surface = surface;

        if (decoder != null){

            if (surface != null){

                boolean newDecoding = decoding;

                if (decoding)
                    setDecodingState(false);

                if (format != null){
                    try{
                        decoder.configure(format, surface, null, 0);
                    }catch (Exception ex) {}

                    if (!newDecoding)
                        newDecoding = true;
                }

                if (newDecoding){
                    setDecodingState(newDecoding);
                }

            }else if (decoding){
                setDecodingState(false);
            }
        }
    }

    private synchronized void setDecodingState(boolean newDecoding){
        try{
            if (newDecoding != decoding && decoder != null){
                if (newDecoding)
                    decoder.start();
                else
                    decoder.stop();
                decoding = newDecoding;
            }
        } catch (Exception ex) {}
    }

    @Override
    public void run(){
        messageHandler.obtainMessage(HANDLER_CONNECTING, 0, 0, null).sendToTarget();

        try {
            TimeUnit.SECONDS.sleep(3);
        }catch (InterruptedException e){ }

        byte[] nal = new byte[NAL_SIZE_INC];
        int nalLen = 0;
        int numZeroes = 0;
        int numReadErrors = 0;

        try{
            decoder = MediaCodec.createDecoderByType("video/avc");

            buffer = new byte[BUFFER_SIZE];
            reader = new TcpIpReader(Settings.RPi_IP, Settings.RPi_video_port);

            if (!reader.isConnected())
                throw new Exception();

            messageHandler.obtainMessage(HANDLER_CONNECTED, 0, 0, null).sendToTarget();

            while (!isInterrupted()) {
                int len = reader.read(buffer);
                if (isInterrupted()) break;

                if (len > 0){

                    numReadErrors = 0;

                    for (int i = 0; i < len && !isInterrupted(); i++){

                        if (nalLen == nal.length)
                            nal = Arrays.copyOf(nal, nal.length + NAL_SIZE_INC);

                        nal[nalLen++] = buffer[i];

                        if (buffer[i] == 0){
                            numZeroes++;
                        }else{
                            if (buffer[i] == 1 && numZeroes == 3){
                                if (nalLen > 4){
                                    int nalType = processNal(nal, nalLen - 4);
                                    if (isInterrupted()) break;
                                    if (nalType == -1){
                                        nal[0] = nal[1] = nal[2] = 0;
                                        nal[3] = 1;
                                    }
                                }
                                nalLen = 4;
                            }
                            numZeroes = 0;
                        }
                    }
                }else{
                    numReadErrors++;
                    if (numReadErrors >= MAX_READ_ERRORS){
                        messageHandler.obtainMessage(HANDLER_DISCONNECTED, 0, 0, null).sendToTarget();
                        break;
                    }
                }

                if (format != null && decoding){

                    if (isInterrupted()) break;

                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

                    int index;
                    do{

                        index = decoder.dequeueOutputBuffer(info, 0);

                        if (isInterrupted()) break;

                        if (index >= 0)
                            decoder.releaseOutputBuffer(index, true);

                    } while (index >= 0);
                }
            }
        }catch (Exception ex){

            if (reader == null || !reader.isConnected())
                messageHandler.obtainMessage(HANDLER_FAILED_TO_CONNECT, 0, 0, null).sendToTarget();
            else
                messageHandler.obtainMessage(HANDLER_DISCONNECTED, 0, 0, null).sendToTarget();
            ex.printStackTrace();
        }

        if (reader != null){
            try{
                reader.close();
            }catch (Exception ex) {}
            reader = null;
        }

        if (decoder != null){
            try{
                setDecodingState(false);
                decoder.release();
            }catch (Exception ex) {}
            decoder = null;
        }

        messageHandler.obtainMessage(HANDLER_DISCONNECTED, 0, 0, null).sendToTarget();
    }

    private int processNal(byte[] nal, int nalLen){

        // get the NAL type
        int nalType = (nalLen > 4 && nal[0] == 0 && nal[1] == 0 && nal[2] == 0 && nal[3] == 1) ? (nal[4] & 0x1F) : -1;

        // process the first SPS record we encounter
        if (nalType == 7 && !decoding){

            format = MediaFormat.createVideoFormat("video/avc", Settings.RPi_video_W, Settings.RPi_video_H);
            presentationTimeInc = 1000000 / Settings.RPi_video_fps;
            presentationTime = System.nanoTime() / 1000;
            decoder.configure(format, surface, null, 0);
            setDecodingState(true);
            inputBuffers = decoder.getInputBuffers();
        }

        // queue the frame
        if (nalType > 0 && decoding){

            int index = decoder.dequeueInputBuffer(0);

            if (index >= 0){
                ByteBuffer inputBuffer = inputBuffers[index];
                inputBuffer.put(nal, 0, nalLen);
                decoder.queueInputBuffer(index, 0, nalLen, presentationTime, 0);
                presentationTime += presentationTimeInc;
            }
        }
        return nalType;
    }
}
