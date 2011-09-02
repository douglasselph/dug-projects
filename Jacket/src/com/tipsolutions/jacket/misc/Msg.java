package com.tipsolutions.jacket.misc;

import android.util.Log;

public class Msg {

	static String TAG = "Jacket";
	
	public static void setTAG(String tag) {
		TAG = tag;
	}
	
	public static String build(String ... strings) {
		StringBuffer sbuf = new StringBuffer();
		for (int i = 0; i < strings.length; i++) {
			sbuf.append(strings[i]);
		}
		return sbuf.toString();
	}
	
	public static void err(String ... strings) {
		err(build(strings));
	}
	
	public static void err(String msg) {
		Log.e(TAG, msg);
	}
}
