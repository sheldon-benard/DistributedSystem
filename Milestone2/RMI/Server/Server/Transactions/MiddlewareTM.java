package Server.Transactions;

import java.util.*;
import Server.Middleware.*;


public class MiddlewareTM extends TransactionManager implements Runnable {

    private Integer startXid = 0;
    private int timetolive;
    private Middleware mw;

    public int start() {
        int xid;
        synchronized (startXid) {
            xid = ++this.startXid;
        }
        this.writeActiveData(xid, new Transaction(xid, this.timetolive));
        return xid;
    }

    public MiddlewareTM(int timetolive, Middleware mw) {
        super();
        this.mw = mw;
        this.timetolive = timetolive;
        (new Thread(this)).start();
    }

    public void run() {
        while(true) {
            try {
                synchronized(activeTransactions) {
                    System.out.println("Checking time to live");
                    Set<Integer> keyset = activeTransactions.keySet();
                    for (Integer key : keyset) {
                        Transaction t = activeTransactions.get(key);

                        if (t != null && t.expired()) {
                            System.out.println(t.getXid() + " expired; aborting this transaction");
                            this.mw.abort(t.getXid());
                        }
                    }
                }
                Thread.sleep(5000); // sleep 5 seconds

            } catch (Exception e)
            {
                System.out.println(e.getLocalizedMessage());
            }
        }
    }
}