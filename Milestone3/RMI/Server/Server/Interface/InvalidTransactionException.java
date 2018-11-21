package Server.Interface;

public class InvalidTransactionException extends Exception
{
    private int m_xid = 0;

    public InvalidTransactionException(int xid, String msg)
    {
        super("Transaction " + xid + " is invalid:" + msg);
        m_xid = xid;
    }

    int getXId()
    {
        return m_xid;
    }
}
