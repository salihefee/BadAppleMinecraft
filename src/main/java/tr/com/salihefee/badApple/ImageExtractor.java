package tr.com.salihefee.badApple;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.nio.file.Files;

public class ImageExtractor implements Runnable {
    private final Path videoPath;
    private final String size;
    private final JavaPlugin plugin;
    private final CommandSender sender;

    public ImageExtractor(Path videoPath, String size, JavaPlugin plugin, CommandSender sender) {
        this.videoPath = videoPath;
        this.size = size;
        this.plugin = plugin;
        this.sender = sender;
        plugin.getLogger().info("ImageExtractor size: " + size);
    }

    public void run() {
        try {
            int exitCode = VideoProcessing.extractImages(videoPath, size, plugin);
            if (exitCode != 0) {
                plugin.getLogger().severe("Failed to extract images.");
            } else {
                plugin.getLogger().info("Extraction done.");

                Path framesDir = Paths.get(videoPath.getParent().toString(),
                        videoPath.getFileName().toString().substring(0,
                                videoPath.getFileName().toString().lastIndexOf('.')) + "frames");

                long count;
                try (Stream<Path> fileList = Files.list(framesDir)) {
                    count = fileList.filter(Files::isRegularFile).count();
                }

                sender.sendMessage("Extraction done. " + count + " frames extracted.");
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            plugin.getLogger().severe("Failed to extract images.");
        }
    }
}