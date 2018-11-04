package Server.RMI;

import Server.Interface.*;
import Server.Common.*;
import Server.Middleware.*;

import java.rmi.NotBoundException;
import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RMIMiddleware extends Middleware {

    private static String s_serverName = "Middleware";
    private static String s_rmiPrefix = "group7";

    public static void main(String[] args) {

        // Args: name,host,port: Flight,localhost,1099
        if (args.length == 3) {
            try {
                String[] flightInfo = args[0].split(",");
                String[] carInfo = args[1].split(",");
                String[] roomInfo = args[2].split(",");

                s_flightServer = new ServerConfig(s_rmiPrefix + flightInfo[0],flightInfo[1],flightInfo[2]);
                s_carServer = new ServerConfig(s_rmiPrefix + carInfo[0],carInfo[1],carInfo[2]);
                s_roomServer = new ServerConfig(s_rmiPrefix + roomInfo[0],roomInfo[1],roomInfo[2]);
            }
            catch(Exception e){
                System.err.println((char)27 + "[31;1mMiddleware exception: " + (char)27 + "[0mUncaught exception");
                e.printStackTrace();
                System.exit(1);
            }
        }
        else {
            System.err.println((char)27 + "[31;1mMiddleware exception: " + (char)27 + "[0mInvalid number of arguments; expected 3 args");
            System.exit(1);
        }

        // Set the security policy
        if (System.getSecurityManager() == null)
        {
            System.setSecurityManager(new SecurityManager());
        }

        // Try to connect to the ResourceManagers and register middleware resource manager
        try {
            RMIMiddleware middleware = new RMIMiddleware(s_serverName);
            middleware.connectServers();
            middleware.createServerEntry();
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mMiddleware exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }

    }

    public RMIMiddleware(String name)
    {
        super(name);
    }

    public void connectServers()
    {
        connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name);
        connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name);
        connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name);
    }

    public void createServerEntry() {
        // Create the RMI server entry
        try {
            // we don't need to create a Middleware object, since 'this' already is one

            // Dynamically generate the stub (client proxy)
            IResourceManager resourceManager = (IResourceManager)UnicastRemoteObject.exportObject(this, 0);

            // Bind the remote object's stub in the registry
            Registry l_registry;
            try {
                l_registry = LocateRegistry.createRegistry(1099);
            } catch (RemoteException e) {
                l_registry = LocateRegistry.getRegistry(1099);
            }
            final Registry registry = l_registry;
            registry.rebind(s_rmiPrefix + s_serverName, resourceManager);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        registry.unbind(s_rmiPrefix + s_serverName);
                        System.out.println("'" + s_serverName + "' resource manager unbound");
                    }
                    catch(Exception e) {
                        //System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
                        //e.printStackTrace();
                    }
                    System.out.println("'" + s_serverName + "' Shut down");
                }
            });
            System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_serverName + "'");
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }



}