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
import com.dugsolutions.fell.map.MapGrid;

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
				cam.position.set(cam.viewportWidth / 2, cam.viewportHeight / 2,
						cam.viewportWidth / 2);
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
			}
			return false;
		}
	}

	SpriteBatch batch;
	Texture texture;
	Sprite sprite;
	Camera cam;
	boolean adjXY;
	float startZ;
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
		// {
		// AtlasRegion region = textureAtlas.findRegion("Grass03");
		// AtlasRegion region2 = textureAtlas.findRegion("Tree12");
		//
		// meshObj3 = new MeshObj2(region, 50f, 50f, 300f, 300f, 2);
		// meshObj4 = new MeshObj2(region2, 100f, 100f, 300f, 300f, 2);
		// }
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
		mapGrid.setZ(0, 3f);

		AtlasRegion grass = textureAtlas.findRegion("Grass01");
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				mapGrid.setRegion(x, y, grass);
			}
		}
		mapGrid.build();
	}
	
	void initMounds()
	{
	
	}

	void initCamera() {
		cam = new OrthographicCamera(Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());
		cam.position.set(cam.viewportWidth / 2, cam.viewportHeight / 2,
				cam.viewportWidth / 2);
		cam.direction.set(0, 0, -1);
		cam.near = 1;
		cam.far = 1000;
		startZ = cam.position.z;
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
