package tr.com.salihefee.badApple;

import org.bukkit.plugin.java.JavaPlugin;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class VideoProcessing {
    public static int extractImages(Path videoPath, String size, JavaPlugin plugin) throws IOException, InterruptedException {
        plugin.getLogger().info("ExtractImages size: " + size);

        Path videoParent = videoPath.getParent();
        Path videoFile = videoPath.getFileName();
        String videoFileName = videoFile.toString();
        String videoFileNameNoExtension = videoFileName.substring(0, videoFileName.lastIndexOf('.'));
        Path framesDir = Paths.get(videoParent.toString(), videoFileNameNoExtension + "frames");

        ProcessBuilder processBuilder = new ProcessBuilder();
        Process process;
        int exitCode = 0;

        try {
            Files.createDirectory(framesDir);
        } catch (FileAlreadyExistsException e) {
            exitCode = 1;
        }

        if (exitCode != 0 && Objects.requireNonNull(new File(framesDir.toString()).listFiles()).length != 0) {
            File dir = new File(framesDir.toString());
            File[] files = Objects.requireNonNull(dir.listFiles());
            boolean deleteSuccess = false;
            for (File file : files)
                deleteSuccess = file.delete();
            if (!deleteSuccess)
                return 1;
        }

        processBuilder.command("ffmpeg", "-i", videoPath.toString(), "-vf", "fps=20, scale=-1:" + size,
                Paths.get(framesDir.toString(), "frame%d.png").toString());

        try {
            process = processBuilder.start();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to start ffmpeg.");
            return 1;
        }

        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            plugin.getLogger().severe("Interrupted while waiting for ffmpeg.");
            Thread.currentThread().interrupt();
            return 1;
        }
        return exitCode;
    }

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
        final float redWeight = 0.299f;
        final float greenWeight = 0.587f;
        final float blueWeight = 0.114f;

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