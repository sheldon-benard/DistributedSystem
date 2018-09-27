package Server.Middleware;

import Server.Common.*;
import Server.Middleware.ServerConfig;
import java.util.Vector;
import java.util.*;
import java.net.*;
import java.io.*;

public class Middleware extends ResourceManager {

    protected TCPClient m_flightResourceManager = null;
    protected TCPClient m_carResourceManager = null;
    protected TCPClient m_roomResourceManager = null;

    public Middleware(String p_name, String flightIP, int flightPort, String carIP, int carPort, String roomIP, int roomPort)
    {
        super(p_name);
        m_flightResourceManager = new TCPClient(flightIP,flightPort);
        m_carResourceManager = new TCPClient(carIP,carPort);
        m_roomResourceManager = new TCPClient(roomIP,roomPort);
    }

    public void close() {
        m_flightResourceManager.stopTCPClient();
        m_carResourceManager.stopTCPClient();
        m_roomResourceManager.stopTCPClient();
    }

    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice){
        Trace.info("addFlight - Redirect to Flight Resource Manager");
        String command = String.format("AddFlight,%d,%d,%d,%d",id,flightNum,flightSeats,flightPrice);

        synchronized (m_flightResourceManager) {
            try {
                try {
                    return toBool(m_flightResourceManager.sendMessage(command));
                } catch (IOException e) {
                    m_flightResourceManager.connect();
                    return toBool(m_flightResourceManager.sendMessage(command));
                }
            } catch(Exception e) {
                Trace.error(e.toString());
                return false;
            }
        }
    }

    /*public boolean addCars(int id, String location, int numCars, int price)
    {
        synchronized (m_carResourceManager) {
            Trace.info("addCars - Redirect to Car Resource Manager");
            try {
                return m_carResourceManager.addCars(id, location, numCars, price);
            } catch (IOException e) {
                connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name);
                return m_carResourceManager.addCars(id, location, numCars, price);
            } catch (Exception e) {
                Trace.error(e.toString());
                return false;
            }
        }

    }

    public boolean addRooms(int id, String location, int numRooms, int price)
    {
        synchronized (m_roomResourceManager) {
            Trace.info("addRooms - Redirect to Room Resource Manager");
            try {
                return m_roomResourceManager.addRooms(id, location, numRooms, price);
            } catch (IOException e) {
                connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name);
                return m_roomResourceManager.addRooms(id, location, numRooms, price);
            } catch (Exception e) {
                Trace.error(e.toString());
                return false;
            }
        }
    }

    public boolean deleteFlight(int id, int flightNum)
    {
        synchronized (m_flightResourceManager) {
            Trace.info("deleteFlight - Redirect to Flight Resource Manager");
            try {
                return m_flightResourceManager.deleteFlight(id, flightNum);
            } catch (IOException e) {
                connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name);
                return m_flightResourceManager.deleteFlight(id, flightNum);
            } catch (Exception e) {
                Trace.error(e.toString());
                return false;
            }
        }
    }

    public boolean deleteCars(int id, String location)
    {
        synchronized (m_carResourceManager) {
            Trace.info("deleteCars - Redirect to Car Resource Manager");
            try {
                return m_carResourceManager.deleteCars(id, location);
            } catch (IOException e) {
                connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name);
                return m_carResourceManager.deleteCars(id, location);
            } catch (Exception e) {
                Trace.error(e.toString());
                return false;
            }
        }
    }

    public boolean deleteRooms(int id, String location)
    {
        synchronized (m_roomResourceManager) {
            Trace.info("deleteRooms - Redirect to Room Resource Manager");
            try {
                return m_roomResourceManager.deleteRooms(id, location);
            } catch (IOException e) {
                connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name);
                return m_roomResourceManager.deleteRooms(id, location);
            } catch (Exception e) {
                Trace.error(e.toString());
                return false;
            }
        }
    }

    public boolean deleteCustomer(int xid, int customerID)
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
                if (type.equals("flight")) {
                    synchronized (m_flightResourceManager) {
                        m_flightResourceManager.removeReservation(xid, customerID, reserveditem.getKey(), reserveditem.getCount());
                    }
                }
                else if (type.equals("car")) {
                    synchronized (m_carResourceManager) {
                        m_carResourceManager.removeReservation(xid, customerID, reserveditem.getKey(), reserveditem.getCount());
                    }
                }
                else if (type.equals("room")) {
                    synchronized (m_roomResourceManager) {
                        m_roomResourceManager.removeReservation(xid, customerID, reserveditem.getKey(), reserveditem.getCount());
                    }
                }
                else
                    Trace.error("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--reservedKey (" + reservedKey + ") wasn't of expected type.");
            }
            // Remove the customer from the storage
            removeData(xid, customer.getKey());
            Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
            return true;
        }

    }

    public int queryFlight(int id, int flightNumber)
    {
        Trace.info("queryFlight - Redirect to Flight Resource Manager");
        try {
            return m_flightResourceManager.queryFlight(id, flightNumber);
        } catch (IOException e) {
            connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name);
            return m_flightResourceManager.queryFlight(id, flightNumber);
        } catch (Exception e) {
            Trace.error(e.toString());
            return -1;
        }
    }

    public int queryCars(int id, String location)
    {
        Trace.info("queryCars - Redirect to Car Resource Manager");
        try {
            return m_carResourceManager.queryCars(id, location);
        } catch (IOException e) {
            connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name);
            return m_carResourceManager.queryCars(id, location);
        } catch (Exception e) {
            Trace.error(e.toString());
            return -1;
        }
    }

    public int queryRooms(int id, String location)
    {
        Trace.info("queryRooms - Redirect to Room Resource Manager");
        try {
            return m_roomResourceManager.queryRooms(id, location);
        } catch (IOException e) {
            connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name);
            return m_roomResourceManager.queryRooms(id, location);
        } catch (Exception e) {
            Trace.error(e.toString());
            return -1;
        }
    }

    public int queryFlightPrice(int id, int flightNumber)
    {
        Trace.info("queryFlightPrice - Redirect to Flight Resource Manager");
        try {
            return m_flightResourceManager.queryFlightPrice(id, flightNumber);
        } catch (IOException e) {
            connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name);
            return m_flightResourceManager.queryFlightPrice(id, flightNumber);
        } catch (Exception e) {
            Trace.error(e.toString());
            return -1;
        }
    }

    public int queryCarsPrice(int id, String location)
    {
        Trace.info("queryCarsPrice - Redirect to Car Resource Manager");
        try {
            return m_carResourceManager.queryCarsPrice(id, location);
        } catch (IOException e) {
            connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name);
            return m_carResourceManager.queryCarsPrice(id, location);
        } catch (Exception e) {
            Trace.error(e.toString());
            return -1;
        }
    }

    public int queryRoomsPrice(int id, String location)
    {
        Trace.info("queryRoomsPrice - Redirect to Room Resource Manager");
        try {
            return m_roomResourceManager.queryRoomsPrice(id, location);
        } catch (IOException e) {
            connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name);
            return m_roomResourceManager.queryRoomsPrice(id, location);
        } catch (Exception e) {
            Trace.error(e.toString());
            return -1;
        }
    }

    public boolean reserveFlight(int xid, int customerID, int flightNumber)
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

    public boolean reserveCar(int xid, int customerID, String location)
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

    public boolean reserveRoom(int xid, int customerID, String location)
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

    public boolean bundle(int xid, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room)
    {
        Trace.info("RM::bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ") called" );
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }

        HashMap<String, Integer> countMap = countFlights(flightNumbers);
        HashMap<Integer, Integer> flightPrice = new HashMap<Integer, Integer>();
        int carPrice;
        int roomPrice;

        synchronized (m_flightResourceManager) {

            // Check availability
            for (String key : countMap.keySet()) {
                int keyInt;

                try {
                    keyInt = Integer.parseInt(key);
                }
                catch(Exception e) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--could not parse flightNumber");
                    return false;
                }

                int price = m_flightResourceManager.itemsAvailable(xid,Flight.getKey(keyInt),countMap.get(key));

                if (price < 0) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + key + " doesn't have enough spots");
                    return false;
                }
                else {
                    flightPrice.put(keyInt,price);
                }
            }

            if (car && room) {
                synchronized (m_carResourceManager) {

                    carPrice = m_carResourceManager.itemsAvailable(xid,Car.getKey(location),1);

                    if (carPrice < 0) {
                        Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--car-" + location + " doesn't have enough spots");
                        return false;
                    }
                    synchronized (m_roomResourceManager) {

                        roomPrice = m_roomResourceManager.itemsAvailable(xid,Room.getKey(location),1);

                        if (roomPrice < 0) {
                            Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--room-" + location + " doesn't have enough spots");
                            return false;
                        }

                        m_roomResourceManager.reserveRoom(xid, customerID, location);
                        customer.reserve(Room.getKey(location), location, roomPrice);
                        writeData(xid, customer.getKey(), customer);
                    }

                    m_carResourceManager.reserveCar(xid, customerID, location);
                    customer.reserve(Car.getKey(location), location, carPrice);
                    writeData(xid, customer.getKey(), customer);

                }

            }
            else if (car) {
                synchronized (m_carResourceManager) {

                    carPrice = m_carResourceManager.itemsAvailable(xid,Car.getKey(location),1);

                    if (carPrice < 0) {
                        Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--car-" + location + " doesn't have enough spots");
                        return false;
                    }
                    m_carResourceManager.reserveCar(xid, customerID, location);
                    customer.reserve(Car.getKey(location), location, carPrice);
                    writeData(xid, customer.getKey(), customer);

                }
            }
            else if (room) {
                synchronized (m_roomResourceManager) {

                    roomPrice = m_roomResourceManager.itemsAvailable(xid,Room.getKey(location),1);

                    if (roomPrice < 0) {
                        Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--room-" + location + " doesn't have enough spots");
                        return false;
                    }

                    m_roomResourceManager.reserveRoom(xid, customerID, location);
                    customer.reserve(Room.getKey(location), location, roomPrice);
                    writeData(xid, customer.getKey(), customer);
                }
            }

            // Reserve
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



    }*/

    public String getName() {
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

    private boolean toBool(String s) throws Exception {
        return Boolean.parseBoolean(s);
    }

}