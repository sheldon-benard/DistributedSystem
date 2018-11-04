package Client;

import java.util.*;
import java.net.*;
import java.io.*;
import java.lang.StringBuilder;

import Server.Interface.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;

import java.rmi.ConnectException;
import java.rmi.ServerException;
import java.rmi.UnmarshalException;

abstract class ClientTest extends Client {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_YELLOW = "\u001B[33m";

    public ClientTest()
    {
        super();
    }

    public void start(int clients, double throughput) {

        int waitTime = (int)((1000 * clients) / throughput);

        long startTime = 10*1000 + System.currentTimeMillis();
        long variation = 30 + 1;
        HashMap<Long, double[]> times = new HashMap<Long, double[]>();

        for (int i = 0; i < clients; i++) {
            new Thread() {
                @Override
                public void run() {
                    long id = Thread.currentThread().getId();
                    times.put(id, new double[50]);
                    double l = Math.random();
                    int v;
                    if (l < 0.5)
                        v = waitTime - ((int)(variation*Math.random()));
                    else
                        v = waitTime + ((int)(variation*Math.random()));

                    while (System.currentTimeMillis() < startTime){}

                    for (int i = 0; i < 150; i++) {
                        try {
                            double rt = oneResourceManagerTransaction(i, i, i);
                            if (i >= 100)
                                times.get(id)[i - 100] = rt;
                            Thread.sleep((int) (v - rt));
                        } catch(Exception e){}
                    }
                    System.out.println(times);


                }

            }.start();

        }


    }

    private double oneResourceManagerTransaction(int flightNum, int flightSeats, int flightPrice) throws Exception{
        long startTime = System.currentTimeMillis();
        int xid = m_resourceManager.start();
        m_resourceManager.addFlight(xid, flightNum, flightSeats, flightPrice);
        m_resourceManager.queryFlight(xid, flightNum);
        m_resourceManager.addFlight(xid, flightNum, flightSeats, flightPrice);
        m_resourceManager.queryFlightPrice(xid, flightNum);
        m_resourceManager.addFlight(xid, flightNum, flightSeats, flightPrice);
        m_resourceManager.deleteFlight(xid, flightNum);
        long responseTime = System.currentTimeMillis() - startTime;
        return responseTime;
    }

    private double allResourceManagerTransaction(int custID, int flightNum, int number, int price, String location) throws Exception {
        long startTime = System.currentTimeMillis();
        int xid = m_resourceManager.start();
        m_resourceManager.addFlight(xid, flightNum, number, price);
        m_resourceManager.addCars(xid, location, number, price);
        m_resourceManager.addRooms(xid, location, number, price);
        m_resourceManager.newCustomer(xid, custID);
        m_resourceManager.reserveFlight(xid, custID, flightNum);
        m_resourceManager.reserveCar(xid, custID, location);
        long responseTime = System.currentTimeMillis() - startTime;
        return responseTime;
    }


}

public class RMIClientTest extends ClientTest
{
    private static String s_serverHost = "localhost";
    private static int s_serverPort = 1099;
    private static String s_serverName = "Middleware";
    private static int clients = 1;
    private static double throughput = 1.0;

    private static String s_rmiPrefix = "group7";

    public static void main(String args[])
    {
        if (args.length > 0)
        {
            clients = Integer.parseInt(args[0]);
            throughput = Double.parseDouble(args[1]);
        }
        if (args.length > 2)
        {
            s_serverHost = args[2];
        }
        if (args.length > 3) {
            s_serverName = args[3];
        }
        if (args.length > 4)
        {
            System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUsage: java client.RMIClient [server_hostname [server_rmiobject]]");
            System.exit(1);
        }

        // Set the security policy
        if (System.getSecurityManager() == null)
        {
            System.setSecurityManager(new SecurityManager());
        }

        // Get a reference to the RMIRegister
        try {
            RMIClientTest client = new RMIClientTest();
            client.connectServer();
            client.start(clients, throughput);
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public RMIClientTest()
    {
        super();
    }

    public void connectServer()
    {
        connectServer(s_serverHost, s_serverPort, s_serverName);
    }

    public void connectServer(String server, int port, String name)
    {
        try {
            boolean first = true;
            while (true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(server, port);
                    m_resourceManager = (IResourceManager)registry.lookup(s_rmiPrefix + name);
                    System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
                    break;
                }
                catch (NotBoundException|RemoteException e) {
                    if (first) {
                        System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
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
}

