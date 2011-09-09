package uk.org.smithfamily.mslogger.log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import uk.org.smithfamily.mslogger.ApplicationSettings;
import android.text.format.DateFormat;
import android.util.Log;

public enum FRDLogManager
{
    INSTANCE;
    private FRDLogFile           frdLog = new FRDLogFile();
    private FRDLogFileHeader     header = frdLog.getHeader();
    private FRDLogFileBody       body   = frdLog.getBody();
    private BufferedOutputStream os;
    private File                 logFile;
    private long                 startTime;

    public long getStartTime()
    {
        return startTime;
    }

    public void write(byte[] buffer) throws IOException
    {
        if (os == null)
        {
            startTime = System.currentTimeMillis();
            createLogFile();
            writeHeader();
        }
        body.addRecord(buffer);
        os.write(body.getCurrentRecord().getBytes());
    }

    private void writeHeader() throws IOException
    {

        os.write(header.getHeaderRecord());
    }

    private void createLogFile() throws FileNotFoundException
    {

        Date now = new Date();

        String fileName = DateFormat.format("yyyyMMddkkmmss", now).toString() + ".frd";

        logFile = new File(ApplicationSettings.INSTANCE.getDataDir(), fileName);

        os = new BufferedOutputStream(new FileOutputStream(logFile));
    }

    public FRDLogFile getFRDLogFile()
    {

        return frdLog;
    }

    public void close()
    {
        try
        {
            if (os != null)
            {
                os.flush();
                os.close();
            }
        }
        catch (IOException e)
        {
            Log.e(ApplicationSettings.TAG,"FRDLogManager.close()",e);
        }

        os = null;
        logFile = null;

    }

    public void stopLog()
    {
        close();
    }
}
