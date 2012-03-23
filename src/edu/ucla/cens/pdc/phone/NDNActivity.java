package edu.ucla.cens.pdc.phone;

import edu.ucla.cens.pdc.phone.Second;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
	
	private class ProgressThread extends Thread {
		public void run() {
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
			_ndn_manager.registerHandler();
			_ndn_manager.startSync();
			startActivity(new Intent(NDNActivity.this,Second.class));
		}
	}
	
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
	
		_tv_status = (TextView) findViewById(R.id.tvStatusMain);
		if (_tv_status == null)
			throw new Error("Could not find tvStatus");
		
		_connect_remote_button = (Button) findViewById(R.id.Connect);
		if(_connect_remote_button == null)
			throw new Error("Could not find Remote Button");
		
		_connect_remote_button.setOnClickListener(this);
		
		_etRemoteHost = (EditText) findViewById(R.id.etRemoteIP);
		_etRemotePort = (EditText) findViewById(R.id.etRemotePort);
		
		/*_sync_button = (Button) findViewById(R.id.Sync);
		if(_sync_button == null)
			throw new Error("Could not find Sync Button");
		_register_handler_button = (Button) findViewById(R.id.RegisterHandler);
		if(_register_handler_button == null)
			throw new Error("Could not find Register Handler Button");
		_start_sensing = (Button) findViewById(R.id.StartSensing);
		_stop_sensing = (Button) findViewById(R.id.StopSensing);
		
		_register_handler_button.setOnClickListener(this);
		_sync_button.setOnClickListener(this);
		_start_sensing.setOnClickListener(this);
		_stop_sensing.setOnClickListener(this);*/
		
		_ndn_manager = NDNManager.getInstance();
		_ndn_manager.setCallbackInterface(this);
		_ndn_manager.InitializeCCNx(getApplicationContext());
		Log.i(TAG, "Binding");
	
		
		_pd = new ProgressDialog(this);
	    _pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	    _pd.setMessage("Connecting to Server");
	    //pd.setIndeterminate(false);
	    _pd.setCancelable(true);
	    
	    _progThr = new ProgressThread();
	}
	
	@Override
	public void onClick(View v)
	{
		Log.d(TAG, "OnClickListener " + String.valueOf(v.getId()));
	
		switch (v.getId()) {
		
			case R.id.Connect:
					_pd.show();
					_progThr.start();
					/*
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
					*/
					break;
			default:
					break;
		}
	}
	
	
	public void onDestroy()
	{
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
	
	private EditText _etRemoteHost, _etRemotePort;
	
	private ProgressThread _progThr;
	
	private ProgressDialog _pd;
	
	private TextView _tv_status;
}
