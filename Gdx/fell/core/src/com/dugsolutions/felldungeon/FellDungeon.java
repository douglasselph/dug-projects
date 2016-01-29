package com.dugsolutions.felldungeon;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class FellDungeon extends ApplicationAdapter {
	final static String TAG = "FellDungeon";

	static float ADJ = 0.01f;

	class MyInputAdapter extends InputAdapter {

		int triggerX0;
		int triggerX1;
		int triggerY0;
		int triggerY1;

		MyInputAdapter() {
			triggerX0 = Gdx.graphics.getWidth() / 4;
			triggerX1 = (Gdx.graphics.getWidth() * 3) / 4;
			triggerY0 = Gdx.graphics.getHeight() / 4;
			triggerY1 = (Gdx.graphics.getHeight() * 3) / 4;
		}

		boolean move(int screenX, int screenY) {
			if (screenX < triggerX0) {
				if (adjXY) {
					cam.position.x += 10;
				} else {
					cam.direction.x += ADJ;
				}
			} else if (screenX > triggerX1) {
				if (adjXY) {
					cam.position.x -= 10;
				} else {
					cam.direction.x -= ADJ;
				}
			}
			if (screenY < triggerY0) {
				if (adjXY) {
					cam.position.y -= 10;
				} else {
					cam.direction.y -= ADJ;
				}
			} else if (screenY > triggerY1) {
				if (adjXY) {
					cam.position.y += 10;
				} else {
					cam.direction.y += ADJ;
				}
			} else {
				return false;
			}
			Gdx.app.log(TAG, "CAM X=" + cam.position.x + ", Y="
					+ cam.position.y + ", DX=" + cam.direction.x + ", DY="
					+ cam.direction.y);
			return true;
		}

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer,
				int button) {
			if (button == 1) {
				cam.direction.y = 0;
				cam.direction.x = 0;
				cam.position.z = startZ;
				return true;
			}
			return move(screenX, screenY);
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			return false;
		}

		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			return move(screenX, screenY);
		}

		@Override
		public boolean scrolled(int amount) {
			cam.position.z += 10 * amount;
			Gdx.app.log(TAG, "Z=" + cam.position.z);
			return true;
		}

		@Override
		public boolean keyTyped(char character) {
			if (character == 'z') {
				adjXY = !adjXY;
				Gdx.app.log(TAG, "ADJXY=" + adjXY);
			} else if (character == 'c') {
				if (cam instanceof OrthographicCamera) {
					PerspectiveCamera pcam = new PerspectiveCamera(45,
							cam.viewportWidth, cam.viewportHeight);
					pcam.position.set(cam.position);
					pcam.direction.set(cam.direction);
					pcam.near = cam.near;
					pcam.far = cam.far;
					cam = pcam;
					Gdx.app.log(TAG, "PERSPECTIVE");

				} else {
					OrthographicCamera ocam = new OrthographicCamera(
							cam.viewportWidth, cam.viewportHeight);
					ocam.position.set(cam.position);
					ocam.direction.set(cam.direction);
					ocam.near = cam.near;
					ocam.far = cam.far;
					cam = ocam;
					Gdx.app.log(TAG, "ORTHOGRAPHIC");
				}
			}
			return false;
		}
	}

	SpriteBatch batch;
	Texture texture;
	Sprite sprite;
	Mesh mesh;
	ShaderProgram shaderProgram;
	Camera cam;
	boolean adjXY;
	float startZ;

	@Override
	public void create() {
		ShaderProgram.pedantic = false;

		batch = new SpriteBatch();
		texture = new Texture("badlogic.jpg");
		sprite = new Sprite(texture);
		sprite.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		Gdx.app.log(TAG, "WINDOW SIZE=" + Gdx.graphics.getWidth() + ", "
				+ Gdx.graphics.getHeight());

		float x, y; // Mesh location in the world
		float width, height; // Mesh width and height

		x = y = 50f;
		width = height = 300f;

		float verts[] = buildVerts(x, y, width, height, 2);

		// Create a mesh out of two triangles rendered clockwise without indices
		mesh = new Mesh(true, verts.length / 5, 0, new VertexAttribute(
				VertexAttributes.Usage.Position, 3,
				ShaderProgram.POSITION_ATTRIBUTE), new VertexAttribute(
				VertexAttributes.Usage.TextureCoordinates, 2,
				ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

		mesh.setVertices(verts);

		shaderProgram = new ShaderProgram(Gdx.files.internal(
				"shaderVertex.glsl").readString(), Gdx.files.internal(
				"shaderFragment.glsl").readString());

		String log = shaderProgram.getLog();
		if (!shaderProgram.isCompiled())
			throw new GdxRuntimeException(log);
		if (log != null && log.length() != 0)
			System.out.println("Shader Log: " + log);

		cam = new OrthographicCamera(Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());
		cam.position.set(cam.viewportWidth / 2, cam.viewportHeight / 2,
				cam.viewportWidth / 2);
		cam.direction.set(0, 0, -1);
		cam.near = 1;
		cam.far = 1000;
		startZ = cam.position.z;

		Gdx.input.setInputProcessor(new MyInputAdapter());

		Gdx.app.log(TAG, "VP SIZE=" + cam.viewportWidth + ", "
				+ cam.viewportHeight);
	}

	@Override
	public void render() {
		// Gdx.gl20.glViewport(0, 0, Gdx.graphics.getWidth(),
		// Gdx.graphics.getHeight());
		Gdx.gl20.glClearColor(0.2f, 0.2f, 0.2f, 1);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
		Gdx.gl20.glEnable(GL20.GL_TEXTURE_2D);
		Gdx.gl20.glEnable(GL20.GL_BLEND);
		Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		cam.update();
		batch.setProjectionMatrix(cam.combined);

		batch.begin();
		sprite.draw(batch);
		batch.end();

		texture.bind();
		shaderProgram.begin();
		shaderProgram.setUniformMatrix("u_projTrans",
				batch.getProjectionMatrix());
		shaderProgram.setUniformi("u_texture", 0);
		mesh.render(shaderProgram, GL20.GL_TRIANGLES);
		shaderProgram.end();
	}

	float[] buildVerts(float startX, float startY, float width, float height,
			int size) {
		float[] verts;
		int i = 0;
		float x, y;
		float u, v;
		float cellSizeX = width / size;
		float cellSizeY = height / size;
		float cellSizeU = 1f / size;
		float cellSizeV = 1f / size;
		final int count = 6 * 5 * size * size;
		verts = new float[count];

		for (y = startY, v = 1; y <= height; y += cellSizeY, v -= cellSizeV) {
			for (x = startX, u = 0; x <= width; x += cellSizeX, u += cellSizeU) {
				// Bottom left vertex triangle 1
				verts[i++] = x; // X
				verts[i++] = y; // Y
				verts[i++] = 0; // Z
				verts[i++] = u; // U
				verts[i++] = v; // V

				// Top left vertex triangle 1
				verts[i++] = x; // X
				verts[i++] = y + cellSizeY; // Y
				verts[i++] = 0; // Z
				verts[i++] = u; // U
				verts[i++] = v - cellSizeV; // V

				// Top right vertex triangle 1
				verts[i++] = x + cellSizeX;
				verts[i++] = y + cellSizeY;
				verts[i++] = 0;
				verts[i++] = u + cellSizeU;
				verts[i++] = v - cellSizeV;
				
				// Bottom left vertex triangle 2
				verts[i++] = x; // X
				verts[i++] = y; // Y
				verts[i++] = 0; // Z
				verts[i++] = u; // U
				verts[i++] = v; // V
				
				// Top right vertex triangle 2
				verts[i++] = x + cellSizeX;
				verts[i++] = y + cellSizeY;
				verts[i++] = 0;
				verts[i++] = u + cellSizeU;
				verts[i++] = v - cellSizeV;
				
				// Bottom right vertex triangle 2
				verts[i++] = x + cellSizeX; // X
				verts[i++] = y; // Y
				verts[i++] = 0; // Z
				verts[i++] = u + cellSizeU; // U
				verts[i++] = v; // V
			}
		}
		Gdx.app.log(TAG, "COUNT=" + count + ", i=" + i);
		return verts;

	}
}
