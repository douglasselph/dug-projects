package com.tipsolutions.jacket.view;

import android.widget.ArrayAdapter;

public class SpinnerControl {
	
	final protected String name;
	final protected int arg;
	
	public SpinnerControl(String _name, int _arg) {
		name = _name;
		arg = _arg;
	}

	@Override
	public String toString() {
		return name;
	}
	
	public String getName() { return name; }
	public int getArg() { return arg; }
	
	public static int locateSelection(ArrayAdapter<SpinnerControl> adapter, int code) {
		for (int i = 0; i < adapter.getCount(); i++) {
			if (adapter.getItem(i).arg == code) {
				return i;
			}
		}
		return 0;
	}
}
