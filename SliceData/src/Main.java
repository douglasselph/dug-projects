import com.tipsolutions.jacket.data.Shape;
import com.tipsolutions.jacket.image.TextureManager;

public class Main {
	
	public interface ICreate {
		Shape create();
	};
	
	static class ProcessData {
		
		void run(String filename, ICreate create) {
			
			System.out.println("Writing " + filename);
			Shape wdata = writeData(filename, create);
			System.out.println("Reading " + filename);
			Shape rdata = readData(filename, create);
			System.out.println("Comparing results");
			
			wdata.compare(filename, rdata, new Shape.MessageWriter() {
				@Override
				public void msg(String tag, String msg) {
					StringBuffer sbuf = new StringBuffer();
					sbuf.append(tag);
					sbuf.append(": ");
					sbuf.append(msg);
					System.out.println(sbuf.toString());
				}
			});
		}
		
		Shape writeData(String filename, ICreate create) {
			Shape d = create.create();
			d.fill();
			d.writeData(filename);
			return d;
		}
		
		Shape readData(String filename, ICreate create) {
			Shape d = create.create();
			d.readData(filename, mTM);
			return d;
		}
	};
	
	static TextureManager mTM;

	public static void main(String[] args) throws Exception {
		
		mTM = new TextureManager();
		ProcessData process = new ProcessData();
		
		process.run("hank.data", new ICreate() {
			@Override
			public Shape create() {
				return new data.hank();
			}
		});
		process.run("cube.data", new ICreate() {
			@Override
			public Shape create() {
				return new data.Cube();
			}
		});
		process.run("suzanne.data", new ICreate() {
			@Override
			public Shape create() {
				return new data.Suzanne();
			}
		});
		process.run("wingL.data", new ICreate() {
			@Override
			public Shape create() {
				return new data.Wing_L();
			}
		});
		process.run("wingArm.data", new ICreate() {
			@Override
			public Shape create() {
				return new data.wing.WingArm();
			}
		});
	}

}