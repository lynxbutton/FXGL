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
                //exportSoul(soulList, fullPath.dropLast(3))
            }
        }
        else if(isDae)
        {
            val daeList = fileLoad(fullPath)
            if(daeList.isNotEmpty())
            {
                val soulList = convertDaetoSoul(daeList)
                var tempList = ""
                for(line in soulList)
                {
                    tempList += "$line\n"
                }
                val compressedSoul = compressData(tempList)
                //print(compressedSoul.decodeToString())

                exportSoul(compressedSoul.decodeToString(), fullPath.dropLast(3))
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
                                || mtlLine.startsWith("Kd") || mtlLine.startsWith("Ks") || mtlLine.startsWith("Ke")
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
        if(line.contains("10e-"))
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
        var findTexture = false
        var image = ""
        var material = ""

        var i = 0
        if(daeList.size != 0)
        {
            for(line in daeList)
            {
                val lineTrim = line.trim()
                var beginLine = ""

                if(lineTrim.startsWith("<mesh>")){inMesh = true}

                if(lineTrim.startsWith("<init_from>") && lineTrim.contains(".png"))
                {
                    image = daeList[i].substringAfter('>').substringBefore('<')
                }
                else if(lineTrim.startsWith("<material"))
                {
                    material = daeList[i].substringAfter("name=\"").substringBefore("\">")
                }

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
                    findTexture = true
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

                if(findTexture && inMesh)
                {
                    soulList.add("newmtl $material")
                    soulList.add("Ns 100.000000")
                    soulList.add("Ka 0.200000 0.200000 0.200000")
                    soulList.add("Ks 0.000000 0.000000 0.000000")
                    soulList.add("Ke 0.000000 0.000000 0.000000")
                    soulList.add("map_Kd $image")
                    soulList.add("usemtl $material")
                    findFaces = true
                    findTexture = false
                }

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

        //print(soulList)
        return soulList
    }

    fun compressData(txt : String) : ByteArray
    {
        val textArray = txt.encodeToByteArray() //Original text as byte array
        var compressedData = byteArrayOf() //New text as byte array

        val maxWindow = 500 // Maximum number of elements in search buffer
        var indexSearchFirst = 0 //What index the search buffer starts at

        var lookAhead = ArrayList<Byte>() //Elements that are about to show up
        textArray.forEach { lookAhead.add(it) }

        val searchBuffer = ArrayList<Byte>() //Elements that have previously appeared
        var skip = 0 //Whether to skip characters and how many
        var x = 0 //Counts the number of loop iterations

        for(char in textArray)
        {
            if(skip == 0)
            {
                //print("$searchBuffer\n$char\n$lookAhead\n")

                if(searchBuffer.size > 1)
                {

                    val temp = findLongestMatch(char, x, indexSearchFirst,searchBuffer, lookAhead)
                    if(temp.length > 1)
                    {
                        /*for (byte in lookAhead) {
                            print(byte.toChar())
                        }*/
                        skip = temp.substringAfter(',').substringBefore(">").toInt() - 1
                        //print("\n$skip\
                    }
                    if(lookAhead.isNotEmpty()){lookAhead.removeFirst()}
                    searchBuffer.add(char)
                    compressedData += temp.encodeToByteArray()
                    //print("$temp")
                }
                else
                {
                    //print("${char.toChar()}")
                    compressedData += char
                    if(lookAhead.isNotEmpty()){lookAhead.removeFirst()}
                    searchBuffer.add(char)
                }
            }
            else
            {
                skip -= 1
                if(lookAhead.isNotEmpty()){lookAhead.removeFirst()}
                searchBuffer.add(char)
            }

            if(searchBuffer.size > maxWindow)
            {
                searchBuffer.removeFirst()
                indexSearchFirst += 1
            }
            x += 1
        }
        //print(compressedData.decodeToString())

        return compressedData
    }

    fun findLongestMatch(char : Byte, num : Int, indexOffset : Int, search : ArrayList<Byte>, look : ArrayList<Byte>) : String
    {
        val occurTimes = ArrayList<Int>()
        var i = 0
        for(element in search)
        {
            if(element == char)
            {
                occurTimes.add(i)
            }
            i += 1
        }
        //print("CHARACTER: ${char.toChar()} OCCURENCES: $occurTimes\n")


        var token = ""

        for(index in occurTimes)
        {
            var createToken = true
            var length = 0
            var y = 0
            //print("\nNEW OCCURENCE\n")
            for(byte in look)
            {
                //print("${byte}\n")
                //val temp = index.toString().length + length.toString().length + 3

                if(createToken)
                {
                    if(index + y < search.size)
                    {
                        val temp = "<${index + indexOffset},$length>"
                        if(byte == search[index + y])
                        {
                            //print("\n${char} & ${byte} & ${search[index + y]} & $length\n")
                            //print("${index} & $y & ${index + y}\n")
                            //print(look)
                            length += 1
                            y += 1
                        }
                        else if(length > 2 && num + length >= search.size - 1 + look.size - 1)
                        {
                            length -= 2
                            if(temp.length > length)
                            {
                                token = ""
                            }
                            else{
                                token = "<${index + indexOffset},$length>"
                            }
                            createToken = false
                        }
                        else if(length > 2 && temp.length >= token.length)
                        {
                            length -= 1
                            if(temp.length > length)
                            {
                                token = ""
                            }
                            else{
                                token = "<${index + indexOffset},$length>"
                            }
                            createToken = false

                        }
                        else if(length < 2 && y > 0 || length > 0)
                        {
                            createToken = false
                            token = ""
                        }
                    }
                }
            }
        }

        if(token.isNotEmpty())
        {
            return token
        }
        else
        {
            return char.toChar().toString()
        }
    }

    private fun exportSoul(soulList: String, path: String)
    {
        val fileName = path + "soul"

        val file = File(fileName)
        var content = ""
        for(line in soulList)
        {
            content += line
            //content += "\n"
        }
        //val newContent = compressData(content)
        // create a new file
        file.writeText(content)
    }
}