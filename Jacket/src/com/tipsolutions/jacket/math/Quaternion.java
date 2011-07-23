package com.tipsolutions.jacket.math;

public class Quaternion {
	
    public final static Quaternion IDENTITY = new Quaternion(0, 0, 0, 1);

    /**
     * Check a quaternion... if it is null or its doubles are NaN or infinite, return false. Else return true.
     * 
     * @param quat
     *            the quaternion to check
     * @return true or false as stated above.
     */
    public static boolean isValid(final Quaternion quat) {
        if (quat == null) {
            return false;
        }
        if (Double.isNaN(quat.getX()) || Double.isInfinite(quat.getX())) {
            return false;
        }
        if (Double.isNaN(quat.getY()) || Double.isInfinite(quat.getY())) {
            return false;
        }
        if (Double.isNaN(quat.getZ()) || Double.isInfinite(quat.getZ())) {
            return false;
        }
        if (Double.isNaN(quat.getW()) || Double.isInfinite(quat.getW())) {
            return false;
        }
        return true;
    }
    protected double _w = 1;
    protected double _x = 0;
    protected double _y = 0;
    
    protected double _z = 0;
    
    public Quaternion() {
        this(IDENTITY);
    }
    
    public Quaternion(final double x, final double y, final double z, final double w) {
        _x = x;
        _y = y;
        _z = z;
        _w = w;
    }

    public Quaternion(final Quaternion source) {
        this(source.getX(), source.getY(), source.getZ(), source.getW());
    }

    /**
     * Internally increments the fields of this quaternion with the field values of the given quaternion.
     * 
     * @param quat
     * @return this quaternion for chaining
     */
    public Quaternion add(final Quaternion quat) {
        setX(getX() + quat.getX());
        setY(getY() + quat.getY());
        setZ(getZ() + quat.getZ());
        setW(getW() + quat.getW());
        return this;
    }

    /**
     * Rotates the given vector by this quaternion. If supplied, the result is stored into the supplied "store" vector.
     * 
     * @param vec
     *            the vector to multiply this quaternion by.
     * @param store
     *            the vector to store the result in. If store is null, a new vector is created. Note that it IS safe for
     *            vec and store to be the same object.
     * @return the store vector, or a new vector if store is null.
     * @throws NullPointerException
     *             if vec is null
     * 
     *             if the given store is read only.
     */
    public Vector3 apply(final Vector3 vec, Vector3 store) {
        if (store == null) {
            store = new Vector3();
        }
        if (vec.equals(Vector3.ZERO)) {
            store.set(0, 0, 0);
        } else {
            final double x = getW() * getW() * vec.getX() + 2 * getY() * getW() * vec.getZ() - 2 * getZ() * getW()
                    * vec.getY() + getX() * getX() * vec.getX() + 2 * getY() * getX() * vec.getY() + 2 * getZ()
                    * getX() * vec.getZ() - getZ() * getZ() * vec.getX() - getY() * getY() * vec.getX();
            final double y = 2 * getX() * getY() * vec.getX() + getY() * getY() * vec.getY() + 2 * getZ() * getY()
                    * vec.getZ() + 2 * getW() * getZ() * vec.getX() - getZ() * getZ() * vec.getY() + getW() * getW()
                    * vec.getY() - 2 * getX() * getW() * vec.getZ() - getX() * getX() * vec.getY();
            final double z = 2 * getX() * getZ() * vec.getX() + 2 * getY() * getZ() * vec.getY() + getZ() * getZ()
                    * vec.getZ() - 2 * getW() * getY() * vec.getX() - getY() * getY() * vec.getZ() + 2 * getW()
                    * getX() * vec.getY() - getX() * getX() * vec.getZ() + getW() * getW() * vec.getZ();
            store.set(x, y, z);
        }
        return store;
    }

    /**
     * @param x
     * @param y
     * @param z
     * @param w
     * @return the dot product of this quaternion with the given x,y,z and w values.
     */
    public double dot(final double x, final double y, final double z, final double w) {
        return getX() * x + getY() * y + getZ() * z + getW() * w;
    }

    /**
     * @param quat
     * @return the dot product of this quaternion with the given quaternion.
     */
    public double dot(final Quaternion quat) {
        return dot(quat.getX(), quat.getY(), quat.getZ(), quat.getW());
    }

