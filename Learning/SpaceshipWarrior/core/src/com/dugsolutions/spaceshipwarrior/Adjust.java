package com.dugsolutions.spaceshipwarrior;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

public class Adjust
{
	public static Adjust getInstance()
	{
		return sAdjust;
	}

	public static void Init()
	{
		new Adjust();
	}

	static Adjust	sAdjust;

    float           counter;
	Matrix4			mx = new Matrix4();
    Matrix4         dx = new Matrix4();
    Vector3         tx = new Vector3();

	public Adjust()
	{
		sAdjust = this;
        mx.idt();
        dx.idt();
	}

	public void inc(float x, float y)
	{
        mx.translate(x, y, 0);
        dx.idt();
        counter += 1;
	}

	public void next(float delta)
	{
        dx.idt();

        if (counter > 0)
        {
            mx.getTranslation(tx);
            counter -= delta;
            tx.scl(delta);
            dx.setTranslation(tx);
        }
        else
        {
            counter = 0;
        }
	}

    public Matrix4 getMx()
    {
        return dx;
    }

}
