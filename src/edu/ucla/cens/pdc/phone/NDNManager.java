package edu.ucla.cens.pdc.phone;

import java.io.IOException;

import org.ccnx.android.ccnlib.CCNxConfiguration;
import org.ccnx.android.ccnlib.CCNxServiceCallback;
import org.ccnx.android.ccnlib.CCNxServiceControl;
import org.ccnx.android.ccnlib.CCNxServiceStatus.SERVICE_STATUS;
import org.ccnx.android.ccnlib.CcndWrapper;
import org.ccnx.android.ccnlib.CcndWrapper.CCND_OPTIONS;
import org.ccnx.android.ccnlib.RepoWrapper.REPO_OPTIONS;
import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.config.ConfigurationException;
import org.ccnx.ccn.io.RepositoryFileOutputStream;
import org.ccnx.ccn.io.RepositoryOutputStream;
import org.ccnx.ccn.io.content.ConfigSlice;
import org.ccnx.ccn.io.content.ContentDecodingException;
import org.ccnx.ccn.profiles.ccnd.CCNDaemonException;
import org.ccnx.ccn.profiles.ccnd.SimpleFaceControl;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.ContentObject;
import org.ccnx.ccn.protocol.KeyLocator;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;
import org.ccnx.ccn.protocol.SignedInfo.ContentType;

import android.content.Context;
import android.util.Log;

/**
 * Top level class for NDN code.
 * 
 * @author Gauresh Rane
 */

public class NDNManager implements CCNxServiceCallback {
	
	private NDNManager () {

	}
	
	public static NDNManager getInstance() {
		if(_ndn_manager == null) {
			_ndn_manager = new NDNManager();
		}
		return _ndn_manager;
	}
	
	public void InitializeCCNx(Context ctx) {
		_ccnx_service = new CCNxServiceControl(ctx);
		_ccnx_service.registerCallback(this);
		_ccnx_service.setCcndOption(CCND_OPTIONS.CCND_DEBUG, "INFO");
		_ccnx_service.setRepoOption(REPO_OPTIONS.REPO_DEBUG, "INFO");
		_ccnx_service.startAllInBackground();
		CCNxConfiguration.config(ctx);
	}
	
	public void setCallbackInterface(NDNManagerCallback cb) {
		_callback_interface = cb;
	}

	@Override
	public void newCCNxStatus(SERVICE_STATUS st) {
		switch (st) {
		case CCND_INITIALIZING:
			_callback_interface.postProcess("Initializing CCNd ...");
			break;
		case CCND_OFF:
			_callback_interface.postProcess("CCNd is off.");
			break;
		case CCND_RUNNING:
			_callback_interface.postProcess("CCNd is running.");
			break;
		case CCND_TEARING_DOWN:
			_callback_interface.postProcess("Tearing down CCNd ...");
			break;
		case START_ALL_DONE:
			Log.i(TAG, "CCNx started. START_ALL_DONE");
			_callback_interface.postProcess("CCNx started", false);
			break;
		case START_ALL_ERROR:
			_callback_interface.postProcess("Error while starting CCNx");
			_callback_interface.postProcess("Error while starting CCNx", false);
			Log.e(TAG, "CCNx failed to start. START_ALL_ERROR");
			break;
		}
		
	}
	
