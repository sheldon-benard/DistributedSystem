// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import Server.Interface.*;
import Server.Transactions.*;

import java.util.*;
import java.rmi.RemoteException;
import java.io.*;

public class ResourceManager implements IResourceManager
{
	protected String m_name = "";
	protected RMHashMap m_data = new RMHashMap();
	protected TransactionManager tm;
	protected HashMap<Integer, Long> DB = new HashMap<Integer, Long>();

	public ResourceManager(String p_name)
	{
		m_name = p_name;
		tm = new TransactionManager();
	}

	protected void setTransactionManager(TransactionManager tm) {
		this.tm = tm;
	}

	public void addTransaction(int xid) throws RemoteException {
		Trace.info("RM::addTransaction(" + xid + ") called");
		if (!tm.xidActive(xid)) {
			Trace.info("Transaction added");
			Transaction t = new Transaction(xid);
			tm.writeActiveData(xid, t);
		}
	}


	// Reads a data item
	protected RMItem readData(int xid, String key) throws InvalidTransactionException
	{
		if(!tm.xidActive(xid))
			throw new InvalidTransactionException(xid, "Not a valid transaction");

		Transaction t = tm.readActiveData(xid);

		if (!t.hasData(key)) {
			synchronized (m_data) {
				long ta = curr();
				RMItem item = m_data.get(key);
				if (item != null) {
					t.writeData(xid, key, (RMItem) item.clone());
				}
				else {
					t.writeData(xid, key, null);
				}
				DB.put(xid, curr() - ta);
			}
		}

		return t.readData(xid, key);

	}

	// Writes a data item
	protected void writeData(int xid, String key, RMItem value) throws InvalidTransactionException
	{
		if(!tm.xidActive(xid))
			throw new InvalidTransactionException(xid, "Not a valid transaction");
		readData(xid, key); // this ensures that the data is copied in the transaction local copy

		Transaction t = tm.readActiveData(xid);
		t.writeData(xid, key, value);
	}

	// Remove the item out of storage
	protected void removeData(int xid, String key) throws InvalidTransactionException
	{
		if(!tm.xidActive(xid))
			throw new InvalidTransactionException(xid, "Not a valid transaction");
		readData(xid, key); // this ensures that the data is copied in the transaction local copy

		Transaction t = tm.readActiveData(xid);
		t.writeData(xid, key, null);
	}

	// Deletes the encar item
	protected boolean deleteItem(int xid, String key) throws InvalidTransactionException
	{
		Trace.info("RM::deleteItem(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		// Check if there is such an item in the storage
		if (curObj == null)
		{
			Trace.warn("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
			return false;
		}
		else
		{
			if (curObj.getReserved() == 0)
			{
				removeData(xid, curObj.getKey());
				Trace.info("RM::deleteItem(" + xid + ", " + key + ") item deleted");
				return true;
			}
			else
			{
				Trace.info("RM::deleteItem(" + xid + ", " + key + ") item can't be deleted because some customers have reserved it");
				return false;
			}
		}
	}

	// Query the number of available seats/rooms/cars
	protected int queryNum(int xid, String key) throws InvalidTransactionException
	{
		Trace.info("RM::queryNum(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		int value = 0;  
		if (curObj != null)
		{
			value = curObj.getCount();
		}
		Trace.info("RM::queryNum(" + xid + ", " + key + ") returns count=" + value);
		return value;
	}    

	// Query the price of an item
	protected int queryPrice(int xid, String key) throws InvalidTransactionException
	{
		Trace.info("RM::queryPrice(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		int value = 0; 
		if (curObj != null)
		{
			value = curObj.getPrice();
		}
		Trace.info("RM::queryPrice(" + xid + ", " + key + ") returns cost=$" + value);
		return value;        
	}

	// Reserve an item
	public boolean reserveItem(int xid, int customerID, String key, String location) throws InvalidTransactionException
	{
		if (itemsAvailable(xid, key,1) < 0)
			return false;
		ReservableItem item = (ReservableItem)readData(xid,key);
		// Decrease the number of available items in the storage
		item.setCount(item.getCount() - 1);
		item.setReserved(item.getReserved() + 1);
		writeData(xid, item.getKey(), item);

		Trace.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
		return true;

	}

	public int itemsAvailable(int xid, String key, int quantity) throws InvalidTransactionException {
		// Check if the item is available
		ReservableItem item = (ReservableItem)readData(xid, key);
		if (item == null)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + key + ") failed--item doesn't exist");
			return -1;
		}
		else if (item.getCount() < quantity)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + key + ") failed--Not enough items");
			return -1;
		}

		return item.getPrice();
	}

