package tr.com.salihefee.badApple;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;

public class VideoProcessing {

    private static final float redWeight = 0.299f;
    private static final float greenWeight = 0.587f;
    private static final float blueWeight = 0.114f;

    public static int[][] readImage(BufferedImage image) throws IOException {
        int width = image.getWidth();
        int height = image.getHeight();
        final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        final boolean hasAlphaChannel = image.getAlphaRaster() != null;
        int[][] grayscaleArray = new int[height][width];

        final int pixelLength = hasAlphaChannel ? 4 : 3;
        convertToGrayscale(pixels, grayscaleArray, pixelLength, width);

        return grayscaleArray;
    }

    private static void convertToGrayscale(byte[] pixels, int[][] grayscaleArray, int pixelLength, int width) {
        for (int pixel = 0, row = 0, col = 0; pixel + pixelLength <= pixels.length; pixel += pixelLength) {
            int blue = pixels[pixel + (pixelLength - 3)] & 0xff;
            int green = pixels[pixel + (pixelLength - 2)] & 0xff;
            int red = pixels[pixel + (pixelLength - 1)] & 0xff;
            grayscaleArray[row][col] = Math.round(red * redWeight + green * greenWeight + blue * blueWeight);
            col++;
            if (col == width) {
                col = 0;
                row++;
            }
        }
    }
}
