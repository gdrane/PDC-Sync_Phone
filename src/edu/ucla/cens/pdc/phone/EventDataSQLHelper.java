package edu.ucla.cens.pdc.phone;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/** Helper to the database, manages versions and creation */
public class EventDataSQLHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "events.db";
	private static final int DATABASE_VERSION = 1;

	// Table name
	public static final String TABLE = "events";
	public static final String TABLE2 = "gpsdata";
	// Columns
	public static final String USERID = "user_id";
	public static final String TIME = "time";
	public static final String ACCEL = "accel";
	public static final String GPS = "gps";


	public EventDataSQLHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.v("aditya","inside onCreate");
		String sql = "create table " + TABLE + "( " + USERID
				+ " text not null, " + TIME + " DATETIME, "
				+ ACCEL + " text not null);";
		Log.d("EventsData", "onCreate: " + sql);
		String sql2 = "create table " + TABLE2 + "( " + USERID
				+ " text not null, " + TIME + " DATETIME, "
				+ GPS + " text not null);";
		try{


		db.execSQL(sql);
		db.execSQL(sql2);
		}
		catch(SQLiteException e) {
	        Log.e("createerr",e.toString());
	    }
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion >= newVersion)
			return;

		String sql = null;
		if (oldVersion == 1) 
			sql = "alter table " + TABLE + " add note text;";
		if (oldVersion == 2)
			sql = "";

		Log.d("EventsData", "onUpgrade	: " + sql);
		if (sql != null)
			db.execSQL(sql);
	}

}