package Server.Transactions;

import java.util.*;


public class MiddlewareTM extends TransactionManager implements Runnable {

    private Integer startXid = 0;

    public int start() {
        int xid;
        synchronized (startXid) {
            xid = ++this.startXid;
        }
        this.writeActiveData(xid, new Transaction(xid, this.timetolive));
        return xid;
    }

    public MiddlewareTM(int timetolive) {
        super();
        this.timetolive = timetolive;
        (new Thread(this)).start();
    }

    public void run() {
        while(true) {
            try {
                synchronized(activeTransactions) {
                    Set<Integer> keyset = activeTransactions.keySet();
                    for (Integer key : keyset) {
                        Transaction t = activeTransactions.get(key);

                        if (t.expired()) {
                            System.out.println(t.getXid() + " expired; aborting this transaction");
                            this.abort(t.getXid());
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