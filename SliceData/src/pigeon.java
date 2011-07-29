/* THIS IS A GENERATED FILE */

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import com.tipsolutions.jacket.data.ShapeData;
import com.tipsolutions.jacket.math.Matrix4f;


class pigeon extends ShapeData {
	@Override protected ShapeData [] _getChildren() {
		ShapeData [] children = new ShapeData[13];
		children[0] = new pigeon_Feather();
		children[1] = new pigeon_Feather001();
		children[2] = new pigeon_Feather002();
		children[3] = new pigeon_Feather003();
		children[4] = new pigeon_Feather004();
		children[5] = new pigeon_Feather005();
		children[6] = new pigeon_Feather006();
		children[7] = new pigeon_Feather007();
		children[8] = new pigeon_Feather008();
		children[9] = new pigeon_Feather009();
		children[10] = new pigeon_Feather010();
		children[11] = new pigeon_Feather011();
		children[12] = new pigeon_Wing();
		return children;
	}

	@Override protected Matrix4f _getMatrix() {
		return new Matrix4f(1.000000f, 0.000000f, 0.000000f, -2.000000f,
		                    0.000000f, 1.000000f, 0.000000f, 0.000000f,
		                    0.000000f, 0.000000f, 1.000000f, 0.000000f,
		                    0.000000f, 0.000000f, 0.000000f, 1.000000f);
	}};
