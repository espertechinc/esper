package com.espertech.esperio.socket;

import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class SupportSocketUtil {

    public static void sendSingleObject(int port, Object object) throws UnknownHostException, IOException
    {
        Socket requestSocket = new Socket("localhost", port);
        ObjectOutputStream outStream = new ObjectOutputStream(requestSocket.getOutputStream());
        outStream.writeObject(object);
        outStream.flush();
        outStream.close();
        requestSocket.close();
    }
}