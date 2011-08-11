/* THIS IS A GENERATED FILE */

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import com.tipsolutions.jacket.data.ShapeData;
import com.tipsolutions.jacket.math.Matrix4f;


class wing1 extends ShapeData {
	@Override protected ShapeData [] _getChildren() {
		ShapeData [] children = new ShapeData[13];
		children[0] = new wing1_Feather();
		children[1] = new wing1_Feather001();
		children[2] = new wing1_Feather002();
		children[3] = new wing1_Feather003();
		children[4] = new wing1_Feather004();
		children[5] = new wing1_Feather005();
		children[6] = new wing1_Feather006();
		children[7] = new wing1_Feather007();
		children[8] = new wing1_Feather008();
		children[9] = new wing1_Feather009();
		children[10] = new wing1_Feather010();
		children[11] = new wing1_Feather011();
		children[12] = new wing1_Wing();
		return children;
	}

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(1.000000f, 0.000000f, 0.000000f, -2.000000f,
		                    0.000000f, 1.000000f, 0.000000f, 0.000000f,
		                    0.000000f, 0.000000f, 1.000000f, 0.000000f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}};
