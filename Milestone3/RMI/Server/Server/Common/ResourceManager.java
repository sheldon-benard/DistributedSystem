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

import java.rmi.NotBoundException;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class ResourceManager implements IResourceManager
{
	protected String m_name = "";
	protected RMHashMap m_data = new RMHashMap();
	protected TransactionManager tm;
	protected Logger log = null;

	protected int mode = 0;

	protected String mwName;
    protected String mwHost;
    protected int mwPort;

	public ResourceManager(String p_name, String mwName, String mwHost, int mwPort)
	{
		m_name = p_name;
		this.mwName = mwName;
		this.mwHost = mwHost;
		this.mwPort = mwPort;
		tm = new TransactionManager();
		log = new Logger(m_name, this, m_data);
		setTransactionManager(tm);
	}

	public void resetCrashes() throws RemoteException {
		System.out.println("Resetting crash mode");
		this.mode = 0;
		this.log.setMode(mode);
	}

	public void crashMiddleware(int mode) throws RemoteException {
	}

	public void crashResourceManager(String rm, int mode) throws RemoteException {
		System.out.println("Setting crash mode to " + mode);
		this.mode = mode;
		this.log.setMode(mode);
	}

	public ResourceManager(String p_name, boolean setTM)
	{
		m_name = p_name;
		log = new Logger(m_name, this, m_data);
		if (setTM) setTransactionManager(tm);
	}

	protected void setTransactionManager(TransactionManager tm) {
		this.tm = tm;
		log.setTM(tm);
		log.setupEnv();
	}

	public boolean prepare(int xid) throws RemoteException,TransactionAbortedException, InvalidTransactionException {
        Trace.info("RM:prepare(" + xid + ") called");
        this.log.prepare(xid, "Prepared", "Prepared");

        if (this.mode == 1) {
            Trace.info("Mode=" + this.mode + "; crashing");
            System.exit(1);
        }

        String decision;

        if (!tm.xidExists(xid))
            decision = "InvalidTransactionException";
        else if (!tm.xidActive(xid))
            decision = (tm.xidCommitted(xid)) ? "commit" : "abort";
        else {
            tm.readActiveData(xid).setIsPrepared(true);
            flush_in_progress();
            decision = "commit";
        }
        Trace.info("RM:" + xid  + " decision=" + decision);
        this.log.prepare(xid, "Decision", decision);

        if (this.mode == 2) {
            Trace.info("Mode=" + this.mode + "; crashing");
            System.exit(1);
        }

        boolean d = decision.equals("commit");
        if (this.mode == 3) {
            Trace.info("Mode=" + this.mode + "; crashing after sending answer");
            new Thread(){
                public void run(){
                    try {
                        Thread.sleep(500);
                    } catch(Exception e){}
                    System.exit(1);
                }
            }.start();
        }
		this.log.prepare(xid, "Sent", "Sent");
        return d;
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

		if(tm.xidPrepared(xid))
			throw new InvalidTransactionException(xid, "Transaction has been prepared for 2PC; no further action available");

		Transaction t = tm.readActiveData(xid);

		if (!t.hasData(key)) {
			synchronized (m_data) {
				RMItem item = m_data.get(key);
				if (item != null) {
					t.writeData(xid, key, (RMItem) item.clone());
				}
				else {
					t.writeData(xid, key, null);
				}
			}
		}
		RMItem i = t.readData(xid, key);
		flush_in_progress();
		return i;

	}

	// Writes a data item
	protected void writeData(int xid, String key, RMItem value) throws InvalidTransactionException
	{
		if(!tm.xidActive(xid))
			throw new InvalidTransactionException(xid, "Not a valid transaction");

		readData(xid, key); // this ensures that the data is copied in the transaction local copy

		Transaction t = tm.readActiveData(xid);
		t.writeData(xid, key, value);
		flush_in_progress();
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
	public int queryFlight(int xid, int flightNum) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return queryNum(xid, Flight.getKey(flightNum));
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
	public int queryFlightPrice(int xid, int flightNum) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return queryPrice(xid, Flight.getKey(flightNum));
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
	public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return reserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
	}

	// Adds car reservation to this customer
	public boolean reserveCar(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return reserveItem(xid, customerID, Car.getKey(location), location);
	}

	// Adds room reservation to this customer
	public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return reserveItem(xid, customerID, Room.getKey(location), location);
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

	public int start() throws RemoteException {
		return -1;
	}

	public boolean commit(int xid) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		if (this.mode == 3) {
			System.exit(1);
		}
		System.out.println("Commit transaction:" + xid);
		this.log.prepare(xid,"MWDecision", "commit");

        if (this.mode == 4) {
            Trace.info("Mode=" + this.mode + "; crashing");
            System.exit(1);
        }

		//flush transaction to m_data
		if(!tm.xidActive(xid))
			throw new InvalidTransactionException(xid, "RM: Not a valid transaction");

		Transaction t = tm.readActiveData(xid);
		RMHashMap m = t.getData();

		synchronized (m_data) {
			Set<String> keyset = m.keySet();
			for (String key : keyset) {
				System.out.println("Write:(" + key + "," + m.get(key) + ")");
				m_data.put(key, m.get(key));
			}
		}
		flush_committed(xid);
		this.log.lastCommitted(xid);
		// Move to inactive transactions
		tm.writeActiveData(xid, null);
		tm.writeInactiveData(xid, new Boolean(true));
		flush_in_progress();
		this.log.removePrepared(xid);
		return true;
	}

	protected void flush_committed(int xid) {
		log.flush_committed(xid);
	}
	protected void flush_in_progress() {
		log.flush_in_progress();
	}

	public void recover(int xid, Object prepared, Object decision, Object sent, Object mwdecision) {
	    if (prepared == null) {
            return; // prepared should never be null
        }
        if (decision == null) {
	        // Query the middleware to see if still active
            Trace.info("Recovering=" + xid + " Started preparing; query middleware to see if still active");
            if (!askMWActive(xid)) {
                // abort: not active -> aborted at MW
                Trace.info("Recovering=" + xid + " Abort xid");
                try {abort(xid);}
                catch(Exception e) {}
            }
            else {
				// Else, continue  with recover, the MW is trying to reconnect
				Trace.info("Recovering=" + xid + " Transaction still active at MW");
			}
        }
        else if (sent == null) {
			// So decision != null
			String dec = decision.toString();
			Trace.info("Recovering=" + xid + " Made decision but did not send: " + dec);
			if (dec.equals("InvalidTransactionException")){
				// Nothing to do, as transaction doesn't exist
				return;
			}
			else if (dec.equals("abort")) {
				// decision to abort
				try {abort(xid);}
				catch(Exception e) {}
			}
			else {
				// May still be active
				if (!askMWActive(xid)) {
					// abort: not active -> aborted at MW
					Trace.info("Recovering=" + xid + " Abort xid");
					try {abort(xid);}
					catch(Exception e) {}
				}
				else {
					// Else, continue  with recover, the MW is trying to reconnect
					Trace.info("Recovering=" + xid + " Transaction still active at MW");
				}
			}
		}
        else if (mwdecision == null) {
	        // So decision != null
            String dec = decision.toString();
            Trace.info("Recovering=" + xid + " Made decision and sent it: " + dec);
            if (dec.equals("InvalidTransactionException")){
                // Nothing to do, as transaction doesn't exist
                return;
            }
            else if (dec.equals("abort")) {
                // decision to abort
                try {abort(xid);}
                catch(Exception e) {}
            }
            else {
                // decision to commit -> we must ask if still active -> indefinitely
				Trace.info("Either prepare is being called right now, or commit is still running on the middleware");
				Trace.info("So: run a thread asking for MWDecision AND continue with making the stub available");
				new Thread(){
					public void run(){
						if (!askMWDecisionIndefinitely(xid)) {
							try { abort(xid); }
							catch (Exception e) { }
						}
						else {
							try { commit(xid); }
							catch (Exception e) { }
						}
					}
				}.start();

                // However, MW could still be trying to reconnect, so start thread
            }
        }
        else {
	        //Received decision from MW
            String dec = mwdecision.toString();
			Trace.info("Recovering=" + xid + " Received MW decision: " + dec);
            if (dec.equals("abort")) {
                // decision to abort
                try {abort(xid);}
                catch(Exception e) {}
            }
            else {
                // decision to commit
                try {commit(xid);}
                catch(Exception e) {}
            }
        }
    }
    public boolean getMWDecision(int xid) throws RemoteException {return false;}
	public boolean isActive(int xid) throws RemoteException {return false;}

    public boolean askMWActive(int xid) {
        int waitTime = 30 * 1000; // ms
		//int waitTime = 5 * 1000; // ms
        int wait = 200;
        int loops = waitTime / wait;
        String name = this.mwName;
        String server = this.mwHost;
        int port = this.mwPort;

		// Set the security policy
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}
        try {
            boolean first = true;
            while (true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(server, port);
					IResourceManager mw = (IResourceManager)registry.lookup(name);
                    System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + name + "]");
                    boolean result = mw.isActive(xid);
                    Trace.info("isActive(" + xid + ")==" + result);
                    return result;
                }
                catch (NotBoundException|RemoteException e) {
                    //System.out.println(e.toString());
                	loops--;
                    if (loops < 0) {
                        System.out.println("Timed out waiting for '" + name + "' server [" + server + ":" + port + "/" + name + "]");
                        return false;
                    }
                    if (first) {
                        System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/" + name + "]");
                        first = false;
                    }
                }
                Thread.sleep(wait);
            }
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception in askMWActive");
            e.printStackTrace();
        }
        return false;
    }

	public boolean askMWDecisionIndefinitely(int xid) {
		int wait = 2000;
		String name = this.mwName;
		String server = this.mwHost;
		int port = this.mwPort;

		// Set the security policy
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}
		try {
			boolean first = true;
			while (true) {
				try {
					Registry registry = LocateRegistry.getRegistry(server, port);
					IResourceManager mw = (IResourceManager)registry.lookup(name);
					System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + name + "]");
					return mw.getMWDecision(xid);
				}
				catch (NotBoundException|RemoteException e) {
					if (first) {
						System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/" + name + "]");
						first = false;
					}
				}
				Thread.sleep(wait);
			}
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
		}
		return false;
	}

	public void abort(int xid) throws RemoteException, InvalidTransactionException {
		if (this.mode == 3) {
			System.exit(1);
		}
		System.out.println("Abort transaction:" + xid);
        this.log.prepare(xid,"MWDecision", "abort");

        if (this.mode == 4) {
            Trace.info("Mode=" + this.mode + "; crashing");
            System.exit(1);
        }

		if(!tm.xidActive(xid))
			throw new InvalidTransactionException(xid, "Not a valid transaction");

		tm.writeActiveData(xid, null);
		tm.writeInactiveData(xid, new Boolean(false));
		flush_in_progress();
        this.log.removePrepared(xid);
	}

	public String getName() throws RemoteException
	{
		return m_name;
	}
}
 
