package com.tipsolutions.jacket.shape;

import java.nio.FloatBuffer;

import com.tipsolutions.jacket.math.Constants;
import com.tipsolutions.jacket.math.Vector3;

public class Sphere extends Shape {

    public enum TextureMode {
        Linear, Projected, Polar;
    }
    protected int mZSamples = 16;
    protected int mRadialSamples = 16;
    public double mRadius = 3;
    public Vector3 mCenter = new Vector3();
    protected TextureMode mTextureMode = TextureMode.Linear;
    protected boolean mViewInside = false;

	public Sphere() {
	}
	
	public Sphere(double radius) {
		mRadius = radius;
		set();
	}
	
	// Construct a sphere with the center at 0,0 with the given raduis.
	public void set() {
        int numverts = (mZSamples - 2) * (mRadialSamples + 1) + 2;
        double fInvRS = 1.0 / mRadialSamples;
        double fZFactor = 2.0 / (mZSamples - 1);
        
        FloatBuffer vertBuf = setVertexBuf(numverts*3);
        FloatBuffer normBuf = setNormalBuf(numverts*3);
        FloatBuffer texBuf = setTextureBuf(numverts*2);

        // Generate points on the unit circle to be used in computing the mesh
        // points on a sphere slice.
        double[] afSin = new double[(mRadialSamples + 1)];
        double[] afCos = new double[(mRadialSamples + 1)];
        for (int iR = 0; iR < mRadialSamples; iR++) {
            double fAngle = Constants.TWO_PI * fInvRS * iR;
            afCos[iR] = Math.cos(fAngle);
            afSin[iR] = Math.sin(fAngle);
        }
        afSin[mRadialSamples] = afSin[0];
        afCos[mRadialSamples] = afCos[0];

        // generate the sphere itself
        int i = 0;
        Vector3 tempVa = new Vector3();
        Vector3 tempVb = new Vector3();
        Vector3 tempVc = new Vector3();
        
        for (int iZ = 1; iZ < (mZSamples - 1); iZ++) {
            final double fAFraction = Constants.HALF_PI * (-1.0f + fZFactor * iZ); // in (-pi/2, pi/2)
            final double fZFraction = Math.sin(fAFraction); // in (-1,1)
            final double fZ = mRadius * fZFraction;

            // compute center of slice
            final Vector3 kSliceCenter = tempVb.set(mCenter);
            kSliceCenter.setZ(kSliceCenter.getZ() + fZ);

            // compute radius of slice
            final double fSliceRadius = Math.sqrt(Math.abs(mRadius * mRadius - fZ * fZ));

            // compute slice vertices with duplication at end point
            Vector3 kNormal;
            final int iSave = i;
            for (int iR = 0; iR < mRadialSamples; iR++) {
                final double fRadialFraction = iR * fInvRS; // in [0,1)
                final Vector3 kRadial = tempVc.set(afCos[iR], afSin[iR], 0.0);
                kRadial.multiply(fSliceRadius, tempVa);
                vertBuf.put((float) (kSliceCenter.getX() + tempVa.getX())).put(
                        (float) (kSliceCenter.getY() + tempVa.getY())).put(
                        (float) (kSliceCenter.getZ() + tempVa.getZ()));

                BufferUtils.populateFromBuffer(tempVa, vertBuf, i);
                
                kNormal = tempVa.subtract(mCenter);
                kNormal.normalize();
                if (!mViewInside) {
                    normBuf.put(kNormal.getXf()).put(kNormal.getYf()).put(kNormal.getZf());
                } else {
                    normBuf.put(-kNormal.getXf()).put(-kNormal.getYf()).put(-kNormal.getZf());
                }

                if (mTextureMode == TextureMode.Linear) {
                    texBuf.put((float) fRadialFraction).put(
                                (float) (0.5 * (fZFraction + 1.0)));
                } else if (mTextureMode == TextureMode.Projected) {
                    texBuf.put((float) fRadialFraction).put(
                            (float) (Constants.INV_PI * (Constants.HALF_PI + Math.asin(fZFraction))));
                } else if (mTextureMode == TextureMode.Polar) {
                    double r = (Constants.HALF_PI - Math.abs(fAFraction)) / Math.PI;
                    double u = r * afCos[iR] + 0.5;
                    double v = r * afSin[iR] + 0.5;
                    texBuf.put((float) u).put((float) v);
                }

                i++;
            }

            BufferUtils.copyInternalVector3(vertBuf, iSave, i);
            BufferUtils.copyInternalVector3(normBuf, iSave, i);

            if (mTextureMode == TextureMode.Linear) {
                texBuf.put(1.0f).put((float) (0.5 * (fZFraction + 1.0)));
            } else if (mTextureMode == TextureMode.Projected) {
                texBuf.put(1.0f).put(
                    (float) (Constants.INV_PI * (Constants.HALF_PI + Math.asin(fZFraction))));
            } else if (mTextureMode == TextureMode.Polar) {
                final float r = (float) ((Constants.HALF_PI - Math.abs(fAFraction)) / Math.PI);
                texBuf.put(r + 0.5f).put(0.5f);
            }

            i++;
        }

        // south pole
        vertBuf.position(i * 3);
        vertBuf.put(mCenter.getXf()).put(mCenter.getYf()).put((float) (mCenter.getZ() - mRadius));

        normBuf.position(i * 3);
        if (!mViewInside) {
            // TODO: allow for inner texture orientation later.
            normBuf.put(0).put(0).put(-1);
        } else {
            normBuf.put(0).put(0).put(1);
        }

        texBuf.position(i * 2);
        if (mTextureMode == TextureMode.Polar) {
            texBuf.put(0.5f).put(0.5f);
        } else {
            texBuf.put(0.5f).put(0.0f);
        }

        i++;

        // north pole
        vertBuf.put(mCenter.getXf()).put(mCenter.getYf()).put((float) (mCenter.getZ() + mRadius));

        if (!mViewInside) {
            normBuf.put(0).put(0).put(1);
        } else {
            normBuf.put(0).put(0).put(-1);
        }

        if (mTextureMode == TextureMode.Polar) {
            texBuf.put(0.5f).put(0.5f);
        } else {
            texBuf.put(0.5f).put(1.0f);
        }
		mBounds.setMinX((float) (mCenter.getX()-mRadius));
		mBounds.setMaxX((float) (mCenter.getX()+mRadius));
		mBounds.setMinY((float) (mCenter.getY()-mRadius));
		mBounds.setMaxY((float) (mCenter.getY()+mRadius));
		mBounds.setMinZ((float) (mCenter.getZ()-mRadius));
		mBounds.setMaxZ((float) (mCenter.getZ()+mRadius));
	}
	
}
