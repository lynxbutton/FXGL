package com.almasb.fxgl.scene3d.computingProject

import java.io.File
import java.nio.charset.Charset
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class CompressSoul : Compress3D {
    override fun compress(name : String)
    {
        val isObj = name.endsWith("obj")
        val isDae = name.endsWith("dae")
        val fullPath = Path(name).absolutePathString()
        if(isObj)
        {
            val objList = fileLoad(fullPath)
            val mtlList = fileLoad(fullPath.dropLast(3) + "mtl")

            if(objList.isNotEmpty() && mtlList.isNotEmpty())
            {
                val soulList = convertObjtoSoul(objList, mtlList)
                exportSoul(soulList, fullPath.dropLast(3))
            }
        }
        else if(isDae)
        {
            val daeList = fileLoad(fullPath)
            if(daeList.isNotEmpty())
            {
                val soulList = convertDaetoSoul(daeList)
                exportSoul(soulList, fullPath.dropLast(3))
            }
        }
    }
    private fun fileLoad(name: String): ArrayList<String> {
        val list = ArrayList<String>()
        try{
            File(name).bufferedReader().useLines {
                it.forEach { line ->
                    list.add(line)
                }
            }
        }
        catch(exception: Exception)
        {
            print("OBJ/MTL/DAE file cannot be found.")
        }

        return list
    }

    private fun convertObjtoSoul(objList: ArrayList<String>, mtlList: ArrayList<String>): ArrayList<String> {
        val soulList = ArrayList<String>()
        if(objList.size != 0)
        {
            //Check for vertex, vertex normals & vertex textures
            for(line in objList)
            {
                if(line.first() == 'v' || line.startsWith("vt") || line.startsWith("vn"))
                {
                    soulList.add(line)
                }
                else if(line.startsWith("usemtl"))
                {
                    //Check for a readable MTL file
                    if(mtlList.size != 0)
                    {
                        //Checks for new material, color ambient, diffuse, specular, specular power and diffuse map
                        for(mtlLine in mtlList)
                        {
                            if(mtlLine.startsWith("newmtl") || mtlLine.startsWith("Ka")
                                || mtlLine.startsWith("Kd") || mtlLine.startsWith("Ks")
                                || mtlLine.startsWith("Ns") || mtlLine.startsWith("map_Kd"))
                            {
                                soulList.add(mtlLine)
                            }
                        }
                        soulList.add(line)
                    }
                    else{ print("No MTL File to support Geometry\n") }
                }
                else if(line.startsWith("f"))
                {
                    soulList.add(line)
                }
            }
        }
        else { print("OBJ File is empty\n") }

        return soulList
    }

    private fun convertNum(line : String) : String
    {
        var newLine = ""
        if(line == "10e-7")
        {
            newLine = "0.000001"
        }
        else
        {
            newLine = line
        }
        return newLine
    }

    private fun convertDaetoSoul(daeList : ArrayList<String>) : ArrayList<String>
    {
        val soulList = ArrayList<String>()
        var inMesh = false
        var findFaces = false

        var i = 0
        if(daeList.size != 0)
        {
            for(line in daeList)
            {
                val lineTrim = line.trim()
                var beginLine = ""


                if(lineTrim.startsWith("<mesh>")){inMesh = true}

                if(inMesh)
                {
                    if(lineTrim.startsWith("<source id") && lineTrim.endsWith("positions\">"))
                    {
                        beginLine = "v"
                    }
                    else if(lineTrim.startsWith("<source id") && lineTrim.endsWith("normals\">"))
                    {
                        beginLine = "vn"
                    }
                    else if(lineTrim.startsWith("<source id") && lineTrim.contains("map"))
                    {
                        beginLine = "vt"
                    }
                }

                if(beginLine == "v" || beginLine == "vn")
                {
                    val arr = daeList[i + 1].substringAfter(">").split(" ")
                    var num = 0
                    while(num <= arr.size)
                    {
                        if(num % 3 == 0 && num != 0)
                        {
                            var soulTemp = "$beginLine ${convertNum(arr[num - 3])} ${convertNum(arr[num - 2])} ${convertNum(arr[num - 1])}"
                            if(soulTemp.endsWith("</float_array>"))
                            {
                                soulTemp = soulTemp.substringBefore('<')
                            }
                            soulList.add(soulTemp)
                        }

                        num += 1
                    }
                }
                else if(beginLine == "vt")
                {
                    findFaces = true
                    val arr = daeList[i + 1].substringAfter(">").split(" ")
                    var num = 0
                    while(num <= arr.size)
                    {
                        if(num % 2 == 0 && num != 0)
                        {
                            var soulTemp = "$beginLine ${convertNum(arr[num - 2])} ${convertNum(arr[num - 1])}"
                            if(soulTemp.endsWith("</float_array>"))
                            {
                                soulTemp = soulTemp.substringBefore('<')
                            }
                            soulList.add(soulTemp)
                        }

                        num += 1
                    }
                }
                else{beginLine = ""}

                if(findFaces && inMesh && lineTrim.startsWith("<triangles"))
                {
                    val arr = daeList[i + 4].substringAfter(">").split(" ")

                    var num = 0
                    while(num < arr.size)
                    {
                        if(num != 0)
                        {
                            soulList.add("f ${arr[num - 9].toInt()+1}/${arr[num - 7].toInt()+1}/${arr[num - 8].toInt()+1} ${arr[num - 6].toInt()+1}/${arr[num - 4].toInt()+1}/${arr[num - 5].toInt()+1} ${arr[num - 3].toInt()+1}/${arr[num - 1].toInt()+1}/${arr[num - 2].toInt()+1}")
                        }

                        num += 9
                    }
                }

                i += 1
            }
        }
        else { print("DAE File is empty\n") }

        print(soulList)
        return soulList
    }

    private fun compressData(content: String) : String
    {
        //LZ77 style approach to compression

        val contentBytes = content.toByteArray(Charsets.UTF_8)


        /*var output = ""
        val maxSlideWindow = 4096

        val searchBuffer = ArrayList<Int>()
        val byteArray = content.toByteArray(Charsets.UTF_8)
        val checkChar = ArrayList<Int>()

        var i = 0

        for(byte in byteArray)
        {
            var index = elementsInArr(checkChar, searchBuffer)
            if(elementsInArr(checkChar + byte.toInt(), searchBuffer) == -1 || byteArray.size - 1 == i)
            {
                if(elementsInArr(checkChar + byte.toInt(), searchBuffer) != -1 || byteArray.size - 1 == i)
                {
                    checkChar.add(byte.toInt())
                }

                if(checkChar.size > 1)
                {
                    val offset = i - index - checkChar.size
                    val length = checkChar.size

                    val token = "<$offset,$length>"

                    if(token.length > length)
                    {
                        checkChar.forEach {
                            output += it.toChar()
                        }
                    }
                    else
                    {
                        output += token
                    }
                    searchBuffer.addAll(checkChar)
                }
                else
                {
                    checkChar.forEach {
                        output += it.toChar()
                    }
                    searchBuffer.addAll(checkChar)
                }
                checkChar.clear()
            }
            checkChar.add(byte.toInt())

            if(searchBuffer.size > maxSlideWindow)
            {
                searchBuffer.removeAt(0)
            }
            i += 1
        }*/

        /*for(byte in byteArray)
        {
            //checkChar.add(byte.toInt())
            //var index = elementsInArr(checkChar, searchBuffer)
            var temp = checkChar
            temp += byte.toInt()
            //print(temp)
            if(elementsInArr(temp, searchBuffer) == -1 || i == byteArray.size - 1)
            {
                if(i == byteArray.size - 1 && elementsInArr(temp,searchBuffer) != -1)
                {
                    checkChar.add(byte.toInt())
                }
                if(checkChar.size > 1)
                {
                    //val index = elementsInArr(checkChar.subList(0, checkChar.size - 2), searchBuffer)
                    val index = elementsInArr(checkChar, searchBuffer)
                    val offset = i - index - checkChar.size
                    val length = checkChar.size

                    val token = "<$offset,$length>"

                    if(token.length > length)
                    {
                        //print("\n")
                        checkChar.forEach{
                            newContent += it.toChar()
                        }
                    }
                    else
                    {
                        //print(token)
                        newContent += token
                    }
                    checkChar.forEach {
                        searchBuffer.add(it)
                    }
                }
                else
                {
                    //print("\n${byte.toChar()}")
                    //newContent += byte.toChar()
                    checkChar.forEach{
                        newContent += it.toChar()
                    }
                    checkChar.forEach {
                        searchBuffer.add(it)
                    }
                }

                checkChar.clear()
            }

            checkChar.add(byte.toInt())

            if(searchBuffer.size > maxSlideWindow)
            {
                searchBuffer.removeAt(0)
            }

            i += 1
        }*/

        return ""
    }

    private fun elementsInArr(checkEle : List<Int>, elements: ArrayList<Int>) : Int
    {
        var i = 0
        var offset = 0

        for(element in elements)
        {
            if(checkEle.size <= offset)
            {
                return i - checkEle.size
            }
            if(checkEle[offset] == element)
            {
                offset += 1
            }
            else
            {
                offset = 0
            }
            i += 1
        }

        return -1
    }

    private fun exportSoul(soulList: ArrayList<String>, path: String)
    {
        val fileName = path + "soul"

        val file = File(fileName)
        var content = ""
        for(line in soulList)
        {
            content += line
            content += "\n"
        }
        //val newContent = compressData(content)
        // create a new file
        file.writeText(content)
    }
}