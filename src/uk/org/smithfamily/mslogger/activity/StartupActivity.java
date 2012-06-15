package uk.org.smithfamily.mslogger.activity;

import uk.org.smithfamily.mslogger.*;
import uk.org.smithfamily.mslogger.ecuDef.ECUFingerprint;
import uk.org.smithfamily.mslogger.ecuDef.Megasquirt;
import uk.org.smithfamily.mslogger.log.EmailManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.*;
import android.widget.TextView;

/**
 * 
 * 
 */
public class StartupActivity extends Activity
{
    private static final int SELECT_EGO        = 1;
    private static final int SELECT_MAP        = 2;
    private static final int SELECT_EMAIL      = 3;
    private static final int REQUEST_ENABLE_BT = 0;

    private class StartupHandler extends Handler
    {

        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            switch (msg.what)
            {
            case MSLoggerApplication.GOT_SIG:
                checkSig((String) msg.obj);
                break;
            case MSLoggerApplication.MESSAGE_TOAST:
                showMessage(msg);
            }
        }
    }

    private Handler          mHandler = null;
    private BluetoothAdapter mBluetoothAdapter;
    private TextView         msgBox;

    /**
     *  Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mHandler = new StartupHandler();
        setContentView(R.layout.startup);
        msgBox = (TextView) findViewById(R.id.identify_progress_msg);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Bluetooth is not supported on this Android device
        if (mBluetoothAdapter == null)
        {
            finishDialogNoBluetooth();
            return;
        }

        boolean bluetoothOK = mBluetoothAdapter.isEnabled();
        
        // Bluetooth is enabled, we can start!
        if (bluetoothOK)
        {
            initSequence();
        }
        // Bluetooth is not enabled, request the user to enable it
        else
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    /**
     * Initialise the sequence of prompt to the user to select all his preferences
     */
    private void initSequence()
    {
        if (ApplicationSettings.INSTANCE.getPref("maptype") == null)
        {
            selectMAP();
            return;
        }
        if (ApplicationSettings.INSTANCE.getPref("egotype") == null)
        {
            selectEGO();
            return;
        }
        if (ApplicationSettings.INSTANCE.getPref("email_offered") == null)
        {
            ApplicationSettings.INSTANCE.setPref("email_offered", "yes");
            selectEmail();
            return;
        }
        if (!ApplicationSettings.INSTANCE.btDeviceSelected())
        {
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, MSLoggerApplication.REQUEST_CONNECT_DEVICE);
            return;
        }
        if(ApplicationSettings.INSTANCE.getEcuDefinition() == null)
        {
            startFingerprint();
        }
        else
        {

            Intent intent = new Intent();

            setResult(Activity.RESULT_OK, intent);

            finish();
        }

    }

    /**
     *  Start the activity to prompt the user for his email address
     */
    private void selectEmail()
    {
        Intent i = new Intent(this, EmailSelectActivity.class);
        startActivityForResult(i, SELECT_EMAIL);
    }

    /**
     *  Start the activity to prompt the user for his EGO device
     */
    private void selectEGO()
    {
        Intent i = new Intent(this, EGOSelectActivity.class);
        startActivityForResult(i, SELECT_EGO);
    }

    /**
     *  Start the activity to prompt the user for his MAP sensor
     */
    private void selectMAP()
    {
        Intent i = new Intent(this, MAPSelectActivity.class);
        startActivityForResult(i, SELECT_MAP);

    }

    /**
     * 
     * @param msg
     */
    public void showMessage(Message msg)
    {
        String text = msg.getData().getString(MSLoggerApplication.MSG_ID);
        msgBox.setText(text);
    }

    /**
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
        case REQUEST_ENABLE_BT:
            if (resultCode == Activity.RESULT_OK)
            {
                initSequence();
            }
            else
            {
                finishDialogNoBluetooth();
            }
            break;
        case MSLoggerApplication.REQUEST_CONNECT_DEVICE:

            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK)
            {
                // Get the device MAC address
                String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

                ApplicationSettings.INSTANCE.setBluetoothMac(address);
                initSequence();
            }
            break;

        case SELECT_EGO:
        case SELECT_MAP:
        case SELECT_EMAIL:
            initSequence();
            break;

        }
    }

    /**
     * 
     * @param sig
     */
    public void checkSig(String sig)
    {
        Megasquirt ecu = ApplicationSettings.INSTANCE.getEcuForSig(sig);
        
        // ECU is not supported by MSLogger
        if (ecu == null)
        {
            unrecognisedEcu(sig);
        }
        else
        {
            ApplicationSettings.INSTANCE.setEcu(ecu);

            Intent intent = new Intent();

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();

        }
    }

    /**
     * 
     */
    private void startFingerprint()
    {
        Thread t = new Thread(new ECUFingerprint(mHandler, mBluetoothAdapter));
        t.start();
    }

    /**
     * It was determinated that the android device don't support Bluetooth, so we tell the user
     */
    public void finishDialogNoBluetooth()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.no_bt).setIcon(android.R.drawable.ic_dialog_info).setTitle(R.string.app_name).setCancelable(false)
                .setPositiveButton(R.string.bt_ok, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * It was determinated that the Megasquirt ECU detected is not supported by MSLogger, ask the user if he want
     * to tell the developer about it
     * 
     * @param sig
     */
    private void unrecognisedEcu(final String sig)
    {
        if (isFinishing())
        {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unrecognised_ecu).setTitle(R.string.app_name).setPositiveButton(R.string.bt_ok, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                constructEmail(sig);
            }

        }).setNegativeButton(R.string.cancel_buy_button, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });

        AlertDialog alert = builder.create();
        if (!isFinishing())
        {
            alert.show();
        }
    }

    /**
     * Construct the email to send to the developer about the unrecognised firmware signature
     * 
     * @param sig The signature of the unsupported firmware
     */
    private void constructEmail(String sig)
    {
        EmailManager.email(this, "dave@mslogger.co.uk", null, "Unrecognised firmware signature", "An unknown firmware was detected with a signature of '" + sig
                + "'.\n\nPlease consider this for the next release.", null);

    }

}