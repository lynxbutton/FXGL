package com.almasb.fxgl.scene3d.computingProject

import com.almasb.fxgl.dsl.FXGL
import com.almasb.fxgl.scene3d.Model3D
import com.almasb.fxgl.scene3d.Model3DLoader
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.CullFace
import javafx.scene.shape.MeshView
import javafx.scene.shape.TriangleMesh
import javafx.scene.shape.VertexFormat
import java.io.BufferedReader
import java.io.File
import java.net.URL

/**
 * TODO: revisit implementation
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class SoulModelLoader : Model3DLoader {

    companion object {
        private val soulParsers = linkedMapOf<(String) -> Boolean, (List<String>, SoulData) -> Unit>()
        //private val mtlParsers = linkedMapOf<(String) -> Boolean, (List<String>, MtlData) -> Unit>()

        init {
            soulParsers[{ it.startsWith("g") }] = Companion::parseGroup
            soulParsers[{ it.startsWith("s") }] = Companion::parseSmoothing
            soulParsers[{ it.startsWith("vt") }] = Companion::parseVertexTextures
            soulParsers[{ it.startsWith("vn") }] = Companion::parseVertexNormals
            soulParsers[{ it.startsWith("v ") }] = Companion::parseVertices
            soulParsers[{ it.startsWith("f") }] = Companion::parseFaces
            soulParsers[{ it.startsWith("usemtl") }] = Companion::parseUseMaterial

            soulParsers[{ it.startsWith("newmtl") }] = Companion::parseNewMaterial
            soulParsers[{ it.startsWith("Ka") }] = Companion::parseColorAmbient
            soulParsers[{ it.startsWith("Kd") }] = Companion::parseColorDiffuse
            soulParsers[{ it.startsWith("Ks") }] = Companion::parseColorSpecular
            soulParsers[{ it.startsWith("Ns") }] = Companion::parseSpecularPower
            soulParsers[{ it.startsWith("map_Kd") }] = Companion::parseDiffuseMap
        }

        private fun parseGroup(tokens: List<String>, data: SoulData) {
            val groupName = if (tokens.isEmpty()) "default" else tokens[0]

            data.groups += SoulGroup(groupName)
        }

        private fun parseSmoothing(tokens: List<String>, data: SoulData) {
            data.currentGroup.currentSubGroup.smoothingGroup = tokens.toSmoothingGroup()
        }

        private fun parseVertexTextures(tokens: List<String>, data: SoulData) {
            data.vertexTextures += tokens.toFloats2()
        }

        private fun parseVertexNormals(tokens: List<String>, data: SoulData) {
            data.vertexNormals += tokens.toFloats3()
        }

        private fun parseVertices(tokens: List<String>, data: SoulData) {
            // for -Y
            // .mapIndexed { index, fl -> if (index == 1) -fl else fl }
            data.vertices += tokens.toFloats3()
        }

        private fun parseFaces(tokens: List<String>, data: SoulData) {
            if (tokens.size > 3) {
                for (i in 2 until tokens.size) {
                    parseFaceVertex(tokens[0], data)
                    parseFaceVertex(tokens[i - 1], data)
                    parseFaceVertex(tokens[i], data)
                }
            } else {
                tokens.forEach { token ->
                    parseFaceVertex(token, data)
                }
            }
        }

        /**
         * Each token is of form v1/(vt1)/(vn1).
         * Case v1
         * Case v1/vt1
         * Case v1//n1
         * Case v1/vt1/vn1
         */
        private fun parseFaceVertex(token: String, data: SoulData) {
            val faceVertex = token.split("/")

            // JavaFX format is vertices, normals and tex
            when (faceVertex.size) {
                // f v1
                1 -> {
                    data.currentGroup.currentSubGroup.faces += faceVertex[0].toInt() - 1
                    // add vt1 as 0
                    data.currentGroup.currentSubGroup.faces += 0
                }

                // f v1/vt1
                2 -> {
                    data.currentGroup.currentSubGroup.faces += faceVertex[0].toInt() - 1
                    // add vt1
                    data.currentGroup.currentSubGroup.faces += faceVertex[1].toInt() - 1
                }

                // f v1//vn1
                // f v1/vt1/vn1
                3 -> {
                    data.currentGroup.currentSubGroup.faces += faceVertex[0].toInt() - 1
                    data.currentGroup.currentSubGroup.faces += faceVertex[2].toInt() - 1
                    // add vt1 if present, else 0
                    data.currentGroup.currentSubGroup.faces += (faceVertex[1].toIntOrNull() ?: 1) - 1
                    data.currentGroup.currentSubGroup.vertexFormat = VertexFormat.POINT_NORMAL_TEXCOORD
                }
            }
        }

        /*private fun parseMaterialLib(tokens: List<String>, data: SoulData) {
            val fileName = tokens[0]
            print(tokens)
            val mtlURL = URL(data.url.toExternalForm().substringBeforeLast('/') + '/' + fileName)

            val mtlData = loadMtlData(mtlURL)

            data.materials += mtlData.materials
            data.ambientColors += mtlData.ambientColors
        }*/

        private fun parseUseMaterial(tokens: List<String>, data: SoulData) {
            data.currentGroup.subGroups += SubGroup()
            data.currentGroup.currentSubGroup.material = data.materials[tokens[0]]
                ?: throw RuntimeException("Material with name ${tokens[0]} not found")

            data.currentGroup.currentSubGroup.ambientColor =
                data.ambientColors[data.currentGroup.currentSubGroup.material]
        }

        private fun List<String>.toFloats2(): List<Float> {
            return this.take(2).map { it.toFloat() }
        }

        private fun List<String>.toFloats3(): List<Float> {
            return this.take(3).map { it.toFloat() }
        }

        private fun List<String>.toColor(): Color {
            val rgb = this.toFloats3().map { if (it > 1.0) 1.0 else it.toDouble() }
            return Color.color(rgb[0], rgb[1], rgb[2])
        }

        private fun List<String>.toSmoothingGroup(): Int {
            return if (this[0] == "off") 0 else this[0].toInt()
        }

        private fun parseNewMaterial(tokens: List<String>, data: SoulData) {
            data.currentMaterial = PhongMaterial()
            data.materials[tokens[0]] = data.currentMaterial
        }

        private fun parseColorAmbient(tokens: List<String>, data: SoulData) {
            data.ambientColors[data.currentMaterial] = tokens.toColor()
        }

        private fun parseColorDiffuse(tokens: List<String>, data: SoulData) {
            data.currentMaterial.diffuseColor = tokens.toColor()
        }

        private fun parseColorSpecular(tokens: List<String>, data: SoulData) {
            data.currentMaterial.specularColor = tokens.toColor()
        }

        private fun parseSpecularPower(tokens: List<String>, data: SoulData) {
            data.currentMaterial.specularPower = tokens[0].toDouble()
        }

        private fun parseDiffuseMap(tokens: List<String>, data: SoulData) {
            val ext = data.url.toExternalForm().substringBeforeLast("/") + "/"

            data.currentMaterial.diffuseMap = FXGL.getAssetLoader().loadImage(URL(ext + tokens[0]))
        }

        private fun loadSoulData(url: URL): SoulData {
            val data = SoulData(url)

            //decompress it here and pass string to parsers
            //val content = decompressSoul(url)
            val inputString = url.openStream().bufferedReader().use { it.readText() }
            val content = decompressSoul(inputString)
            print(content.decodeToString())
            //val inputString = "tex<3,1>s.forEach(<8,1>un<6,1>ti<13,1>n <10,1>t<24,3>Obj)<10,1>{<12,1> <14,1> <4,3> <8,7> <28,1>c<42,1>nv<45,1>s<52,1>r<58,1>m<54,1>v<62,1>(<41,8>;<40,24>add<37,23>}<53,2>"
            //val content = decompressSoul(inputString)
            //print(content)
            //load(content, soulParsers, data)
            load(content.decodeToString(), soulParsers, data)

            return data
        }

        fun decompressSoul(txt : String) : ByteArray
        {
            val textArray = txt.encodeToByteArray() //compressed data as a byte array
            var uncompressedData = byteArrayOf(); //new uncompressed data as a byte array
            var inToken = false // Boolean to check if we are in the token
            var findLength = false // Boolean to check if we are looking for the length

            var index = ""
            var length = ""

            for(char in textArray)
            {
                if(char.toInt().toChar() == '<')
                {
                    inToken = true
                }
                else if(char.toInt().toChar() == ',' && inToken)
                {
                    findLength = true
                }
                else if(char.toInt().toChar() == '>' && findLength)
                {
                    inToken = false
                    findLength = false
                    var num = 0
                    while(num < length.toInt())
                    {
                        uncompressedData += uncompressedData[index.toInt() + num]

                        num += 1
                    }
                    index = ""
                    length = ""
                }
                else if(inToken && !findLength)
                {
                    index += char.toInt().toChar()
                }
                else if(inToken && findLength)
                {
                    length += char.toInt().toChar()
                }
                else
                {
                    uncompressedData += char
                }
            }

            return uncompressedData
        }

        private fun <T> load(
            content: String,
            parsers: Map<(String) -> Boolean, (List<String>, T) -> Unit>,
            data: T
        ) {

            val contentArr = content.split("\n").toTypedArray()
            contentArr.forEach { line ->

                val lineTrimmed = line.trim()

                for ((condition, action) in parsers) {
                    if (condition.invoke(lineTrimmed)) {
                        // drop identifier
                        val tokens = lineTrimmed.split(" +".toRegex()).drop(1)

                        action.invoke(tokens, data)
                        break
                    }
                }
            }
        }
        /*private fun <T> load(url: URL,
                             parsers: Map<(String) -> Boolean, (List<String>, T) -> Unit>,
                             data: T) {

            url.openStream().bufferedReader().useLines {
                it.forEach { line ->

                    val lineTrimmed = line.trim()

                    for ((condition, action) in parsers) {
                        if (condition.invoke(lineTrimmed) ) {
                            // drop identifier
                            val tokens = lineTrimmed.split(" +".toRegex()).drop(1)

                            action.invoke(tokens, data)
                            break
                        }
                    }
                }
            }
        }*/
    }

    // TODO: smoothing groups
    override fun load(url: URL): Model3D {
        try {
            val data = loadSoulData(url)
            val modelRoot = Model3D()

            data.groups.forEach {
                val groupRoot = Model3D()
                groupRoot.properties["name"] = it.name

                it.subGroups.forEach {

                    // TODO: ?
                    if (!it.faces.isEmpty()) {

                        val mesh = TriangleMesh(it.vertexFormat)

                        mesh.points.addAll(*data.vertices.map { it * 0.05f }.toFloatArray())

                        // if there are no vertex textures, just add 2 values
                        if (data.vertexTextures.isEmpty()) {
                            mesh.texCoords.addAll(*FloatArray(2) { _ -> 0.0f })
                        } else {
                            mesh.texCoords.addAll(*data.vertexTextures.toFloatArray())
                        }

                        if (it.vertexFormat === VertexFormat.POINT_NORMAL_TEXCOORD) {
                            // if there are no vertex normals, just add 3 values
                            if (data.vertexNormals.isEmpty()) {
                                mesh.normals.addAll(*FloatArray(3) { _ -> 0.0f })
                            } else {
                                mesh.normals.addAll(*data.vertexNormals.toFloatArray())
                            }
                        }

                        mesh.faces.addAll(*it.faces.toIntArray())

                        if (it.smoothingGroups.isNotEmpty()) {
                            mesh.faceSmoothingGroups.addAll(*it.smoothingGroups.toIntArray())
                        }

                        val view = MeshView(mesh)
                        view.material = it.material
                        view.cullFace = CullFace.NONE

                        groupRoot.addMeshView(view)
                    }
                }

                modelRoot.addModel(groupRoot)
            }

            return modelRoot
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Load failed for URL: $url Error: $e")
        }
    }
}