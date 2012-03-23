package edu.ucla.cens.pdc.phone;

import java.util.ArrayList;
import java.util.Calendar;

import org.json.simple.JSONObject;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.Log;

public class ReadingsService extends Service implements SensorEventListener {
	
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
		
	/**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class ReadingsServiceBinder extends Binder {
        ReadingsService getService() {
            return ReadingsService.this;
        }
    }

	@Override
	public void onCreate() {
		TelephonyManager manager = 
				(TelephonyManager)getSystemService(TELEPHONY_SERVICE);
	    _imei = manager.getDeviceId();
	     
	    //for accelerometer
	    sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
	    sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				100000000);
	    eventsData = new EventDataSQLHelper(getApplicationContext()); 
	    Log.i(TAG, "onCreate ReadingsService called command called");
	    
		// super.onResume();
		Log.v("aditya", "inside on resume");
		EventDataSQLHelper mSQLDataHelper = new EventDataSQLHelper(this);
		SQLiteDatabase db = mSQLDataHelper.getReadableDatabase();
		SQLiteDatabase dbWrite = mSQLDataHelper.getWritableDatabase();
		Cursor curRead = 
				db.query(EventDataSQLHelper.TABLE, 
						new String[]{"user_id","time"},null,null,null,null,null);
		Log.v("count","count is"+curRead.getCount()+"");
		if(curRead.getCount() > 120){
			curRead.moveToFirst();
			dbWrite.delete(EventDataSQLHelper.TABLE, null, null);
			Log.v("resume", "Deleting Records");
		}
		curRead.close();
		// for location 
		curRead = db.query(EventDataSQLHelper.TABLE2,
				new String[]{"user_id","time"},null,null,null,null,null);
		Log.v("count","count for "+
				EventDataSQLHelper.TABLE2+" is "+curRead.getCount()+"");
		if(curRead.getCount() > 120){
			curRead.moveToFirst();
			dbWrite.delete(EventDataSQLHelper.TABLE2, null, null);
			Log.v("resume", "Deleting Records");
		}
		curRead.close();
		db.close();
		dbWrite.close();
  	}
  
	 @Override
	 public void onDestroy() {
		super.onDestroy();
	    eventsData.close();
	 }
  
	private double getVariance(){
		ArrayList<Float> tempXList;
		ArrayList<Float> tempYList;
		ArrayList<Float> tempZList;
		
	  	double sum = 0.0;
	  	double var = 0.0;
	  	double avg = 0.0;
	  	
	  	//Getting values from buffers	    	
	  	tempXList = modifyXValues(true,false,false,0);
	  	
	  	tempYList = modifyYValues(true,false,false,0);
	
	  	tempZList = modifyZValues(true,false,false,0);
	  	
	  	ArrayList<Double> tempForce = new ArrayList<Double>();
	  	double grav = SensorManager.GRAVITY_EARTH;
	  	for(int i = 0 ;i < tempZList.size();i++){
	  		float xsquare = (float)Math.pow(tempXList.get(i)/grav,2);
	  		float ysquare = (float)Math.pow(tempYList.get(i)/grav,2);
	  		float zsquare = (float)Math.pow(tempZList.get(i)/grav,2);
	  		double eucDist = Math.sqrt(xsquare + ysquare + zsquare);
	  		eucDist *= 10e10;
	  		eucDist /= 10e10;
	  		tempForce.add((double)eucDist);
	  	}	    	
	  	
	  	int dataSize = tempForce.size();
	  	//Not calculating and logging anything if dataSize is zero
	  	if(dataSize == 0)
	  		return -1;
	  	
			sum = 0.0;
	  	for (int i = 0; i < dataSize; i++)
			{
				sum += tempForce.get(i);
			}
	  	
	  	//Calculating average
			avg = sum / dataSize;
			
			avg  *= 10e10;	//For precision
			avg /= 10e10;
			
			sum = 0.0;
	  	
			for (int i = 0; i < dataSize; i++)
			{
				sum += Math.pow((tempForce.get(i) - avg), 2.0);
			}
			
			
			//Calculating variance
			
			var = sum / dataSize;
			var *= 10e10; //For precision
			var /= 10e10;
				
			//For Clearing Buffered Values
			modifyXValues(false,false,true,0);
		    modifyYValues(false,false,true,0);
		    modifyZValues(false,false,true,0);
		    
		    Log.i(TAG, "variance = " + var);
			
		    return var;	
	}
	
	
  private synchronized ArrayList<Float> modifyXValues(boolean get,boolean add,boolean clear,float value){
  	ArrayList<Float> temp = new ArrayList<Float>();
  	if(get == true){
  		for(float val:x_val){
  			temp.add(val);
  		}    		    		
  	}
  	if(add == true){    		
  			x_val.add(value);    		
  	}
  	if(clear == true){
  		x_val.clear();
  	}
  	return temp;
  }
  
  private synchronized ArrayList<Float> modifyYValues(boolean get,boolean add,boolean clear,float value){
  	ArrayList<Float> temp = new ArrayList<Float>();
  	if(get == true){
  		for(float val:y_val){
  			temp.add(val);
  		}    		    		
  	}
  	if(add == true){    		
  			y_val.add(value);    		
  	}
  	if(clear == true){
  		y_val.clear();
  	}
  	return temp;
  }   
  
  
  private synchronized ArrayList<Float> modifyZValues(boolean get,boolean add,boolean clear,float value){
  	ArrayList<Float> temp = new ArrayList<Float>();
  	if(get == true){
  		for(float val:z_val){
  			temp.add(val);
  		}    		    		
  	}
  	if(add == true){    		
  			z_val.add(value);    		
  	}
  	if(clear == true){
  		z_val.clear();
  	}
  	return temp;
  }

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
	
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
			x_val.add(event.values[0]);
			y_val.add(event.values[1]);
			z_val.add(event.values[2]);
		}
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject addEvent() {
		double avgVar = calcAvgVar();
		JSONObject jsonObject = new JSONObject();
	    SQLiteDatabase db = eventsData.getWritableDatabase();
	    ContentValues values = new ContentValues();
	    Calendar calendar = Calendar.getInstance();    
		jsonObject.put("AVG_VAR", avgVar);
	    values.put(EventDataSQLHelper.USERID, _imei);
	    values.put(EventDataSQLHelper.TIME, 
	    		DateFormat.format("yyyy-MM-dd kk:mm:ss", calendar.getTime()) + 
	    		"");
	    values.put(EventDataSQLHelper.ACCEL, "" + avgVar);
	    db.insert(EventDataSQLHelper.TABLE, null, values);
	    return jsonObject;
	}
	
	private double calcAvgVar() {
		double avg = 0;
		for(double single_val : varArray)
			avg += single_val;
		return (avg / varArray.size());
	}
	
	public void updateVarArray() {
		varArray.add(getVariance());
	}
	
	public void clearVarArray() {
		varArray.clear();
	}
	
	
	private Binder mBinder = new ReadingsServiceBinder();
	private static EventDataSQLHelper eventsData;
	private SensorManager sensorManager;
	String sensorReading;
	private static ArrayList<Float> x_val = new ArrayList<Float>();
	private static ArrayList<Float> y_val = new ArrayList<Float>();
	private static ArrayList<Float> z_val = new ArrayList<Float>();
	int count =0;
	//Variance Array
	private static ArrayList<Double> varArray = new ArrayList<Double>();
	private static String _imei;
	static final String TAG = "READINGSACTIVITY";
}