    /**
     * Updates this quaternion to represent a rotation formed by the given three axes. These axes are assumed to be
     * orthogonal and no error checking is applied. It is the user's job to insure that the three axes being provided
     * indeed represent a proper right handed coordinate system.
     * 
     * @param xAxis
     *            vector representing the x-axis of the coordinate system.
     * @param yAxis
     *            vector representing the y-axis of the coordinate system.
     * @param zAxis
     *            vector representing the z-axis of the coordinate system.
     * @return this quaternion for chaining
     */
    public Quaternion fromAxes(final Vector3 xAxis, final Vector3 yAxis, final Vector3 zAxis) {
        return fromRotationMatrix(xAxis.getX(), yAxis.getX(), zAxis.getX(), 
        						  xAxis.getY(), yAxis.getY(), zAxis.getY(),
        						  xAxis.getZ(), yAxis.getZ(), zAxis.getZ());
    }

    /**
     * Updates this quaternion to represent a rotation formed by the given three axes. These axes are assumed to be
     * orthogonal and no error checking is applied. It is the user's job to insure that the three axes being provided
     * indeed represent a proper right handed coordinate system.
     * 
     * @param axes
     *            the array containing the three vectors representing the coordinate system.
     * @return this quaternion for chaining
     * @throws IllegalArgumentException
     *             if the given axes array is smaller than 3 elements.
     */
    public Quaternion fromAxes(final Vector3[] axes) {
        if (axes.length < 3) {
            throw new IllegalArgumentException("axes array must have at least three elements");
        }
        return fromAxes(axes[0], axes[1], axes[2]);
    }

    /**
     * Updates this quaternion from the given Euler rotation angles, applied in the given order: heading, attitude,
     * bank.
     * 
     * @param heading
     *            the Euler heading angle in radians. (rotation about the y axis)
     * @param attitude
     *            the Euler attitude angle in radians. (rotation about the z axis)
     * @param bank
     *            the Euler bank angle in radians. (rotation about the x axis)
     * @return this quaternion for chaining
     * @see <a
     *      href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/eulerToQuaternion/index.htm">euclideanspace.com-eulerToQuaternion</a>
     */
    // THIS CODE WAS GIVING ME INACCURACIES, SO I ABANDONED IT.
//    public Quaternion fromEulerAngles(final double heading, final double attitude, final double bank) {
//        double angle = heading * 0.5;
//        final double sinHeading = Math.sin(angle);
//        final double cosHeading = Math.cos(angle);
//        angle = attitude * 0.5;
//        final double sinAttitude = Math.sin(angle);
//        final double cosAttitude = Math.cos(angle);
//        angle = bank * 0.5;
//        final double sinBank = Math.sin(angle);
//        final double cosBank = Math.cos(angle);
//
//        // variables used to reduce multiplication calls.
//        final double cosHeadingXcosAttitude = cosHeading * cosAttitude;
//        final double sinHeadingXsinAttitude = sinHeading * sinAttitude;
//        final double cosHeadingXsinAttitude = cosHeading * sinAttitude;
//        final double sinHeadingXcosAttitude = sinHeading * cosAttitude;
//
//        final double w = (cosHeadingXcosAttitude * cosBank - sinHeadingXsinAttitude * sinBank);
//        final double x = (cosHeadingXcosAttitude * sinBank + sinHeadingXsinAttitude * cosBank);
//        final double y = (sinHeadingXcosAttitude * cosBank + cosHeadingXsinAttitude * sinBank);
//        final double z = (cosHeadingXsinAttitude * cosBank - sinHeadingXcosAttitude * sinBank);
//
//        set(x, y, z, w);
//
//        return normalize();
//    }
    
//    public Quaternion fromEulerAngles(final double[] angles) {
//        if (angles.length != 3) {
//            throw new IllegalArgumentException("Angles array must have three elements");
//        }
//        return fromEulerAngles(angles[0], angles[1], angles[2]);
//    }
   
    protected Quaternion setFromAxisAngle(Vector3f axis, double angle) {
    	double sin_a = Math.sin( angle / 2 );
	    double cos_a = Math.cos( angle / 2 );

	    setX(axis.getX() * sin_a);
    	setY(axis.getY() * sin_a);
    	setZ(axis.getZ() * sin_a);
    	setW(cos_a);
    	
    	normalize();
    	
    	return this;
    }
    
