/*
 * (c) Faisal Khan. Created on 10/2/2022.
 */

package com.quranapp.android.utils.univ;

import android.graphics.Bitmap;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Android Bitmap Object to .bmp image (Windows BMP v3 24bit) file util class
 * <p>
 * ref : http://en.wikipedia.org/wiki/BMP_file_format
 *
 * @author ultrakain ( ultrasonic@gmail.com )
 * @since 2012-09-27
 */
public class BitmapUtils {
    private static final int BMP_WIDTH_OF_TIMES = 4;
    private static final int BYTE_PER_PIXEL = 3;

    /**
     * Android Bitmap Object to Window's v3 24bit Bmp Format File
     */
    public static void save(Bitmap orgBitmap, FileOutputStream out) throws IOException {
        //image size
        int width = orgBitmap.getWidth();
        int height = orgBitmap.getHeight();

        //image dummy data size
        //reason : bmp file's width equals 4's multiple
        int dummySize = 0;
        byte[] dummyBytesPerRow = null;
        boolean hasDummy = false;
        if (isBmpWidth4Times(width)) {
            hasDummy = true;
            dummySize = BMP_WIDTH_OF_TIMES - (width % BMP_WIDTH_OF_TIMES);
            dummyBytesPerRow = new byte[dummySize * BYTE_PER_PIXEL];
            Arrays.fill(dummyBytesPerRow, (byte) 0xFF);
        }

        int[] pixels = new int[width * height];
        int imageSize = pixels.length * BYTE_PER_PIXEL + (height * dummySize * BYTE_PER_PIXEL);
        int imageDataOffset = 0x36;
        int fileSize = imageSize + imageDataOffset;

        //Android Bitmap Image Data
        orgBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        //ByteArrayOutputStream baos = new ByteArrayOutputStream(fileSize);
        ByteBuffer buffer = ByteBuffer.allocate(fileSize);

        //  BITMAP FILE HEADER Write Start
        buffer.put((byte) 0x42);
        buffer.put((byte) 0x4D);

        //size
        buffer.put(writeInt(fileSize));

        //reserved
        buffer.put(writeShort((short) 0));
        buffer.put(writeShort((short) 0));

        //image data start offset
        buffer.put(writeInt(imageDataOffset));

        // BITMAP FILE HEADER Write End

        //*******************************************

        // BITMAP INFO HEADER Write Start
        //size
        buffer.put(writeInt(0x28));

        //width, height
        buffer.put(writeInt(width));
        buffer.put(writeInt(height));

        //planes
        buffer.put(writeShort((short) 1));

        //bit count
        buffer.put(writeShort((short) 24));

        //bit compression
        buffer.put(writeInt(0));

        //image data size
        buffer.put(writeInt(imageSize));

        //horizontal resolution in pixels per meter
        buffer.put(writeInt(0));

        //vertical resolution in pixels per meter (unreliable)
        buffer.put(writeInt(0));

        //컬러 사용 유무
        buffer.put(writeInt(0));

        //중요하게 사용하는 색
        buffer.put(writeInt(0));

        /** BITMAP INFO HEADER Write End */

        int row = height;
        int col = width;
        int startPosition = 0;
        int endPosition = 0;

        while (row > 0) {

            startPosition = (row - 1) * col;
            endPosition = row * col;

            for (int i = startPosition; i < endPosition; i++) {
                buffer.put(write24BitForPixel(pixels[i]));

                if (hasDummy) {
                    if (isBitmapWidthLastPixel(width, i)) {
                        buffer.put(dummyBytesPerRow);
                    }
                }
            }
            row--;
        }


        out.write(buffer.array());
        out.flush();
        out.close();
    }

    /**
     * Is last pixel in Android Bitmap width
     */
    private static boolean isBitmapWidthLastPixel(int width, int i) {
        return i > 0 && (i % (width - 1)) == 0;
    }

    /**
     * BMP file is a multiples of 4?
     */
    private static boolean isBmpWidth4Times(int width) {
        return width % BMP_WIDTH_OF_TIMES > 0;
    }

    /**
     * Write integer to little-endian
     */
    private static byte[] writeInt(int value) {
        byte[] b = new byte[4];

        b[0] = (byte) (value & 0x000000FF);
        b[1] = (byte) ((value & 0x0000FF00) >> 8);
        b[2] = (byte) ((value & 0x00FF0000) >> 16);
        b[3] = (byte) ((value & 0xFF000000) >> 24);

        return b;
    }

    /**
     * Write integer pixel to little-endian byte array
     */
    private static byte[] write24BitForPixel(int value) {
        byte[] b = new byte[3];

        b[0] = (byte) (value & 0x000000FF);
        b[1] = (byte) ((value & 0x0000FF00) >> 8);
        b[2] = (byte) ((value & 0x00FF0000) >> 16);

        return b;
    }

    /**
     * Write short to little-endian byte array
     */
    private static byte[] writeShort(short value) {
        byte[] b = new byte[2];

        b[0] = (byte) (value & 0x00FF);
        b[1] = (byte) ((value & 0xFF00) >> 8);

        return b;
    }
}