	public long curr() {
		return System.currentTimeMillis();
	}

	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
		Flight curObj = (Flight)readData(xid, Flight.getKey(flightNum));
		if (curObj == null)
		{
			// Doesn't exist yet, add it
			Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addFlight(" + xid + ") created new flight " + flightNum + ", seats=" + flightSeats + ", price=$" + flightPrice);
		}
		else
		{
			// Add seats to existing flight and update the price if greater than zero
			curObj.setCount(curObj.getCount() + flightSeats);
			if (flightPrice > 0)
			{
				curObj.setPrice(flightPrice);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addFlight(" + xid + ") modified existing flight " + flightNum + ", seats=" + curObj.getCount() + ", price=$" + flightPrice);
		}
		return true;
	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current price
	public boolean addCars(int xid, String location, int count, int price) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		Car curObj = (Car)readData(xid, Car.getKey(location));
		if (curObj == null)
		{
			// Car location doesn't exist yet, add it
			Car newObj = new Car(location, count, price);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addCars(" + xid + ") created new location " + location + ", count=" + count + ", price=$" + price);
		}
		else
		{
			// Add count to existing car location and update price if greater than zero
			curObj.setCount(curObj.getCount() + count);
			if (price > 0)
			{
				curObj.setPrice(price);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addCars(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
		}
		return true;
	}

	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public boolean addRooms(int xid, String location, int count, int price) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		Room curObj = (Room)readData(xid, Room.getKey(location));
		if (curObj == null)
		{
			// Room location doesn't exist yet, add it
			Room newObj = new Room(location, count, price);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addRooms(" + xid + ") created new room location " + location + ", count=" + count + ", price=$" + price);
		} else {
			// Add count to existing object and update price if greater than zero
			curObj.setCount(curObj.getCount() + count);
			if (price > 0)
			{
				curObj.setPrice(price);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addRooms(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
		}
		return true;
	}

	// Deletes flight
	public boolean deleteFlight(int xid, int flightNum) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return deleteItem(xid, Flight.getKey(flightNum));
	}

	// Delete cars at a location
	public boolean deleteCars(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return deleteItem(xid, Car.getKey(location));
	}

	// Delete rooms at a location
	public boolean deleteRooms(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return deleteItem(xid, Room.getKey(location));
	}

	// Returns the number of empty seats in this flight
	public long[] queryFlight(int xid, int flightNum) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		long x = 0;
		DB.put(xid, x);
		long t = curr();
		queryNum(xid, Flight.getKey(flightNum));

		return new long[] {curr() - t, DB.get(xid)};

	}

	// Returns the number of cars available at a location
	public int queryCars(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return queryNum(xid, Car.getKey(location));
	}

	// Returns the amount of rooms available at a location
	public int queryRooms(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return queryNum(xid, Room.getKey(location));
	}

	// Returns price of a seat in this flight
	public long[] queryFlightPrice(int xid, int flightNum) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		long x = 0;
		DB.put(xid, x);
		long t = curr();
		queryPrice(xid, Flight.getKey(flightNum));

		return new long[] {curr() - t, DB.get(xid)};
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return queryPrice(xid, Car.getKey(location));
	}

	// Returns room price at this location
	public int queryRoomsPrice(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return queryPrice(xid, Room.getKey(location));
	}

