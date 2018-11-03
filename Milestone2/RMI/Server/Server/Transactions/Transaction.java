package Server.Transactions;

import Server.Common.*;
import java.util.Date;

public class Transaction {

    protected int xid;
    protected RMHashMap m_data = new RMHashMap();
    private Date date = new Date();
    private long lastAction =   date.getTime();
    private int timeToLive; // in milliseconds

    // Time to live in seconds
    public Transaction (int xid, int timeToLive) {
        this.xid = xid;
        this.timeToLive = timeToLive * 1000;
    }

    protected boolean expired() {
        long time = date.getTime();
        if (time > lastAction + timeToLive)
            return true;
        return false;
    }

    protected int getXid() {
        return this.xid;
    }

    // Writes a data item
    protected void writeData(int xid, String key, RMItem value)
    {
        synchronized(m_data) {
            m_data.put(key, value);
        }
    }

    // Reads a data item
    protected RMItem readData(int xid, String key)
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