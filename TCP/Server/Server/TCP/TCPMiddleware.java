package Server.TCP;

import Server.Common.*;
import Server.Middleware.*;
import java.util.*;
import java.io.*;
import java.net.*;

public class TCPMiddleware extends Middleware{

    private static TCPMiddleware middleware = null;

    private static int s_serverPort = 6666;
    private ServerSocket serverSocket;

    private static String flightIP = "localhost";
    private static int flightPort = 6667;

    private static String carIP = "localhost";
    private static int carPort = 6668;

    private static String roomIP = "localhost";
    private static int roomPort = 6669;

    public static void main(String[] args) {
        try {

            if (args.length > 2) {
                String[] flightInfo = args[0].split(",");
                String[] carInfo = args[1].split(",");
                String[] roomInfo = args[2].split(",");

                flightIP = flightInfo[0];
                flightPort = Integer.parseInt(flightInfo[1]);

                carIP = carInfo[0];
                carPort = Integer.parseInt(carInfo[1]);

                roomIP = roomInfo[0];
                roomPort = Integer.parseInt(roomInfo[1]);
            }

            middleware = new TCPMiddleware("Middleware",flightIP,flightPort,carIP,carPort,roomIP,roomPort);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    middleware.stop();
                }
            });
            System.out.println("Starting 'Middleware:" + s_serverPort + "'");
            middleware.start(s_serverPort);
        } catch(Exception e) {
            System.err.println((char)27 + "[31;1mMiddleware exception: " + (char)27 + e.toString());
            System.exit(1);
        }
    }

    public TCPMiddleware(String p_name, String flightIP, int flightPort, String carIP, int carPort, String roomIP, int roomPort)
    {
        super(p_name,flightIP,flightPort,carIP,carPort,roomIP,roomPort);
    }

    private void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Listening on port: " + port);
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
            this.close();
            serverSocket.close();
            System.out.println("'Middleware:" + s_serverPort + "' Server Socket closed");
        }
        catch(IOException e) {
            System.err.println((char)27 + "[31;1mMiddleware exception: " + (char)27 + e.toString());
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

                Vector<String> parsedCommand = Parser.parse(inputLine);

                if (parsedCommand == null) {
                    out.println("");
                    in.close();
                    out.close();
                    clientSocket.close();
                    return;
                }

                String result = middleware.execute(parsedCommand);

                out.println(result);
                in.close();
                out.close();
                clientSocket.close();
            } catch(IOException e) {
                System.err.println((char)27 + "[31;1mMiddleware exception: " + (char)27 + "[0m" + e.getLocalizedMessage());
            }
        }
    }

}