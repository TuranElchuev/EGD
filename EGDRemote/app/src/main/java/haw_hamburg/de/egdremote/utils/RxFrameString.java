package haw_hamburg.de.egdremote.utils;

import java.io.IOException;
import java.io.InputStream;

/*
Author: Turan Elchuev, turan.elchuev@haw-hamburg.de, 02/2019

An implementation of the IRxFrame interface for catching String.

Will distinguish strings using the value of the 'stopSequence':

bytes are collected until stop sequence is detected, everything up to
the stop sequence is appended to a string, stop sequence values will
not be appended.
*/

public class RxFrameString implements IRxFrame<String> {

    private long timestamp;
    private String data;

    /*
      This particular stop sequence is designed to match that of the
      Serial.println() method of Arduino. It makes the App Arduino-compatible,
      in other words, strings sent from Arduino via Bluetooth by calling Serial.println()
      will be successfully caught by this App.

      In case of transmission using the write() function from Arduino
      or RPi, "\r\n" should be sent at the end if a string is meant to be sent to this App.

    */
    private char[] stopSequence = new char[]{'\r', '\n'};

    @Override
    public void catchNextFrame(InputStream inputStream) throws IOException {

        char ch = 0;

        while (true) {
            boolean success = true;

            StringBuilder sb = new StringBuilder();
            if (ch != 0)
                sb.append(ch);

            while ((inputStream.available() == 0)) ; // wait until something arrives

            // append bytes to string until first char of stop sequence is reached
            while ((ch = (char) inputStream.read()) != stopSequence[0]) {
                sb.append(ch);
            }

            // check that stop sequence is fully received
            for (int i = 1; i < stopSequence.length; i++) {
                if ((ch = (char) inputStream.read()) != stopSequence[i]) {
                    success = false;
                    break;
                }
            }

            // If failed to fully receive stop sequence, start over
            if (!success)
                continue;

            ch = 0;
            data = sb.toString();
            timestamp = System.currentTimeMillis();
            break;
        }
    }

    @Override
    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return data;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }
}
