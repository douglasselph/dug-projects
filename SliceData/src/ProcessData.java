import com.tipsolutions.jacket.data.FigureData;

public class ProcessData {
	
	public interface ICreate {
		FigureData create();
	};

	void run(String filename, ICreate create) {
		
		System.out.println("Writing " + filename);
		FigureData wdata = writeData(filename, create);
		System.out.println("Reading " + filename);
		FigureData rdata = readData(filename, create);
		System.out.println("Comparing results");
		
		wdata.compare(rdata, new FigureData.MessageWriter() {
			@Override
			public void msg(String msg) {
				System.out.println(msg);
			}
		});
	}
	
	FigureData writeData(String filename, ICreate create) {
		FigureData d = create.create();
		d.fill();
		d.writeData(filename);
		return d;
	}
	
	FigureData readData(String filename, ICreate create) {
		FigureData d = create.create();
		d.readData(filename);
		return d;
	}
}
