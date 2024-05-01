package com.almasb.fxgl.scene3d.computingProject;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;
import static com.almasb.fxgl.dsl.FXGL.*;

public class CompressionMain {

    public static byte[] compress(byte[] data){
        try(ByteArrayOutputStream out = new ByteArrayOutputStream();
            DeflaterOutputStream deflater = new DeflaterOutputStream(out)){

            deflater.write(data);
            deflater.finish();

            return out.toByteArray();

        } catch (Exception e){
            System.out.println("error " + e);
        }
        return new byte[0];
    }
    public static byte[] decompress(byte[] compressedData){
        try(ByteArrayOutputStream out = new ByteArrayOutputStream();
            InflaterOutputStream inflater = new InflaterOutputStream(out)){

            inflater.write(compressedData);
            inflater.finish();

            return out.toByteArray();

        } catch (Exception e){
            System.out.println("error " + e);
        }
        return new byte[0];
    }
    public static void main(String[] args) throws Exception{
        byte[] original = Files.readAllBytes(Paths.get("fxgl-samples/src/main/resources/assets/models/fox1.soul"));

        byte[] compressed = compress(original);

        System.out.println("Original Size: " + original.length);
        System.out.println("Compressed Size: " + compressed.length);

        byte[] decompressed = decompress(compressed);

        System.out.println(Arrays.equals(original, decompressed));

        new CompressSoul().compress("fxgl-samples/src/main/resources/assets/models/fox.obj");

    }
}