	public synchronized boolean startCCNx()
	{
		Log.i(TAG, "RemoteHost is: " + _remotehost);
		Log.i(TAG, "Remote Port is:" + _remoteport);
		assert _ccnx_service != null : "CCNX Service is NULL";
		Log.i(TAG, "Start All Done");
		Log.i(TAG, _ccnx_service.getCcndStatus().toString());
		Log.i(TAG, _ccnx_service.getRepoStatus().toString());
		if (!_ccnx_service.isAllRunning()) {
			if(!SERVICE_STATUS.SERVICE_RUNNING.equals(
					_ccnx_service.getCcndStatus())) {
				_ccnx_service.startCcnd();
				_callback_interface.postProcess(
						"CCND wasn't running, starting CCND..");
			}
			if(!SERVICE_STATUS.SERVICE_RUNNING.equals(
					_ccnx_service.getRepoStatus())) {
				_ccnx_service.startRepo();
				_callback_interface.postProcess(
						"Key Repository wasn't running, starting Repo..");
			}
			Log.i(TAG, "Something was not started");
			_callback_interface.postProcess("Calm down, CCNx is not up yet", 
					false);
			return false;
		}

		// DataGenerator.start(_context,
		// 		_context.getResources().openRawResource(R.raw.test), 1, 1);

		_ccnx_service.connect();
		try {
		
			SimpleFaceControl.getInstance().connectTcp(_remotehost,
					_remoteport);
		}
		catch (CCNDaemonException e) {
			Log.e(TAG, "Unable to make connection", e);
			_callback_interface.postProcess(
					"Unable to connect to hub: " + e.getLocalizedMessage(), 
					true);
			return false;
		}
		return true;
	}
	
	public void addTestObject() {
		try {
			String timepass = "TIMEPASS";
			byte[] data = timepass.getBytes();
			RepositoryOutputStream ros = new RepositoryOutputStream(
					ContentName.fromNative(DATA_NAMESPACE + "/data1"), _locator,
					_ccn_handle.getDefaultPublisher(), ContentType.DATA, null, _ccn_handle);
			ros.write(data, 0, data.length);
			ros.close();
		} catch (MalformedContentNameStringException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public synchronized void startSync() {
		if(_ccnx_service.isAllRunning()) {
			try {
				ContentName topology_name = ContentName.fromNative(TOPOLOGY_NAMESPACE);
				ContentName data_prefix_name =  ContentName.fromNative(DATA_NAMESPACE);
				ConfigSlice.checkAndCreate(topology_name , data_prefix_name,
						null, _ccn_handle);
			} catch (MalformedContentNameStringException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ContentDecodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
		} else {
			_callback_interface.postProcess("CCND/CCNR may not be running, could not start sync this time");
		}
		
	}
	
	public void registerHandler() {
		try {
			Log.i(TAG, "Opening handle");
			_ccn_handle = CCNHandle.open();
			_ccn_handle.registerFilter(ContentName.fromNative("/ndn/ucla.edu/apps"), 
				CCNxListenerImpl.getInstance());
			_ccn_handle.keyManager().setKeyLocator(null, _locator);
			_locator = new KeyLocator(
							ContentName.fromNative("/ndn/ucla.edu/apps/test_app/key"), 
						_ccn_handle.getDefaultPublisher());

		} catch (ConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (MalformedContentNameStringException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public synchronized void shutdown()
	{
		try {
			_ccn_handle.unregisterFilter(ContentName.fromNative("/ndn/ucla.edu/apps"), 
					CCNxListenerImpl.getInstance());
		} catch (MalformedContentNameStringException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setNamespace(String namespace)
	{
		_namespace = namespace;
	}

	public void setRemotehost(String remotehost)
	{
		_remotehost = remotehost;
	}

	public void setRemoteport(int remoteport)
	{
		_remoteport = remoteport;
	}
	
	public CCNHandle getCCNHandle() {
		return _ccn_handle;
	}
	
	private static CcndWrapper _ccnd_wrapper;
	
	private CCNxServiceControl _ccnx_service;

	private NDNManagerCallback _callback_interface;
	
	private CCNHandle _ccn_handle;
	
	private final String TAG = "NDNManager";
	
	private String _namespace;
	
	private String _remotehost;
	
	private int _remoteport;
	
	private static NDNManager _ndn_manager;
	
	private static final String TOPOLOGY_NAMESPACE = "/ndn/ucla.edu/apps/synctopology" +
			"/test_app";
	
	private static final  String DATA_NAMESPACE = "/ndn/ucla.edu/apps/test_app/" +
			"repo";
	
	KeyLocator _locator = null;
	
}