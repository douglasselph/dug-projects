import com.tipsolutions.jacket.data.ShapeData;

public class ProcessData {
	
	public interface ICreate {
		ShapeData create();
	};

	void run(String filename, ICreate create) {
		
		System.out.println("Writing " + filename);
		ShapeData wdata = writeData(filename, create);
		System.out.println("Reading " + filename);
		ShapeData rdata = readData(filename, create);
		System.out.println("Comparing results");
		
		wdata.compare(rdata, new ShapeData.MessageWriter() {
			@Override
			public void msg(String msg) {
				System.out.println(msg);
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
		d.readData(filename);
		return d;
	}
}
