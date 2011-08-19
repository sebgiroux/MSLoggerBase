package uk.org.smithfamily.mslogger.comms;

import java.io.IOException;
import java.util.UUID;

import uk.org.smithfamily.mslogger.ApplicationSettings;
import uk.org.smithfamily.mslogger.log.DebugLogManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class SerialComm extends MsComm
{
	UUID						RFCOMM_UUID	= UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	protected BluetoothSocket	sock;

	private String locateAdapter()
	{
		return ApplicationSettings.INSTANCE.getBluetoothMac();
	}

	boolean init()
	{
		String btAddr = locateAdapter();
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);

		// registerReceiver(mReceiver, filter); // Don't forget to unregister
		// during onDestroy
		// mBluetoothAdapter.startDiscovery();
		BluetoothDevice remote = mBluetoothAdapter.getRemoteDevice(btAddr);
		try
		{
			sock = remote.createRfcommSocketToServiceRecord(RFCOMM_UUID);
			sock.connect();
			is = sock.getInputStream();
			os = sock.getOutputStream();
			setConnected(true);
		}
		catch (IOException e)
		{
			Log.e("BT", "IOException", e);
			DebugLogManager.INSTANCE.logException(e);
			return false;
		}
		return true;
	}

	@Override
	protected boolean openDevice()
	{
		return init();
	}

	@Override
	protected boolean closeDevice(boolean force)
	{
		try
		{
			os.close();
			is.close();
			sock.close();
			setConnected(false);
		}
		catch (IOException e)
		{
			DebugLogManager.INSTANCE.logException(e);
		}

		return true;
	}
}