    public Quaternion setFromAngles(double ax, double ay, double az) {
    	Quaternion qx = new Quaternion();
    	qx.setFromAxisAngle(Vector3f.UNIT_X, ax);
    	Quaternion qy = new Quaternion();
    	qy.setFromAxisAngle(Vector3f.UNIT_Y, ay);
    	Quaternion qz = new Quaternion();
    	qz.setFromAxisAngle(Vector3f.UNIT_Z, az);
    	set(qx);
    	multiply(qy);
    	multiply(qz);
    	return this;
    }
    
    /**
     * Sets the value of this quaternion to the rotation described by the given matrix values.
     * 
     * @param m00
     * @param m01
     * @param m02
     * @param m10
     * @param m11
     * @param m12
     * @param m20
     * @param m21
     * @param m22
     * @return this quaternion for chaining
     */
    public Quaternion fromRotationMatrix(final double m00, final double m01, final double m02, 
    							         final double m10, final double m11, final double m12, 
    							         final double m20, final double m21, final double m22) {
        // Uses the Graphics Gems code, from
        // ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z
        // *NOT* the "Matrix and Quaternions FAQ", which has errors!

        // the trace is the sum of the diagonal elements; see
        // http://mathworld.wolfram.com/MatrixTrace.html
        final double t = m00 + m11 + m22;

        // we protect the division by s by ensuring that s>=1
        double x, y, z, w;
        if (t >= 0) { // |w| >= .5
            double s = Math.sqrt(t + 1); // |s|>=1 ...
            w = 0.5 * s;
            s = 0.5 / s; // so this division isn't bad
            x = (m21 - m12) * s;
            y = (m02 - m20) * s;
            z = (m10 - m01) * s;
        } else if ((m00 > m11) && (m00 > m22)) {
            double s = Math.sqrt(1.0 + m00 - m11 - m22); // |s|>=1
            x = s * 0.5; // |x| >= .5
            s = 0.5 / s;
            y = (m10 + m01) * s;
            z = (m02 + m20) * s;
            w = (m21 - m12) * s;
        } else if (m11 > m22) {
            double s = Math.sqrt(1.0 + m11 - m00 - m22); // |s|>=1
            y = s * 0.5; // |y| >= .5
            s = 0.5 / s;
            x = (m10 + m01) * s;
            z = (m21 + m12) * s;
            w = (m02 - m20) * s;
        } else {
            double s = Math.sqrt(1.0 + m22 - m00 - m11); // |s|>=1
            z = s * 0.5; // |z| >= .5
            s = 0.5 / s;
            x = (m02 + m20) * s;
            y = (m21 + m12) * s;
            w = (m10 - m01) * s;
        }
        return set(x, y, z, w);
    }

    /**
     * Sets the value of this quaternion to the rotation described by the given matrix.
     * 
     * @param matrix
     * @return this quaternion for chaining
     * @throws NullPointerException
     *             if matrix is null.
     */
    public Quaternion fromRotationMatrix(final Matrix3f matrix) {
        return fromRotationMatrix(
        		matrix.getValue(0,0), matrix.getValue(0,1), matrix.getValue(0,2), 
        		matrix.getValue(1,0), matrix.getValue(1,1), matrix.getValue(1,2), 
        		matrix.getValue(2,0), matrix.getValue(2,1), matrix.getValue(2,2));
    }
    
    public Quaternion fromRotationMatrix(final Matrix4f matrix) {
        return fromRotationMatrix(
        		matrix.getValue(0,0), matrix.getValue(0,1), matrix.getValue(0,2), 
        		matrix.getValue(1,0), matrix.getValue(1,1), matrix.getValue(1,2), 
        		matrix.getValue(2,0), matrix.getValue(2,1), matrix.getValue(2,2));
    }
    
    public Rotate getRotate() {
//    	normalize();
//
//	    double cos_a = getW();
//	    double angle = Math.acos( cos_a ) * 2;
//	    double sin_a = Math.sqrt( 1.0 - cos_a * cos_a );
//	    
//	    if (Math.abs(sin_a) < 0.0005) {
//	    	sin_a = 1;
//	    }
//	    Rotate rotate = new Rotate();
//	    rotate.setAngleX(getX() / sin_a);
//	    rotate.setAngleY(getY() / sin_a);
//	    rotate.setAngleZ(getZ() / sin_a);
//	    
//	    return rotate;

    	double [] angles = toEulerAngles();
    	return new Rotate(MathUtils.clamp(angles[2]), 
    					  MathUtils.clamp(angles[0]), 
    					  MathUtils.clamp(angles[1]));
    }

