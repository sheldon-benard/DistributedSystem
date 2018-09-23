package Server.Middleware;

import Server.Interface.*;
import Server.Common.*;
import Server.Middleware.ServerConfig;
import java.util.Vector;
import java.rmi.RemoteException;
import java.rmi.ConnectException;

import java.rmi.NotBoundException;
import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Middleware extends ResourceManager {

    protected IResourceManager m_flightResourceManager = null;
    protected IResourceManager m_carResourceManager = null;
    protected IResourceManager m_roomResourceManager = null;

    protected static ServerConfig s_flightServer;
    protected static ServerConfig s_carServer;
    protected static ServerConfig s_roomServer;

    public Middleware(String p_name)
    {
        super(p_name);
    }

    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException {
        Trace.info("addFlight - Redirect to Flight Resource Manager");
        try {
            return m_flightResourceManager.addFlight(id, flightNum, flightSeats, flightPrice);
        } catch (ConnectException e) {
            connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name);
            return m_flightResourceManager.addFlight(id, flightNum, flightSeats, flightPrice);
        } catch(Exception e) {
            Trace.error(e.toString());
            return false;
        }
    }

    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException
    {
        Trace.info("addCars - Redirect to Car Resource Manager");
        try {
            return m_carResourceManager.addCars(id, location, numCars, price);
        } catch (ConnectException e) {
            connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name);
            return m_carResourceManager.addCars(id, location, numCars, price);
        } catch(Exception e) {
            Trace.error(e.toString());
            return false;
        }

    }

    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException
    {
        Trace.info("addRooms - Redirect to Room Resource Manager");
        try {
            return m_roomResourceManager.addRooms(id, location, numRooms, price);
        } catch (ConnectException e) {
            connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name);
            return m_roomResourceManager.addRooms(id,location, numRooms, price);
        } catch(Exception e) {
            Trace.error(e.toString());
            return false;
        }
    }

    public boolean deleteFlight(int id, int flightNum) throws RemoteException
    {
        Trace.info("deleteFlight - Redirect to Flight Resource Manager");
        try {
            return m_flightResourceManager.deleteFlight(id, flightNum);
        } catch (ConnectException e) {
            connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name);
            return m_flightResourceManager.deleteFlight(id, flightNum);
        } catch(Exception e) {
            Trace.error(e.toString());
            return false;
        }
    }

    public boolean deleteCars(int id, String location) throws RemoteException
    {
        Trace.info("deleteCars - Redirect to Car Resource Manager");
        try {
            return m_carResourceManager.deleteCars(id, location);
        } catch (ConnectException e) {
            connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name);
            return m_carResourceManager.deleteCars(id, location);
        } catch(Exception e) {
            Trace.error(e.toString());
            return false;
        }
    }

    public boolean deleteRooms(int id, String location) throws RemoteException
    {
        Trace.info("deleteRooms - Redirect to Room Resource Manager");
        try {
            return m_roomResourceManager.deleteRooms(id, location);
        } catch (ConnectException e) {
            connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name);
            return m_roomResourceManager.deleteRooms(id, location);
        } catch(Exception e) {
            Trace.error(e.toString());
            return false;
        }
    }

    public boolean deleteCustomer(int xid, int customerID) throws RemoteException
    {
        Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
            return false;
        }
        else
        {
            // Increase the reserved numbers of all reservable items which the customer reserved.
            RMHashMap reservations = customer.getReservations();
            for (String reservedKey : reservations.keySet())
            {
                String type = reservedKey.split("-")[0];
                ReservedItem reserveditem = customer.getReservedItem(reservedKey);
                if (type.equals("flight"))
                    m_flightResourceManager.removeReservation(xid, customerID, reserveditem.getKey(),reserveditem.getCount());
                else if (type.equals("car"))
                    m_carResourceManager.removeReservation(xid, customerID, reserveditem.getKey(),reserveditem.getCount());
                else if (type.equals("room"))
                    m_roomResourceManager.removeReservation(xid, customerID, reserveditem.getKey(),reserveditem.getCount());
                else
                    Trace.error("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--reservedKey (" + reservedKey + ") wasn't of expected type.");
            }
            // Remove the customer from the storage
            removeData(xid, customer.getKey());
            Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
            return true;
        }

    }

    public int queryFlight(int id, int flightNumber) throws RemoteException
    {
        Trace.info("queryFlight - Redirect to Flight Resource Manager");
        try {
            return m_flightResourceManager.queryFlight(id, flightNumber);
        } catch (ConnectException e) {
            connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name);
            return m_flightResourceManager.queryFlight(id, flightNumber);
        } catch(Exception e) {
            Trace.error(e.toString());
            return -1;
        }
    }

    public int queryCars(int id, String location) throws RemoteException
    {
        Trace.info("queryCars - Redirect to Car Resource Manager");
        try {
            return m_carResourceManager.queryCars(id, location);
        } catch (ConnectException e) {
            connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name);
            return m_carResourceManager.queryCars(id, location);
        } catch(Exception e) {
            Trace.error(e.toString());
            return -1;
        }
    }

    public int queryRooms(int id, String location) throws RemoteException
    {
        Trace.info("queryRooms - Redirect to Room Resource Manager");
        try {
            return m_roomResourceManager.queryRooms(id, location);
        } catch (ConnectException e) {
            connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name);
            return m_roomResourceManager.queryRooms(id, location);
        } catch(Exception e) {
            Trace.error(e.toString());
            return -1;
        }
    }

    public int queryFlightPrice(int id, int flightNumber) throws RemoteException
    {
        Trace.info("queryFlightPrice - Redirect to Flight Resource Manager");
        try {
            return m_flightResourceManager.queryFlightPrice(id, flightNumber);
        } catch (ConnectException e) {
            connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name);
            return m_flightResourceManager.queryFlightPrice(id, flightNumber);
        } catch(Exception e) {
            Trace.error(e.toString());
            return -1;
        }
    }

    public int queryCarsPrice(int id, String location) throws RemoteException
    {
        Trace.info("queryCarsPrice - Redirect to Car Resource Manager");
        try {
            return m_carResourceManager.queryCarsPrice(id, location);
        } catch (ConnectException e) {
            connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name);
            return m_carResourceManager.queryCarsPrice(id, location);
        } catch(Exception e) {
            Trace.error(e.toString());
            return -1;
        }
    }

    public int queryRoomsPrice(int id, String location) throws RemoteException
    {
        Trace.info("queryRoomsPrice - Redirect to Room Resource Manager");
        try {
            return m_roomResourceManager.queryRoomsPrice(id, location);
        } catch (ConnectException e) {
            connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name);
            return m_roomResourceManager.queryRoomsPrice(id, location);
        } catch(Exception e) {
            Trace.error(e.toString());
            return -1;
        }
    }

    public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException
    {return false;}

    public boolean reserveCar(int id, int customerID, String location) throws RemoteException
    {return false;}

    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException
    {return false;}

    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException
    {return false;}

    public String getName() throws RemoteException {
        return m_name;
    }

    protected void connectServer(String type, String server, int port, String name)
    {
        try {
            boolean first = true;
            while (true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(server, port);

                    switch(type) {
                        case "Flight": {
                            m_flightResourceManager = (IResourceManager)registry.lookup(name);
                            break;
                        }
                        case "Car": {
                            m_carResourceManager = (IResourceManager)registry.lookup(name);
                            break;
                        }
                        case "Room": {
                            m_roomResourceManager = (IResourceManager)registry.lookup(name);
                            break;
                        }
                    }
                    System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + name + "]");
                    break;
                }
                catch (NotBoundException|RemoteException e) {
                    if (first) {
                        System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/" + name + "]");
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