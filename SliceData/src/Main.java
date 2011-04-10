
public class Main {
	
	static final String HankFile = "hank.data";
	
	public static void main(String[] args) throws Exception {
		System.out.println("Writing " + HankFile);
		hank hw = writeData(HankFile);
		System.out.println("Reading " + HankFile);
		hank hr = readData(HankFile);
		System.out.println("Comparing results");
		hw.compare(hr, new FigureData.MessageWriter() {
			@Override
			public void msg(String msg) {
				System.out.println(msg);
			}
		});
	}
	
	static hank writeData(String filename) {
		hank h = new hank();
		h.fill();
		h.writeData(filename);
		return h;
	}
	
	static hank readData(String filename) {
		hank h = new hank();
		h.readData(filename);
		return h;
	}

}