    public double getW() {
        return _w;
    }

    public float getWf() {
        return (float) _w;
    }
    
    public double getX() {
        return _x;
    }
    
    public float getXf() {
        return (float) _x;
    }
    
    public double getY() {
        return _y;
    }
    
    public float getYf() {
        return (float) _y;
    }
    
    public double getZ() {
        return _z;
    }

    public float getZf() {
        return (float) _z;
    }

    /**
     * internally inverts this quaternion's values as if multiplyLocal(-1) had been called.
     * 
     * @return this quaternion for chaining
     */
    public Quaternion invert() {
        return multiply(-1);
    }

    /**
     * @return true if this quaternion is (0, 0, 0, 1)
     */
    public boolean isIdentity() {
        if (equals(IDENTITY)) {
            return true;
        }

        return false;
    }

    /**
     * Modifies this quaternion to equal the rotation required to point the z-axis at 'direction' and the y-axis to
     * 'up'.
     * 
     * @param direction
     *            where to 'look' at
     * @param up
     *            a vector indicating the local up direction.
     */
    public void lookAt(final Vector3 direction, final Vector3 up) {
        Vector3 zAxis = new Vector3(direction).normalize();
        Vector3 xAxis = new Vector3(up).normalize().cross(zAxis);
        Vector3 yAxis = new Vector3(zAxis).cross(xAxis);
        fromAxes(xAxis, yAxis, zAxis);
        normalize();
    }
    
    /**
     * @return the magnitude of this quaternion. basically sqrt({@link #magnitude()})
     */
    public double magnitude() {
        final double magnitudeSQ = magnitudeSquared();
        if (magnitudeSQ == 1.0) {
            return 1.0;
        }
        return Math.sqrt(magnitudeSQ);
    }
    
    /**
     * @return the squared magnitude of this quaternion.
     */
    public double magnitudeSquared() {
        return getW() * getW() + getX() * getX() + getY() * getY() + getZ() * getZ();
    }
    
    /**
     * Multiplies each value of this quaternion by the given scalar value. The result is stored in this quaternion.
     * 
     * @param scalar
     *            the quaternion to multiply this quaternion by.
     * @return this quaternion for chaining.
     */
    public Quaternion multiply(final double scalar) {
        setX(getX() * scalar);
        setY(getY() * scalar);
        setZ(getZ() * scalar);
        setW(getW() * scalar);
        return this;
    }

    /**
     * Multiplies this quaternion by the supplied quaternion values. The result is stored locally.
     * 
     * @param qx
     * @param qy
     * @param qz
     * @param qw
     * @return this quaternion for chaining
     */
    public Quaternion multiply(final double qx, final double qy, final double qz, final double qw) {
        final double x = getX() * qw + getY() * qz - getZ() * qy + getW() * qx;
        final double y = -getX() * qz + getY() * qw + getZ() * qx + getW() * qy;
        final double z = getX() * qy - getY() * qx + getZ() * qw + getW() * qz;
        final double w = -getX() * qx - getY() * qy - getZ() * qz + getW() * qw;
        return set(x, y, z, w);
    }
    
    /**
     * Multiplies this quaternion by the supplied matrix. The result is stored locally.
     * 
     * @param matrix
     *            the matrix to apply to this quaternion.
     * @return this quaternion for chaining
     * @throws NullPointerException
     *             if matrix is null.
     */
    public Quaternion multiply(final Matrix3f matrix) {
        final double oldX = getX(), oldY = getY(), oldZ = getZ(), oldW = getW();
        fromRotationMatrix(matrix);
        final double tempX = getX(), tempY = getY(), tempZ = getZ(), tempW = getW();

        final double x = oldX * tempW + oldY * tempZ - oldZ * tempY + oldW * tempX;
        final double y = -oldX * tempZ + oldY * tempW + oldZ * tempX + oldW * tempY;
        final double z = oldX * tempY - oldY * tempX + oldZ * tempW + oldW * tempZ;
        final double w = -oldX * tempX - oldY * tempY - oldZ * tempZ + oldW * tempW;
        return set(x, y, z, w);
    }
    
