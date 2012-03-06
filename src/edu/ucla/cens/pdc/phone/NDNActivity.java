package edu.ucla.cens.pdc.phone;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Screen to make configuration choices. No CCNx code here. After user presses
 * Connect button, we startup the Android Manager.
 */
public final class NDNActivity extends Activity implements
		OnClickListener, NDNManagerCallback {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);		
		if (android.os.Build.VERSION.SDK_INT > 9) {
		      StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		      StrictMode.setThreadPolicy(policy);
		}
		
		setContentView(R.layout.main);
	
		Log.i(TAG, "onCreate()");
	
		_tv_status = (TextView) findViewById(R.id.tvStatus);
		if (_tv_status == null)
			throw new Error("Could not find tvStatus");
		
		_connect_remote_button = (Button) findViewById(R.id.Connect);
		if(_connect_remote_button == null)
			throw new Error("Could not find Remote Button");
		
		_sync_button = (Button) findViewById(R.id.Sync);
		if(_sync_button == null)
			throw new Error("Could not find Sync Button");
		_register_handler_button = (Button) findViewById(R.id.RegisterHandler);
		if(_register_handler_button == null)
			throw new Error("Could not find Register Handler Button");
		_start_sensing = (Button) findViewById(R.id.StartSensing);
		_stop_sensing = (Button) findViewById(R.id.StopSensing);
		
		_register_handler_button.setOnClickListener(this);
		_sync_button.setOnClickListener(this);
		_connect_remote_button.setOnClickListener(this);
		_start_sensing.setOnClickListener(this);
		_stop_sensing.setOnClickListener(this);
	
		_etRemoteHost = (EditText) findViewById(R.id.etRemoteIP);
		_etRemotePort = (EditText) findViewById(R.id.etRemotePort);
		
		_ndn_manager = NDNManager.getInstance();
		_ndn_manager.setCallbackInterface(this);
		_ndn_manager.InitializeCCNx(getApplicationContext());
		Log.i(TAG, "Binding");
		bindService(new Intent(NDNActivity.this, 
	            ReadingsService.class), mReadingsServiceConnection, 
	            Context.BIND_AUTO_CREATE);
		bindService(new Intent(NDNActivity.this, 
					BackgroundService.class), mBackgroundServiceConnection, 
					Context.BIND_AUTO_CREATE);
		bindService(new Intent(NDNActivity.this, LocationListenerService.class),
	            mLocationListenerServiceConnection, 
	            Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onClick(View v)
	{
		Log.d(TAG, "OnClickListener " + String.valueOf(v.getId()));
	
		switch (v.getId()) {
		
			case R.id.Connect:
					if(_etRemoteHost.getText().toString().equals("") ||
						_etRemotePort.getText().toString().equals(""))
					{
					postProcess("You have not entered remote host ip or port", 
					false);
					}
					_ndn_manager.setRemotehost(
							_etRemoteHost.getText().toString());
					_ndn_manager.
						setRemoteport(Integer.parseInt(_etRemotePort.
										getText().toString()));
					_ndn_manager.startCCNx();
					break;
			case R.id.RegisterHandler:
					_ndn_manager.registerHandler();
					break;
	
			case R.id.Sync:
					_ndn_manager.startSync();
					_ndn_manager.addTestObject();
					break;
					
			case R.id.StartSensing:
					mBackgroundService.startSensing();
					break;
			case R.id.StopSensing:
					mBackgroundService.stopSensing();
					break;
	
			default:
					break;
		}
	}
	
	
	public void onDestroy()
	{
		_ndn_manager.shutdown();
		unbindService(mReadingsServiceConnection);
		unbindService(mBackgroundServiceConnection);
		unbindService(mLocationListenerServiceConnection);
		super.onDestroy();
	}
	
	@Override
	public void postProcess(boolean status)
	{
		final Message msg = Message.obtain();
		msg.what = 1;
		msg.obj = status;
		_handler.sendMessage(msg);
	}
	
	@Override
	public void postProcess(String text, boolean show_longer)
	{
		final Message msg = Message.obtain();
		msg.what = 0;
		msg.arg1 = show_longer ? 1 : 0;
		msg.obj = text;
		_handler.sendMessage(msg);
	}
	
	@Override
	public void postProcess(String text)
	{
		final Message msg = Message.obtain();
		msg.what = 2;
		msg.obj = text;
		_handler.sendMessage(msg);
	}
	
	private final Handler _handler = new Handler() {
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what) {
			case 0: {
				final String text = (String) msg.obj;
				final Toast toast = Toast.makeText(NDNActivity.this, text,
						msg.arg1 == 0 ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
				toast.show();
				break;
			}
			case 1:
				final boolean status = (Boolean) msg.obj;
				Log.i(TAG, "Connection State Changed");
				if (status) {
					_conn_state = ConnectionState.STARTED;
				} else {
					_conn_state = ConnectionState.INITIAL;
				}
				// enableView(status);
				break;
			case 2:
				final String text = (String) msg.obj;
				_tv_status.setText(text);
			}
		}
	};
	
	 private ServiceConnection mBackgroundServiceConnection = 
			 new ServiceConnection() {
		 public void onServiceConnected(ComponentName className, IBinder service) {
	            // This is called when the connection with the service has been
	            // established, giving us the service object we can use to
	            // interact with the service.  Because we have bound to a explicit
	            // service that we know is running in our own process, we can
	            // cast its IBinder to a concrete class and directly access it.
	            mBackgroundService =
	            		((BackgroundService.BackgroundServiceBinder)service).
	            		getService();
	            Log.i(TAG, "Background Service Connected");
	        }
	
		 public void onServiceDisconnected(ComponentName className) {
	            // This is called when the connection with the service has been
	            // unexpectedly disconnected -- that is, its process crashed.
	            // Because it is running in our same process, we should never
	            // see this happen.
	            mBackgroundService = null;
	        }
	 };
	
	 private ServiceConnection mLocationListenerServiceConnection = 
			 new ServiceConnection() {
		 public void onServiceConnected(ComponentName className, IBinder service) {
	            // This is called when the connection with the service has been
	            // established, giving us the service object we can use to
	            // interact with the service.  Because we have bound to a explicit
	            // service that we know is running in our own process, we can
	            // cast its IBinder to a concrete class and directly access it.
	            mLocationListenerService = 
	            		((LocationListenerService.
	            				LocationListenerServiceBinder)service).
	            				getService();
	            Log.i(TAG, "Location Listener Service Connected");
	        }
	
		 public void onServiceDisconnected(ComponentName className) {
	            // This is called when the connection with the service has been
	            // unexpectedly disconnected -- that is, its process crashed.
	            // Because it is running in our same process, we should never
	            // see this happen.
	            mLocationListenerService = null;
	        }
	 };
	 
	  private ServiceConnection mReadingsServiceConnection = 
			  new ServiceConnection() {
	        public void onServiceConnected(ComponentName className, 
	        		IBinder service) {
	            // This is called when the connection with the service has been
	            // established, giving us the service object we can use to
	            // interact with the service.  Because we have bound to a explicit
	            // service that we know is running in our own process, we can
	            // cast its IBinder to a concrete class and directly access it.
	            mReadingsService = 
	            		((ReadingsService.ReadingsServiceBinder)service).
	            		getService();
	            Log.i(TAG, "Acclerometer Readings Service Connected");
	        }
	
	        public void onServiceDisconnected(ComponentName className) {
	            // This is called when the connection with the service has been
	            // unexpectedly disconnected -- that is, its process crashed.
	            // Because it is running in our same process, we should never
	            // see this happen.
	            mReadingsService = null;
	        }
	    };
	    
    public static ReadingsService getReadingsService() {
    	return mReadingsService;
    }
    
    public static LocationListenerService getLocationListenerService() {
    	return mLocationListenerService;
    }
    
    public static BackgroundService getBackgroundService() {
    	return mBackgroundService;
    }

	enum ConnectionState {
		INITIAL, STARTED
	}
	
	private ConnectionState _conn_state = ConnectionState.INITIAL;
	
	protected final static String TAG = "NDNActivity";
	
	private NDNManager _ndn_manager;
	
	protected static final String PREF_NAMESPACE = "namespace";
	
	protected static final String PREF_REC_URL = "recURL";
	
	protected static final String PREF_REMOTEHOST = "remotehost";
	
	protected static final String PREF_REMOTEPORT = "remoteport";
	
	protected static final String PREF_REC_AUTH = "recAuth";	
	
	protected static final String PREF_REC_STARTPDVDONE = "startPDVDone";
	
	protected static final String PREF_REC_REMHOSTIP = "remoteHostIP";
	
	protected static final String PREF_REC_REMHOSTPORT = "remoteHostPort";
	
	private Button _sync_button, _register_handler_button, _start_sensing,
	_connect_remote_button, _stop_sensing;
	
	private TextView _tv_status;
	
	private EditText _etRemoteHost, _etRemotePort;
	
	private static ReadingsService mReadingsService;
	
	private static BackgroundService mBackgroundService;
	
	private static LocationListenerService mLocationListenerService;
}
