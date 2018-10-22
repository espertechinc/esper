package com.espertech.esperio.socket;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class SupportSocketClientObject {

    private final Socket requestSocket;
    private final ObjectOutputStream outStream;

    public SupportSocketClientObject(int port) throws UnknownHostException, IOException {
        requestSocket = new Socket("localhost", port);
        outStream = new ObjectOutputStream(requestSocket.getOutputStream());
    }

    public void send(Object object) throws IOException {
        outStream.writeObject(object);
        outStream.flush();
    }

    public void close() throws IOException {
        outStream.close();
        requestSocket.close();
    }
}
