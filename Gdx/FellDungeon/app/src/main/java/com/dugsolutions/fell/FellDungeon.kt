package com.dugsolutions.fell

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Vector3
import com.dugsolutions.fell.data.MeshObj2
import com.dugsolutions.fell.db.DbCamera
import com.dugsolutions.fell.db.FellDatabaseHelper
import com.dugsolutions.fell.map.MapGrid
import com.dugsolutions.fell.map.gen2.GenZConstant
import com.dugsolutions.fell.map.gen2.GenZMap

class FellDungeon : ApplicationAdapter() {

    companion object {
        const val TAG = "FellDungeon"
        private const val ADJ = 0.01f
        private const val ZOOM = 0.02f
    }

    private inner class Box constructor(var cx: Int, var cy: Int, var radius: Int) {
        fun within(x: Int, y: Int): Boolean {
            val x0 = cx - radius
            val x1 = cx + radius
            val y0 = cy - radius
            val y1 = cy + radius
            return x in x0..x1 && y >= y0 && y <= y1
        }
    }

    private inner class MyInputAdapter : InputAdapter() {
        private var triggerX0MoveLeft: Int
        private var triggerX1MoveRight: Int
        private var triggerY0MoveUp: Int
        private var triggerY1MoveDown: Int
        private var centerTrigger: Box
        private var zoomIn = false
        fun move(screenX: Int, screenY: Int): Boolean {
            val camera = cam ?: return false
            if (screenX < triggerX0MoveLeft) {
                if (adjXY) {
                    camera.position.x += 10f
                } else {
                    camera.direction.x += ADJ
                }
            } else if (screenX > triggerX1MoveRight) {
                if (adjXY) {
                    camera.position.x -= 10f
                } else {
                    camera.direction.x -= ADJ
                }
            } else if (screenY < triggerY0MoveUp) {
                if (adjXY) {
                    camera.position.y -= 10f
                } else {
                    camera.direction.y -= ADJ
                }
            } else if (screenY > triggerY1MoveDown) {
                if (adjXY) {
                    camera.position.y += 10f
                } else {
                    camera.direction.y += ADJ
                }
            } else if (centerTrigger.within(screenX, screenY)) {
                if (camera is OrthographicCamera) {
                    if (zoomIn) {
                        camera.zoom += ZOOM
                    } else {
                        camera.zoom -= ZOOM

                    }
                }
            } else {
                return false
            }
            logCamera()
            return true
        }

        override fun touchDown(
            screenX: Int, screenY: Int, pointer: Int, button: Int
        ): Boolean {
            val camera = cam ?: return false
            if (button == 1) {
                camera.direction.set(startDir)
                camera.position.set(startPos)
                return true
            }
            return move(screenX, screenY)
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            return false
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            return move(screenX, screenY)
        }

        override fun scrolled(amountX: Float, amountY: Float): Boolean {
            val camera = cam ?: return false
            camera.position.z += 10 * amountX
            log("Z=" + camera.position.z)
            return true
        }

        override fun keyTyped(character: Char): Boolean {
            val camera = cam ?: return false
            val manager = cameraManager ?: return false
            when (character) {
                'z' -> {
                    adjXY = !adjXY
                    if (adjXY) {
                        log("ADJUST POSITION")
                    } else {
                        log("ADJUST DIRECTION")
                    }
                }
                'c' -> {
                    if (camera is OrthographicCamera) {
                        val pcam = PerspectiveCamera(
                            45f,
                            camera.viewportWidth, camera.viewportHeight
                        )
                        pcam.position.set(camera.position)
                        pcam.direction.set(camera.direction)
                        pcam.near = camera.near
                        pcam.far = camera.far
                        cam = pcam
                        log("PERSPECTIVE")
                    } else {
                        val ocam = OrthographicCamera(
                            camera.viewportWidth, camera.viewportHeight
                        )
                        ocam.position.set(camera.position)
                        ocam.direction.set(camera.direction)
                        ocam.near = camera.near
                        ocam.far = camera.far
                        cam = ocam
                        log("ORTHOGRAPHIC")
                    }
                }
                'o' -> {
                    camera.position.set(oPos)
                    camera.direction.set(oDir)
                }
                's' -> {
                    manager.store()
                }
                'n' -> {
                    manager.load()
                }
                'd' -> {
                    manager.delete()
                }
                'Z' -> {
                    zoomIn = !zoomIn
                    (camera as? OrthographicCamera)?.zoom = 1f
                    logCamera()
                }
                'p' -> {
                    camera.position.x = 0f
                    camera.position.y = 0f
                    logCamera()
                }
            }
            return false
        }

        init {
            val width = Gdx.graphics.width
            val height = Gdx.graphics.height
            triggerX0MoveLeft = width / 4
            triggerX1MoveRight = width * 3 / 4
            triggerY0MoveUp = height / 4
            triggerY1MoveDown = height * 3 / 4
            centerTrigger = Box(width / 2, height / 2, width / 5)
        }
    }

