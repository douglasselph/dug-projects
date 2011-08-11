import com.tipsolutions.jacket.data.ShapeData;
import com.tipsolutions.jacket.image.TextureManager;

public class Main {
	
	public interface ICreate {
		ShapeData create();
	};
	
	static class ProcessData {
		
		void run(String filename, ICreate create) {
			
			System.out.println("Writing " + filename);
			ShapeData wdata = writeData(filename, create);
			System.out.println("Reading " + filename);
			ShapeData rdata = readData(filename, create);
			System.out.println("Comparing results");
			
			wdata.compare(filename, rdata, new ShapeData.MessageWriter() {
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
		
		ShapeData writeData(String filename, ICreate create) {
			ShapeData d = create.create();
			d.fill();
			d.computeBounds();
			d.writeData(filename);
			return d;
		}
		
		ShapeData readData(String filename, ICreate create) {
			ShapeData d = create.create();
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
			public ShapeData create() {
				return new hank();
			}
		});
		process.run("cube.data", new ICreate() {
			@Override
			public ShapeData create() {
				return new cube();
			}
		});
		process.run("susan.data", new ICreate() {
			@Override
			public ShapeData create() {
				return new susan();
			}
		});
		process.run("wing1.data", new ICreate() {
			@Override
			public ShapeData create() {
				return new wing1();
			}
		});
	}

}