    /**
     * Multiplies this quaternion by the supplied quaternion. The result is stored locally.
     * 
     * @param quat
     *            The Quaternion to multiply this one by.
     * @return this quaternion for chaining
     * @throws NullPointerException
     *             if quat is null.
     */
    public Quaternion multiply(final Quaternion quat) {
        return multiply(quat.getX(), quat.getY(), quat.getZ(), quat.getW());
    }
    /**
     * @return this quaternion, modified to be unit length, for chaining.
     */
    public Quaternion normalize() {
        final double n = 1.0 / magnitude();
        final double x = getX() * n;
        final double y = getY() * n;
        final double z = getZ() * n;
        final double w = getW() * n;
        return set(x, y, z, w);
    }
    public Quaternion set(final double x, final double y, final double z, final double w) {
        setX(x);
        setY(y);
        setZ(z);
        setW(w);
        return this;
    }
    
    public Quaternion set(final Quaternion other) {
        setX(other.getX());
        setY(other.getY());
        setZ(other.getZ());
        setW(other.getW());
        return this;
    }
    
    public void set(final Rotate rotate) {
    	setFromAngles(rotate.getAngleX(), rotate.getAngleY(), rotate.getAngleZ());
    }

    /**
     * Sets the value of this quaternion to (0, 0, 0, 1). Equivalent to calling set(0, 0, 0, 1)
     * 
     * @return this quaternion for chaining
     */
    public Quaternion setIdentity() {
        return set(0, 0, 0, 1);
    }

    public void setW(final double w) {
        _w = w;
    }

    public void setX(final double x) {
        _x = x;
    }

    public void setY(final double y) {
        _y = y;
    }

    public void setZ(final double z) {
        _z = z;
    }
    /**
     * Internally decrements the fields of this quaternion by the field values of the given quaternion.
     * 
     * @param quat
     * @return this quaternion for chaining.
     */
    public Quaternion subtract(final Quaternion quat) {
        setX(getX() - quat.getX());
        setY(getY() - quat.getY());
        setZ(getZ() - quat.getZ());
        setW(getW() - quat.getW());
        return this;
    }

    /**
     * Converts this quaternion to a rotation matrix and then extracts rotation axes.
     * 
     * @param axes
     *            the array of vectors to be filled.
     * @throws IllegalArgumentException
     *             if the given axes array is smaller than 3 elements.
     */
    public void toAxes(final Vector3f axes []) {
        if (axes.length < 3) {
            throw new IllegalArgumentException("axes array must have at least three elements");
        }
        Matrix3f tempMat = toRotationMatrix3f();
        axes[0] = tempMat.getColumn(0);
        axes[1] = tempMat.getColumn(1);
        axes[2] = tempMat.getColumn(2);
    }

    public double [] toEulerAngles() {
    	return toEulerAngles(null);
    }
    /**
     * Converts this quaternion to Euler rotation angles in radians (heading (y), attitude (z), bank (x)).
     * 
     * @param store
     *            the double[] array to store the computed angles in. If null, a new double[] will be created
     * @return the double[] array, filled with heading (y), attitude (z) and bank (x) in that order..
     * @throws IllegalArgumentException
     *             if non-null store is not at least length 3
     */
    public double [] toEulerAngles(final double[] store) {
        double[] result = store;
        if (result == null) {
            result = new double[3];
        } else if (result.length < 3) {
            throw new IllegalArgumentException("store array must have at least three elements");
        }
        final double sqw = getW() * getW();
        final double sqx = getX() * getX();
        final double sqy = getY() * getY();
        final double sqz = getZ() * getZ();
        final double unit = sqx + sqy + sqz + sqw; // if normalized is one, otherwise
        // is correction factor
        final double test = getX() * getY() + getZ() * getW();
        if (test > 0.499 * unit) { // singularity at north pole
            result[0] = 2 * Math.atan2(getX(), getW());
            result[1] = Constants.HALF_PI;
            result[2] = 0;
        } else if (test < -0.499 * unit) { // singularity at south pole
            result[0] = -2 * Math.atan2(getX(), getW());
            result[1] = -Constants.HALF_PI;
            result[2] = 0;
        } else {
            result[0] = Math.atan2(2 * getY() * getW() - 2 * getX() * getZ(), sqx - sqy - sqz + sqw);
            result[1] = Math.asin(2 * test / unit);
            result[2] = Math.atan2(2 * getX() * getW() - 2 * getY() * getZ(), -sqx + sqy - sqz + sqw);
        }
        return result;
    }

