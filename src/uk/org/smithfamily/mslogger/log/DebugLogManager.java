package uk.org.smithfamily.mslogger.log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import uk.org.smithfamily.mslogger.ApplicationSettings;
import android.os.Environment;
import android.text.format.DateFormat;

/**
 * Class that is used to help debugging. Will log the specified log level to a log file that can be sent by users to developers
 */
public enum DebugLogManager
{
    INSTANCE;

    private File       logFile;
    private FileWriter os;
    private String     absolutePath;

    /**
     * Create the log file where the log will be saved
     * @throws IOException
     */
    private void createLogFile() throws IOException
    {
        if (!ApplicationSettings.INSTANCE.isWritable())
        {
            return;
        }

        File dir = new File(Environment.getExternalStorageDirectory(), "MSLogger");
        dir.mkdirs();

        if (logFile == null)
        {
            Date now = new Date();

            String fileName = DateFormat.format("yyyyMMddkkmmss", now).toString() + ".log";
            logFile = new File(dir, fileName);
        }
        absolutePath = logFile.getAbsolutePath();
        os = new FileWriter(logFile, true);

    }

    /**
     * Main function to write in the log file
     * 
     * @param s         The log to write
     * @param logLevel  The level of log
     */
    public synchronized void log(String s, int logLevel)
    {
        // Make sure the user want to save a log of that level
        if (!checkLogLevel(logLevel))
        {
            return;
        }
        // Make sure we have write permission
        if (!ApplicationSettings.INSTANCE.isWritable())
        {
            return;
        }

        try
        {
            if (logFile == null || os == null)
                createLogFile();

            long now = System.currentTimeMillis();
            os.write(String.format("%tc:%tL:%s:%s\n", now, now, Thread.currentThread().getName(), s));
            os.flush();
        }
        catch (IOException e)
        {
            System.err.println("Could not write '" + s + "' to the log file : " + e.getLocalizedMessage());
        }
    }

    /**
     * Check if the user preference is set to accept the specified log level
     * 
     * @param logLevel   The specified log level of the user
     * @return           true if log is accepted, false otherwise
     */
    private boolean checkLogLevel(int logLevel)
    {
        return (ApplicationSettings.INSTANCE.getLoggingLevel() <= logLevel);
    }

    /**
     * Log exception into the log file
     * 
     * @param ex    The exception to log
     */
    public synchronized void logException(Exception ex)
    {
        // Make sure we have write permission
        if (!ApplicationSettings.INSTANCE.isWritable())
        {
            return;
        }

        if (os == null)
        {
            try
            {
                createLogFile();
            }
            catch (IOException e)
            {
                System.err.println("Could not create the log file : " + e.getLocalizedMessage());
            }
        }
        PrintWriter pw = new PrintWriter(os);
        try
        {
            os.write(ex.getLocalizedMessage() + "\n");
        }
        catch (IOException e)
        {
        }
        ex.printStackTrace(pw);
        try
        {
            os.flush();
        }
        catch (IOException e)
        {
        }
    }

    /**
     * @return The absolute path to the log file
     */
    public String getAbsolutePath()
    {
        return absolutePath;
    }

    /**
     * This helper function can be used to log a bytes array to log, prefixed by the specified message
     * 
     * @param msg       Log to be saved
     * @param result    Bytes to be saved after the log
     * @param logLevel  The log level
     */
    public void log(String msg, byte[] result, int logLevel)
    {
        if (!checkLogLevel(logLevel))
        {
            return;
        }
        
        StringBuffer b = new StringBuffer(msg);
        for (int i = 0; i < result.length; i++)
        {
            b.append(String.format(" %02x", result[i]));
        }
        log(b.toString(), logLevel);
    }
}
