package tr.com.salihefee.badApple;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;

public class VideoPlayer implements Runnable {
    private final String framesPath;
    private final World world;
    private final int width;
    private final int height;
    private final int startX;
    private final int startY;
    private final int startZ;
    private final JavaPlugin plugin;
    private final int length;
    private final int[] currentFrame;

    public VideoPlayer(String framesPath, World world, int width, int height, int startX,
                       int startY, int startZ, JavaPlugin plugin, int length, int[] currentFrame) {
        this.framesPath = framesPath;
        this.world = world;
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.plugin = plugin;
        this.length = length;
        this.currentFrame = currentFrame;
    }

    public void run() {
        if (currentFrame[0] > length) {
            for (int yOffset = height - 1; yOffset > 0; yOffset--) {
                for (int xOffset = 0; xOffset < width; xOffset++) {
                    world.getBlockAt(new Location(world, startX + xOffset, startY - yOffset, startZ))
                            .setType(Material.AIR);
                }
            }

            return;
        }

        BufferedImage image;
        int[][] intensities;

        File file = new File(Paths.get(framesPath, "/frame"
                + currentFrame[0] + ".png").toString());

        try (FileInputStream fis = new FileInputStream(file)) {
            image = ImageIO.read(fis);
            intensities = VideoProcessing.readImage(image);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to read image from file: " + file);
            return;
        }

        for (int yOffset = height - 1; yOffset > 0; yOffset--) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                world.getBlockAt(new Location(world, startX + xOffset, startY - yOffset, startZ))
                        .setType(BadApple.materials[Math.round(
                                intensities[yOffset][xOffset] / ((float) 255 / (BadApple.materials.length - 1)))]);
            }
        }

        plugin.getLogger().info("Rendered frame " + file.getName());

        currentFrame[0]++;
    }
}