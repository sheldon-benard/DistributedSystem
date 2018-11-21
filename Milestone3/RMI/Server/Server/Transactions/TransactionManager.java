package Server.Transactions;

import java.util.*;

public class TransactionManager {

    protected HashMap<Integer, Transaction> activeTransactions = new  HashMap<Integer, Transaction>();
    protected HashMap<Integer, Boolean> inactiveTransactions = new  HashMap<Integer, Boolean>(); //true -> committed; false -> aborted

    public TransactionManager () {
    }

    public boolean xidActive(int xid) {
        synchronized(activeTransactions) {
            synchronized (inactiveTransactions) {
                Set<Integer> keyset = activeTransactions.keySet();
                Set<Integer> inkeyset = inactiveTransactions.keySet();
                return keyset.contains(xid) && !inkeyset.contains(xid);
            }
        }
    }

    // Reads a data item
    public Transaction readActiveData(int xid)
    {
        synchronized(activeTransactions) {
            Transaction t = activeTransactions.get(xid);
            return t;
        }
    }

    // Reads a data item
    public Boolean readInactiveData(int xid)
    {
        synchronized(inactiveTransactions) {
            Boolean t = inactiveTransactions.get(xid);
            return t;
        }
    }

    // Writes a data item
    public void writeActiveData(int xid, Transaction t)
    {
        synchronized(activeTransactions) {
            activeTransactions.put(xid, t);
        }
    }

    // Writes a data item
    public void writeInactiveData(int xid, Boolean t)
    {
        synchronized(inactiveTransactions) {
            inactiveTransactions.put(xid, t);
        }
    }


}