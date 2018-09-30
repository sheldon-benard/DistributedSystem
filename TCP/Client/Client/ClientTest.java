package Client;

import java.util.*;
import java.net.*;
import java.io.*;
import java.lang.StringBuilder;

public class ClientTest extends Client {

    public ClientTest(TCPClient tcp)
    {
        super(tcp);
    }

    public void start() {
        String[] tests = new String[] {
                "Addflight,1,100,10,100",
                "Addflight,1,200,20,200",
                "Addflight,1,300,20,200",
                "AddCustomerID,1,123",
                "AddCustomerID,1,124",
                "AddCustomerID,1,125",
                "AddCars,1,Montreal,15,15",
                "AddRooms,1,Montreal,15,15",

                "DeleteFlight,1,300",
                "DeleteCustomer,1,125",
                "DeleteCars,1,Montreal",
                "DeleteRooms,1,Montreal",

                "QueryCars,1,Montreal",
                "QueryRooms,1,Montreal",

                "AddCars,1,Montreal,15,15",
                "AddRooms,1,Montreal,15,15",

                "QueryFlight,1,100",
                "QueryFlight,1,300",

                "QueryCustomer,1,123",
                "QueryCustomer,1,125",

                "QueryCars,1,Montreal",
                "QueryRooms,1,Montreal",

                "QueryFlightPrice,1,100",
                "QueryRoomsPrice,1,Montreal",
                "QueryCarsPrice,1,Montreal",

                "ReserveFlight,1,123,100",
                "DeleteFlight,1,100",

                "ReserveCar,1,123,Montreal",
                "ReserveRoom,1,123,Montreal",
                "Summary,1",

                "Analytics,1,0",
                "Analytics,1,1",
                "Analytics,1,20",

                "Quit"
        };
        Vector<String> arguments = new Vector<String>();

        for (String test : tests) {
            arguments = parse(test);
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