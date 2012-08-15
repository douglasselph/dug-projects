package com.tipsolutions.jacket.math;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class BufUtils
{
	public static final int	TYPE_FLOAT	= 1;
	public static final int	TYPE_SHORT	= 2;

	public static abstract class Buffer<BUFTYPE>
	{
		ByteBuffer	mRoot;
		BUFTYPE		mBuf;

		public Buffer(int size)
		{
			mRoot = ByteBuffer.allocateDirect(size * getSize());
			mRoot.order(ByteOrder.nativeOrder()); // Get this from android
													// platform
			mBuf = asBuffer(mRoot);
		}

		void set(ByteBuffer buf)
		{
			mRoot = buf;
			mBuf = asBuffer(buf);
		}

		public int capacity()
		{
			return capacity(mBuf);
		}

		public BUFTYPE getBuf()
		{
			return mBuf;
		}

		public ByteBuffer getRootBuf()
		{
			mRoot.rewind();
			return mRoot;
		}

		public abstract BUFTYPE asBuffer(ByteBuffer buf);

		public abstract int capacity(BUFTYPE buf);

		public abstract void rewind(BUFTYPE buf);

		abstract int getSize();

		abstract int getType();

		public void readBuffer(DataInputStream dataStream) throws IOException, Exception
		{
			int type = dataStream.readInt();
			int size = dataStream.readInt();

			if (type != getType())
			{
				if (getType() == TYPE_FLOAT)
				{
					throw new Exception("Expected float type");
				}
				if (getType() == TYPE_SHORT)
				{
					throw new Exception("Expected short type");
				}
				throw new Exception("Expected a different type");
			}
			ByteBuffer vbb = ByteBuffer.allocateDirect(size);
			vbb.order(ByteOrder.nativeOrder()); // Get this from android
												// platform
			if (vbb.hasArray())
			{
				dataStream.read(vbb.array(), 0, size);
			}
			else
			{
				byte[] dst = new byte[size];
				dataStream.read(dst, 0, size);
				vbb.rewind();
				vbb.put(dst);
				vbb.rewind();
			}
			set(vbb);
		}

		public void writeBuffer(DataOutputStream dataStream) throws IOException
		{
			ByteBuffer vbb = getRootBuf();
			if (vbb != null)
			{
				dataStream.writeInt(getType());
				dataStream.writeInt(vbb.limit());
				if (vbb.hasArray())
				{
					dataStream.write(vbb.array(), 0, vbb.limit());
				}
				else
				{
					vbb.rewind();
					byte[] dst = new byte[vbb.limit()];
					vbb.get(dst);
					dataStream.write(dst, 0, vbb.limit());
					vbb.rewind();
				}
			}
		}
	}

	public static class FloatBuf extends Buffer<FloatBuffer>
	{
		public FloatBuf(int size)
		{
			super(size);
		}

		@Override
		public FloatBuffer asBuffer(ByteBuffer buf)
		{
			return buf.asFloatBuffer();
		}

		@Override
		public int capacity(FloatBuffer buf)
		{
			return buf.capacity();
		}

		public float get()
		{
			return mBuf.get();
		}

		@Override
		int getSize()
		{
			return 4;
		}

		@Override
		int getType()
		{
			return TYPE_FLOAT;
		}

		public boolean hasRemaining()
		{
			return mBuf.hasRemaining();
		}

		public int limit()
		{
			return mBuf.limit();
		}

		public int position()
		{
			return mBuf.position();
		}

		public int remaining()
		{
			return mBuf.remaining();
		}

		public void rewind()
		{
			rewind(getBuf());
		}

		@Override
		public void rewind(FloatBuffer buf)
		{
			buf.rewind();
		}

		public String toString()
		{
			StringBuffer sbuf = new StringBuffer();

			rewind();
			while (hasRemaining())
			{
				sbuf.append(position());
				sbuf.append("=");
				sbuf.append(get());
				sbuf.append(" ");
			}
			return sbuf.toString();
		}

		public String toString(int breakEvery)
		{
			StringBuffer sbuf = new StringBuffer();

			rewind();
			int count = 0;
			boolean firstTime = true;
			while (hasRemaining())
			{
				if (firstTime)
				{
					firstTime = false;
				}
				else
				{
					if (++count >= breakEvery)
					{
						sbuf.append("\n");
						count = 0;
					}
					else
					{
						sbuf.append(" ");
					}
				}
				sbuf.append(position());
				sbuf.append("=");
				sbuf.append(get());
			}
			return sbuf.toString();
		}
	}

	public static class ShortBuf extends Buffer<ShortBuffer>
	{
		public ShortBuf(int size)
		{
			super(size);
		}

		@Override
		public ShortBuffer asBuffer(ByteBuffer buf)
		{
			return buf.asShortBuffer();
		}

		@Override
		public int capacity(ShortBuffer buf)
		{
			return buf.capacity();
		}

		public short get()
		{
			return mBuf.get();
		}

		@Override
		int getSize()
		{
			return 2;
		}

		@Override
		int getType()
		{
			return TYPE_SHORT;
		}

		public boolean hasRemaining()
		{
			return mBuf.hasRemaining();
		}

		public int limit()
		{
			return mBuf.limit();
		}

		public int position()
		{
			return mBuf.position();
		}

		public void put(short v)
		{
			mBuf.put(v);
		}

		public int remaining()
		{
			return mBuf.remaining();
		}

		public void rewind()
		{
			rewind(getBuf());
		}

		@Override
		public void rewind(ShortBuffer buf)
		{
			buf.rewind();
		}

		public String toString()
		{
			StringBuffer sbuf = new StringBuffer();

			rewind();
			while (hasRemaining())
			{
				sbuf.append(position());
				sbuf.append("=");
				sbuf.append(get());
				sbuf.append("|");
			}
			return sbuf.toString();
		}
	};

}
