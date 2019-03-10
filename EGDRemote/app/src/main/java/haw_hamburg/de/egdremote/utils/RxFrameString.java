package haw_hamburg.de.egdremote.utils;

import java.io.IOException;
import java.io.InputStream;

public class RxFrameString implements IRxFrame<String> {

    private long timestamp;
    private String data;
    private char[] stopSequence = new char[]{'\r', '\n'};

    @Override
    public void catchNextFrame(InputStream inputStream) throws IOException{

        while (true){
            char ch;
            StringBuilder sb = new StringBuilder();

            while ((inputStream.available() == 0));
            while ((ch = (char)inputStream.read()) != stopSequence[0]){
                sb.append(ch);
            }
            for (int i = 1; i < stopSequence.length; i++){
                if((char)inputStream.read() != stopSequence[i])
                    continue;
            }
            data = sb.toString();
            timestamp = System.currentTimeMillis();
            break;
        }
    }

    @Override
    public String getData(){
        return data;
    }

    @Override
    public String toString(){
        return data;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }
}
