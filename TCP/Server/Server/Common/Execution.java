package Server.Common;

import java.util.*;

public class Execution {

    private static String defaultString = "";
    private static String defaultInt = "-1";
    private static String defaultBool = "false";

    public static String execute(ResourceManager manager, Vector<String> command) {
        char type = 'S';

        try {
            switch (command.get(0).toLowerCase()) {
                case "addflight": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    int flightNumber = Integer.parseInt(command.get(2));
                    int num = Integer.parseInt(command.get(3));
                    int price = Integer.parseInt(command.get(4));
                    return Boolean.toString(manager.addFlight(xid, flightNumber, num, price));
                }
                case "addcars": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    int num = Integer.parseInt(command.get(3));
                    int price = Integer.parseInt(command.get(4));
                    return Boolean.toString(manager.addCars(xid, location, num, price));
                }
                case "addrooms": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    int num = Integer.parseInt(command.get(3));
                    int price = Integer.parseInt(command.get(4));
                    return Boolean.toString(manager.addRooms(xid, location, num, price));
                }
                case "addcustomer": {
                    type = 'I';
                    int xid = Integer.parseInt(command.get(1));
                    return Integer.toString(manager.newCustomer(xid));
                }
                case "addcustomerid": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    int id = Integer.parseInt(command.get(2));
                    return Boolean.toString(manager.newCustomer(xid, id));
                }
                case "deleteflight": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    int flightNum = Integer.parseInt(command.get(2));
                    return Boolean.toString(manager.deleteFlight(xid, flightNum));
                }
                case "deletecars": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    return Boolean.toString(manager.deleteCars(xid, location));
                }
                case "deleterooms": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    return Boolean.toString(manager.deleteRooms(xid, location));
                }
                case "deletecustomer": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    int customerID = Integer.parseInt(command.get(2));
                    return Boolean.toString(manager.deleteCustomer(xid, customerID));
                }
                case "queryflight": {
                    type = 'I';
                    int xid = Integer.parseInt(command.get(1));
                    int flightNum = Integer.parseInt(command.get(2));
                    return Integer.toString(manager.queryFlight(xid, flightNum));
                }
                case "querycars": {
                    type = 'I';
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    return Integer.toString(manager.queryCars(xid, location));
                }
                case "queryrooms": {
                    type = 'I';
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    return Integer.toString(manager.queryRooms(xid, location));
                }
                case "querycustomer": {
                    type = 'S';
                    int xid = Integer.parseInt(command.get(1));
                    int customerID = Integer.parseInt(command.get(2));
                    return manager.queryCustomerInfo(xid, customerID);
                }
                case "summary": {
                    type = 'S';
                    int xid = Integer.parseInt(command.get(1));
                    return manager.Summary(xid);
                }
                case "analytics": {
                    type = 'S';
                    int xid = Integer.parseInt(command.get(1));
                    int upperBound = Integer.parseInt(command.get(2));
                    return manager.Analytics(xid,upperBound);
                }
                case "queryflightprice": {
                    type = 'I';
                    int xid = Integer.parseInt(command.get(1));
                    int flightNum = Integer.parseInt(command.get(2));
                    return Integer.toString(manager.queryFlightPrice(xid, flightNum));
                }
                case "querycarsprice": {
                    type = 'I';
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    return Integer.toString(manager.queryCarsPrice(xid, location));
                }
                case "queryroomsprice": {
                    type = 'I';
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    return Integer.toString(manager.queryRoomsPrice(xid, location));
                }
                case "reserveflight": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    int customerID = Integer.parseInt(command.get(2));
                    int flightNum = Integer.parseInt(command.get(3));
                    return Boolean.toString(manager.reserveFlight(xid, customerID, flightNum));
                }
                case "reservecar": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    int customerID = Integer.parseInt(command.get(2));
                    String location = command.get(3);
                    return Boolean.toString(manager.reserveCar(xid, customerID, location));
                }
                case "reserveroom": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    int customerID = Integer.parseInt(command.get(2));
                    String location = command.get(3);
                    return Boolean.toString(manager.reserveRoom(xid, customerID, location));
                }
                case "bundle": {
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
                case "removereservation": {
                    type = 'B';
                    int xid = Integer.parseInt(command.get(1));
                    int customerID = Integer.parseInt(command.get(2));
                    String reserveditemKey = command.get(3);
                    int reserveditemCount = Integer.parseInt(command.get(4));

                    return Boolean.toString(manager.removeReservation(xid, customerID, reserveditemKey, reserveditemCount));
                }
                case "itemsavailable": {
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