package Server.TCP;

import Server.Common.*;
import java.util.*;
import java.io.*;
import java.net.*;

public class TCPMiddleware {

    private static int s_serverPort = 6666;
    private ServerSocket serverSocket;

    public static void main(String[] args) {
        TCPMiddleware middleware = new TCPMiddleware();
        middleware.start(s_serverPort);
    }

    private void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            while (true)
                new ClientHandler(serverSocket.accept()).start();
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            stop();
        }
    }

    public void stop() {
        try {
            serverSocket.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }


    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String inputLine = in.readLine();
                
                in.close();
                out.close();
                clientSocket.close();
            } catch(IOException e) {
                System.err.println((char)27 + "[31;1mMiddleware exception: " + (char)27 + "[0m" + e.getLocalizedMessage());
            }
        }
    }

}