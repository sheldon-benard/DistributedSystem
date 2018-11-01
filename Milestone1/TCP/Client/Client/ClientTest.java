package Client;

import java.util.*;
import java.net.*;
import java.io.*;
import java.lang.StringBuilder;

public class ClientTest extends Client {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_YELLOW = "\u001B[33m";

    public ClientTest(TCPClient tcp)
    {
        super(tcp);
    }

    public void start() {
        String[][] tests = {
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
                {"Bundle,1,124,200,200,200,200,200,100,100,100,100,Montreal,1,0", "Customer-124 reserved 5xFlight-200, 4xFlight-100, 1xCar-Montreal"},

                {"Analytics,1,9", "Prints: Flight-100 @ 5"},
                {"Analytics,1,20", "Prints: Flight-100 @ 5, Flight-200 @ 15, Cars-Montreal @ 13, Rooms-Montreal @ 14" },

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
            } catch(Exception e) {
                System.out.println("Could not run test: " + test);
            }
        }


    }
}