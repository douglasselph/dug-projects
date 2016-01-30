package com.dugsolutions.fell;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.dugsolutions.fell.map.MapGrid;
import com.dugsolutions.fell.map.gen.GenZConstant;
import com.dugsolutions.fell.map.gen.GenZMap;

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
			Gdx.app.log(TAG, "CAM POS=" + cam.position.toString() + ", DIR="
					+ cam.direction.toString());
			return true;
		}

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer,
				int button) {
			if (button == 1) {
				cam.direction.set(startDir);
				cam.position.set(startPos);
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
				if (adjXY) {
					Gdx.app.log(TAG, "ADJUST POSITION");
				} else {
					Gdx.app.log(TAG, "ADJUST DIRECTION");
				}
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
			} else if (character == 'o') {
				cam.position.set(oPos);
				cam.direction.set(oDir);
			}
			return false;
		}
	}

	SpriteBatch batch;
	Texture texture;
	Sprite sprite;
	Camera cam;
	boolean adjXY;
	Vector3 startPos;
	Vector3 startDir;
	Vector3 oPos;
	Vector3 oDir;

	TextureAtlas textureAtlas;
	// MeshObj2 meshObj3;
	// MeshObj2 meshObj4;
	MapGrid mapGrid;

	@Override
	public void create() {
		ShaderProgram.pedantic = false;

		textureAtlas = new TextureAtlas(Gdx.files.internal("fell.pack"));
		{
			batch = new SpriteBatch();
			texture = new Texture("badlogic.jpg");
			sprite = new Sprite(texture);
			sprite.setSize(200, 200);
			sprite.setPosition(300, 100);
		}
		Gdx.app.log(TAG, "WINDOW SIZE=" + Gdx.graphics.getWidth() + ", "
				+ Gdx.graphics.getHeight());

		initMap();
		initCamera();

		Gdx.input.setInputProcessor(new MyInputAdapter());

		Gdx.app.log(TAG, "VP SIZE=" + cam.viewportWidth + ", "
				+ cam.viewportHeight);
	}

	void initMap() {
		int w = 5;
		int h = 5;

		mapGrid = new MapGrid();
		mapGrid.setPosition(50, 50);
		mapGrid.setSize(w, h, 80f);
		mapGrid.setSubdivide(4);
		mapGrid.setBaseZ(0);

		AtlasRegion grass = textureAtlas.findRegion("Grass01");
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				mapGrid.setRegion(x, y, grass);
			}
		}
		mapGrid.build();

		GenZMap zmap = new GenZMap(mapGrid);
		zmap.setRandomSeed(0);
		zmap.addGenerator(new GenZConstant(50f));
		zmap.run();
	}

	void initMounds() {

	}

	void initCamera() {
		cam = new OrthographicCamera(Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());

		// cam.position.set(cam.viewportWidth / 2, cam.viewportHeight / 2,
		// cam.viewportWidth / 2);
		// cam.direction.set(0, 0, -1);

		startPos = new Vector3(cam.viewportWidth / 2, cam.viewportHeight / 2,
				cam.viewportWidth / 2);
		startDir = new Vector3(0, 0, -1);

		cam.position.set(startPos);
		cam.direction.set(startDir);

		cam.near = 1;
		cam.far = 1000;
		
		oPos = new Vector3(startPos.x, -300f, 570f);
		oDir = new Vector3(0, 0.92f, -1);
	}

	@Override
	public void render() {
		// Gdx.gl20.glViewport(0, 0, Gdx.graphics.getWidth(),
		// Gdx.graphics.getHeight());
		Gdx.gl20.glClearColor(0.6f, 0.2f, 0.2f, 1);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
		Gdx.gl20.glEnable(GL20.GL_TEXTURE_2D);
		Gdx.gl20.glEnable(GL20.GL_BLEND);
		Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		cam.update();

		// meshObj3.render(cam.combined);
		// meshObj4.render(cam.combined);

		// batch.setProjectionMatrix(cam.combined);
		// batch.begin();
		// sprite.draw(batch);
		// batch.end();

		mapGrid.render(cam.combined);
	}
}
