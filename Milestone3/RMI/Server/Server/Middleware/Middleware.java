package Server.Middleware;

import Server.Transactions.*;
import Server.LockManager.*;
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

    protected MiddlewareTM tm;
    protected LockManager lm;
    private int timetolive = 60;
    protected int middlewareMode = 0;

    public Middleware(String p_name)
    {
        super(p_name, false);
        tm = new MiddlewareTM(timetolive, this);
        this.setTransactionManager(tm);
        lm = new LockManager();
        for (String s : this.log.getLocks()) {
            String[] locks = s.split(",");
            try {
                if (locks[2].equals("write"))
                    acquireLock(Integer.parseInt(locks[0]), locks[1], TransactionLockObject.LockType.LOCK_WRITE);
                else
                    acquireLock(Integer.parseInt(locks[0]), locks[1], TransactionLockObject.LockType.LOCK_READ);
            } catch(Exception e){System.out.println(e.getLocalizedMessage());}
        }
        // Recommit prepared
        Set<Integer> xids = new HashSet<Integer>();
        HashMap<Integer, Transaction> activeTransactions = tm.getActiveData();
        for (Integer xid : activeTransactions.keySet())
            xids.add(xid);
        for (Integer xid : xids) {
            if (tm.xidActiveAndPrepared(xid)) {
                try { commit(xid); }
                catch (Exception e) { System.out.println(e.toString()); }
            }
        }
    }

    public int start() throws RemoteException{
        int xid  = tm.start();
        Trace.info("Starting transaction - " + xid);
        this.flush_in_progress();
        return xid;
    }

    public void crashMiddleware(int mode) throws RemoteException {
        System.out.println("Setting MW crash mode to " + mode);
        this.middlewareMode = mode;
    }

    public void resetCrashes() throws RemoteException {
        System.out.println("Resetting crash mode");
        this.middlewareMode = 0;

        try {
            m_flightResourceManager.resetCrashes();
        } catch (Exception e) {
            if (!connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name))
                throw new RemoteException("Flight Server connection timeout");
            m_flightResourceManager.resetCrashes();
        }

        try {
            m_carResourceManager.resetCrashes();
        } catch (Exception e) {
            if (!connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name))
                throw new RemoteException("Car Server connection timeout");
            m_carResourceManager.resetCrashes();
        }

        try {
            m_roomResourceManager.resetCrashes();
        } catch (Exception e) {
            if (!connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name))
                throw new RemoteException("Room Server connection timeout");
            m_roomResourceManager.resetCrashes();
        }
    }

    public void crashResourceManager(String rm, int mode) throws RemoteException {
        if (rm.equalsIgnoreCase("Flight") || rm.equalsIgnoreCase("Flights")) {
            try {
                m_flightResourceManager.crashResourceManager(rm, mode);
            } catch (Exception e) {
                if (!connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name))
                    throw new RemoteException("Flight Server connection timeout");
                m_flightResourceManager.crashResourceManager(rm, mode);
            }
        }
        else if (rm.equalsIgnoreCase("Car") || rm.equalsIgnoreCase("Cars")) {
            try {
                m_carResourceManager.crashResourceManager(rm, mode);
            } catch (Exception e) {
                if (!connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name))
                    throw new RemoteException("Car Server connection timeout");
                m_carResourceManager.crashResourceManager(rm, mode);
            }
        }
        else if (rm.equalsIgnoreCase("Room") || rm.equalsIgnoreCase("Rooms")) {
            try {
                m_roomResourceManager.crashResourceManager(rm, mode);
            } catch (Exception e) {
                if (!connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name))
                    throw new RemoteException("Room Server connection timeout");
                m_roomResourceManager.crashResourceManager(rm, mode);
            }
        }
        else
            throw new RemoteException("Server not crashable");


    }

    public boolean commit(int xid) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        System.out.println("Commit transaction:" + xid);

        checkTransaction(id);
        Transaction t = tm.readActiveData(xid);

        Set<String> resources = t.getResourceManagers();

        Trace.info("Resource=" + resources);

        // Prepare transaction
        this.log.prepare(xid, "Prepared", "Prepared");
        t.setIsPrepared(true);
        this.flush_in_progress();

        if (this.middlewareMode == 1) {
            Trace.info("Crashing Middleware: xid=" + xid + " mode=" + this.middlewareMode);
            Trace.info("Crashing before sending vote");
            System.exit(1);
        }

        if (this.middlewareMode == 2) {
            Trace.info("Crashing Middleware: xid=" + xid + " mode=" + this.middlewareMode);
            new Thread(){
                public void run(){
                    try {
                        if (resources.contains("Flight"))
                            m_flightResourceManager.prepare(xid);
                    } catch(Exception e){}
                    try {
                        if (resources.contains("Car"))
                            m_carResourceManager.prepare(xid);
                    } catch(Exception e){}
                    try {
                        if (resources.contains("Room"))
                            m_roomResourceManager.prepare(xid);
                    } catch(Exception e){}
                }
            }.start();
            try {
                Thread.sleep(750);
            } catch(Exception e){}
            Trace.info("Crashing after sending request, before replies");
            System.exit(1);
        }

        List<Boolean> votes = t.getVotes();
        //Trace.info("Already have "  + votes.size() + " votes");

        boolean flightVote, carVote, roomVote;

        if (resources.contains("Flight") && votes.size() < 1) {
            try {
                flightVote = m_flightResourceManager.prepare(xid);
            } catch (Exception e) {
                if (!connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name)) {
                    Trace.info("Flight Server connection timeout - vote false");
                    flightVote = false;
                } else {
                    flightVote = m_flightResourceManager.prepare(xid);
                }
            }
            Trace.info("Flight Server vote:" + flightVote);
            votes.add(flightVote);
        }
        else if (votes.size() >= 1) {
            flightVote = votes.get(0);
        }
        else {
            flightVote = true; // If Flight not used, say true
            votes.add(flightVote);
        }

        String _votes = "";
        _votes += Boolean.toString(votes.get(0)) + ",";

        this.log.prepare(xid, "Votes", _votes);

        if (resources.contains("Flight") && this.middlewareMode == 3) {
            Trace.info("Crashing Middleware: xid=" + xid + " mode=" + this.middlewareMode);
            Trace.info("Crashing after receiving flight vote");
            System.exit(1);
        }

        if (resources.contains("Car")  && votes.size() < 2) {
            try {
                carVote = m_carResourceManager.prepare(xid);
            } catch (Exception e) {
                if (!connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name)) {
                    Trace.info("Car Server connection timeout - vote false");
                    carVote = false;
                }
                else {
                    carVote = m_carResourceManager.prepare(xid);
                }
            }
            Trace.info("Car Server vote:" + carVote);
            votes.add(carVote);
        }
        else if (votes.size() >= 2) {
            carVote = votes.get(1);
        }
        else {
            carVote = true; // If car not used, say true
            votes.add(carVote);
        }

        _votes += Boolean.toString(votes.get(1)) + ",";

        this.log.prepare(xid, "Votes", _votes);

        if (resources.contains("Car") && this.middlewareMode == 3) {
            Trace.info("Crashing Middleware: xid=" + xid + " mode=" + this.middlewareMode);
            Trace.info("Crashing after receiving car vote");
            System.exit(1);
        }


        if (resources.contains("Room") && votes.size() < 3) {
            try {
                roomVote = m_roomResourceManager.prepare(xid);
            } catch (Exception e) {
                if (!connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name)) {
                    Trace.info("Room Server connection timeout - vote false");
                    roomVote = false;
                }
                else {
                    roomVote = m_roomResourceManager.prepare(xid);
                }
            }
            Trace.info("Room Server vote:" + roomVote);
            votes.add(roomVote);
        }
        else if (votes.size() == 3) {
            roomVote = votes.get(2);
        }
        else {
            roomVote = true; // If room not used, say true
            votes.add(roomVote);
        }

        _votes += Boolean.toString(votes.get(2)) + ",";

        this.log.prepare(xid, "Votes", _votes);

        if (resources.contains("Room") && this.middlewareMode == 3) {
            Trace.info("Crashing Middleware: xid=" + xid + " mode=" + this.middlewareMode);
            Trace.info("Crashing after receiving room vote");
            System.exit(1);
        }

        if (this.middlewareMode == 4) {
            Trace.info("Crashing Middleware: xid=" + xid + " mode=" + this.middlewareMode);
            Trace.info("Crashing after receiving all votes");
            System.exit(1);
        }

        boolean decision = !votes.contains(false);

        if (this.middlewareMode == 5) {
            Trace.info("Crashing Middleware: xid=" + xid + " mode=" + this.middlewareMode);
            Trace.info("Crashing after making decision = " + decision);
            System.exit(1);
        }

        if (!decision) {
            abort(xid);
            if (this.middlewareMode == 7) {
                Trace.info("Crashing Middleware: xid=" + xid + " mode=" + this.middlewareMode);
                Trace.info("Crashing after sending all abort messages");
                System.exit(1);
            }
            return false;
        }


        if (resources.contains("Flight")) {
            try {
                try {
                    m_flightResourceManager.commit(xid);
                } catch (Exception e) {
                    if (connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name))
                        m_flightResourceManager.commit(xid);
                }
            } catch(Exception e) {
                Trace.info(xid + " already committed at flight rm");
            }
        }

        if (this.middlewareMode == 6) {
            Trace.info("Crashing Middleware: xid=" + xid + " mode=" + this.middlewareMode);
            Trace.info("Crashing after sending some commit messages");
            System.exit(1);
        }

        if (resources.contains("Car")) {
            try {
                try {
                    m_carResourceManager.commit(xid);
                }
                catch (Exception e) {
                    if (connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name))
                        m_carResourceManager.commit(xid);
                }
            } catch(Exception e) {
                Trace.info(xid + " already committed at car rm");
            }
        }

        if (resources.contains("Room")) {
            try {
                try {
                    m_roomResourceManager.commit(xid);
                }
                catch (Exception e) {
                    if (connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name))
                        m_roomResourceManager.commit(xid);
                }
            } catch(Exception e) {
                Trace.info(xid + " already committed at room rm");
            }
        }


        if (resources.contains("Customer")) {

            RMHashMap m = t.getData();

            synchronized (m_data) {
                Set<String> keyset = m.keySet();
                for (String key : keyset) {
                    System.out.print("Write:(" + key + "," + m.get(key) + ")");
                    m_data.put(key, m.get(key));
                }
            }
        }
        endTransaction(xid, true);
        this.flush_committed(xid);
        this.flush_in_progress();
        this.log.lastCommitted(xid);
        this.log.removePrepared(xid);

        if (this.middlewareMode == 7) {
            Trace.info("Crashing Middleware: xid=" + xid + " mode=" + this.middlewareMode);
            Trace.info("Crashing after sending all commit messages");
            System.exit(1);
        }
        return true;
    }

    public void abort(int xid) throws RemoteException, InvalidTransactionException {
        System.out.println("Abort transaction:" + xid);
        try {
            checkTransaction(xid);
        } catch(TransactionAbortedException e) {
            throw new InvalidTransactionException(xid, "transaction already aborted");
        }

        Transaction t = tm.readActiveData(xid);

        Set<String> resources = t.getResourceManagers();

        if (resources.contains("Flight")) {
            try {
                try {
                    m_flightResourceManager.abort(xid);
                } catch (Exception e) {
                    if (connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name))
                        m_flightResourceManager.abort(xid);
                }
            } catch(Exception e) {
                Trace.info(xid + " already aborted at flight rm");
            }
        }

        if (this.middlewareMode == 6) {
            Trace.info("Crashing Middleware: xid=" + xid + " mode=" + this.middlewareMode);
            Trace.info("Crashing after sending some abort messages");
            System.exit(1);
        }

        if (resources.contains("Car")) {
            try {
                try {
                    m_carResourceManager.abort(xid);
                }
                catch (Exception e) {
                    if (connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name))
                        m_carResourceManager.abort(xid);
                }
            } catch(Exception e) {
                Trace.info(xid + " already aborted at car rm");
            }
        }

        if (resources.contains("Room")) {
            try {
                try {
                    m_roomResourceManager.abort(xid);
                }
                catch (Exception e) {
                    if (connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name))
                        m_roomResourceManager.abort(xid);
                }
            } catch(Exception e) {
                Trace.info(xid + " already aborted at room rm");
            }
        }

        endTransaction(xid, false);
        this.flush_in_progress();
        this.log.removePrepared(xid);
    }

    private void endTransaction(int xid, boolean commit) {
        // Move to inactive transactions
        tm.writeActiveData(xid, null);
        tm.writeInactiveData(xid, new Boolean(commit));

        lm.UnlockAll(xid);
    }

    private void updateTimeToLive(int xid) {
        tm.readActiveData(xid).updateLastAction();
    }

    public boolean shutdown() throws RemoteException {
        try {
            m_flightResourceManager.shutdown();
        } catch (ConnectException e) {
            if (connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name))
                m_flightResourceManager.shutdown();
        }

        try {
            m_carResourceManager.shutdown();
        } catch (ConnectException e) {
            if (!connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name))
                m_carResourceManager.shutdown();
        }
        try {
            m_roomResourceManager.shutdown();
        } catch (ConnectException e) {
            if (!connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name))
                m_roomResourceManager.shutdown();
        }

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

    public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException,TransactionAbortedException, InvalidTransactionException {
        int id = xid;

        Trace.info("addFlight - Redirect to Flight Resource Manager");
        checkTransaction(id);

        acquireLock(id, Flight.getKey(flightNum), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(id,"Flight");
        try {
            return m_flightResourceManager.addFlight(id, flightNum, flightSeats, flightPrice);
        } catch (Exception e) {
            if (!connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name))
                throw new RemoteException("Server connection timeout");
            return m_flightResourceManager.addFlight(id, flightNum, flightSeats, flightPrice);
        }
    }

    public boolean addCars(int xid, String location, int numCars, int price) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("addCars - Redirect to Car Resource Manager");
        checkTransaction(id);

        acquireLock(id, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(id,"Car");
        try {
            return m_carResourceManager.addCars(id, location, numCars, price);
        } catch (Exception e) {
            if (!connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name))
                throw new RemoteException("Car Server connection timeout");
            return m_carResourceManager.addCars(id, location, numCars, price);
        }
    }

    public boolean addRooms(int xid, String location, int numRooms, int price) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("addRooms - Redirect to Room Resource Manager");
        checkTransaction(id);

        acquireLock(id, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(id,"Room");
        try {
            return m_roomResourceManager.addRooms(id, location, numRooms, price);
        } catch (Exception e) {
            if (!connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name))
                throw new RemoteException("Room Server connection timeout");
            return m_roomResourceManager.addRooms(id, location, numRooms, price);
        }

    }

    public boolean deleteFlight(int xid, int flightNum) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("deleteFlight - Redirect to Flight Resource Manager");
        checkTransaction(id);

        acquireLock(id, Flight.getKey(flightNum), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(id,"Flight");
        try {
            return m_flightResourceManager.deleteFlight(id, flightNum);
        } catch (Exception e) {
            if (!connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name))
                throw new RemoteException("Flight Server connection timeout");
            return m_flightResourceManager.deleteFlight(id, flightNum);
        }

    }

    public boolean deleteCars(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("deleteCars - Redirect to Car Resource Manager");
        checkTransaction(id);

        acquireLock(id, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(id,"Car");
        try {
            return m_carResourceManager.deleteCars(id, location);
        } catch (Exception e) {
            if (!connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name))
                throw new RemoteException("Car Server connection timeout");
            return m_carResourceManager.deleteCars(id, location);
        }

    }

    public boolean deleteRooms(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("deleteRooms - Redirect to Room Resource Manager");
        checkTransaction(id);

        acquireLock(id, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(id,"Room");
        try {
            return m_roomResourceManager.deleteRooms(id, location);
        } catch (Exception e) {
            if (!connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name))
                throw new RemoteException("Room Server connection timeout");
            return m_roomResourceManager.deleteRooms(id, location);
        }

    }

    public String queryCustomerInfo(int xid, int customerID) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        checkTransaction(xid);
        acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,"Customer");
        return super.queryCustomerInfo(xid,customerID);
    }

    public int newCustomer(int xid) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        checkTransaction(xid);

        Trace.info("RM::newCustomer(" + xid + ") called");
        // Generate a globally unique ID for the new customer
        int cid = Integer.parseInt(String.valueOf(xid) +
                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                String.valueOf(Math.round(Math.random() * 100 + 1)));
        Customer customer = new Customer(cid);
        acquireLock(xid, customer.getKey(), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(id,"Customer");
        writeData(xid, customer.getKey(), customer);
        Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
        return cid;
    }

    public boolean newCustomer(int xid, int customerID) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") called");

        int id = xid;
        checkTransaction(xid);
        acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Customer");

        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
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
        int id = xid;
        Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
        checkTransaction(xid);

        acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Customer");
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
            return false;
        }
        else {
            acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            // Increase the reserved numbers of all reservable items which the customer reserved.
            RMHashMap reservations = customer.getReservations();
            for (String reservedKey : reservations.keySet()) {
                String type = reservedKey.split("-")[0];
                ReservedItem reserveditem = customer.getReservedItem(reservedKey);
                if (type.equals("flight")) {
                    acquireLock(xid, reservedKey, TransactionLockObject.LockType.LOCK_WRITE);
                    addResourceManagerUsed(id,"Flight");
                    try {
                        m_flightResourceManager.removeReservation(xid, customerID, reserveditem.getKey(), reserveditem.getCount());
                    } catch (Exception e) {
                        if (!connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name)) {
                            abort(xid);
                            throw new RemoteException("Flight Server connection timeout during deletion event; abort");
                        }
                        m_flightResourceManager.removeReservation(xid, customerID, reserveditem.getKey(), reserveditem.getCount());
                    }
                } else if (type.equals("car")) {
                    acquireLock(xid, reservedKey, TransactionLockObject.LockType.LOCK_WRITE);
                    addResourceManagerUsed(id,"Car");
                    try {
                        m_carResourceManager.removeReservation(xid, customerID, reserveditem.getKey(), reserveditem.getCount());
                    } catch (Exception e) {
                        if (!connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name)) {
                            abort(xid);
                            throw new RemoteException("Car Server connection timeout during deletion event; abort");
                        }
                        m_carResourceManager.removeReservation(xid, customerID, reserveditem.getKey(), reserveditem.getCount());
                    }
                } else if (type.equals("room")) {
                    acquireLock(xid, reservedKey, TransactionLockObject.LockType.LOCK_WRITE);
                    addResourceManagerUsed(id,"Room");
                    try {
                        m_roomResourceManager.removeReservation(xid, customerID, reserveditem.getKey(), reserveditem.getCount());
                    } catch (Exception e) {
                        if (!connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name)) {
                            abort(xid);
                            throw new RemoteException("Room Server connection timeout during deletion event; abort");
                        }
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

    public int queryFlight(int xid, int flightNumber) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("queryFlight - Redirect to Flight Resource Manager");
        checkTransaction(id);

        acquireLock(id, Flight.getKey(flightNumber), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Flight");
        try {
            return m_flightResourceManager.queryFlight(id, flightNumber);
        } catch (Exception e) {
            if (!connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name))
                throw new RemoteException("Flight Server connection timeout");
            return m_flightResourceManager.queryFlight(id, flightNumber);
        }
    }

    public int queryCars(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("queryCars - Redirect to Car Resource Manager");
        checkTransaction(id);

        acquireLock(id, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Car");
        try {
            return m_carResourceManager.queryCars(id, location);
        } catch (Exception e) {
            if (!connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name))
                throw new RemoteException("Car Server connection timeout");
            return m_carResourceManager.queryCars(id, location);
        }
    }

    public int queryRooms(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("queryRooms - Redirect to Room Resource Manager");
        checkTransaction(id);

        acquireLock(id, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Room");
        try {
            return m_roomResourceManager.queryRooms(id, location);
        } catch (Exception e) {
            if (!connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name))
                throw new RemoteException("Room Server connection timeout");
            return m_roomResourceManager.queryRooms(id, location);
        }
    }

    public int queryFlightPrice(int xid, int flightNumber) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("queryFlightPrice - Redirect to Flight Resource Manager");
        checkTransaction(id);

        acquireLock(id, Flight.getKey(flightNumber), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Flight");
        try {
            return m_flightResourceManager.queryFlightPrice(id, flightNumber);
        } catch (Exception e) {
            if (!connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name))
                throw new RemoteException("Flight Server connection timeout");
            return m_flightResourceManager.queryFlightPrice(id, flightNumber);
        }
    }

    public int queryCarsPrice(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("queryCarsPrice - Redirect to Car Resource Manager");
        checkTransaction(id);

        acquireLock(id, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Car");
        try {
            return m_carResourceManager.queryCarsPrice(id, location);
        } catch (Exception e) {
            if (!connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name))
                throw new RemoteException("Car Server connection timeout");
            return m_carResourceManager.queryCarsPrice(id, location);
        }
    }

    public int queryRoomsPrice(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("queryRoomsPrice - Redirect to Room Resource Manager");
        checkTransaction(id);

        acquireLock(id, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Room");
        try {
            return m_roomResourceManager.queryRoomsPrice(id, location);
        } catch (Exception e) {
            if (!connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name))
                throw new RemoteException("Room Server connection timeout");
            return m_roomResourceManager.queryRoomsPrice(id, location);
        }
    }

    public boolean reserveFlight(int xid, int customerID, int flightNumber) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        String key = Flight.getKey(flightNumber);

        Trace.info("RM::reserveFlight(" + xid + ", customer=" + customerID + ", " + key + ") called" );
        checkTransaction(xid);
        // Check customer exists

        acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,"Customer");
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + flightNumber + ")  failed--customer doesn't exist");
            return false;
        }

        acquireLock(xid, key, TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,"Flight");
        int price;
        try {
            price = m_flightResourceManager.itemsAvailable(xid, key, 1);
        } catch (Exception e) {
            if (!connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name))
                throw new RemoteException("Flight Server connection timeout");
            price = m_flightResourceManager.itemsAvailable(xid, key, 1);
        }

        if (price < 0) {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + flightNumber + ")  failed--item unavailable");
            return false;
        }
        acquireLock(xid, key, TransactionLockObject.LockType.LOCK_WRITE);

        try {
            if (m_flightResourceManager.reserveFlight(xid, customerID, flightNumber)) {
                acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
                customer.reserve(key, String.valueOf(flightNumber), price);
                writeData(xid, customer.getKey(), customer);
                return true;
            }
        } catch (Exception e) {
            if (!connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name))
                throw new RemoteException("Flight Server connection timeout");
            if (m_flightResourceManager.reserveFlight(xid, customerID, flightNumber)) {
                acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
                customer.reserve(key, String.valueOf(flightNumber), price);
                writeData(xid, customer.getKey(), customer);
                return true;
            }
        }
        Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + flightNumber + ")  failed--Could not reserve item");
        return false;

    }

    public boolean reserveCar(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        String key = Car.getKey(location);

        Trace.info("RM::reserveCar(" + xid + ", customer=" + customerID + ", " + key + ") called" );
        checkTransaction(xid);
        // Check customer exists

        acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Customer");
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }

        acquireLock(xid, key, TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Car");
        int price;
        try {
            price = m_carResourceManager.itemsAvailable(xid, key, 1);
        } catch (Exception e) {
            if (!connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name))
                throw new RemoteException("Car Server connection timeout");
            price = m_carResourceManager.itemsAvailable(xid, key, 1);
        }

        if (price < 0) {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--item unavailable");
            return false;
        }

        acquireLock(xid, key, TransactionLockObject.LockType.LOCK_WRITE);

        try {
            if (m_carResourceManager.reserveCar(xid, customerID, location)) {
                acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
                customer.reserve(key, location, price);
                writeData(xid, customer.getKey(), customer);
                return true;
            }
        } catch (Exception e) {
            if (!connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name))
                throw new RemoteException("Car Server connection timeout");
            if (m_carResourceManager.reserveCar(xid, customerID, location)) {
                acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
                customer.reserve(key, location, price);
                writeData(xid, customer.getKey(), customer);
                return true;
            }
        }
        Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--Could not reserve item");
        return false;

    }

    public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        String key = Room.getKey(location);

        Trace.info("RM::reserveRoom(" + xid + ", customer=" + customerID + ", " + key + ") called" );
        checkTransaction(xid);
        // Check customer exists

        acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Customer");
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }

        acquireLock(xid, key, TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Room");
        int price;
        try {
            price = m_roomResourceManager.itemsAvailable(xid, key, 1);
        } catch (Exception e) {
            if (!connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name))
                throw new RemoteException("Room Server connection timeout");
            price = m_roomResourceManager.itemsAvailable(xid, key, 1);
        }

        if (price < 0) {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--item unavailable");
            return false;
        }

        acquireLock(xid, key, TransactionLockObject.LockType.LOCK_WRITE);
        try {
            if (m_roomResourceManager.reserveRoom(xid, customerID, location)) {
                acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
                customer.reserve(key, location, price);
                writeData(xid, customer.getKey(), customer);
                return true;
            }
        } catch (Exception e) {
            if (!connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name))
                throw new RemoteException("Room Server connection timeout");
            if (m_roomResourceManager.reserveRoom(xid, customerID, location)) {
                acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
                customer.reserve(key, location, price);
                writeData(xid, customer.getKey(), customer);
                return true;
            }
        }
        Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--Could not reserve item");
        return false;
    }

    public boolean bundle(int xid, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("RM::bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ") called" );
        checkTransaction(xid);

        acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Customer");
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

        if (car && room) {
            // Check flight availability
            for (String key : countMap.keySet()) {
                int keyInt;

                try {
                    keyInt = Integer.parseInt(key);
                } catch (Exception e) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--could not parse flightNumber");
                    return false;
                }
                acquireLock(xid, Flight.getKey(keyInt), TransactionLockObject.LockType.LOCK_READ);
                addResourceManagerUsed(id,"Flight");
                int price;
                try {
                    price = m_flightResourceManager.itemsAvailable(xid, Flight.getKey(keyInt), countMap.get(key));
                } catch (Exception e) {
                    if (!connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name))
                        throw new RemoteException("Flight Server connection timeout");
                    price = m_flightResourceManager.itemsAvailable(xid, Flight.getKey(keyInt), countMap.get(key));
                }
                if (price < 0) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + key + " doesn't have enough spots");
                    return false;
                } else {
                    flightPrice.put(keyInt, price);
                }
            }
            acquireLock(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ);
            addResourceManagerUsed(id,"Car");
            try {
                carPrice = m_carResourceManager.itemsAvailable(xid, Car.getKey(location), 1);
            } catch (Exception e) {
                if (!connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name))
                    throw new RemoteException("Car Server connection timeout");
                carPrice = m_carResourceManager.itemsAvailable(xid, Car.getKey(location), 1);
            }
            if (carPrice < 0) {
                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--car-" + location + " doesn't have enough spots");
                return false;
            }

            acquireLock(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ);
            addResourceManagerUsed(id,"Room");
            try {
                roomPrice = m_roomResourceManager.itemsAvailable(xid, Room.getKey(location), 1);
            } catch (Exception e) {
                if (!connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name))
                    throw new RemoteException("Room Server connection timeout");
                roomPrice = m_roomResourceManager.itemsAvailable(xid, Room.getKey(location), 1);
            }
            if (roomPrice < 0) {
                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--room-" + location + " doesn't have enough spots");
                return false;
            }

            acquireLock(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
            try {
                m_roomResourceManager.reserveRoom(xid, customerID, location);
            } catch (Exception e) {
                if (!connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name)) {
                    abort(xid);
                    throw new RemoteException("Room Server connection timeout during bundle reservations; abort");
                }
                m_roomResourceManager.reserveRoom(xid, customerID, location);
            }

            acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            addResourceManagerUsed(id,"Customer");
            customer.reserve(Room.getKey(location), location, roomPrice);

            writeData(xid, customer.getKey(), customer);

            acquireLock(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
            try {
                m_carResourceManager.reserveCar(xid, customerID, location);
            } catch (Exception e) {
                if (!connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name)) {
                    abort(xid);
                    throw new RemoteException("Car Server connection timeout during bundle reservations; abort");
                }
                m_carResourceManager.reserveCar(xid, customerID, location);
            }

            // Already have customer LOCK_WRITE
            customer.reserve(Car.getKey(location), location, carPrice);
            writeData(xid, customer.getKey(), customer);


        } else if (car) {
            // Check flight availability
            for (String key : countMap.keySet()) {
                int keyInt;

                try {
                    keyInt = Integer.parseInt(key);
                } catch (Exception e) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--could not parse flightNumber");
                    return false;
                }
                acquireLock(xid, Flight.getKey(keyInt), TransactionLockObject.LockType.LOCK_READ);
                addResourceManagerUsed(id,"Flight");
                int price;
                try {
                    price = m_flightResourceManager.itemsAvailable(xid, Flight.getKey(keyInt), countMap.get(key));
                } catch (Exception e) {
                    if (!connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name))
                        throw new RemoteException("Flight Server connection timeout");
                    price = m_flightResourceManager.itemsAvailable(xid, Flight.getKey(keyInt), countMap.get(key));
                }

                if (price < 0) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + key + " doesn't have enough spots");
                    return false;
                } else {
                    flightPrice.put(keyInt, price);
                }
            }
            acquireLock(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ);
            addResourceManagerUsed(id,"Car");
            try {
                carPrice = m_carResourceManager.itemsAvailable(xid, Car.getKey(location), 1);
            } catch (Exception e) {
                if (!connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name))
                    throw new RemoteException("Car Server connection timeout");
                carPrice = m_carResourceManager.itemsAvailable(xid, Car.getKey(location), 1);
            }

            if (carPrice < 0) {
                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--car-" + location + " doesn't have enough spots");
                return false;
            }
            acquireLock(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
            try {
                m_carResourceManager.reserveCar(xid, customerID, location);
            } catch (Exception e) {
                if (!connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name)) {
                    abort(xid);
                    throw new RemoteException("Car Server connection timeout during bundle reservations; abort");
                }
                m_carResourceManager.reserveCar(xid, customerID, location);
            }

            acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            addResourceManagerUsed(id,"Customer");
            customer.reserve(Car.getKey(location), location, carPrice);
            writeData(xid, customer.getKey(), customer);


        } else if (room) {
            // Check flight availability
            for (String key : countMap.keySet()) {
                int keyInt;

                try {
                    keyInt = Integer.parseInt(key);
                } catch (Exception e) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--could not parse flightNumber");
                    return false;
                }
                acquireLock(xid, Flight.getKey(keyInt), TransactionLockObject.LockType.LOCK_READ);
                addResourceManagerUsed(id,"Flight");
                int price;
                try {
                    price = m_flightResourceManager.itemsAvailable(xid, Flight.getKey(keyInt), countMap.get(key));
                } catch (Exception e) {
                    if (!connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name))
                        throw new RemoteException("Flight Server connection timeout");
                    price = m_flightResourceManager.itemsAvailable(xid, Flight.getKey(keyInt), countMap.get(key));
                }

                if (price < 0) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + key + " doesn't have enough spots");
                    return false;
                } else {
                    flightPrice.put(keyInt, price);
                }
            }
            acquireLock(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ);
            addResourceManagerUsed(id,"Room");
            try {
                roomPrice = m_roomResourceManager.itemsAvailable(xid, Room.getKey(location), 1);
            } catch (Exception e) {
                if (!connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name))
                    throw new RemoteException("Room Server connection timeout");
                roomPrice = m_roomResourceManager.itemsAvailable(xid, Room.getKey(location), 1);
            }

            if (roomPrice < 0) {
                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--room-" + location + " doesn't have enough spots");
                return false;
            }
            acquireLock(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
            try {
                m_roomResourceManager.reserveRoom(xid, customerID, location);
            } catch (Exception e) {
                if (!connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name)) {
                    abort(xid);
                    throw new RemoteException("Room Server connection timeout during bundle reservations; abort");
                }
                m_roomResourceManager.reserveRoom(xid, customerID, location);
            }

            acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            addResourceManagerUsed(id,"Customer");
            customer.reserve(Room.getKey(location), location, roomPrice);
            writeData(xid, customer.getKey(), customer);

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
                acquireLock(xid, Flight.getKey(keyInt), TransactionLockObject.LockType.LOCK_READ);
                addResourceManagerUsed(id,"Flight");
                int price;
                try {
                    price = m_flightResourceManager.itemsAvailable(xid, Flight.getKey(keyInt), countMap.get(key));
                } catch (Exception e) {
                    if (!connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name))
                        throw new RemoteException("Flight Server connection timeout");
                    price = m_flightResourceManager.itemsAvailable(xid, Flight.getKey(keyInt), countMap.get(key));
                }

                if (price < 0) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + key + " doesn't have enough spots");
                    return false;
                } else {
                    flightPrice.put(keyInt, price);
                }
            }
        }

        if (flightPrice.keySet().size() > 0) {
            acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            addResourceManagerUsed(id,"Customer");
        }
        // Reserve flights
        for (Integer key : flightPrice.keySet()) {
            for (int i = 0; i < countMap.get(String.valueOf(key)); i++) {
                int price = flightPrice.get(key);

                acquireLock(xid, Flight.getKey(key), TransactionLockObject.LockType.LOCK_WRITE);
                try {
                    m_flightResourceManager.reserveFlight(xid, customerID, key);
                } catch (Exception e) {
                    if (!connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name)) {
                        abort(xid);
                        throw new RemoteException("Flight Server connection timeout during bundle reservations; abort");
                    }
                    m_flightResourceManager.reserveFlight(xid, customerID, key);
                }
                customer.reserve(Flight.getKey(key), String.valueOf(key), price);
                writeData(xid, customer.getKey(), customer);
            }
        }


        Trace.info("RM:bundle() -- succeeded");
        return true;

    }

    public String Analytics(int xid, String k, int upperBound) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("RM::Analytics(" + xid + ", upperBound=" + upperBound + ") called" );

        checkTransaction(xid);
        Transaction t = tm.readActiveData(xid);
        RMHashMap m = t.getData();
        Set<String> keyset = new HashSet<String>(m.keySet());
        keyset.addAll(m_data.keySet());

        String summary = "";

        Set<String> printed = new HashSet<String>();

        for (String key: keyset) {
            acquireLock(xid, key, TransactionLockObject.LockType.LOCK_READ);
            addResourceManagerUsed(id,"Customer");
            Customer customer = (Customer)readData(xid, key);

            if (customer == null)
                continue;

            Set<String> reservations = customer.getReservations().keySet();

            for (String reservation: reservations) {

                if (printed.contains(reservation))
                    continue;
                printed.add(reservation);

                String type = reservation.split("-")[0];

                switch (type) {
                    case "flight": {
                        addResourceManagerUsed(id,"Flight");
                        try {
                            summary += m_flightResourceManager.Analytics(xid, reservation, upperBound);
                        } catch (Exception e) {
                            if (!connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name))
                                throw new RemoteException("Flight Server connection timeout");
                            summary += m_flightResourceManager.Analytics(xid, reservation, upperBound);
                        }
                        break;
                    }
                    case "car": {
                        addResourceManagerUsed(id,"Car");
                        try {
                            summary += m_carResourceManager.Analytics(xid, reservation, upperBound);
                        } catch (Exception e) {
                            if (!connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name))
                                throw new RemoteException("Car Server connection timeout");
                            summary += m_carResourceManager.Analytics(xid, reservation, upperBound);
                        }
                        break;
                    }
                    case "room": {
                        addResourceManagerUsed(id,"Room");
                        try {
                            summary += m_roomResourceManager.Analytics(xid, reservation, upperBound);
                        } catch (Exception e) {
                            if (!connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name))
                                throw new RemoteException("Room Server connection timeout");
                            summary += m_roomResourceManager.Analytics(xid, reservation, upperBound);
                        }
                        break;
                    }
                }
            }
        }

        return summary;
    }

    public String Summary(int xid) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;

        checkTransaction(xid);
        Transaction t = tm.readActiveData(xid);
        RMHashMap m = t.getData();
        Set<String> keyset = new HashSet<String>(m.keySet());
        keyset.addAll(m_data.keySet());

        String summary = "";

        for (String key: keyset) {
            String type = key.split("-")[0];
            if (!type.equals("customer"))
                continue;
            acquireLock(xid, key, TransactionLockObject.LockType.LOCK_READ);
            addResourceManagerUsed(id,"Customer");
            Customer customer = (Customer)readData(xid, key);
            if (customer != null)
                summary += customer.getSummary();

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

    protected boolean connectServer(String type, String server, int port, String name)
    {
        int waitTime = 30 * 1000; // ms
        //int waitTime = 5 * 1000; // ms
        int wait = 500;
        int loops = waitTime / wait;
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
                    return true;
                }
                catch (NotBoundException|RemoteException e) {
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
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
        }
        return false;
    }

    protected boolean connectServerIndefinitely(String type, String server, int port, String name)
    {
        int wait = 5000;
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
                    return true;
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

    public boolean isActive(int xid) throws RemoteException {
        Trace.info("isActive(" + xid + ") ==" + tm.xidActive(xid));
        return tm.xidActive(xid);
    }

    public boolean getMWDecision(int xid) throws RemoteException {
        if (!tm.xidActive(xid))
            return tm.xidCommitted(xid);
        Transaction t = tm.readActiveData(xid);
        List<Boolean> votes = t.getVotes();
        while (tm.xidActive(xid) && votes.size() != 3) {
            try{Thread.sleep(500);}
            catch(Exception e){}
        }
        if (!tm.xidActive(xid))
            return tm.xidCommitted(xid);
        return !votes.contains(false);
    }

    protected void checkTransaction(int xid) throws TransactionAbortedException, InvalidTransactionException{
        if(tm.readActiveData(xid) != null) {
            updateTimeToLive(xid);
            return;
        }
        Trace.info("Transaction is not active: throw error");

        Boolean v = tm.readInactiveData(xid);
        if (v == null)
            throw new InvalidTransactionException(xid, "MW: The transaction doesn't exist");
        else if (v.booleanValue() == true)
            throw new InvalidTransactionException(xid, "MW: The transaction has already been committed");
        else
            throw new TransactionAbortedException(xid, "MW: The transaction has been aborted");
    }

    protected void acquireLock(int xid, String data, TransactionLockObject.LockType lockType) throws RemoteException, TransactionAbortedException, InvalidTransactionException{
        try {
            boolean lock = lm.Lock(xid, data, lockType);
            if (!lock) {
                Trace.info("LM::lock(" + xid + ", " + data + ", " + lockType + ") Unable to lock");
                throw new InvalidTransactionException(xid, "LockManager-Unable to lock");
            }
        } catch (DeadlockException e) {
            Trace.info("LM::lock(" + xid + ", " + data + ", " + lockType + ") " + e.getLocalizedMessage());
            abort(xid);
            throw new TransactionAbortedException(xid, "The transaction has been aborted due to a deadlock");
        }
    }

    protected void addResourceManagerUsed(int xid, String resource) throws RemoteException {
        Transaction t = tm.readActiveData(xid);
        t.addResourceManager(resource);

        try {

            switch (resource) {
                case "Flight": {
                    m_flightResourceManager.addTransaction(xid);
                    break;
                }
                case "Car": {
                    m_carResourceManager.addTransaction(xid);
                    break;
                }
                case "Room": {
                    m_roomResourceManager.addTransaction(xid);
                    break;
                }
                case "Customer": {
                    this.addTransaction(xid);
                }
            }

        } catch (Exception e) {

            switch (resource) {
                case "Flight": {
                    if(connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name))
                        m_flightResourceManager.addTransaction(xid);
                    else
                        throw new RemoteException("Can't connect to Flight server to add transaction");
                    break;
                }
                case "Car": {
                    if(connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name))
                        m_carResourceManager.addTransaction(xid);
                    else
                        throw new RemoteException("Can't connect to Car server to add transaction");
                    break;
                }
                case "Room": {
                    if(connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name))
                        m_roomResourceManager.addTransaction(xid);
                    else
                        throw new RemoteException("Can't connect to Room server to add transaction");
                    break;
                }
                case "Customer": {
                    this.addTransaction(xid);
                }
            }
        }


    }

}