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
    public long[][] times = new long[50][4];
    public int clients = 1;
    public double throughput = 1.0;
    public long startTime = 0;

    public ClientTest()
    {
        super();
    }

    public void run() {

        int waitTime = (int)((1000 * clients) / throughput);
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
                long[] rt = oneResourceManagerTransaction();
                if (i >= (int)Thread.currentThread().getId()*200 + 100)
                    times[i - ((int)Thread.currentThread().getId()*200 + 100)] = rt;
                if ((int)(v - rt[0]) < 0)
                    continue;
                Thread.sleep((int) (v - rt[0]));
            } catch(Exception e){}
        }

    }

    public void setupEnv() {
        System.out.println("SETUP");
        try {
            long[] x = m_resourceManager.start();
            System.out.println(x[0] + "-" + x[1]);
            int xid = (int)x[0];
            for (int i = 1; i <= 100; i++) {
                m_resourceManager.addFlight(xid, i, 10000, 500 + i);
                m_resourceManager.addCars(xid, "Montreal" + i, 10000, 100 + i);
                m_resourceManager.addRooms(xid, "Montreal" + i, 10000, 100 + i);
            }
            for (int i = 1; i <= 500; i++) {
                m_resourceManager.newCustomer(xid, i);
            }
            m_resourceManager.commit(xid);
        } catch(Exception e){
            System.out.println(e.toString());
            System.exit(-1);
        }
        System.out.println("END SETUP");
    }


    private long[] oneResourceManagerTransaction() throws Exception{
        long startTime = System.currentTimeMillis();
        int i = (int)(Math.random()*100 + 1);
        int j = (int)(Math.random()*500 + 1);

        long[] x = m_resourceManager.start();
        int xid = (int)x[0];
        long[] a = m_resourceManager.queryFlight(xid, i);
        long[] b = m_resourceManager.queryFlightPrice(xid, i);
        long[] c = m_resourceManager.reserveFlight(xid, j, i);
        long[] d = m_resourceManager.commit(xid);
        long responseTime = System.currentTimeMillis() - startTime;
        return new long[] {responseTime, x[1] + a[0] + b[0] + c[0] + d[0], a[1] + b[1] + c[1] + d[1], a[2] + b[2] + c[2] + d[2]};
    }

    private long[] manyResourceManagerTransaction() throws Exception{
        long startTime = System.currentTimeMillis();
        int i = (int)(Math.random()*100 + 1);
        int j = (int)(Math.random()*500 + 1);

        long[] x = m_resourceManager.start();
        int xid = (int)x[0];
        long[] a = m_resourceManager.reserveFlight(xid, j, i);
        long[] b = m_resourceManager.reserveCar(xid, j, "Montreal" + i);
        long[] c = m_resourceManager.reserveRoom(xid, j, "Montreal" + i);
        long[] d = m_resourceManager.commit(xid);
        long responseTime = System.currentTimeMillis() - startTime;
        return new long[] {responseTime, x[1] + a[0] + b[0] + c[0] + d[0], a[1] + b[1] + c[1] + d[1], a[2] + b[2] + c[2] + d[2]};
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
        long startTime = 5*1000 + System.currentTimeMillis();
        // Get a reference to the RMIRegister
        try {
            RMIClientTest[] c = new RMIClientTest[cli];
            Thread[] thread = new Thread[cli];
            for (int i = 0; i < cli; i++) {
                c[i] = new RMIClientTest();
                c[i].clients = cli;
                c[i].throughput = tp;
                c[i].startTime = startTime;
                c[i].connectServer();
                if (i == 0)
                    c[i].setupEnv();
                thread[i] = new Thread(c[i]);
                thread[i].start();
            }

            for (int i = 0; i < cli; i++) {
                thread[i].join();
            }
            System.out.println("DATA\n\n");
            for (int i = 0; i < cli; i++) {
                for (int j = 0; j < c[i].times.length; j++) {
                    for (int k = 0; k < c[i].times[j].length; k++) {
                        System.out.print(c[i].times[j][k]);
                        if (k != c[i].times[j].length - 1)
                            System.out.print(",");
                    }
                    System.out.println();
                }
                System.out.println();
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

