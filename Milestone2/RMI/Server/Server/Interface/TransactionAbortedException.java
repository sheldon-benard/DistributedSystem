package Server.Interface;

public class TransactionAbortedException extends Exception
{
    private int m_xid = 0;

    public TransactionAbortedException(int xid, String msg)
    {
        super("The transaction " + xid + " is aborted:" + msg);
        m_xid = xid;
    }

    int getXId()
    {
        return m_xid;
    }
}