	public String queryCustomerInfo(int xid, int customerID) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("RM::queryCustomerInfo(" + xid + ", " + customerID + ") failed--customer doesn't exist");
			// NOTE: don't change this--WC counts on this value indicating a customer does not exist...
			return "";
		}
		else
		{
			Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ")");
			System.out.println(customer.getBill());
			return customer.getBill();
		}
	}

	public int newCustomer(int xid) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
        	Trace.info("RM::newCustomer(" + xid + ") called");
		// Generate a globally unique ID for the new customer
		int cid = Integer.parseInt(String.valueOf(xid) +
			String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
			String.valueOf(Math.round(Math.random() * 100 + 1)));
		Customer customer = new Customer(cid);
		writeData(xid, customer.getKey(), customer);
		Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
		return cid;
	}

	public boolean newCustomer(int xid, int customerID) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			customer = new Customer(customerID);
			writeData(xid, customer.getKey(), customer);
			Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") created a new customer");
			return true;
		}
		else
		{
			Trace.info("INFO: RM::newCustomer(" + xid + ", " + customerID + ") failed--customer already exists");
			return false;
		}
	}

	public boolean deleteCustomer(int xid, int customerID) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return false;
	}

	public boolean removeReservation(int xid, int customerID, String reserveditemKey, int reserveditemCount) throws RemoteException,TransactionAbortedException, InvalidTransactionException {
		Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditemKey + " " +  reserveditemCount +  " times");
		ReservableItem item  = (ReservableItem)readData(xid, reserveditemKey);
		Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditemKey + " which is reserved " +  item.getReserved() +  " times and is still available " + item.getCount() + " times");
		item.setReserved(item.getReserved() - reserveditemCount);
		item.setCount(item.getCount() + reserveditemCount);
		writeData(xid, item.getKey(), item);
		return true;
	}

	// Adds flight reservation to this customer
	public long[] reserveFlight(int xid, int customerID, int flightNum) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		long x = 0;
		DB.put(xid, x);
		long t = curr();
		reserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));

		return new long[] {curr() - t, DB.get(xid)};
	}

	// Adds car reservation to this customer
	public long[] reserveCar(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		long x = 0;
		DB.put(xid, x);
		long t = curr();
		reserveItem(xid, customerID, Car.getKey(location), location);

		return new long[] {curr() - t, DB.get(xid)};
	}

	// Adds room reservation to this customer
	public long[] reserveRoom(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		long x = 0;
		DB.put(xid, x);
		long t = curr();
		reserveItem(xid, customerID, Room.getKey(location), location);

		return new long[] {curr() - t, DB.get(xid)};
	}

	// Reserve bundle 
	public boolean bundle(int xid, int customerId, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return false;
	}

	public String Summary(int xid) throws RemoteException,TransactionAbortedException, InvalidTransactionException{
		String summary = "";

		for (String key: m_data.keySet()) {
			String type = key.split("-")[0];
			if (!type.equals("customer"))
				continue;
			Customer customer = (Customer)readData(xid, key);
			summary += customer.getSummary();

		}
		return summary;
	}

	public String Analytics(int xid, String key, int upperBound) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		String summary = "";

		int quantity = 	queryNum(xid, key);
		if (quantity <= upperBound)
			summary += key + ": RemainingQuantity=" + quantity + "\n";

		return summary;
	}

	public boolean shutdown() throws RemoteException {
		new Thread() {
			@Override
			public void run() {
				System.out.print("Shutting down...");
				try {
					sleep(5000);
				} catch (InterruptedException e) {
					// I don't care
				}
				System.out.println("done");
				System.exit(0);
			}

		}.start();
		return true;
	}

	public long[] start() throws RemoteException {
		return null;
	}

	public long[] commit(int xid) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		long x = 0;
		DB.put(xid, x);
		long ta = curr();

		System.out.println("Commit transaction:" + xid);
		//flush transaction to m_data
		if(!tm.xidActive(xid))
			throw new InvalidTransactionException(xid, "RM: Not a valid transaction");

		Transaction t = tm.readActiveData(xid);
		RMHashMap m = t.getData();

		long t2 = curr();
		synchronized (m_data) {
			Set<String> keyset = m.keySet();
			for (String key : keyset) {
				System.out.println("Write:(" + key + "," + m.get(key) + ")");
				m_data.put(key, m.get(key));
			}
		}
		long t3 = curr();

		// Move to inactive transactions
		tm.writeActiveData(xid, null);
		tm.writeInactiveData(xid, new Boolean(true));

		return new long[] {curr() - ta, t3 - t2};
	}

	public void abort(int xid) throws RemoteException, InvalidTransactionException {
		System.out.println("Abort transaction:" + xid);

		if(!tm.xidActive(xid))
			throw new InvalidTransactionException(xid, "Not a valid transaction");

		tm.writeActiveData(xid, null);
		tm.writeInactiveData(xid, new Boolean(false));

	}

	public String getName() throws RemoteException
	{
		return m_name;
	}
}
 
