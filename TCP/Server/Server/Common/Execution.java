package Server.Common;

import java.util.*;

public class Execution {

    private static String defaultString = "";
    private static String defaultInt = "-1";
    private static String defaultBool = "false";

    public static String execute(ResourceManager manager, Vector<String> command) {
        char type = 'S';

        try {
            switch (command.get(0)) {
                case "AddFlight": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    int flightNumber = Integer.parseInt(command.get(2));
                    int num = Integer.parseInt(command.get(3));
                    int price = Integer.parseInt(command.get(4));
                    return Boolean.toString(manager.addFlight(xid, flightNumber, num, price));
                }
                case "AddCars": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    int num = Integer.parseInt(command.get(3));
                    int price = Integer.parseInt(command.get(4));
                    return Boolean.toString(manager.addCars(xid, location, num, price));
                }
                case "AddRooms": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    int num = Integer.parseInt(command.get(3));
                    int price = Integer.parseInt(command.get(4));
                    return Boolean.toString(manager.addRooms(xid, location, num, price));
                }
                case "AddCustomer": {
                    type = 'I';
                    int xid = Integer.parseInt(command.get(1));
                    return Integer.toString(manager.newCustomer(xid));
                }
                case "AddCustomerID": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    int id = Integer.parseInt(command.get(2));
                    return Boolean.toString(manager.newCustomer(xid, id));
                }
                case "DeleteFlight": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    int flightNum = Integer.parseInt(command.get(2));
                    return Boolean.toString(manager.deleteFlight(xid, flightNum));
                }
                case "DeleteCars": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    return Boolean.toString(manager.deleteCars(xid, location));
                }
                case "DeleteRooms": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    return Boolean.toString(manager.deleteRooms(xid, location));
                }
                case "DeleteCustomer": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    int customerID = Integer.parseInt(command.get(2));
                    return Boolean.toString(manager.deleteCustomer(xid, customerID));
                }
                case "QueryFlight": {
                    type = 'I';
                    int xid = Integer.parseInt(command.get(1));
                    int flightNum = Integer.parseInt(command.get(2));
                    return Integer.toString(manager.queryFlight(xid, flightNum));
                }
                case "QueryCars": {
                    type = 'I';
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    return Integer.toString(manager.queryCars(xid, location));
                }
                case "QueryRooms": {
                    type = 'I';
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    return Integer.toString(manager.queryRooms(xid, location));
                }
                case "QueryCustomer": {
                    type = 'S';
                    int xid = Integer.parseInt(command.get(1));
                    int customerID = Integer.parseInt(command.get(2));
                    return manager.queryCustomerInfo(xid, customerID);
                }
                case "QueryFlightPrice": {
                    type = 'I';
                    int xid = Integer.parseInt(command.get(1));
                    int flightNum = Integer.parseInt(command.get(2));
                    return Integer.toString(manager.queryFlightPrice(xid, flightNum));
                }
                case "QueryCarsPrice": {
                    type = 'I';
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    return Integer.toString(manager.queryCarsPrice(xid, location));
                }
                case "QueryRoomsPrice": {
                    type = 'I';
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    return Integer.toString(manager.queryRoomsPrice(xid, location));
                }
                case "ReserveFlight": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    int customerID = Integer.parseInt(command.get(2));
                    int flightNum = Integer.parseInt(command.get(3));
                    return Boolean.toString(manager.reserveFlight(xid, customerID, flightNum));
                }
                case "ReserveCar": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    int customerID = Integer.parseInt(command.get(2));
                    String location = command.get(3);
                    return Boolean.toString(manager.reserveCar(xid, customerID, location));
                }
                case "ReserveRoom": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    int customerID = Integer.parseInt(command.get(2));
                    String location = command.get(3);
                    return Boolean.toString(manager.reserveRoom(xid, customerID, location));
                }
                case "Bundle": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    int customerID = Integer.parseInt(command.get(2));

                    Vector<String> flightNumbers = new Vector<String>();
                    for (int i = 0; i < command.size() - 6; ++i) {
                        flightNumbers.add(command.elementAt(3 + i));
                    }

                    // Location
                    String location = command.get(command.size() - 3);
                    boolean car = toBoolean(command.get(command.size() - 2));
                    boolean room = toBoolean(command.get(command.size() - 1));

                    return Boolean.toString(manager.bundle(xid, customerID, flightNumbers, location, car, room));
                }
                case "RemoveReservation": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    int customerID = Integer.parseInt(command.get(2));
                    String reserveditemKey = command.get(3);
                    int reserveditemCount = Integer.parseInt(command.get(4));

                    return Boolean.toString(manager.removeReservation(xid, customerID, reserveditemKey, reserveditemCount));
                }
                case "ItemsAvailable": {
                    type = 'I';
                    int xid = Integer.parseInt(command.get(1));
                    String key = command.get(2);
                    int quantity = Integer.parseInt(command.get(3));

                    return Integer.toString(manager.itemsAvailable(xid, key, quantity));
                }
            }
        } catch(Exception e) {
            System.err.println((char)27 + "[31;1mExecution exception: " + (char)27 + "[0m" + e.getLocalizedMessage());
        }

        if (type == 'S')
            return defaultString;
        else if (type == 'B')
            return defaultBool;
        else
            return defaultInt;

    }

    private static boolean toBoolean(String string)
    {
        return (string.equals("1") || string.equalsIgnoreCase("true")) ;
    }



}