    inner class CameraManager {
        private var list: ArrayList<DbCamera>? = null
        var index = -1

        fun load() {
            val camera = cam ?: return
            val list = list ?: return
            if (list.size > 0) {
                if (++index >= list.size) {
                    index = 0
                }
                list[index].upload(camera)
            }
        }

        fun store() {
            val camera = cam ?: return
            database?.save(camera)
        }

        fun delete() {
            val camera = cam ?: return
            val list = list ?: return
            if (list.size >= 0) {
                for (c in list) {
                    if (c.equals(camera)) {
                        database?.deleteCamera(c)
                        list.remove(c)
                    }
                }
            }
        }

        init {
            database?.let { db -> list = db.queryCameras() }
        }
    }

    private var batch: SpriteBatch? = null
    private var texture: Texture? = null
    private var sprite: Sprite? = null
    var cam: Camera? = null
    var adjXY = true
    var startPos: Vector3? = null
    var startDir: Vector3? = null
    var oPos: Vector3? = null
    var oDir: Vector3? = null
    private lateinit var textureAtlas: TextureAtlas

    private var mapGrid: MapGrid? = null
    var database: FellDatabaseHelper? = null
    var cameraManager: CameraManager? = null

    override fun create() {
        database = FellDatabaseHelper()
        database?.open()
        cameraManager = CameraManager()
        ShaderProgram.pedantic = false
        textureAtlas = TextureAtlas(Gdx.files.internal("fell.pack"))
        run {
            batch = SpriteBatch()
            texture = Texture("badlogic.jpg")
            val sprite = Sprite(texture)
            sprite.setSize(200f, 200f)
            sprite.setPosition(300f, 100f)
            this.sprite = sprite
        }
        Gdx.app.log(
            TAG, "WINDOW SIZE=" + Gdx.graphics.width + ", "
                    + Gdx.graphics.height
        )
        initMap()
        initCamera()
        Gdx.input.inputProcessor = MyInputAdapter()
        cam?.let { cam ->
            Gdx.app.log(
                TAG, "VP SIZE=" + cam.viewportWidth + ", " + cam.viewportHeight
            )
        }
    }

    private fun initMap() {
        val w = 5
        val h = 5
        val mapGrid = MapGrid()
        mapGrid.setPosition(0f, 0f)
        mapGrid.setSize(w, h, 80f)
        mapGrid.subdivide = 4
        mapGrid.baseZ = 0f
        val grass = textureAtlas.findRegion("Grass01")
        for (y in 0 until h) {
            for (x in 0 until w) {
                mapGrid.setRegion(x, y, grass)
            }
        }
        mapGrid.build()
        this.mapGrid = mapGrid
        val zmap = GenZMap(mapGrid)
        zmap.setRandomSeed(0)
        zmap.addGenerator(GenZConstant(50f))
        zmap.run()
        log("MAP SIZE = " + mapGrid.mapSize + ", pos=" + mapGrid.position)
    }

    private fun initCamera() {
        val cam = OrthographicCamera(
            Gdx.graphics.width.toFloat(),
            Gdx.graphics.height.toFloat()
        )
        this.cam = cam

        // cam.position.set(cam.viewportWidth / 2, cam.viewportHeight / 2,
        // cam.viewportWidth / 2);
        // cam.direction.set(0, 0, -1);
        val startPos = Vector3(
            cam.viewportWidth / 2, cam.viewportHeight / 2,
            cam.viewportWidth / 2
        )
        this.startPos = startPos
        startDir = Vector3(0f, 0f, -1f)
        cam.position.set(startPos)
        cam.direction.set(startDir)
        cam.near = 1f
        cam.far = 1000f
        oPos = Vector3(startPos.x, -300f, 570f)
        oDir = Vector3(0f, 0.92f, -1f)
    }

    override fun render() {
        // Gdx.gl20.glViewport(0, 0, Gdx.graphics.getWidth(),
        // Gdx.graphics.getHeight());
        val camera = cam ?: return
        Gdx.gl20.glClearColor(0.6f, 0.2f, 0.2f, 1f)
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl20.glEnable(GL20.GL_TEXTURE_2D)
        Gdx.gl20.glEnable(GL20.GL_BLEND)
        Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        camera.update()

        batch?.let { batch ->
            sprite?.let { sprite ->
                batch.projectionMatrix = camera.combined
                batch.begin()
                sprite.draw(batch)
                batch.end()
            }
        }
        mapGrid?.render(camera.combined)
    }

    fun log(msg: String) {
        Gdx.app.log(TAG, msg)
    }

    private fun logCamera() {
        val camera = cam ?: return
        val sbuf = StringBuffer()
        sbuf.append("camera POS=" + camera.position.toString())
        if (camera.direction != Vector3(0f, 0f, -1f)) {
            sbuf.append(", DIR=" + camera.direction.toString())
        }
        if (camera is OrthographicCamera && camera.zoom != 1f) {
            sbuf.append(", ZOOM=" + camera.zoom)
        }
        log(sbuf.toString())
    }

}