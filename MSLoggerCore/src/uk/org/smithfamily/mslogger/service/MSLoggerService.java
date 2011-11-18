package uk.org.smithfamily.mslogger.service;

import uk.org.smithfamily.mslogger.ApplicationSettings;
import uk.org.smithfamily.mslogger.R;
import uk.org.smithfamily.mslogger.activity.MSLoggerActivity;
import uk.org.smithfamily.mslogger.ecuDef.Megasquirt;
import uk.org.smithfamily.mslogger.log.DatalogManager;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

public class MSLoggerService extends Service
{
    public boolean isLogging()
	{
		return logging;
	}

	private static final int MSLOGGERSERVICE_ID = 0;

    public class MSLoggerBinder extends Binder
    {
        public MSLoggerService getService()
        {
            return MSLoggerService.this;
        }
    }

    private final IBinder mBinder = new MSLoggerBinder();
	private boolean	logging;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        return mBinder;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        disconnect();
    }

    public double getValue(String channelName)
    {
        return ApplicationSettings.INSTANCE.getEcuDefinition().getValue(channelName);
    }

    private void disconnect()
    {
        Toast.makeText(this, R.string.disconnecting_from_ms, Toast.LENGTH_LONG).show();
        ApplicationSettings.INSTANCE.getEcuDefinition().stop();
        removeNotification();
    }

    private void showNotification()
    {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

        int icon = R.drawable.icon;
        long when = System.currentTimeMillis();

        Notification notification = new Notification(icon, getString(R.string.mslogger_is_running), when);

        Context context = getApplicationContext();

        Intent notificationIntent = new Intent(this, MSLoggerActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notification.setLatestEventInfo(context, getString(R.string.mslogger_is_running),
                getString(R.string.logging_to, DatalogManager.INSTANCE.getFilename()), contentIntent);

        mNotificationManager.notify(MSLOGGERSERVICE_ID, notification);
    }

    private void removeNotification()
    {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
        mNotificationManager.cancelAll();
    }

    public void stopLogging()
    {
        logging = false;
        ApplicationSettings.INSTANCE.getEcuDefinition().stopLogging();
        removeNotification();
    }

    public void startLogging()
    {
        showNotification();

        ApplicationSettings.INSTANCE.getEcuDefinition().startLogging();
        logging = true;
    }

    public void connectToECU()
    {
        ApplicationSettings.INSTANCE.getEcuDefinition().initialiseConnection();
    }
}
