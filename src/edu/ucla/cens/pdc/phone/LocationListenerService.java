package edu.ucla.cens.pdc.phone;

import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.widget.Toast;

public class LocationListenerService extends Service implements LocationListener
{
	private static EventDataSQLHelper eventsData;
	private static String _imei;
	private String _latitude, _longitude;
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		LocationManager mlocManager = 
				(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		mlocManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 
				100000000, 0, this);
		TelephonyManager manager = 
				(TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		_imei = manager.getDeviceId();
		eventsData = new EventDataSQLHelper(getApplicationContext());
	}
	
	/**
	 * Class for clients to access.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with
	 * IPC.
	 */
	public class LocationListenerServiceBinder extends Binder {
	    LocationListenerService getService() {
	        return LocationListenerService.this;
	    }
	}
	
	  @Override
	  public void onLocationChanged(Location loc)
	  {
	    _latitude = loc.getLatitude() + "";
	    _longitude = loc.getLongitude() + "";
	  }
	
	  @Override
	  public void onProviderDisabled(String provider)
	  {
	    Toast.makeText(	getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT ).show();
	  }
	
	  @Override
	  public void onProviderEnabled(String provider)
	  {
	    Toast.makeText( getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
		  }
	
		  @Override
		  public void onStatusChanged(String provider, int status, Bundle extras)
		  {
		
		  }
		  
		  
	 @Override
	 public void onDestroy() {
		super.onDestroy();
	    eventsData.close();
	 }
	  
	  public void addGPSData()
	  {
	  	SQLiteDatabase db = eventsData.getWritableDatabase();
	  	JSONObject jsonObject = new JSONObject();
	  	try {
	  		jsonObject.put("latitude", _latitude);
	  		jsonObject.put("longitude", _longitude);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		ContentValues values = new ContentValues();
		Calendar calendar = Calendar.getInstance();
		values.put(EventDataSQLHelper.USERID, _imei);
		values.put(EventDataSQLHelper.TIME, 
				DateFormat.format("yyyy-MM-dd kk:mm:ss", 
						calendar.getTime()) + "");
	    values.put(EventDataSQLHelper.GPS, jsonObject.toString());
	    db.insert(EventDataSQLHelper.TABLE2, null, values);
	  }
  
	  private Binder mBinder = new LocationListenerServiceBinder();
	 
}