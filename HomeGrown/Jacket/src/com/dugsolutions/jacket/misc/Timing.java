package com.dugsolutions.jacket.misc;

import java.util.HashMap;

import android.util.Log;

public class Timing {

	public static Timing Get(Object key) { 
		Timing timing = mTimingMap.get(key);
		if (timing == null) {
			mTimingMap.put(key, timing = new Timing());
		}
		return timing;
	}
	static HashMap<Object,Timing> mTimingMap = new HashMap<Object,Timing>();
	static String mTag = "Jacket";
	
	HashMap<String,Long> mMap = new HashMap<String,Long>();
	
	public Timing() {
	}
	
	static public void SetTag(String tag) {
		mTag = tag;
	}
		
	public void start(String key) {
		mMap.put(key, System.currentTimeMillis());
	}
	
	public void end(String key) {
		if (mMap.get(key) == null) {
			Log.e(mTag, "Unknown key \"" + key + "\"");
		}
		msg(key, System.currentTimeMillis() - mMap.get(key));
	}
	
	protected void msg(String key, long diff) {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append(key);
		sbuf.append(", ");
		sbuf.append("time=");
		sbuf.append(diff);
		sbuf.append(" ms");
		Log.d(mTag, sbuf.toString());
	}
}
