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

    public void start() {
        String[][] tests = {
                {"Start", "Start transaction xid=1"},
                {"Addflight,1,100,10,100", "Expected: Add Flight - 10 for $100" },
                {"Addflight,1,200,20,200", "Expected: Add Flight - 20 for $200" },
                {"Addflight,1,300,20,200","Expected: Add Flight - 20 for $200" },
                {"AddCustomerID,1,123", "Expected: Add Customer ID 123" },
                {"AddCustomerID,1,124","Expected: Add Customer ID 124" },
                {"AddCustomerID,1,125","Expected: Add Customer ID 125" },
                {"AddCars,1,Montreal,15,15","Expected: Add Cars - 15 at Montreal for $15" },
                {"AddRooms,1,Montreal,15,15", "Expected: Add Rooms - 15 at Montreal for $15" },

                {"DeleteFlight,1,300", "Flight-300 delete" },
                {"DeleteCustomer,1,125",  "Customer-125 delete" },
                {"DeleteCars,1,Montreal",  "Cars-Montreal delete" },
                {"DeleteRooms,1,Montreal",  "Rooms-Montreal delete" },

                {"QueryCars,1,Montreal",  "Cars Quantity - 0" },
                {"QueryRooms,1,Montreal",  "Rooms Quantity - 0" },

                {"AddCars,1,Montreal,15,16","Expected: Add Cars - 15 at Montreal for $16" },
                {"AddRooms,1,Montreal,15,17", "Expected: Add Rooms - 15 at Montreal for $17" },

                {"QueryFlight,1,100","Flight has 10 seats" },
                {"QueryFlight,1,300","Flight has 0 seats" },

                {"QueryCustomer,1,123", "Customer 123 has nothing on its bill"},
                {"QueryCustomer,1,125","Customer 125 doesn't exist -> no bill printed" },

                {"QueryCars,1,Montreal","Cars - 15 available" },
                {"QueryRooms,1,Montreal","Rooms - 15 available" },

                {"QueryFlightPrice,1,100","Flight - $100" },
                {"QueryRoomsPrice,1,Montreal","Room - $17" },
                {"QueryCarsPrice,1,Montreal","Car - $16" },

                {"ReserveFlight,1,123,100", "Flight - reserved" },
                {"DeleteFlight,1,100","Flight - Can't be deleted" },

                {"ReserveCar,1,123,Montreal", "Car - reserved" },
                {"ReserveRoom,1,123,Montreal",  "Room - reserved" },
                {"Summary,1", "Customer 123 has Flight-100, Car-Montreal, Room-Montreal reserved" },
                {"Commit,1", "Commit txn=1"},
                {"Start", "Start txn=2"},
                {"Bundle,1,124,200,200,200,200,200,100,100,100,100,Montreal,1,0", "Failure txn=1 committed already"},
                {"Bundle,2,124,200,200,200,200,200,100,100,100,100,Montreal,1,0", "Customer-124 reserved 5xFlight-200, 4xFlight-100, 1xCar-Montreal"},

                {"Summary,2", "Cust 123, 124" },
                {"Analytics,2,9", "Prints: Flight-100 @ 5"},
                {"Analytics,2,20", "Prints: Flight-100 @ 5, Flight-200 @ 15, Cars-Montreal @ 13, Rooms-Montreal @ 14" },
                {"Abort,2", "Txn=2 aborted"},

                {"Quit", "Quit"}
        };
        Vector<String> arguments = new Vector<String>();

        for (String[] test : tests) {
            System.out.println(ANSI_YELLOW + test[1] + ANSI_RESET);
            arguments = parse(test[0]);
            Command cmd = Command.fromString((String)arguments.elementAt(0));
            try {
                execute(cmd, arguments);
                System.out.println();
                Thread.sleep(3000);
            }
            catch (IllegalArgumentException|ServerException e) {
                System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0m" + e.getLocalizedMessage());
            }
            catch (ConnectException|UnmarshalException e) {
                System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mConnection to server lost");
            }
            catch (InvalidTransactionException e) {
                System.err.println((char)27 + "[31;1mInvalid Transaction exception: " + (char)27 + "[0m" + e.getLocalizedMessage());
            }
            catch (TransactionAbortedException e) {
                System.err.println((char)27 + "[31;1mAborted Transaction exception: " + (char)27 + "[0m" + e.getLocalizedMessage());
            }
            catch (Exception e) {
                System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mUncaught exception");
                e.printStackTrace();
            }
        }


    }
}

public class RMIClientTest extends ClientTest
{
    private static String s_serverHost = "localhost";
    private static int s_serverPort = 1099;
    private static String s_serverName = "Middleware";

    private static String s_rmiPrefix = "group7";

    public static void main(String args[])
    {
        if (args.length > 0)
        {
            s_serverHost = args[0];
        }
        if (args.length > 1)
        {
            s_serverName = args[1];
        }
        if (args.length > 2)
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
            client.start();
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

