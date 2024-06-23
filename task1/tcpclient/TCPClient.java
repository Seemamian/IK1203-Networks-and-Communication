package tcpclient;
import java.net.*;
import java.io.*;

public class TCPClient {
    
    public TCPClient() {
    }
    
    private int STATIC_BUFFER_SIZE = 4096;

    public byte[] askServer(String hostname, int port, byte[] toServerBytes) throws IOException {
        byte[] serverResponseBytes;
        byte[] datafromInputStream = new byte[STATIC_BUFFER_SIZE];
        int totalbytesRead;

        try (Socket theclientSocket = new Socket(hostname, port)) {
            OutputStream datatoServer = theclientSocket.getOutputStream();
            datatoServer.write(toServerBytes);

            InputStream datafromServer = theclientSocket.getInputStream();
            ByteArrayOutputStream BUFFER = new ByteArrayOutputStream();

            while ((totalbytesRead = datafromServer.read(datafromInputStream)) >= 0) {
                BUFFER.write(datafromInputStream, 0,totalbytesRead);
            }
            serverResponseBytes = BUFFER.toByteArray();
            theclientSocket.close();
        }

       

        return serverResponseBytes;
    }

    public byte[] askServer(String hostname, int port) throws IOException {
        return askServer(hostname, port, new byte[0]);
    }
}
