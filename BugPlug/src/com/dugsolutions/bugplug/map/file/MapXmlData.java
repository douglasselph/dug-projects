package com.dugsolutions.bugplug.map.file;

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
		byte mSides;

		public EdgeDescription(short w, short h) {
			mRowSize = w;
			mPoints = new byte[w*h];
		}

		public void computeSides() {
			mSides = 0;
			for (int x = 0; x < mRowSize; x++) {
				if (isEdge(x,0)) {
					setSide(Side.N);
					break;
				}
			}
			int numRows = getNumRows();
			for (int x = 0; x < mRowSize; x++) {
				if (isEdge(x,numRows-1)) {
					setSide(Side.S);
					break;
				}
			}
			for (int y = 0; y < numRows; y++) {
				if (isEdge(0,y)) {
					setSide(Side.W);
					break;
				}
			}
			for (int y = 0; y < numRows; y++) {
				if (isEdge(mRowSize-1,y)) {
					setSide(Side.E);
					break;
				}
			}
		}

		public int getNumRows() {
			return mPoints.length / mRowSize;
		}
		
		public boolean hasSide(Side s) {
			return (mSides & s.getValue()) == s.getValue();
		}

		public boolean isEdge(int x, int y) {
			return mPoints[pos(x,y)] != 0;
		}
		
		int pos(int x, int y) {
			return y*mRowSize+x;
		}
		
		public void setEdge(int x, int y) {
			mPoints[pos(x,y)] = 1;
		}
		
		void setSide(Side s) {
			mSides |= s.getValue();
		}
	}
	
	public class MapCell {
		TerrainElement mTerrain = TerrainElement.UNSET;

		public TerrainElement getTerrain() {
			return mTerrain;
		}
		
		public void set(char chr) {
			mTerrain = TerrainElement.get(chr);
		}
	}

	public class MapCells {
		MapCell [] mCells;
		final short mRowSize;

		public MapCells(short width, short numrows) {
			mRowSize = width;
			mCells = new MapCell[mRowSize*numrows];
		}

		public MapCell get(int x, int y) {
			return mCells[y*mRowSize+x];
		}
		
		public short getHeight() {
			return (short) (mCells.length / mRowSize);
		}
		
		public short getWidth() {
			return mRowSize;
		}
	};

	public enum Side {
		N(1), S(2), W(4), E(8);
		
		byte mValue;
		Side(int b) { mValue = (byte)b; }
		
		byte getValue() { return mValue; }
	};

	public enum TerrainElement {
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
		String mFilename;
		short mUnitWidth; // width the file represents in Grid Units
		short mUnitHeight; // height the file represents in Grid Units

		TerrainElement(char n) { mSymbol = n; }

		public String getFilename() { return mFilename; }
		public short getUnitHeight() { return mUnitHeight; }
		public short getUnitWidth() { return mUnitWidth; }
		public void setFilename(String f) { mFilename = f; }
		public void setUnitHeight(short h) { mUnitHeight = h; }
		public void setUnitWidth(short w) { mUnitWidth = w; }
	}

	static final String KEY_MAP = "Map";
	static final String KEY_LAYOUT = "Layout";
	static final String KEY_GRIDUNITSIZE = "GridUnitSize";
	static final String KEY_TERRAIN = "Terrain";
	static final String KEY_EDGE = "Edge";
	static final String NAME_ELEMENT = "element";
	static final String NAME_WIDTH = "width";
	static final String NAME_HEIGHT = "height";

	MapCells mCells;
	short mGridUnitSize; // in mm;
	ArrayList<EdgeDescription> mEdges;
	EdgeDescription mCurEdge;
	TerrainElement mCurTerrain;

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
			} else if (KEY_TERRAIN.equals(localName)) {
				if (mCurTerrain != null) {
					mCurTerrain.setFilename(mData.toString());
				}
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
			public void fill(short x, short y, char chr) {
				mCells.get(x, y).set(chr);  
			}

			public void setSize(short lineLength, short numLines) {
				mCells = new MapCells(lineLength, numLines);
			}
		});
	}

	void fillEdge(String description) {
		fill(description, new Action() {
			public void fill(short x, short y, char chr) {
				if (chr == 'x') {
					mCurEdge.setEdge(x, y);
				}
			}

			public void setSize(short lineLength, short numLines) {
				mCurEdge = new EdgeDescription(lineLength, numLines);
			}	
		});
		mCurEdge.computeSides();
		mEdges.add(mCurEdge);
	}

	public MapCells getCells() {
		return mCells;
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
		} else if (KEY_TERRAIN.equals(localName)) {
			capacity = 256;
			String element = attributes.getValue(NAME_ELEMENT);
			mCurTerrain = TerrainElement.get(element.charAt(0));
			if (mCurTerrain == null) {
				Log.e(TAG, "Could not find terrain element '" + element + "'");
			} else {
				try {
					int width = Integer.parseInt(attributes.getValue(NAME_WIDTH));
					int height = Integer.parseInt(attributes.getValue(NAME_HEIGHT));
					mCurTerrain.setUnitWidth((short)width);
					mCurTerrain.setUnitHeight((short)height);
				} catch (Exception ex) {
					Log.e(TAG, ex.getMessage());
				}
			}
		}
		mData = new StringBuilder(capacity);
	}
}
