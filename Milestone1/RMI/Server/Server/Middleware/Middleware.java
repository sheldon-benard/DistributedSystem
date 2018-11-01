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

        synchronized (m_flightResourceManager) {
            try {
                try {
                    return m_flightResourceManager.addFlight(id, flightNum, flightSeats, flightPrice);
                } catch (ConnectException e) {
                    connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name);
                    return m_flightResourceManager.addFlight(id, flightNum, flightSeats, flightPrice);
                }
            } catch (Exception e) {
                Trace.error(e.toString());
                return false;
            }
        }
    }

    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException
    {
        Trace.info("addCars - Redirect to Car Resource Manager");
        synchronized (m_carResourceManager) {
            try {
                try {
                    return m_carResourceManager.addCars(id, location, numCars, price);
                } catch (ConnectException e) {
                    connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name);
                    return m_carResourceManager.addCars(id, location, numCars, price);
                }
            } catch (Exception e) {
                Trace.error(e.toString());
                return false;
            }
        }

    }

    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException
    {
        Trace.info("addRooms - Redirect to Room Resource Manager");
        synchronized (m_roomResourceManager) {
            try {
                try {
                    return m_roomResourceManager.addRooms(id, location, numRooms, price);
                } catch (ConnectException e) {
                    connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name);
                    return m_roomResourceManager.addRooms(id, location, numRooms, price);
                }
            } catch (Exception e) {
                Trace.error(e.toString());
                return false;
            }
        }
    }

    public boolean deleteFlight(int id, int flightNum) throws RemoteException
    {
        Trace.info("deleteFlight - Redirect to Flight Resource Manager");
        synchronized (m_flightResourceManager) {
            try {
                try {
                    return m_flightResourceManager.deleteFlight(id, flightNum);
                } catch (ConnectException e) {
                    connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name);
                    return m_flightResourceManager.deleteFlight(id, flightNum);
                }
            } catch (Exception e) {
                Trace.error(e.toString());
                return false;
            }
        }
    }

    public boolean deleteCars(int id, String location) throws RemoteException
    {
        Trace.info("deleteCars - Redirect to Car Resource Manager");
        synchronized (m_carResourceManager) {
            try {
                try {
                    return m_carResourceManager.deleteCars(id, location);
                } catch (ConnectException e) {
                    connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name);
                    return m_carResourceManager.deleteCars(id, location);
                }
            } catch (Exception e) {
                Trace.error(e.toString());
                return false;
            }
        }
    }

    public boolean deleteRooms(int id, String location) throws RemoteException
    {
        Trace.info("deleteRooms - Redirect to Room Resource Manager");
        synchronized (m_roomResourceManager) {
            try {
                try {
                    return m_roomResourceManager.deleteRooms(id, location);
                } catch (ConnectException e) {
                    connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name);
                    return m_roomResourceManager.deleteRooms(id, location);
                }
            } catch (Exception e) {
                Trace.error(e.toString());
                return false;
            }
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
        else {
            synchronized (customer) {
            // Increase the reserved numbers of all reservable items which the customer reserved.
            RMHashMap reservations = customer.getReservations();
            for (String reservedKey : reservations.keySet()) {
                String type = reservedKey.split("-")[0];
                ReservedItem reserveditem = customer.getReservedItem(reservedKey);
                if (type.equals("flight")) {
                    synchronized (m_flightResourceManager) {
                        m_flightResourceManager.removeReservation(xid, customerID, reserveditem.getKey(), reserveditem.getCount());
                    }
                } else if (type.equals("car")) {
                    synchronized (m_carResourceManager) {
                        m_carResourceManager.removeReservation(xid, customerID, reserveditem.getKey(), reserveditem.getCount());
                    }
                } else if (type.equals("room")) {
                    synchronized (m_roomResourceManager) {
                        m_roomResourceManager.removeReservation(xid, customerID, reserveditem.getKey(), reserveditem.getCount());
                    }
                } else
                    Trace.error("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--reservedKey (" + reservedKey + ") wasn't of expected type.");
            }
            // Remove the customer from the storage
            removeData(xid, customer.getKey());
            Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
            return true;
            }
        }

    }

    public int queryFlight(int id, int flightNumber) throws RemoteException
    {
        Trace.info("queryFlight - Redirect to Flight Resource Manager");
        try {
            try {
                return m_flightResourceManager.queryFlight(id, flightNumber);
            } catch (ConnectException e) {
                connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name);
                return m_flightResourceManager.queryFlight(id, flightNumber);
            }
        } catch (Exception e) {
            Trace.error(e.toString());
            return -1;
        }
    }

    public int queryCars(int id, String location) throws RemoteException
    {
        Trace.info("queryCars - Redirect to Car Resource Manager");
        try {
            try {
                return m_carResourceManager.queryCars(id, location);
            } catch (ConnectException e) {
                connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name);
                return m_carResourceManager.queryCars(id, location);
            }
        } catch (Exception e) {
            Trace.error(e.toString());
            return -1;
        }
    }

    public int queryRooms(int id, String location) throws RemoteException
    {
        Trace.info("queryRooms - Redirect to Room Resource Manager");
        try {
            try {
                return m_roomResourceManager.queryRooms(id, location);
            } catch (ConnectException e) {
                connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name);
                return m_roomResourceManager.queryRooms(id, location);
            }
        } catch (Exception e) {
            Trace.error(e.toString());
            return -1;
        }
    }

    public int queryFlightPrice(int id, int flightNumber) throws RemoteException
    {
        Trace.info("queryFlightPrice - Redirect to Flight Resource Manager");
        try {
            try {
                return m_flightResourceManager.queryFlightPrice(id, flightNumber);
            } catch (ConnectException e) {
                connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name);
                return m_flightResourceManager.queryFlightPrice(id, flightNumber);
            }
        } catch (Exception e) {
            Trace.error(e.toString());
            return -1;
        }
    }

    public int queryCarsPrice(int id, String location) throws RemoteException
    {
        Trace.info("queryCarsPrice - Redirect to Car Resource Manager");
        try {
            try {
                return m_carResourceManager.queryCarsPrice(id, location);
            } catch (ConnectException e) {
                connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name);
                return m_carResourceManager.queryCarsPrice(id, location);
            }
        } catch (Exception e) {
            Trace.error(e.toString());
            return -1;
        }
    }

    public int queryRoomsPrice(int id, String location) throws RemoteException
    {
        Trace.info("queryRoomsPrice - Redirect to Room Resource Manager");
        try {
            try {
                return m_roomResourceManager.queryRoomsPrice(id, location);
            } catch (ConnectException e) {
                connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name);
                return m_roomResourceManager.queryRoomsPrice(id, location);
            }
        } catch (Exception e) {
            Trace.error(e.toString());
            return -1;
        }
    }

    public boolean reserveFlight(int xid, int customerID, int flightNumber) throws RemoteException
    {
        String key = Flight.getKey(flightNumber);

        Trace.info("RM::reserveFlight(" + xid + ", customer=" + customerID + ", " + key + ") called" );
        // Check customer exists
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + flightNumber + ")  failed--customer doesn't exist");
            return false;
        }
        synchronized (customer) {
            synchronized (m_flightResourceManager) {
                int price = m_flightResourceManager.itemsAvailable(xid, key, 1);

                if (price < 0) {
                    Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + flightNumber + ")  failed--item unavailable");
                    return false;
                }

                if (m_flightResourceManager.reserveFlight(xid, customerID, flightNumber)) {
                    customer.reserve(key, String.valueOf(flightNumber), price);
                    writeData(xid, customer.getKey(), customer);
                    return true;
                }
                Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + flightNumber + ")  failed--Could not reserve item");
                return false;
            }
        }
    }

    public boolean reserveCar(int xid, int customerID, String location) throws RemoteException
    {
        String key = Car.getKey(location);

        Trace.info("RM::reserveCar(" + xid + ", customer=" + customerID + ", " + key + ") called" );
        // Check customer exists
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }
        synchronized (customer) {
            synchronized (m_carResourceManager) {
                int price = m_carResourceManager.itemsAvailable(xid, key, 1);

                if (price < 0) {
                    Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--item unavailable");
                    return false;
                }

                if (m_carResourceManager.reserveCar(xid, customerID, location)) {
                    customer.reserve(key, location, price);
                    writeData(xid, customer.getKey(), customer);
                    return true;
                }
                Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--Could not reserve item");
                return false;
            }
        }
    }

    public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException
    {
        String key = Room.getKey(location);

        Trace.info("RM::reserveRoom(" + xid + ", customer=" + customerID + ", " + key + ") called" );
        // Check customer exists
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }
        synchronized (customer) {
            synchronized (m_roomResourceManager) {
                int price = m_roomResourceManager.itemsAvailable(xid, key, 1);

                if (price < 0) {
                    Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--item unavailable");
                    return false;
                }

                if (m_roomResourceManager.reserveRoom(xid, customerID, location)) {
                    customer.reserve(key, location, price);
                    writeData(xid, customer.getKey(), customer);
                    return true;
                }
                Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--Could not reserve item");
                return false;
            }
        }
    }

    public boolean bundle(int xid, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException
    {
        Trace.info("RM::bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ") called" );
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }
        synchronized (customer) {
            HashMap<String, Integer> countMap = countFlights(flightNumbers);
            HashMap<Integer, Integer> flightPrice = new HashMap<Integer, Integer>();
            int carPrice;
            int roomPrice;

            synchronized (m_flightResourceManager) {

                if (car && room) {
                    synchronized (m_carResourceManager) {
                        synchronized (m_roomResourceManager) {

                            // Check flight availability
                            for (String key : countMap.keySet()) {
                                int keyInt;

                                try {
                                    keyInt = Integer.parseInt(key);
                                } catch (Exception e) {
                                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--could not parse flightNumber");
                                    return false;
                                }

                                int price = m_flightResourceManager.itemsAvailable(xid, Flight.getKey(keyInt), countMap.get(key));

                                if (price < 0) {
                                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + key + " doesn't have enough spots");
                                    return false;
                                } else {
                                    flightPrice.put(keyInt, price);
                                }
                            }

                            carPrice = m_carResourceManager.itemsAvailable(xid, Car.getKey(location), 1);

                            if (carPrice < 0) {
                                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--car-" + location + " doesn't have enough spots");
                                return false;
                            }


                            roomPrice = m_roomResourceManager.itemsAvailable(xid, Room.getKey(location), 1);

                            if (roomPrice < 0) {
                                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--room-" + location + " doesn't have enough spots");
                                return false;
                            }

                            m_roomResourceManager.reserveRoom(xid, customerID, location);
                            customer.reserve(Room.getKey(location), location, roomPrice);
                            writeData(xid, customer.getKey(), customer);


                            m_carResourceManager.reserveCar(xid, customerID, location);
                            customer.reserve(Car.getKey(location), location, carPrice);
                            writeData(xid, customer.getKey(), customer);

                        }
                    }

                } else if (car) {
                    synchronized (m_carResourceManager) {

                        // Check flight availability
                        for (String key : countMap.keySet()) {
                            int keyInt;

                            try {
                                keyInt = Integer.parseInt(key);
                            } catch (Exception e) {
                                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--could not parse flightNumber");
                                return false;
                            }

                            int price = m_flightResourceManager.itemsAvailable(xid, Flight.getKey(keyInt), countMap.get(key));

                            if (price < 0) {
                                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + key + " doesn't have enough spots");
                                return false;
                            } else {
                                flightPrice.put(keyInt, price);
                            }
                        }

                        carPrice = m_carResourceManager.itemsAvailable(xid, Car.getKey(location), 1);

                        if (carPrice < 0) {
                            Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--car-" + location + " doesn't have enough spots");
                            return false;
                        }
                        m_carResourceManager.reserveCar(xid, customerID, location);
                        customer.reserve(Car.getKey(location), location, carPrice);
                        writeData(xid, customer.getKey(), customer);

                    }
                } else if (room) {
                    synchronized (m_roomResourceManager) {

                        // Check flight availability
                        for (String key : countMap.keySet()) {
                            int keyInt;

                            try {
                                keyInt = Integer.parseInt(key);
                            } catch (Exception e) {
                                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--could not parse flightNumber");
                                return false;
                            }

                            int price = m_flightResourceManager.itemsAvailable(xid, Flight.getKey(keyInt), countMap.get(key));

                            if (price < 0) {
                                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + key + " doesn't have enough spots");
                                return false;
                            } else {
                                flightPrice.put(keyInt, price);
                            }
                        }

                        roomPrice = m_roomResourceManager.itemsAvailable(xid, Room.getKey(location), 1);

                        if (roomPrice < 0) {
                            Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--room-" + location + " doesn't have enough spots");
                            return false;
                        }

                        m_roomResourceManager.reserveRoom(xid, customerID, location);
                        customer.reserve(Room.getKey(location), location, roomPrice);
                        writeData(xid, customer.getKey(), customer);
                    }
                }
                else{
                    // Check flight availability
                    for (String key : countMap.keySet()) {
                        int keyInt;

                        try {
                            keyInt = Integer.parseInt(key);
                        } catch (Exception e) {
                            Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--could not parse flightNumber");
                            return false;
                        }

                        int price = m_flightResourceManager.itemsAvailable(xid, Flight.getKey(keyInt), countMap.get(key));

                        if (price < 0) {
                            Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + key + " doesn't have enough spots");
                            return false;
                        } else {
                            flightPrice.put(keyInt, price);
                        }
                    }
                }

                // Reserve flights
                for (Integer key : flightPrice.keySet()) {
                    for (int i = 0; i < countMap.get(String.valueOf(key)); i++) {
                        int price = flightPrice.get(key);

                        m_flightResourceManager.reserveFlight(xid, customerID, key);
                        customer.reserve(Flight.getKey(key), String.valueOf(key), price);
                        writeData(xid, customer.getKey(), customer);
                    }
                }

            }
            Trace.info("RM:bundle() -- succeeded");
            return true;
        }
    }

    public String Analytics(int xid, int upperBound)
    {
        String summary = "";
        summary += "Flight Quantities\n";
        try {
            summary += m_flightResourceManager.Analytics(xid, upperBound);
        } catch (Exception e) {
            summary += "Error in retrieving quantities\n";
        }

        summary += "Car Quantities\n";
        try {
            summary += m_carResourceManager.Analytics(xid, upperBound);
        } catch (Exception e) {
            summary += "Error in retrieving quantities\n";
        }

        summary += "Room Quantities\n";
        try {
            summary += m_roomResourceManager.Analytics(xid, upperBound);
        } catch (Exception e) {
            summary += "Error in retrieving quantities\n";
        }

        return summary;
    }

    public String getName() throws RemoteException {
        return m_name;
    }

    protected HashMap<String, Integer> countFlights(Vector<String> flightNumbers) {
        HashMap<String, Integer> map = new HashMap<String, Integer>();

        for (String flightNumber : flightNumbers) {
            if (map.containsKey(flightNumber))
                map.put(flightNumber, map.get(flightNumber) + 1);
            else
                map.put(flightNumber, 1);
        }
        return map;
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