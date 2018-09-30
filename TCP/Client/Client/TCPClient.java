package Client;

import java.io.*;
import java.net.*;

public class TCPClient
{
	private static String s_serverHost = "localhost";
	private static int s_serverPort = 6666;

	private Socket clientSocket;
	private PrintWriter out;
	private BufferedReader in;
	private String host;
	private int port;

	public static void main(String args[])
	{
	    boolean debug = false;
		try {
		    debug = (args[0].equals("true")) ? true : false;

			if (args.length > 1) {
				s_serverHost = args[1];
			}
			if (args.length > 2) {
				s_serverPort = Integer.parseInt(args[2]);
			}
		} catch(Exception e) {
			System.err.println((char)27 + "[31;1mTCPClient exception: " + (char)27 + "[0m" + e.toString());
			System.exit(1);
		}

		try {
			if (!debug) {
				Client client = new Client(new TCPClient(s_serverHost, s_serverPort));
				client.start();
			}
			else {
				ClientTest client = new ClientTest(new TCPClient(s_serverHost, s_serverPort));
				client.start();
			}
		} 
		catch (Exception e) {    
			System.err.println((char)27 + "[31;1mTCPClient exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public TCPClient(String host, int port) {
		this.host = host;
		this.port = port;
		this.connect(true);
	}

	public void connect(boolean print) {
		boolean first = true;
		try {
			while (true) {
				try {
					clientSocket = new Socket(host, port);
					out = new PrintWriter(clientSocket.getOutputStream(), true);
					in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					if (print) System.out.println("Connected to host:" + this.host + " port:" + this.port);
					break;
				} catch (IOException e) {
					if (first) {
						System.out.println("Waiting for host:" + this.host + " port:" + this.port);
						first = false;
					}
				}

				Thread.sleep(500);
			}
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}

	}

	public String sendMessage(String message) throws IOException {
		out.println(message);
		String returnString = "";
		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			if (returnString.length() == 0)
				returnString += inputLine;
			else
				returnString += "\n" + inputLine;
		}
		connect(false);
		return returnString;
	}

	public void stopTCPClient() {
		try {
			in.close();
			out.close();
			clientSocket.close();
		} catch(Exception e) {
			System.err.println((char)27 + "[31;1mUnable to quit TCPClient: " + (char)27 + "[0m" + e.toString());
		}
	}


}

