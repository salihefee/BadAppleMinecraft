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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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

    @Override
    public void onEnable() {
        getLogger().info("BadAppleMinecraft enabled");

        Objects.requireNonNull(getCommand("badapple")).setExecutor(this);
        Objects.requireNonNull(getCommand("cancel")).setExecutor(this);
        Objects.requireNonNull(getCommand("extract")).setExecutor(this);

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("BadAppleMinecraft disabled");
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        event.setJoinMessage(event.getPlayer().getDisplayName() + " is here.");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             String[] args) {
        if (label.equalsIgnoreCase("badapple")) {
            return handleBadAppleCommand(sender, args);
        } else if (label.equalsIgnoreCase("cancel")) {
            return handleCancelCommand();
        } else if (label.equalsIgnoreCase("extract")) {
            return handleExtractCommand(sender, args);
        }
        return false;
    }

    private boolean handleBadAppleCommand(CommandSender sender, String[] args) {
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

        int[] currentFrame = {1};

        synchronized (renderTasks) {
            renderTasks.add(getServer().getScheduler().runTaskTimer(this, new VideoPlayer(framesPath, world,
                    width, height, startX, startY, startZ, this, length, currentFrame), 0, 1));
        }

        return true;
    }

    private boolean handleCancelCommand() {
        getLogger().info("Cancelling...");
        synchronized (renderTasks) {
            for (BukkitTask task : renderTasks) {
                task.cancel();
            }
        }
        return true;
    }

    private boolean handleExtractCommand(CommandSender sender, String[] args) {
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
        getLogger().info("Video size: " + videoSize);

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
}