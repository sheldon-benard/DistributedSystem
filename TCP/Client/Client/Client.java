package Client;

import java.util.*;
import java.net.*;
import java.io.*;
import java.lang.StringBuilder;

public class Client
{
	private TCPClient connection;

	public Client(TCPClient tcp)
	{
		this.connection = tcp;
	}


	public void start()
	{
		// Prepare for reading commands
		System.out.println();
		System.out.println("Location \"help\" for list of supported commands");

		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		while (true)
		{
			// Read the next command
			String command = "";
			Vector<String> arguments = new Vector<String>();
			try {
				System.out.print((char)27 + "[32;1m\n>] " + (char)27 + "[0m");
				command = stdin.readLine().trim();
			}
			catch (IOException io) {
				System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0m" + io.getLocalizedMessage());
				io.printStackTrace();
				System.exit(1);
			}



			try {
				arguments = parse(command);
				Command cmd = Command.fromString((String)arguments.elementAt(0));
				try {
					execute(cmd, arguments);
				}
				catch (IOException e) {
					connection.connect(true);
					execute(cmd, arguments);
				}
			}
			catch (IllegalArgumentException e) {
				System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0m" + e.getLocalizedMessage());
			}
			catch (Exception e) {
				System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mUncaught exception");
				e.printStackTrace();
			}
		}
	}

