package uk.org.smithfamily.mslogger.comms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Observable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import uk.org.smithfamily.mslogger.ApplicationSettings;
import android.util.Log;

public abstract class MsComm extends Observable
{
    protected InputStream  is;
    protected OutputStream os;
    private boolean        connected       = false;
    private boolean        writeBlocks     = false;
    private int            interWriteDelay = 0;
    protected long         lastComms       = System.currentTimeMillis();
    ExecutorService        executor        = Executors.newFixedThreadPool(1);

    protected abstract boolean openDevice();

    protected abstract boolean closeDevice(boolean force);

    protected synchronized void throttle()
    {

        long now = System.currentTimeMillis();
        if ((lastComms + interWriteDelay) <= now)
        {
            lastComms = now;
            return;
        }
        long duration = now - lastComms + interWriteDelay;
        try
        {
            Thread.sleep(duration);
        }
        catch (InterruptedException e)
        {
            Log.e(ApplicationSettings.TAG,"throttle()",e);
        }

    }

    protected MsComm()
    {
    }

    private synchronized void testConnection() throws LostCommsException
    {
        if (!connected && !openDevice())
            throw new LostCommsException();
    }

    public synchronized void flush() throws LostCommsException
    {
        if(os == null || is == null)
            return;
        
        try
        {
            os.flush();
            @SuppressWarnings("unused")
            int cnt = 0;
            while (is.available() > 0)
            {
                cnt = is.read();
            }
        }
        catch (IOException e)
        {
            Log.e(ApplicationSettings.TAG,"flush()",e);
        }
    }

    public synchronized void readWithTimeout(final byte[] bytes, long timeout, TimeUnit unit) throws LostCommsException
    {
 /*
         testConnection();
        FutureTask<Boolean> future = new FutureTask<Boolean>(new Callable<Boolean>()
        {
            public Boolean call()
            {
                try
                {
                    read(bytes);
                }
                catch (LostCommsException e)
                {
                    DebugLogManager.INSTANCE.logException(e);
                }
                return true;
            }
        });
        executor.execute(future);
        try
        {
            future.get(timeout, unit);
        }
        catch (Exception e)
        {
            throw new LostCommsException(e);

        }
*/
        read(bytes);
    }

    public synchronized void read(byte[] bytes) throws LostCommsException
    {
        testConnection();
        try
        {
            int nBytes = bytes.length;
            int bytesRead = 0;
            byte[] buffer = new byte[bytes.length];
            // System.out.println("Want to read " + bytes.length + " is has " +
            // is.available());
            Log.d(ApplicationSettings.TAG,"Need to read "+bytes.length+" bytes");
            while (bytesRead < nBytes)
            {
                //@SuppressWarnings("unused")
                //int available = is.available();
                
                int result = is.read(buffer, bytesRead, nBytes - bytesRead);
                        if (result == -1)
                    break;
                        
                bytesRead += result;
                //Log.d(ApplicationSettings.TAG,"Read "+result+" bytes "+(bytes.length-bytesRead)+" to go.");
            }
            
            synchronized (bytes)
            {
                System.arraycopy(buffer, 0, bytes, 0, bytes.length);
            }
        }
        catch (IOException e)
        {
            throw new LostCommsException(e);
        }
        finally
        {
        }
    }

    public synchronized boolean write(byte[] buf)
    {
        if (!connected && !openDevice())
            return false;

        boolean ok = true;
        try
        {
//            throttle();

            if (writeBlocks)
            {
                os.write(buf);
            }
            else
            {
                for (int n = 0; n < buf.length; n++)
                {
                    os.write(buf[n]);
                    throttle();
                }
            }

        }
        catch (IOException e)
        {
            Log.e(ApplicationSettings.TAG,"MsComm.write()",e);
            close();
            ok = false;
        }
        return ok;

    }

    public synchronized String read(int nBytes)
    {
        if (!connected && !openDevice())
            return null;

        StringBuffer bytes = new StringBuffer();

        try
        {
            int i = 0;
            while (i < nBytes && is.available() > 0)
            {
                int c = is.read();
                if (c == -1)
                    break;
                bytes.append((char) c);
            }
            return bytes.toString();
        }
        catch (IOException e)
        {
            Log.e(ApplicationSettings.TAG,"MsComm.read()",e);
            close();
            return "";
        }

    }

    public synchronized boolean isConnected()
    {
        return connected;
    }

    public synchronized void setConnected(boolean connected)
    {
        this.connected = connected;
        notifyObservers();
    }

    public synchronized InputStream getIs()
    {
        return is;
    }

    public synchronized OutputStream getOs()
    {
        return os;
    }

    protected synchronized void open()
    {
        openDevice();
    }

    public synchronized void close()
    {
        closeDevice(true);
    }

    public synchronized boolean openConnection()
    {
        close();
        open();
        return isConnected();
    }

    public synchronized String getSignature(byte[] sigCommand) throws LostCommsException
    {
        testConnection();
        String lastVal = "NotConnected";

        String sig = "NotGotItYet";
        for (int x = 0; x < 10 && !lastVal.equals(sig); x++)
        {
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            flush();
            lastVal = sig;
            write(sigCommand);
            throttle();
            sig = read(63);
        }
        flush();
        return sig;
    }

    public synchronized void setInterWriteDelay(int interWriteDelay)
    {
        this.interWriteDelay = interWriteDelay;
    }

}
