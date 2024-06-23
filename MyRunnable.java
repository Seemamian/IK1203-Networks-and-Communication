import java.io.*;
import java.net.*;
import tcpclient.TCPClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyRunnable implements Runnable {

    private Socket socket;
    String endOfHeader = "\r\n\r\n";

    public MyRunnable(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        ByteArrayOutputStream BUFFER = new ByteArrayOutputStream();
        boolean headerComplete = false;
        int totalBytesRead;
        final int STATIC_BUFFER_SIZE = 4096;

        try (InputStream inputStream = socket.getInputStream()) {
            while (!headerComplete) {
                byte[] clientDataBuffer = new byte[STATIC_BUFFER_SIZE];
                totalBytesRead = inputStream.read(clientDataBuffer);

                if (totalBytesRead > 0) {
                    BUFFER.write(clientDataBuffer, 0, totalBytesRead);
                    if (BUFFER.toString().contains(endOfHeader)) {
                        headerComplete = true;
                    }
                } else {
                    break;
                }
            }

            String serverResponse = BUFFER.toString();

            if (!isGetRequestValid(serverResponse)) {
                sendBadRequestResponse(socket);
                throw new Exception("Bad Request");
            }

            String[] parsedRequest = parse(serverResponse);

            if (parsedRequest == null || parsedRequest.length == 0) {
                sendNotFoundResponse(socket);
                throw new Exception("Bad Request");
            }

            String hostname = "";
            int port = 0;
            boolean shutdown = false;
            Integer limit = null;
            Integer timeout = null;
            byte[] requestData = new byte[0];

            for (String item : parsedRequest) {
                String[] parts = item.split("=");
                if (parts.length < 2) {
                    sendBadRequestResponse(socket);
                    throw new Exception();
                }
                if (item.contains("timeout")) {
                    String timeoutValue = parts[1];
                    timeout = Integer.parseInt(timeoutValue);
                } else if (item.contains("shutdown")) {
                    shutdown = Boolean.parseBoolean(parts[1]);
                } else if (item.contains("limit")) {
                    String limitValue = parts[1];
                    limit = Integer.parseInt(limitValue);
                } else if (item.contains("hostname")) {
                    hostname = parts[1];
                } else if (item.contains("port")) {
                    String portParts = parts[1]; 
                    port = Integer.parseInt(portParts); 
                } else if (item.contains("string")) {
                    String stringValue = parts[1];
                    requestData = stringValue.getBytes();
                }
            }

            try (OutputStream outputStream = socket.getOutputStream()) {
                StringBuilder httpResponse = new StringBuilder("HTTP/1.1 200 OK\r\n\r\n");
                TCPClient tcpClient = new TCPClient(shutdown, timeout, limit);
                byte[] response = tcpClient.askServer(hostname, port, requestData);
                httpResponse.append(new String(response, StandardCharsets.UTF_8));
                outputStream.write(httpResponse.toString().getBytes());
            } catch (IOException e) {
                System.err.println("Error communicating with TCP server: " + e.getMessage());
                sendInternalServerErrorResponse(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
            closeSocket(socket);
        } catch (Exception e) {
            e.printStackTrace();
            closeSocket(socket);
        }
    }

    private boolean isGetRequestValid(String httpRequest) {
        String[] lines = httpRequest.split("\r\n");
        if (lines.length < 1) return false;
        String requestLine = lines[0];
        String[] parts = requestLine.split(" ");
        return parts.length >= 3 && parts[0].equals("GET") && parts[2].equals("HTTP/1.1");
    }

    private void sendBadRequestResponse(Socket clientSocket) {
        sendResponse(clientSocket, "HTTP/1.1 400 Bad Request\r\n\r\n");
    }

    private void sendNotFoundResponse(Socket clientSocket) {
        sendResponse(clientSocket, "HTTP/1.1 404 Not Found\r\n\r\n");
    }

    private void sendInternalServerErrorResponse(Socket clientSocket) {
        sendResponse(clientSocket, "HTTP/1.1 500 Internal Server Error\r\n\r\n");
    }

    private void sendResponse(Socket clientSocket, String response) {
        OutputStream outputStream = null;
        try {
            outputStream = clientSocket.getOutputStream();
            outputStream.write(response.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Error sending response: " + e.getMessage());
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close(); 
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            closeSocket(clientSocket);
        }
    }

    private void closeSocket(Socket clientSocket) {
        try {
            clientSocket.close(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String[] parse(String httpRequest) {
        String[] lines = httpRequest.split("\r\n");
        String requestLine = lines[0];
        String[] parts = requestLine.split(" ");

        if (parts.length < 3 || !parts[0].equals("GET") || !parts[2].equals("HTTP/1.1")) {
            return new String[0];
        }

        String url = parts[1];
        String[] urlParts = url.split("\\?");
        if (urlParts.length < 2 || !urlParts[0].equals("/ask")) {
            return new String[0];
        }

        String paramsString = urlParts[1];
        String[] params = paramsString.split("&");

        
        List<String> parsedParams = new ArrayList<>();
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length != 2) {
                continue;
            }
            parsedParams.add(param);
        }

        System.out.println("Parsed parameters: i parse");
        for (String param : parsedParams) {
            System.out.println(param);
        }

        String[] resultArray = parsedParams.toArray(new String[parsedParams.size()]);
        return resultArray;
    }
}
