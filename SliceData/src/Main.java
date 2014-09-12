import com.dugsolutions.jacket.image.DummyTextureManager;
import com.dugsolutions.jacket.image.ITextureManager;
import com.dugsolutions.jacket.shape.Shape;
import com.dugsolutions.jacket.shape.Shape.AnimControlOp;
import com.dugsolutions.jacket.shape.Shape.AnimSet;

public class Main
{
	public interface ICreate
	{
		Shape create();
	};

	static class ProcessData
	{
		Shape readData(String filename, ICreate create)
		{
			Shape d = create.create();
			d.readData(filename, mTM);
			return d;
		}

		void run(String filename, ICreate create)
		{
			System.out.println("Writing " + filename);
			Shape wdata = writeData(filename, create);
			System.out.println("Reading " + filename);
			Shape rdata = readData(filename, create);
			System.out.println("Comparing results");

			wdata.compare(filename, rdata, new Shape.MessageWriter()
			{
				@Override
				public void msg(String tag, String msg)
				{
					StringBuffer sbuf = new StringBuffer();
					sbuf.append(tag);
					sbuf.append(": ");
					sbuf.append(msg);
					System.out.println(sbuf.toString());
				}
			});
		}

		Shape writeData(String filename, ICreate create)
		{
			Shape d = create.create();
			d.writeData(filename);
			return d;
		}
	};

	static ITextureManager	mTM;

	public static void main(String[] args) throws Exception
	{
		mTM = new DummyTextureManager();
		ProcessData process = new ProcessData();

		process.run("hank.data", new ICreate()
		{
			@Override
			public Shape create()
			{
				return new data.hank().fill();
			}
		});
		process.run("cube.data", new ICreate()
		{
			@Override
			public Shape create()
			{
				return new data.Cube().fill();
			}
		});
		process.run("suzanne.data", new ICreate()
		{
			@Override
			public Shape create()
			{
				return new data.Suzanne().fill();
			}
		});
		process.run("wingL.data", new ICreate()
		{
			@Override
			public Shape create()
			{
				return new data.Wing_L().fill();
			}
		});
		process.run("wingArm.data", new ICreate()
		{
			@Override
			public Shape create()
			{
				Shape shape = new data.wing.WingArm().fill();
				AnimSet animSet = shape.getAnimBone().getAnimSet().get(0);
				animSet.setControlOp(AnimControlOp.ControlShape);
				animSet.cullToTime(animSet.getStartTime(), 1.0f);
				return shape;
			}
		});
	}

}