package com.espertech.esperio.socket;

import java.net.Socket;
import java.net.UnknownHostException;
import java.net.ServerSocket;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;

public class SupportSocketClientCSV {

    private final Socket socket;
    private final BufferedWriter wr;

    public SupportSocketClientCSV(int port) throws IOException {
        socket = new Socket("localhost", port);
        wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public void send(String csv) throws IOException
    {
        wr.write(csv);
        wr.flush();
    }

    public void close() throws IOException {
        wr.close();
        socket.close();
    }
}