package tr.com.salihefee.badapplerealtime;

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
import java.util.ArrayList;
import java.util.Objects;
import java.time.Instant;

import javax.imageio.ImageIO;

public final class BadApple extends JavaPlugin implements Listener {
    private static final Material[] materials = {
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

    private static final ArrayList<BukkitTask> renderTasks = new ArrayList<>();

    private static String videoWithExtension = "badapple.mp4";

    private static String video;

    private static int extractImages(String size) {
        ProcessBuilder processBuilder = new ProcessBuilder();

        processBuilder.command("bash", "-c", "ffmpeg -i /home/salihefee/Documents/MineStorage/MineStorageInput/" + videoWithExtension + " -vf 'fps=20, scale=-1:" + size + "' /home/salihefee/Documents/MineStorage/MineStorageInput/" + video + "frames/frame%d.png");

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return exitCode;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("Hello, World!");

        Objects.requireNonNull(getCommand("badappler")).setExecutor(this);
        Objects.requireNonNull(getCommand("cancelr")).setExecutor(this);

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Bye, World :(");
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        event.setJoinMessage(event.getPlayer().getDisplayName() + " is here.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (label.equalsIgnoreCase("badappler")) {
            if (args.length == 0) {
                sender.sendMessage("Please specify a video.");
                return false;
            }

            videoWithExtension = args[0];

            video = videoWithExtension.substring(0, videoWithExtension.lastIndexOf('.'));

            int exitCode = extractImages(args[1]);

            if (exitCode != 0) {
                System.out.println("Failure!");
                return false;
            }

            World world = Bukkit.getWorld("world");

            Player player = (Player) sender;

            Location playerLoc = player.getLocation();

            BufferedImage firstFrame;

            int[][] firstFrameIntensities;

            try {
                firstFrame = ImageIO.read(new File(String.format("/home/salihefee/Documents/MineStorage/MineStorageInput/%sframes/frame1.png", video)));
                firstFrameIntensities = VideoProcessing.readImage(firstFrame);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            int width = firstFrameIntensities[0].length;
            int height = firstFrameIntensities.length;

            int fps = 20;

            int startX = (int) playerLoc.getX() - width / 2;
            int startY = (int) playerLoc.getY() + height / 2;

            int playerFacing = (int) playerLoc.getYaw();

            int startZ;

            if (playerFacing < -90 || playerFacing > 90) startZ = (int) playerLoc.getZ() - 50;

            else startZ = (int) playerLoc.getZ() + 50;

            assert world != null;

            // /home/salihefee/Documents/Python/badapple/badapplemc

//            File[] files = Objects.requireNonNull(new File("/home/salihefee/Documents/Python/badapple/badapplemc").listFiles(File::isFile));

            final int[] i = {1};

            int length = Objects.requireNonNull(new File(String.format("/home/salihefee/Documents/MineStorage/MineStorageInput/%sframes", video)).listFiles(File::isFile)).length;

            long start = Instant.now().toEpochMilli();

            renderTasks.add(getServer().getScheduler().runTaskTimer(this, () -> {

                if (i[0] > length) {
                    return;
                }

                BufferedImage image;
                int[][] intensities;

                File file = new File("/home/salihefee/Documents/MineStorage/MineStorageInput/" + video + "frames/frame" + i[0] + ".png");

                try {
                    image = ImageIO.read(file);
                    intensities = VideoProcessing.readImage(image);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                for (int yOffset = height - 1; yOffset > 0; yOffset--) {
                    for (int xOffset = 0; xOffset < width; xOffset++) {
                        world.getBlockAt(new Location(world, startX + xOffset, startY - yOffset, startZ))
                                .setType(materials[Math.round(intensities[yOffset][xOffset] / ((float) 255 / (materials.length - 1)))]);
                    }
                }

                getLogger().info("Rendered frame " + file.getName());

                i[0]++;
            }, 0, 20 / fps));

            return true;
        } else if (label.equalsIgnoreCase("cancelr")) {
            getLogger().info("Cancelling...");
//            run = false;
            for (BukkitTask task : renderTasks) {
                task.cancel();
            }
            return true;
        }
        return false;
    }
}