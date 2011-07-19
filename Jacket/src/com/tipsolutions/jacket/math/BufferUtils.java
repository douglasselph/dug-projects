package com.tipsolutions.jacket.math;

import java.nio.FloatBuffer;

public class BufferUtils {
    /**
     * Copies a Vector3 from one position in the buffer to another. The index values are in terms of vector number (eg,
     * vector number 0 is positions 0-2 in the FloatBuffer.)
     * 
     * @param buf
     *            the buffer to copy from/to
     * @param fromPos
     *            the index of the vector to copy
     * @param toPos
     *            the index to copy the vector to
     */
    public static void copyInternalVector3(final FloatBuffer buf, final int fromPos, final int toPos) {
        copyInternal(buf, fromPos * 3, toPos * 3, 3);
    }
    
    /**
     * Copies floats from one position in the buffer to another.
     * 
     * @param buf
     *            the buffer to copy from/to
     * @param fromPos
     *            the starting point to copy from
     * @param toPos
     *            the starting point to copy to
     * @param length
     *            the number of floats to copy
     */
    public static void copyInternal(final FloatBuffer buf, final int fromPos, final int toPos, final int length) {
        final float[] data = new float[length];
        buf.position(fromPos);
        buf.get(data);
        buf.position(toPos);
        buf.put(data);
    }
    
    /**
     * Updates the values of the given vector from the specified buffer at the index provided.
     * 
     * @param vector
     *            the vector to set data on
     * @param buf
     *            the buffer to read from
     * @param index
     *            the position (in terms of vectors, not floats) to read from the buf
     */
    public static void populateFromBuffer(final Vector3 vector, final FloatBuffer buf, final int index) {
        vector.setX(buf.get(index * 3));
        vector.setY(buf.get(index * 3 + 1));
        vector.setZ(buf.get(index * 3 + 2));
    }
}
