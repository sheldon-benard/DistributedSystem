package Server.Middleware;

import Server.Common.*;
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
        return toBool(send(m_flightResourceManager,'B',command,true));
    }

    public boolean addCars(int id, String location, int numCars, int price)
    {
        Trace.info("addCars - Redirect to Car Resource Manager");
        String command = String.format("AddCars,%d,%s,%d,%d",id,location,numCars,price);
        return toBool(send(m_carResourceManager,'B',command,true));
    }

    public boolean addRooms(int id, String location, int numRooms, int price)
    {
        Trace.info("addRooms - Redirect to Room Resource Manager");
        String command = String.format("AddRooms,%d,%s,%d,%d",id,location,numRooms,price);
        return toBool(send(m_roomResourceManager,'B',command,true));
    }

    public boolean deleteFlight(int id, int flightNum)
    {
        Trace.info("deleteFlight - Redirect to Flight Resource Manager");
        String command = String.format("DeleteFlight,%d,%d",id,flightNum);
        return toBool(send(m_flightResourceManager,'B',command,true));
    }

    public boolean deleteCars(int id, String location)
    {
        Trace.info("deleteCars - Redirect to Car Resource Manager");
        String command = String.format("DeleteCars,%d,%s",id,location);
        return toBool(send(m_carResourceManager,'B',command,true));
    }

    public boolean deleteRooms(int id, String location)
    {
        Trace.info("deleteRooms - Redirect to Room Resource Manager");
        String command = String.format("DeleteRooms,%d,%s",id,location);
        return toBool(send(m_roomResourceManager,'B',command,true));
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
            synchronized (customer) {
                // Increase the reserved numbers of all reservable items which the customer reserved.
                RMHashMap reservations = customer.getReservations();
                for (String reservedKey : reservations.keySet()) {
                    String type = reservedKey.split("-")[0];
                    ReservedItem reserveditem = customer.getReservedItem(reservedKey);
                    String command = String.format("RemoveReservation,%d,%d,%s,%d", xid, customerID, reserveditem.getKey(), reserveditem.getCount());

                    if (type.equals("flight")) {
                        synchronized (m_flightResourceManager) {
                            try {
                                m_flightResourceManager.sendMessage(command);
                            } catch (Exception e) {
                            }
                        }
                    } else if (type.equals("car")) {
                        synchronized (m_carResourceManager) {
                            try {
                                m_carResourceManager.sendMessage(command);
                            } catch (Exception e) {
                            }
                        }
                    } else if (type.equals("room")) {
                        synchronized (m_roomResourceManager) {
                            try {
                                m_roomResourceManager.sendMessage(command);
                            } catch (Exception e) {
                            }
                        }
                    } else
                        Trace.error("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--reservedKey (" + reservedKey + ") wasn't of expected type.");
                }
                // Remove the customer from the storage
                removeData(xid, customer.getKey());
                Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
            }
            return true;
        }

    }

    public int queryFlight(int id, int flightNumber)
    {
        Trace.info("queryFlight - Redirect to Flight Resource Manager");
        String command = String.format("QueryFlight,%d,%d",id,flightNumber);
        return toInt(send(m_flightResourceManager,'I',command,false));
    }

    public int queryCars(int id, String location)
    {
        Trace.info("queryCars - Redirect to Car Resource Manager");
        String command = String.format("QueryCars,%d,%s",id,location);
        return toInt(send(m_carResourceManager,'I',command,false));
    }

    public int queryRooms(int id, String location)
    {
        Trace.info("queryRooms - Redirect to Room Resource Manager");
        String command = String.format("QueryRooms,%d,%s",id,location);
        return toInt(send(m_roomResourceManager,'I',command,false));
    }

    public int queryFlightPrice(int id, int flightNumber)
    {
        Trace.info("queryFlightPrice - Redirect to Flight Resource Manager");
        String command = String.format("QueryFlightPrice,%d,%d",id,flightNumber);
        return toInt(send(m_flightResourceManager,'I',command,false));
    }

    public int queryCarsPrice(int id, String location)
    {
        Trace.info("queryCarsPrice - Redirect to Car Resource Manager");
        String command = String.format("QueryCarsPrice,%d,%s",id,location);
        return toInt(send(m_carResourceManager,'I',command,false));
    }

    public int queryRoomsPrice(int id, String location)
    {
        Trace.info("queryRoomsPrice - Redirect to Room Resource Manager");
        String command = String.format("QueryRoomsPrice,%d,%s",id,location);
        return toInt(send(m_roomResourceManager,'I',command,false));
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
        synchronized (customer) {
            synchronized (m_flightResourceManager) {
                int price = -1;
                try {
                    price = toInt(m_flightResourceManager.sendMessage(String.format("ItemsAvailable,%d,%s,%d", xid, key, 1)));
                } catch (Exception e) {
                }


                if (price < 0) {
                    Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + flightNumber + ")  failed--item unavailable");
                    return false;
                }

                boolean reserved = false;

                try {
                    String c = String.format("ReserveFlight,%d,%d,%d", xid, customerID, flightNumber);
                    reserved = toBool(m_flightResourceManager.sendMessage(c));
                } catch (Exception e) {
                }

                if (reserved) {
                    customer.reserve(key, String.valueOf(flightNumber), price);
                    writeData(xid, customer.getKey(), customer);
                    return true;
                }
                Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + flightNumber + ")  failed--Could not reserve item");
                return false;
            }
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
        synchronized (customer) {
            synchronized (m_carResourceManager) {
                int price = -1;
                try {
                    price = toInt(m_carResourceManager.sendMessage(String.format("ItemsAvailable,%d,%s,%d", xid, key, 1)));
                } catch (Exception e) {
                }

                if (price < 0) {
                    Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--item unavailable");
                    return false;
                }

                boolean reserved = false;

                try {
                    reserved = toBool(m_carResourceManager.sendMessage(String.format("ReserveCar,%d,%d,%s", xid, customerID, location)));
                } catch (Exception e) {
                }

                if (reserved) {
                    customer.reserve(key, location, price);
                    writeData(xid, customer.getKey(), customer);
                    return true;
                }
                Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--Could not reserve item");
                return false;
            }
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
        synchronized (customer) {
            synchronized (m_roomResourceManager) {
                int price = -1;
                try {
                    price = toInt(m_roomResourceManager.sendMessage(String.format("ItemsAvailable,%d,%s,%d", xid, key, 1)));
                } catch (Exception e) {
                }

                if (price < 0) {
                    Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--item unavailable");
                    return false;
                }

                boolean reserved = false;

                try {
                    reserved = toBool(m_roomResourceManager.sendMessage(String.format("ReserveRoom,%d,%d,%s", xid, customerID, location)));
                } catch (Exception e) {
                }

                if (reserved) {
                    customer.reserve(key, location, price);
                    writeData(xid, customer.getKey(), customer);
                    return true;
                }
                Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--Could not reserve item");
                return false;
            }
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
        synchronized (customer) {
            HashMap<String, Integer> countMap = countFlights(flightNumbers);
            HashMap<Integer, Integer> flightPrice = new HashMap<Integer, Integer>();
            int carPrice = -1;
            int roomPrice = -1;

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

                                int price = -1;
                                try {
                                    price = toInt(m_flightResourceManager.sendMessage(String.format("ItemsAvailable,%d,%s,%d", xid, Flight.getKey(keyInt), countMap.get(key))));
                                } catch (Exception e) {
                                }

                                if (price < 0) {
                                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + key + " doesn't have enough spots");
                                    return false;
                                } else {
                                    flightPrice.put(keyInt, price);
                                }
                            }

                            try {
                                carPrice = toInt(m_carResourceManager.sendMessage(String.format("ItemsAvailable,%d,%s,%d", xid, Car.getKey(location), 1)));
                            } catch (Exception e) {
                            }

                            if (carPrice < 0) {
                                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--car-" + location + " doesn't have enough spots");
                                return false;
                            }


                            try {
                                roomPrice = toInt(m_roomResourceManager.sendMessage(String.format("ItemsAvailable,%d,%s,%d", xid, Room.getKey(location), 1)));
                            } catch (Exception e) {
                            }

                            if (roomPrice < 0) {
                                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--room-" + location + " doesn't have enough spots");
                                return false;
                            }

                            try {
                                toBool(m_roomResourceManager.sendMessage(String.format("ReserveRoom,%d,%d,%s", xid, customerID, location)));
                            } catch (Exception e) {
                            }

                            customer.reserve(Room.getKey(location), location, roomPrice);
                            writeData(xid, customer.getKey(), customer);


                            //m_carResourceManager.reserveCar(xid, customerID, location);

                            try {
                                toBool(m_carResourceManager.sendMessage(String.format("ReserveCar,%d,%d,%s", xid, customerID, location)));
                            } catch (Exception e) {
                            }
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

                            int price = -1;
                            try {
                                price = toInt(m_flightResourceManager.sendMessage(String.format("ItemsAvailable,%d,%s,%d", xid, Flight.getKey(keyInt), countMap.get(key))));
                            } catch (Exception e) {
                            }

                            if (price < 0) {
                                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + key + " doesn't have enough spots");
                                return false;
                            } else {
                                flightPrice.put(keyInt, price);
                            }
                        }

                        try {
                            carPrice = toInt(m_carResourceManager.sendMessage(String.format("ItemsAvailable,%d,%s,%d", xid, Car.getKey(location), 1)));
                        } catch (Exception e) {
                        }

                        if (carPrice < 0) {
                            Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--car-" + location + " doesn't have enough spots");
                            return false;
                        }
                        //m_carResourceManager.reserveCar(xid, customerID, location);
                        try {
                            toBool(m_carResourceManager.sendMessage(String.format("ReserveCar,%d,%d,%s", xid, customerID, location)));
                        } catch (Exception e) {
                        }
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

                            int price = -1;
                            try {
                                price = toInt(m_flightResourceManager.sendMessage(String.format("ItemsAvailable,%d,%s,%d", xid, Flight.getKey(keyInt), countMap.get(key))));
                            } catch (Exception e) {
                            }

                            if (price < 0) {
                                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + key + " doesn't have enough spots");
                                return false;
                            } else {
                                flightPrice.put(keyInt, price);
                            }
                        }

                        try {
                            roomPrice = toInt(m_roomResourceManager.sendMessage(String.format("ItemsAvailable,%d,%s,%d", xid, Room.getKey(location), 1)));
                        } catch (Exception e) {
                        }

                        if (roomPrice < 0) {
                            Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--room-" + location + " doesn't have enough spots");
                            return false;
                        }

                        //m_roomResourceManager.reserveRoom(xid, customerID, location);
                        try {
                            toBool(m_roomResourceManager.sendMessage(String.format("ReserveRoom,%d,%d,%s", xid, customerID, location)));
                        } catch (Exception e) {
                        }

                        customer.reserve(Room.getKey(location), location, roomPrice);
                        writeData(xid, customer.getKey(), customer);
                    }
                }
                else {
                    // Check flight availability
                    for (String key : countMap.keySet()) {
                        int keyInt;

                        try {
                            keyInt = Integer.parseInt(key);
                        } catch (Exception e) {
                            Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--could not parse flightNumber");
                            return false;
                        }

                        int price = -1;
                        try {
                            price = toInt(m_flightResourceManager.sendMessage(String.format("ItemsAvailable,%d,%s,%d", xid, Flight.getKey(keyInt), countMap.get(key))));
                        } catch (Exception e) {
                        }

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

                        //m_flightResourceManager.reserveFlight(xid, customerID, key);
                        try {
                            toBool(m_flightResourceManager.sendMessage(String.format("ReserveFlight,%d,%d,%d", xid, customerID, key)));
                        } catch (Exception e) {
                        }
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
            summary += m_flightResourceManager.sendMessage(String.format("Analytics,%d,%d", xid, upperBound)) + "\n";
        } catch (Exception e) {
            summary += "Error in retrieving quantities\n";
        }

        summary += "Car Quantities\n";
        try {
            summary += m_carResourceManager.sendMessage(String.format("Analytics,%d,%d", xid, upperBound)) + "\n";
        } catch (Exception e) {
            summary += "Error in retrieving quantities\n";
        }

        summary += "Room Quantities\n";
        try {
            summary += m_roomResourceManager.sendMessage(String.format("Analytics,%d,%d", xid, upperBound)) + "\n";
        } catch (Exception e) {
            summary += "Error in retrieving quantities\n";
        }

        return summary;
    }


    private String send(TCPClient comm, char returnType, String command, boolean sync) {
        String res;
        try {
            if (sync) {
                synchronized (comm) {
                    try {
                        res = comm.sendMessage(command);
                        if (res.equals(""))
                            throw new IOException();
                        return res;
                    } catch (IOException e) {
                        comm.connect();
                        return comm.sendMessage(command);
                    }
                }
            }
            else {
                try {
                    res = comm.sendMessage(command);
                    if (res.equals(""))
                        throw new IOException();
                    return res;
                } catch (IOException e) {
                    comm.connect();
                    return comm.sendMessage(command);
                }
            }

        } catch(Exception e) {
            Trace.error(e.toString());
            if (returnType == 'B')
                return "false";
            else if (returnType == 'I')
                return "-1";
            else
                return "";
        }
    }

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

    private boolean toBool(String s) {
        try {
            return Boolean.parseBoolean(s);
        } catch (Exception e) {
            System.out.println("toBool exception: " + e.getLocalizedMessage());
            return false;
        }
    }

    private int toInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            System.out.println("toInt exception: " + e.getLocalizedMessage());
            return -1;
        }
    }

}