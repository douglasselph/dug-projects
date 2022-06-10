package com.dugsolutions.fell.map

import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.dugsolutions.fell.map.MapSquareData
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4

class MapSquareUI(  // This square is displaying this region in the atlas.
    var region: AtlasRegion
) : MapSquareData() {

    var mesh: Mesh? = null

    fun build(
        startX: Float, startY: Float, width: Float, height: Float,
        startZ: Float
    ) {
        build(
            startX, startY, width, height, region.u, region.v,
            region.u2, region.v2, startZ
        )
        val mesh = Mesh(
            true, verts.size / 6, indices.size,
            VertexAttribute(
                VertexAttributes.Usage.Position, 3,
                ShaderProgram.POSITION_ATTRIBUTE
            ), VertexAttribute(
                VertexAttributes.Usage.ColorPacked, 4,
                ShaderProgram.COLOR_ATTRIBUTE
            ), VertexAttribute(
                VertexAttributes.Usage.TextureCoordinates, 2,
                ShaderProgram.TEXCOORD_ATTRIBUTE + "0"
            )
        )
        this.mesh = mesh
        mesh.setVertices(verts)
        mesh.setIndices(indices)
    }

    fun render(program: ShaderProgram, projMatrix: Matrix4?) {
        region.texture.bind()
        program.begin()
        program.setUniformMatrix("u_projTrans", projMatrix)
        program.setUniformi("u_texture", 0)
        mesh?.render(program, GL20.GL_TRIANGLES)
        program.end()
    }

    override fun setElevations(startX: Float, startY: Float, width: Float, height: Float, startZ: Float) {
        super.setElevations(startX, startY, width, height, startZ)
        mesh?.setVertices(verts)
    }
}