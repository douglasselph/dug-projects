import com.tipsolutions.jacket.data.ShapeData;

public class Main {
	
	public static void main(String[] args) throws Exception {
		
		ProcessData process = new ProcessData();
		process.run("hank.data", new ProcessData.ICreate() {
			@Override
			public ShapeData create() {
				return new hank();
			}
		});
		process.run("cube.data", new ProcessData.ICreate() {
			@Override
			public ShapeData create() {
				return new cube();
			}
		});
		process.run("susan.data", new ProcessData.ICreate() {
			@Override
			public ShapeData create() {
				return new susan();
			}
		});
		process.run("pigeon2.data", new ProcessData.ICreate() {
			@Override
			public ShapeData create() {
				return new pigeon2();
			}
		});
	}
	

}