    /**
     * @param store
     *            the matrix to store our result in. If null, a new matrix is created.
     * @return the rotation matrix representation of this quaternion (normalized)
     * 
     *         if store is not null and is read only.
     */
    public Matrix3f toRotationMatrix(final Matrix3f store) {
        Matrix3f result = store;
        if (result == null) {
            result = new Matrix3f();
        }
        final double norm = magnitudeSquared();
        final double s = (norm > 0.0 ? 2.0 / norm : 0.0);

        // compute xs/ys/zs first to save 6 multiplications, since xs/ys/zs
        // will be used 2-4 times each.
        final double xs = getX() * s;
        final double ys = getY() * s;
        final double zs = getZ() * s;
        final double xx = getX() * xs;
        final double xy = getX() * ys;
        final double xz = getX() * zs;
        final double xw = getW() * xs;
        final double yy = getY() * ys;
        final double yz = getY() * zs;
        final double yw = getW() * ys;
        final double zz = getZ() * zs;
        final double zw = getW() * zs;

        // using s=2/norm (instead of 1/norm) saves 9 multiplications by 2 here
        result.setValue(0, 0, 1.0 - (yy + zz));
        result.setValue(0, 1, xy - zw);
        result.setValue(0, 2, xz + yw);
        result.setValue(1, 0, xy + zw);
        result.setValue(1, 1, 1.0 - (xx + zz));
        result.setValue(1, 2, yz - xw);
        result.setValue(2, 0, xz - yw);
        result.setValue(2, 1, yz + xw);
        result.setValue(2, 2, 1.0 - (xx + yy));

        return result;
    }

    /**
     * @param store
     *            the matrix to store our result in. If null, a new matrix is created.
     * @return the rotation matrix representation of this quaternion (normalized)
     */
    public Matrix4f toRotationMatrix(final Matrix4f store) {
        Matrix4f result = store;
        if (result == null) {
            result = new Matrix4f();
        }
        final double norm = magnitude();
        final double s = (norm == 1.0 ? 2.0 : (norm > 0.0 ? 2.0 / norm : 0));

        // compute xs/ys/zs first to save 6 multiplications, since xs/ys/zs
        // will be used 2-4 times each.
        final double xs = getX() * s;
        final double ys = getY() * s;
        final double zs = getZ() * s;
        final double xx = getX() * xs;
        final double xy = getX() * ys;
        final double xz = getX() * zs;
        final double xw = getW() * xs;
        final double yy = getY() * ys;
        final double yz = getY() * zs;
        final double yw = getW() * ys;
        final double zz = getZ() * zs;
        final double zw = getW() * zs;

        // using s=2/norm (instead of 1/norm) saves 9 multiplications by 2 here
        result.setValue(0, 0, 1.0 - (yy + zz));
        result.setValue(0, 1, xy - zw);
        result.setValue(0, 2, xz + yw);
        result.setValue(1, 0, xy + zw);
        result.setValue(1, 1, 1.0 - (xx + zz));
        result.setValue(1, 2, yz - xw);
        result.setValue(2, 0, xz - yw);
        result.setValue(2, 1, yz + xw);
        result.setValue(2, 2, 1.0 - (xx + yy));

        return result;
    }

    public Matrix3f toRotationMatrix3f() {
    	return toRotationMatrix((Matrix3f)null);
    }

    public Matrix4f toRotationMatrix4f() {
    	return toRotationMatrix((Matrix4f)null);
    }
    
    @Override
    public String toString() {
    	StringBuffer sbuf = new StringBuffer();
    	sbuf.append("[X=");
    	sbuf.append(getX());
    	sbuf.append(",Y=");
    	sbuf.append(getY());
    	sbuf.append(",Z=");
    	sbuf.append(getZ());
    	sbuf.append(",W=");
    	sbuf.append(getW());
    	sbuf.append("]");
        return sbuf.toString();
    }


}
