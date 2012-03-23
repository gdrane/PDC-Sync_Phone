package edu.ucla.cens.pdc.phone;

import java.util.Calendar;

import org.json.simple.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;

public class BackgroundService extends Service{

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
	
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class BackgroundServiceBinder extends Binder {
        BackgroundService getService() {
            return BackgroundService.this;
        }
    }

	
	@Override
	public void onCreate() {
		if(mStartTime == 0L) {
			Log.v(TAG,"WIFI STARTED");
			mStartTime = System.currentTimeMillis();
			// mHandler.removeCallbacks(mUpdateTimeTask);
        	// mHandler.postDelayed(mUpdateTimeTask, 1000);
		}    
		TelephonyManager manager = 
				(TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		_imei = manager.getDeviceId();
		_stopSensing = true;
		Log.i(TAG, "onCreate BackgroundService called command called");		
	}
	
	private static Runnable mUpdateFeatureTimeTask = new Runnable(){
		public void run(){
			//Log.v(TAG,"Storing Variance");
			Second.getReadingsService().updateVarArray();	
			if(!_stopSensing)
				mFeatureHandler.postAtTime(this,SystemClock.uptimeMillis() + ONE_SECOND);		
		}
	};

	private static Runnable mPutInDatabaseTask = new Runnable() {
		@SuppressWarnings("unchecked")
		public void run() {
			Log.v(TAG,"Put in Database called");
			JSONObject jsonObjAccel = Second.getReadingsService().
					addEvent();
			//Making way for new values
			Second.getReadingsService().clearVarArray();
			JSONObject jsonObjGPS = Second.getLocationListenerService().
					addGPSData();
			JSONObject jsonObjMerge = new JSONObject();
			jsonObjMerge.putAll(jsonObjGPS);
			jsonObjMerge.putAll(jsonObjAccel);
			jsonObjMerge.put("IMEI", _imei);
			NDNManager.getInstance().addObjectToRepo(jsonObjMerge.toString());
			Log.i(TAG,"JSON Object Merge: " + jsonObjMerge);
			
			if(!_stopSensing)
				mDatabaseHandler.postAtTime(this, 
						SystemClock.uptimeMillis() + ONE_MINUTE);
		}
	};
	
	public void startSensing() {
		_stopSensing = false;
		mFeatureHandler.removeCallbacks(mUpdateFeatureTimeTask);
    	mFeatureHandler.postDelayed(mUpdateFeatureTimeTask,900);
    	mDatabaseHandler.removeCallbacks(mPutInDatabaseTask);
    	mDatabaseHandler.postAtTime(mPutInDatabaseTask, ONE_MINUTE);	
	}
	
	public void  stopSensing() {
		_stopSensing = true;
		mDatabaseHandler.removeCallbacks(mPutInDatabaseTask);
		mFeatureHandler.removeCallbacks(mUpdateFeatureTimeTask);	
	}
	
	// This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
     private final IBinder mBinder = new BackgroundServiceBinder();
     
	 public static final int  ONE_SECOND = 1000;
	 public static final int  ONE_MINUTE = 60 * ONE_SECOND;
	 public static final int  ONE_HOUR = 60 * ONE_MINUTE; 
	 public static final int  ONE_DAY = 24 * ONE_HOUR;
	 public static final int ACCEL_MAGNITUDE_SERVICE_TYPE = 1;
	 
	 //Handler for callback timers
	 private long mStartTime = 0;
	 
	 //Handlers to calculate features
	 private static Handler mFeatureHandler = new Handler();
	 
	 private static Handler mDatabaseHandler = new Handler();
	  
	 private static boolean _stopSensing = false;
	 
	 private static String _imei; 
	 
	 private static final String TAG = "BACKGROUNDSERVICE";
	 
}
