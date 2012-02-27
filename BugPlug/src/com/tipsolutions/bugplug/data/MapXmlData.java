package com.tipsolutions.bugplug.data;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.util.Log;

public class MapXmlData extends XmlParser {

	interface Action {
		void fill(short x, short y, char chr);
		void setSize(short lineLength, short numLines);
	}

	class EdgeDescription {
		byte [] mPoints;
		short mRowSize;
		
		public EdgeDescription(short w, short h) {
			mRowSize = w;
			mPoints = new byte[w*h];
		}
		
		public boolean isEdge(short x, short y) {
			return mPoints[pos(x,y)] != 0;
		}
		
		int pos(short x, short y) {
			return y*mRowSize+x;
		}
		
		public void setEdge(short x, short y) {
			 mPoints[pos(x,y)] = 1;
		}
	}

	class MapCell {
		TerrainElement mTerrain = TerrainElement.UNSET;

		public void set(char chr) {
			mTerrain = TerrainElement.get(chr);
		}
	};

	class MapCells {
		MapCell [] mCells;
		final short mRowSize;

		public MapCells(short width, short numrows) {
			mRowSize = width;
			mCells = new MapCell[mRowSize*numrows];
		}

		public MapCell get(int x, int y) {
			return mCells[y*mRowSize+x];
		}
	};

	enum TerrainElement {
		UNSET('\0'),
		STONE_HARD('S'),
		STONE_SOFT('s'),
		CRACK('C'),
		WATER('w'),
		NUTRIENTS('n'),
		PLANT('P'),
		DIRT('.');

		static public TerrainElement get(char n) {
			for (TerrainElement t : values()) {
				if (t.mSymbol == n) {
					return t;
				}
			}
			return null;
		}
		char mSymbol;

		TerrainElement(char n) { mSymbol = n; }
	}
	static final String KEY_MAP = "Map";
	static final String KEY_LAYOUT = "Layout";
	static final String KEY_GRIDUNITSIZE = "GridUnitSize";

	static final String KEY_EDGE = "Edge";
	MapCells mCells;
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
			} else if (KEY_EDGE.equals(localName)) {
				fillEdge(mData.toString());
			}
		} catch (Exception ex) {
			Log.e(TAG, ex.getMessage());
		}
	};
	
	void fill(String description, Action action) {
		String line = description.trim();
	
		short maxLineLength = 0;
		short numLines = 0;
		short chrPos = 0;

		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) == '\n') {
				if (chrPos > 0) {
					numLines++;
					if (chrPos > maxLineLength) {
						maxLineLength = chrPos;
					}
					chrPos = 0;
				}
			} else {
				chrPos++;
			}
		}
		action.setSize(maxLineLength, numLines);
		
		short x = 0;
		short y = 0;

		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) == '\n') {
				y++;
				x = 0;
			} else {
				action.fill(x, y, line.charAt(i));
				x++;
			}

		}
	}
	
	void fillCells(String description) {
		fill(description, new Action() {
			@Override
			public void fill(short x, short y, char chr) {
				mCells.get(x, y).set(chr);  
			}

			@Override
			public void setSize(short lineLength, short numLines) {
				mCells = new MapCells(lineLength, numLines);
			}
		});
	}
	
	void fillEdge(String description) {
		fill(description, new Action() {
			@Override
			public void fill(short x, short y, char chr) {
				if (chr == 'x') {
					mCurEdge.setEdge(x, y);
				}
			}

			@Override
			public void setSize(short lineLength, short numLines) {
				mCurEdge = new EdgeDescription(lineLength, numLines);
			}	
		});
		mEdges.add(mCurEdge);
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		int capacity = 50;
		if (KEY_MAP.equals(localName)) {
			mEdges = new ArrayList<EdgeDescription>();
		} else if (KEY_GRIDUNITSIZE.equals(localName)) {
		} else if (KEY_LAYOUT.equals(localName)) {
			capacity = 1024;
		} else if (KEY_EDGE.equals(localName)) {
			capacity = 512;
		}
		mData = new StringBuilder(capacity);
	}
}
