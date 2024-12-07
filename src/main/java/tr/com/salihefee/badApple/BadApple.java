package tr.com.salihefee.badApple;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

public final class BadApple extends JavaPlugin implements Listener {
    static final Material[] materials = {
            Material.BLACK_CONCRETE,
            Material.BLACK_WOOL,
            Material.BLACKSTONE,
            Material.CRACKED_DEEPSLATE_TILES,
            Material.CRACKED_DEEPSLATE_BRICKS,
            Material.DEEPSLATE_COAL_ORE,
            Material.BEDROCK,
            Material.DEEPSLATE_COPPER_ORE,
            Material.CRACKED_STONE_BRICKS,
            Material.COBBLESTONE,
            Material.ANDESITE,
            Material.SMOOTH_STONE,
            Material.DIORITE,
            Material.POLISHED_DIORITE,
            Material.WHITE_CONCRETE,
            Material.IRON_BLOCK,
            Material.WHITE_WOOL,
            Material.SNOW_BLOCK
    };

    private static final List<BukkitTask> renderTasks = Collections.synchronizedList(new ArrayList<>());

    private static final Map<String, String> framePaths = new HashMap<>();

    static int extractImages(Path videoPath, String size, JavaPlugin plugin)
            throws IOException, InterruptedException {
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

    @Override
    public void onEnable() {
        getLogger().info("Hello, World!");

        Objects.requireNonNull(getCommand("badapple")).setExecutor(this);
        Objects.requireNonNull(getCommand("cancel")).setExecutor(this);
        Objects.requireNonNull(getCommand("extract")).setExecutor(this);

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Bye, World!");
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        event.setJoinMessage(event.getPlayer().getDisplayName() + " is here.");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (label.equalsIgnoreCase("badapple")) {
            if (args.length == 0) {
                sender.sendMessage("Please specify a video.");
                return false;
            }

            String framesPath = framePaths.get(args[0]);

            if (framesPath == null) {
                sender.sendMessage("Please extract the video first.");
                return false;
            }

            World world = Bukkit.getWorld("world");

            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be run by a player.");
                return false;
            }

            Location playerLoc = player.getLocation();

            BufferedImage firstFrame;

            try {
                firstFrame = ImageIO.read(new File(
                        Paths.get(framesPath, "frame1.png").toString()));
            } catch (IOException e) {
                getLogger().severe("Failed to read first frame.");
                return false;
            }

            int width = firstFrame.getWidth();
            int height = firstFrame.getHeight();

            int startX = (int) playerLoc.getX() - width / 2;
            int startY = (int) playerLoc.getY() + height / 2;

            int playerFacing = (int) playerLoc.getYaw();

            int startZ;

            if (playerFacing < -90 || playerFacing > 90)
                startZ = (int) playerLoc.getZ() - 50;

            else
                startZ = (int) playerLoc.getZ() + 50;

            if (world == null) {
                getLogger().severe("World not found.");
                return false;
            }

            int length = Objects.requireNonNull(
                    new File(framesPath)
                            .listFiles(File::isFile)).length;

            int[] currentFrame = { 1 };

            synchronized (renderTasks) {
                renderTasks.add(getServer().getScheduler().runTaskTimer(this, new videoPlayer(framesPath, world,
                        width, height, startX, startY, startZ, this, length, currentFrame), 0, 1));
            }

            return true;
        } else if (label.equalsIgnoreCase("cancel")) {
            getLogger().info("Cancelling...");
            // run = false;
            synchronized (renderTasks) {
                for (BukkitTask task : renderTasks) {
                    task.cancel();
                }
            }
            return true;
        } else if (label.equalsIgnoreCase("extract")) {
            if (args.length == 0) {
                sender.sendMessage("Please specify a video.");
                return false;
            }
            if (args.length > 2) {
                sender.sendMessage("Received more arguments than expected.");
                return false;
            }

            String videoPathStr = args[0];

            String videoSize = args[1];

            Path videoPath = Paths.get(videoPathStr);

            File videoFile = videoPath.toFile();

            String videoNameNoExtension = videoFile.getName().substring(0, videoFile.getName().lastIndexOf('.'));

            if (!videoFile.exists()) {
                sender.sendMessage("File not found.");
                return false;
            }

            ImageExtractor imageExtractor = new ImageExtractor(videoPath, videoSize, this, sender);
            Thread thread = new Thread(imageExtractor);
            thread.start();

            framePaths.put(videoFile.getName(),
                    Paths.get(videoPath.getParent().toString(), videoNameNoExtension + "frames").toString());

            return true;
        }
        return false;
    }
}

class ImageExtractor implements Runnable {
    private final Path videoPath;
    private final String size;
    private final JavaPlugin plugin;
    private final CommandSender sender;

    public ImageExtractor(Path videoPath, String size, JavaPlugin plugin, CommandSender sender) {
        this.videoPath = videoPath;
        this.size = size;
        this.plugin = plugin;
        this.sender = sender;
    }

    public void run() {
        try {
            int exitCode = BadApple.extractImages(videoPath, size, plugin);
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

class videoPlayer implements Runnable {
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

    public videoPlayer(String framesPath, World world, int width, int height, int startX,
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