	public void execute(Command cmd, Vector<String> arguments) throws NumberFormatException,IOException
	{
		switch (cmd)
		{
			case Help:
			{
				if (arguments.size() == 1) {
					System.out.println(Command.description());
				} else if (arguments.size() == 2) {
					Command l_cmd = Command.fromString((String)arguments.elementAt(1));
					System.out.println(l_cmd.toString());
				} else {
					System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mImproper use of help command. Location \"help\" or \"help,<CommandName>\"");
				}
				break;
			}
			case AddFlight: {
				checkArgumentsCount(5, arguments.size());

				System.out.println("Adding a new flight [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));
				System.out.println("-Flight Seats: " + arguments.elementAt(3));
				System.out.println("-Flight Price: " + arguments.elementAt(4));

				int id = toInt(arguments.elementAt(1));
				int flightNum = toInt(arguments.elementAt(2));
				int flightSeats = toInt(arguments.elementAt(3));
				int flightPrice = toInt(arguments.elementAt(4));

				send(connection, arguments.toString(), "Flight added", "Flight could not be added", 'B');
				break;
			}
			case AddCars: {
				checkArgumentsCount(5, arguments.size());

				System.out.println("Adding new cars [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));
				System.out.println("-Number of Cars: " + arguments.elementAt(3));
				System.out.println("-Car Price: " + arguments.elementAt(4));

				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);
				int numCars = toInt(arguments.elementAt(3));
				int price = toInt(arguments.elementAt(4));

				send(connection, arguments.toString(), "Cars added", "Cars could not be added", 'B');
				break;
			}
			case AddRooms: {
				checkArgumentsCount(5, arguments.size());

				System.out.println("Adding new rooms [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Room Location: " + arguments.elementAt(2));
				System.out.println("-Number of Rooms: " + arguments.elementAt(3));
				System.out.println("-Room Price: " + arguments.elementAt(4));

				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);
				int numRooms = toInt(arguments.elementAt(3));
				int price = toInt(arguments.elementAt(4));

				send(connection, arguments.toString(), "Rooms added", "Rooms could not be added", 'B');
				break;
			}
			case AddCustomer: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");

				int id = toInt(arguments.elementAt(1));

				send(connection, arguments.toString(), "Add Customer ID: ", "Customer could not be added", 'I');
				break;
			}
			case AddCustomerID: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));

				send(connection, arguments.toString(), "Add Customer ID: " + customerID, "Customer could not be added", 'B');
				break;
			}
			case DeleteFlight: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Deleting a flight [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				int flightNum = toInt(arguments.elementAt(2));

				send(connection, arguments.toString(), "Flight Deleted", "Flight could not be deleted", 'B');
				break;
			}
			case DeleteCars: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Deleting all cars at a particular location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				send(connection, arguments.toString(), "Cars Deleted", "Cars could not be deleted", 'B');
				break;
			}
			case DeleteRooms: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Deleting all rooms at a particular location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				send(connection, arguments.toString(), "Rooms Deleted", "Rooms could not be deleted", 'B');
				break;
			}
			case DeleteCustomer: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Deleting a customer from the database [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				
				int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));

				send(connection, arguments.toString(), "Customer Deleted", "Customer could not be deleted", 'B');
				break;
			}
			case QueryFlight: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying a flight [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));
				
				int id = toInt(arguments.elementAt(1));
				int flightNum = toInt(arguments.elementAt(2));

				send(connection, arguments.toString(), "Number of seats available: ", "Could not query number of seats", 'I');
				break;
			}
			case QueryCars: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying cars location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));
				
				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				send(connection, arguments.toString(), "Number of cars at this location: ", "Could not query number of cars", 'I');
				break;
			}
			case QueryRooms: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying rooms location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Room Location: " + arguments.elementAt(2));
				
				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				send(connection, arguments.toString(), "Number of rooms at this location: ", "Could not query number of rooms", 'I');
				break;
			}
			case QueryCustomer: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying customer information [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));

				send(connection, arguments.toString(), "", "Could not query customer", 'S');
				break;
			}
			case QueryFlightPrice: {
				checkArgumentsCount(3, arguments.size());
				
				System.out.println("Querying a flight price [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				int flightNum = toInt(arguments.elementAt(2));

				send(connection, arguments.toString(), "Price of a seat: ", "Could not query price of seat", 'I');
				break;
			}
			case QueryCarsPrice: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying cars price [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				send(connection, arguments.toString(), "Price of cars at this location: ", "Could not query price of cars", 'I');
				break;
			}
			case QueryRoomsPrice: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying rooms price [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Room Location: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				send(connection, arguments.toString(), "Price of rooms at this location: ", "Could not query price of rooms", 'I');
				break;
			}
			case ReserveFlight: {
				checkArgumentsCount(4, arguments.size());

				System.out.println("Reserving seat in a flight [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				System.out.println("-Flight Number: " + arguments.elementAt(3));

				int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));
				int flightNum = toInt(arguments.elementAt(3));

				send(connection, arguments.toString(), "Flight Reserved", "Flight could not be reserved", 'B');
				break;
			}
			case ReserveCar: {
				checkArgumentsCount(4, arguments.size());

				System.out.println("Reserving a car at a location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				System.out.println("-Car Location: " + arguments.elementAt(3));

				int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));
				String location = arguments.elementAt(3);

				send(connection, arguments.toString(), "Car Reserved", "Car could not be reserved", 'B');
				break;
			}
			case ReserveRoom: {
				checkArgumentsCount(4, arguments.size());

				System.out.println("Reserving a room at a location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				System.out.println("-Room Location: " + arguments.elementAt(3));
				
				int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));
				String location = arguments.elementAt(3);

				send(connection, arguments.toString(), "Room Reserved", "Room could not be reserved", 'B');
				break;
			}
			case Bundle: {
				if (arguments.size() < 7) {
					System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mBundle command expects at least 7 arguments. Location \"help\" or \"help,<CommandName>\"");
					break;
				}

				System.out.println("Reserving an bundle [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));

				for (int i = 0; i < arguments.size() - 6; ++i)
				{
					System.out.println("-Flight Number: " + arguments.elementAt(3+i));
				}
				System.out.println("-Car Location: " + arguments.elementAt(arguments.size()-3));
				System.out.println("-Room Location: " + arguments.elementAt(arguments.size()-3));

				StringBuilder s = new StringBuilder();
				s.append("[Bundle,");
				// ID
				s.append(toInt(arguments.elementAt(1)) + ",");

				// Customer ID
				s.append(toInt(arguments.elementAt(2)) + ",");

				Vector<String> flightNumbers = new Vector<String>();
				for (int i = 0; i < arguments.size() - 6; ++i)
				{
					s.append(toInt(arguments.elementAt(3+i)) + ",");
				}

				// Location
				s.append(arguments.elementAt(arguments.size()-3) + ",");

				// T/F reserve Car
				s.append(toBoolean(arguments.elementAt(arguments.size()-2)) + ",");

				// T/F reserve room
				s.append(toBoolean(arguments.elementAt(arguments.size()-1)) + "]");
				send(connection, s.toString(), "Bundle Reserved", "Bundle could not be reserved", 'B');
				break;

			}
			case Summary: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Printing a client-resource summary [xid=" + arguments.elementAt(1) + "]");

				int id = toInt(arguments.elementAt(1));

				send(connection, arguments.toString(), "", "Could not print summary", 'S');
				break;
			}
			case Analytics: {
				checkArgumentsCount(3, arguments.size());
				System.out.println("Printing a quantity summary [xid=" + arguments.elementAt(1) + "]");
				System.out.println("For all items with remaining quantities <= " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				int upperBound = toInt(arguments.elementAt(2));

				send(connection, arguments.toString(), "", "Could not print quantities", 'S');
				break;
			}
			case Quit:
				checkArgumentsCount(1, arguments.size());
				connection.stopTCPClient();
				System.out.println("Quitting client");
				System.exit(0);
		}
	}

	public static void send(TCPClient conn, String args, String success, String failure, char responseType) throws IOException {
		String response = conn.sendMessage(args);
		//System.out.println("Response: " + response);
		if (response.equals("")) {
			conn.connect(false);
			response = conn.sendMessage(args);
		}

		if (response != null) {
			try {
				if (responseType == 'B' && toBoolean(response)) {
					System.out.println(success);
					return;
				}
				else if (responseType == 'I') {
					System.out.println(success + toInt(response));
					return;
				}
				else if (responseType == 'S') {
					System.out.println(success + response);
					return;
				}
			}catch(Exception e) {
				System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0m" + e.getLocalizedMessage());
			}
		}
		System.out.println(failure);
	}


	public static Vector<String> parse(String command)
	{
		Vector<String> arguments = new Vector<String>();
		StringTokenizer tokenizer = new StringTokenizer(command,",");
		String argument = "";
		while (tokenizer.hasMoreTokens())
		{
			argument = tokenizer.nextToken();
			argument = argument.trim();
			arguments.add(argument);
		}
		return arguments;
	}

	public static void checkArgumentsCount(Integer expected, Integer actual) throws IllegalArgumentException
	{
		if (expected != actual)
		{
			throw new IllegalArgumentException("Invalid number of arguments. Expected " + (expected - 1) + ", received " + (actual - 1) + ". Location \"help,<CommandName>\" to check usage of this command");
		}
	}

	public static int toInt(String string) throws NumberFormatException
	{
		return (new Integer(string)).intValue();
	}

	public static boolean toBoolean(String string)// throws Exception
	{
		return (string.equals("1") || string.equalsIgnoreCase("true")) ;
	}
}
