package com.tipsolutions.jacket.data;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.tipsolutions.jacket.data.ShapeData.MessageWriter;

public class ShapeUtils {
	
	static int compare(FloatBuffer buf1, FloatBuffer buf2, String tag, String who, MessageWriter msg) {
		if (buf1 == null && buf2 == null) {
			return 0;
		}
		if (buf1 == null) {
			msg.msg(tag, "First buffer was null");
			return 1;
		} else if (buf2 == null) {
			msg.msg(tag, "Second buffer was null");
			return 1;
		}
		int numDiffs = 0;
		if (buf1.limit() != buf2.limit()) {
			StringBuffer sbuf = new StringBuffer();
			sbuf.append(who);
			sbuf.append(" buffers have different sizes: ");
			sbuf.append(buf1.limit());
			sbuf.append(" != ");
			sbuf.append(buf2.limit());
			msg.msg(tag, sbuf.toString());
			numDiffs++;
		}
		buf1.rewind();
		buf2.rewind();
		float f1, f2;
		
		while (buf1.position() < buf1.limit()) {
			if ((f1 = buf1.get()) != (f2 = buf2.get())) {
    			StringBuffer sbuf = new StringBuffer();
    			sbuf.append(who);
    			sbuf.append(" different value at ");
    			sbuf.append(buf1.position());
    			sbuf.append(" :");
    			sbuf.append(f1);
    			sbuf.append(" != ");
    			sbuf.append(f2);
    			msg.msg(tag, sbuf.toString());
    			numDiffs++;
			}
		}
		return numDiffs;
	}
	
	static boolean compare(MessageWriter msg, String tag, String name, float val1, float val2) {
		if (val1 != val2) {
			StringBuffer sbuf = new StringBuffer();
			sbuf.append(name);
			sbuf.append(":");
			sbuf.append(val1);
			sbuf.append(" != ");
			sbuf.append(val2);
			msg.msg(tag, sbuf.toString());
			return false;
		}
		return true;
    }
	
	static int compare(ShortBuffer buf1, ShortBuffer buf2, String tag, String who, MessageWriter msg) {
		if (buf1 == null && buf2 == null) {
			return 0;
		}
		if (buf1 == null) {
			msg.msg(tag, "First buffer was null");
			return 1;
		} else if (buf2 == null) {
			msg.msg(tag, "Second buffer was null");
			return 1;
		}
		int numDiffs = 0;
		if (buf1.limit() != buf2.limit()) {
			StringBuffer sbuf = new StringBuffer();
			sbuf.append(who);
			sbuf.append(" buffers have different sizes: ");
			sbuf.append(buf1.limit());
			sbuf.append(" != ");
			sbuf.append(buf2.limit());
			msg.msg(tag, sbuf.toString());
			numDiffs++;
		}
		buf1.rewind();
		buf2.rewind();
		short s1, s2;
		
		while (buf1.position() < buf1.limit()) {
			if ((s1 = buf1.get()) != (s2 = buf2.get())) {
    			StringBuffer sbuf = new StringBuffer();
    			sbuf.append(who);
    			sbuf.append(" different value at ");
    			sbuf.append(buf1.position());
    			sbuf.append(" :");
    			sbuf.append(s1);
    			sbuf.append(" != ");
    			sbuf.append(s2);
    			msg.msg(tag, sbuf.toString());
    			numDiffs++;
			}
		}
		return numDiffs;
	}
}
