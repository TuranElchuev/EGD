package haw_hamburg.de.egdremote.utils;

import java.io.IOException;
import java.io.InputStream;

public interface IRxFrame<T> {
    void catchNextFrame(InputStream inputStream) throws IOException;
    String toString();
    T getData();
    long getTimestamp();
}
