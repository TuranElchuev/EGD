package haw_hamburg.de.egdremote.utils;

import java.io.IOException;
import java.io.InputStream;

/*
Author: Turan Elchuev, turan.elchuev@haw-hamburg.de, 02/2019

An interface that is used as received frames in BluetoothCommunicationHandler
and correspondingly items of the FrameLogAdapter.

Generic type T can be used to create an Object of the given type from the data
received in the frame, e.g. a List of integers or bytes or any other object.
If no particular data type is required, simply String can be used.
*/

public interface IRxFrame<T> {

    // This method should catch Frames in the given inputStream according to
    // the
    void catchNextFrame(InputStream inputStream) throws IOException;

    // String representation of the data in frame
    // Will be used e.g. to display the frame in the Log
    String toString();

    // Object representation of the data in frame.
    // Can be simply a string and correspondingly same value as toString().
    T getData();

    // timestamp when Frame was received, used for Log
    long getTimestamp();
}
