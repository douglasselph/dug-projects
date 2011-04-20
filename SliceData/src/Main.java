import com.tipsolutions.jacket.data.FigureData;

public class Main {
	
	static final String HankFile = "hank.data";
	
	public static void main(String[] args) throws Exception {
		
		ProcessData process = new ProcessData();
		process.run("hank.data", new ProcessData.ICreate() {
			@Override
			public FigureData create() {
				return new hank();
			}
		});
		process.run("cube.data", new ProcessData.ICreate() {
			@Override
			public FigureData create() {
				return new cube();
			}
		});
		process.run("susan.data", new ProcessData.ICreate() {
			@Override
			public FigureData create() {
				return new susan();
			}
		});
	}
	

}