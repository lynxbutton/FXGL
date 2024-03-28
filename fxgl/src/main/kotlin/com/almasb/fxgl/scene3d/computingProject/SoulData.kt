package com.almasb.fxgl.scene3d.computingProject

import javafx.scene.paint.Color
import javafx.scene.paint.Material
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.VertexFormat
import java.net.URL

/**
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
internal class SoulData(val url: URL) {
    val groups = arrayListOf<SoulGroup>()
    val vertices = arrayListOf<Float>()
    val vertexNormals = arrayListOf<Float>()
    val vertexTextures = arrayListOf<Float>()

    val materials = hashMapOf<String, Material>()
    val ambientColors = hashMapOf<Material, Color>()

    val currentGroup: SoulGroup
        get() {
            // it is possible there are no groups in the obj file,
            // in which case when asked for current group return default
            // TODO: extract string
            if (groups.isEmpty())
                groups += SoulGroup("default")

            return groups.last()
        }
}

internal class SoulGroup(val name: String) {
    val subGroups = arrayListOf<SubGroup>(SubGroup())

    val currentSubGroup
        get() = subGroups.last()
}

internal class SubGroup {
    val faces = arrayListOf<Int>()
    val smoothingGroups = arrayListOf<Int>()

    // as per OBJ file spec, default is white
    var material: Material = PhongMaterial(Color.WHITE)
    var ambientColor: Color? = null

    var smoothingGroup = -1

    var vertexFormat = VertexFormat.POINT_TEXCOORD
}

internal class MtlData(
    /**
     * URL of the .mtl file.
     */
    val url: URL) {

    val materials = hashMapOf<String, Material>()
    val ambientColors = hashMapOf<Material, Color>()

    lateinit var currentMaterial: PhongMaterial
}