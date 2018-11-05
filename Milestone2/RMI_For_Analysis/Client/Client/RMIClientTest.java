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

abstract class ClientTest extends Client implements Runnable{

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public double[] times = new double[50];
    public int clients = 1;
    public double throughput = 1.0;

    public ClientTest()
    {
        super();
    }

    public void run() {

        int waitTime = (int)((1000 * clients) / throughput);

        long startTime = 5*1000 + System.currentTimeMillis();
        long variation = 30 + 1;

        while (System.currentTimeMillis() < startTime){}

        for (int i = (int)Thread.currentThread().getId()*200; i < (int)Thread.currentThread().getId()*200 + 150; i++) {
            double l = Math.random();
            int v;
            if (l < 0.5)
                v = waitTime - ((int)(variation*Math.random()));
            else
                v = waitTime + ((int)(variation*Math.random()));
            try {
                double rt = oneResourceManagerTransaction(i, i, i);
                if (i >= (int)Thread.currentThread().getId()*200 + 100)
                    times[i - ((int)Thread.currentThread().getId()*200 + 100)] = rt;
                if ((int)(v - rt) < 0)
                    continue;
                Thread.sleep((int) (v - rt));
            } catch(Exception e){}
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
        m_resourceManager.commit(xid);
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
        m_resourceManager.commit(xid);
        long responseTime = System.currentTimeMillis() - startTime;
        return responseTime;
    }


}

public class RMIClientTest extends ClientTest
{
    private static String s_serverHost = "localhost";
    private static int s_serverPort = 1099;
    private static String s_serverName = "Middleware";
    private static int cli = 1;
    private static double tp = 1.0;

    private static String s_rmiPrefix = "group7";

    public static void main(String args[])
    {
        if (args.length > 0)
        {
            cli = Integer.parseInt(args[0]);
            tp = Double.parseDouble(args[1]);
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
            RMIClientTest[] c = new RMIClientTest[cli];
            Thread[] thread = new Thread[cli];
            for (int i = 0; i < cli; i++) {
                c[i] = new RMIClientTest();
                c[i].clients = cli;
                c[i].throughput = tp;
                c[i].connectServer();
                thread[i] = new Thread(c[i]);
                thread[i].start();
            }

            for (int i = 0; i < cli; i++) {
                thread[i].join();
            }

            for (int i = 0; i < cli; i++) {
                System.out.println(Arrays.toString(c[i].times));
            }
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

