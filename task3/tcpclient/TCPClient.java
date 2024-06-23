package tcpclient;
import java.net.*;
import java.io.*;

public class TCPClient {
    
    boolean shutdown;
    Integer timeout;
    Integer limit;

    /* The constructor TCPclient */
    public TCPClient(boolean shutdown, Integer timeout, Integer limit) {
        this.shutdown = shutdown;
        this.timeout = timeout;
        this.limit = limit;
        
    }

    int STATIC_BUFFER_SIZE = 4096;

    public byte[] askServer(String hostname, int port, byte[] toServerBytes) throws IOException {
        int size = 0;
        int totalbytesRead = 0;
        byte[] serverResponseBytes;
        byte[] datafromInputStream = new byte[STATIC_BUFFER_SIZE];
        ByteArrayOutputStream BUFFER = new ByteArrayOutputStream();

        Socket theclientSocket = new Socket(hostname, port);
        OutputStream datatoServer = theclientSocket.getOutputStream();
        datatoServer.write(toServerBytes, 0, toServerBytes.length);

        if (shutdown) {
            theclientSocket.shutdownOutput();
        }
        theclientSocket.setSoTimeout((this.timeout != null) ? this.timeout : 0);

        InputStream datafromServer = theclientSocket.getInputStream();

        try {
            while ((totalbytesRead = datafromServer.read(datafromInputStream)) >= 0) {
                size += totalbytesRead;
                if (limit != null && size >= limit) {
                     {
                        BUFFER.write(datafromInputStream, 0, limit);
                    }
                    break;
                }
                BUFFER.write(datafromInputStream, 0, totalbytesRead);
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Socket timeout occurred: " + e);
            serverResponseBytes = BUFFER.toByteArray();
            theclientSocket.close();
            return serverResponseBytes; 
        }

        serverResponseBytes = BUFFER.toByteArray();
        theclientSocket.close();
        return serverResponseBytes;
    }
}