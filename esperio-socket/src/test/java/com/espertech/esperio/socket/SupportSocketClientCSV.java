package com.espertech.esperio.socket;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class SupportSocketClientCSV {

    private final Socket socket;
    private final BufferedWriter wr;

    public SupportSocketClientCSV(int port) throws IOException {
        socket = new Socket("localhost", port);
        wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public void send(String csv) throws IOException {
        wr.write(csv);
        wr.flush();
    }

    public void close() throws IOException {
        wr.close();
        socket.close();
    }
}