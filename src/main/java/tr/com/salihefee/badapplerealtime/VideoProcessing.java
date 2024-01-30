package tr.com.salihefee.badapplerealtime;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;

public class VideoProcessing {

    public static int[][] readImage(BufferedImage image) throws IOException {

        int w = image.getWidth();
        int h = image.getHeight();
        final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        final boolean hasAlphaChannel = image.getAlphaRaster() != null;
        int[][] result = new int[h][w];

        if (hasAlphaChannel) {
            final int pixelLength = 4;
            for (int pixel = 0, row = 0, col = 0; pixel + 4 <= pixels.length; pixel += pixelLength) {
                int blue = (int) pixels[pixel + 1] & 0xff;
                int green = (int) pixels[pixel + 2] & 0xff;
                int red = (int) pixels[pixel + 3] & 0xff;
                result[row][col] = Math.round(red * 0.299f + green * 0.587f + blue * 0.114f);
                col++;
                if (col == w) {
                    col = 0;
                    row++;
                }
            }
        } else {
            final int pixelLength = 3;
            for (int pixel = 0, row = 0, col = 0; pixel + 3 <= pixels.length; pixel += pixelLength) {
                int blue = (int) pixels[pixel] & 0xff;
                int green = (int) pixels[pixel + 1] & 0xff;
                int red = (int) pixels[pixel + 2] & 0xff;
                result[row][col] = Math.round(red * 0.299f + green * 0.587f + blue * 0.114f);
                col++;
                if (col == w) {
                    col = 0;
                    row++;
                }
            }
        }


        return result;
    }

}
