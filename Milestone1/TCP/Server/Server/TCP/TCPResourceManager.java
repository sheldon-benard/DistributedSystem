// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.TCP;

import Server.Common.*;
import Server.Middleware.*;
import java.util.*;
import java.io.*;
import java.net.*;


public class TCPResourceManager extends ResourceManager
{
    private static TCPResourceManager manager = null;

    private ServerSocket serverSocket;

    private static int port = 6666;

    public static void main(String[] args) {
        String name = null;

        try {

            if (args.length == 1) {
                String[] info = args[0].split(",");

                name = info[0];
                port = Integer.parseInt(info[1]);
            }
            else {
                System.err.println((char)27 + "[31;1mResource Manager exception: " + (char)27 + "Must specify name,port");
                System.exit(1);
            }

            manager = new TCPResourceManager(name);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    manager.stop();
                }
            });
            System.out.println("Starting '" + name + ":" + port + "'");
            manager.start(port);
        } catch(Exception e) {
            System.err.println((char)27 + "[31;1mResource Manager exception: " + (char)27 + e.toString());
            System.exit(1);
        }
    }

    public TCPResourceManager(String p_name)
    {
        super(p_name);
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
            serverSocket.close();
            System.out.println("'" + this.getName() + ":" + port + "' Server Socket closed");
        }
        catch(IOException e) {
            System.err.println((char)27 + "[31;1mResource Manager exception: " + (char)27 + e.toString());
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

                String result = manager.execute(parsedCommand);

                out.println(result);
                in.close();
                out.close();
                clientSocket.close();
            } catch(IOException e) {
                System.err.println((char)27 + "[31;1mResource Manager exception: " + (char)27 + "[0m" + e.getLocalizedMessage());
            }
        }
    }

}
