package com.tipsolutions.bugplug.data;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class XmlParser extends DefaultHandler {

	protected static final String TAG = "XmlManager";

	protected StringBuilder mData;

	
	public XmlParser() {
		super();
		// TODO Auto-generated constructor stub
	}

	public void characters(char[] ch, int start, int length)
			throws SAXException {
		super.characters(ch, start, length);
		mData.append(new String(ch, start, length));
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
	}
	
	public void parse(File filename) {
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(filename);
			parse(inputStream);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
				}
				inputStream = null;
			}
		}
	}

	public void parse(InputStream inputStream) {
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();

			XMLReader xr = sp.getXMLReader();
			xr.setContentHandler(this);

			sp.parse(inputStream, this);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
	}

	public void parse(String input) {
		InputStream inputStream = null;
		try {
			inputStream = new ByteArrayInputStream(input.getBytes("UTF-8"));
			parse(inputStream);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
				}
				inputStream = null;
			}
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		int capacity = 500;
		mData = new StringBuilder(capacity);
	}

}
