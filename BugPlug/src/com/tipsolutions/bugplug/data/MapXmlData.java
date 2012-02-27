package com.tipsolutions.bugplug.data;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.util.Log;

public class MapXmlData extends XmlParser {

	class EdgeDescription {
		short mPoints;
	}
	class MapCell {
		TerrainElement mTerrain;
	}
	enum TerrainElement {
		STONE_HARD,
		STONE_SOFT,
		CRACK,
		WATER,
		NUTRIENTS,
		PLANT,
		DIRT,
	}
	static final String KEY_MAP = "Map";
	static final String KEY_LAYOUT = "Layout";

	static final String KEY_GRIDUNITSIZE = "GridUnitSize";;

	static final String KEY_KEY = "Key";;

	static final String KEY_EDGE = "Edge";;

	MapCell [] mCells;
	short mGridUnitSize; // in mm;
	ArrayList<EdgeDescription> mEdges;
	EdgeDescription mCurEdge;

	public MapXmlData() {
		super();
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		try {
			if (KEY_MAP.equals(localName)) {
			} else if (KEY_GRIDUNITSIZE.equals(localName)) {
				mGridUnitSize = (short) Integer.parseInt(mData.toString());
			} else if (KEY_LAYOUT.equals(localName)) {
				fillCells(mData.toString());
			} else if (KEY_KEY.equals(localName)) {

			} else if (KEY_EDGE.equals(localName)) {

			}
		} catch (Exception ex) {
			Log.e(TAG, ex.getMessage());
		}
	}

	void fillCells(String description) {
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		int capacity = 50;
		if (KEY_MAP.equals(localName)) {
		} else if (KEY_GRIDUNITSIZE.equals(localName)) {
		} else if (KEY_LAYOUT.equals(localName)) {
			capacity = 1024;
		} else if (KEY_KEY.equals(localName)) {

		} else if (KEY_EDGE.equals(localName)) {
			mCurEdge = new EdgeDescription();
		}
		mData = new StringBuilder(capacity);
	}
}
