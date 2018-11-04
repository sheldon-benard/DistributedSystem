package Server.Transactions;

import Server.Common.*;
import java.util.Date;
import java.util.*;

public class Transaction {

    protected int xid;
    protected RMHashMap m_data = new RMHashMap();
    private long lastAction = (new Date()).getTime();
    private Set<String> resourceManagers = new HashSet<String>();
    private int timeToLive; // in milliseconds

    // Time to live in seconds
    public Transaction (int xid, int timeToLive) {
        this.xid = xid;
        this.timeToLive = timeToLive * 1000;
    }

    public Transaction (int xid) {
        this.xid = xid;
    }

    public boolean expired() {
        long time = (new Date()).getTime();
        if (time > lastAction + timeToLive)
            return true;
        return false;
    }

    public void updateLastAction() {
        this.lastAction = (new Date()).getTime();
    }

    public RMHashMap getData() {
        return m_data;
    }

    public void addResourceManager(String rm) {
        resourceManagers.add(rm);
    }

    public Set<String> getResourceManagers() {
        return resourceManagers;
    }

    public int getXid() {
        return this.xid;
    }

    public boolean hasData(String key) {
        synchronized (m_data) {
            Set<String> keyset = m_data.keySet();
            return keyset.contains(key);
        }
    }

    // Writes a data item
    public void writeData(int xid, String key, RMItem value)
    {
        synchronized(m_data) {
            m_data.put(key, value);
        }
    }

    // Reads a data item
    public RMItem readData(int xid, String key)
    {
        synchronized(m_data) {
            RMItem item = m_data.get(key);
            if (item != null) {
                return (RMItem)item.clone();
            }
            return null;
        }
    }



}