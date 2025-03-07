package com.debianrose;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerToggleSneakEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.level.Location;
import cn.nukkit.level.particle.DustParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.TextFormat;

import java.util.HashMap;
import java.util.Map;

public class Main extends PluginBase implements Listener {

    private final Map<String, Region> regions = new HashMap<>();
    private final Map<Player, Location> pos1Map = new HashMap<>();
    private final Map<Player, Location> pos2Map = new HashMap<>();
    private final Map<Player, String> creatingRegions = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info(TextFormat.GREEN + "DpkgRegion enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(TextFormat.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("rg")) {
            if (args.length < 1) {
                player.sendMessage(TextFormat.YELLOW + "Usage: /rg create <Name>");
                return true;
            }

            if (args[0].equalsIgnoreCase("create")) {
                if (args.length < 2) {
                    player.sendMessage(TextFormat.RED + "Please specify a region name!");
                    return true;
                }

                String name = args[1];
                if (regions.containsKey(name)) {
                    player.sendMessage(TextFormat.RED + "A region with this name already exists!");
                    return true;
                }

                creatingRegions.put(player, name);
                player.sendMessage(TextFormat.GREEN + "Please select Position 1 (Left Click) and Position 2 (Right Click).");
            }
        }
        return true;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (creatingRegions.containsKey(player)) {
            if (event.getAction() == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) {
                pos1Map.put(player, event.getBlock().getLocation());
                player.sendMessage(TextFormat.GREEN + "Position 1 set: " + formatLocation(event.getBlock().getLocation()));
                event.setCancelled(true);
            } else if (event.getAction() == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
                pos2Map.put(player, event.getBlock().getLocation());
                player.sendMessage(TextFormat.GREEN + "Position 2 set: " + formatLocation(event.getBlock().getLocation()));
                event.setCancelled(true);

                if (pos1Map.containsKey(player) && pos2Map.containsKey(player)) {
                    showRegionBoundaries(player, pos1Map.get(player), pos2Map.get(player));
                }
            }
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (creatingRegions.containsKey(player)) {
            if (event.isSneaking()) {
                if (pos1Map.containsKey(player) && pos2Map.containsKey(player)) {
                    Location pos1 = pos1Map.get(player);
                    Location pos2 = pos2Map.get(player);

                    double volume = Math.abs(pos1.getX() - pos2.getX()) * Math.abs(pos1.getY() - pos2.getY()) * Math.abs(pos1.getZ() - pos2.getZ());
                    if (!player.isOp() && volume > 1000) {
                        player.sendMessage(TextFormat.RED + "Region volume exceeds 1000 blocks!");
                        return;
                    }

                    createRegion(player, creatingRegions.get(player));
                    creatingRegions.remove(player);
                    player.sendMessage(TextFormat.GREEN + "Region created!");
                } else {
                    player.sendMessage(TextFormat.RED + "Please set both positions first!");
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!canBuild(player, event.getBlock().getLocation())) {
            event.setCancelled(true);
            player.sendMessage(TextFormat.RED + "You cannot break blocks in this region!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!canBuild(player, event.getBlock().getLocation())) {
            event.setCancelled(true);
            player.sendMessage(TextFormat.RED + "You cannot place blocks in this region!");
        }
    }

    private void createRegion(Player player, String name) {
        Location pos1 = pos1Map.get(player);
        Location pos2 = pos2Map.get(player);

        regions.put(name, new Region(name, pos1, pos2));
        player.sendMessage(TextFormat.GREEN + "Region created: " + name);

        pos1Map.remove(player);
        pos2Map.remove(player);
    }

    private boolean canBuild(Player player, Location location) {
        if (player.isOp()) return true;

        for (Region region : regions.values()) {
            if (region.contains(location)) {
                return false;
            }
        }
        return true;
    }

    private void showRegionBoundaries(Player player, Location pos1, Location pos2) {
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        for (double x = minX; x <= maxX; x += 0.5) {
            for (double y = minY; y <= maxY; y += 0.5) {
                player.getLevel().addParticle(new DustParticle(new Vector3(x, y, minZ), 255, 255, 255));
                player.getLevel().addParticle(new DustParticle(new Vector3(x, y, maxZ), 255, 255, 255));
            }
        }

        for (double z = minZ; z <= maxZ; z += 0.5) {
            for (double y = minY; y <= maxY; y += 0.5) {
                player.getLevel().addParticle(new DustParticle(new Vector3(minX, y, z), 255, 255, 255));
                player.getLevel().addParticle(new DustParticle(new Vector3(maxX, y, z), 255, 255, 255));
            }
        }

        for (double x = minX; x <= maxX; x += 0.5) {
            for (double z = minZ; z <= maxZ; z += 0.5) {
                player.getLevel().addParticle(new DustParticle(new Vector3(x, minY, z), 255, 255, 255));
                player.getLevel().addParticle(new DustParticle(new Vector3(x, maxY, z), 255, 255, 255));
            }
        }
    }

    private String formatLocation(Location location) {
        return String.format("X: %.1f, Y: %.1f, Z: %.1f", location.getX(), location.getY(), location.getZ());
    }

    private static class Region {
        private final String name;
        private final Location pos1;
        private final Location pos2;

        public Region(String name, Location pos1, Location pos2) {
            this.name = name;
            this.pos1 = pos1;
            this.pos2 = pos2;
        }

        public boolean contains(Location location) {
            double x = location.getX();
            double y = location.getY();
            double z = location.getZ();

            double minX = Math.min(pos1.getX(), pos2.getX());
            double maxX = Math.max(pos1.getX(), pos2.getX());
            double minY = Math.min(pos1.getY(), pos2.getY());
            double maxY = Math.max(pos1.getY(), pos2.getY());
            double minZ = Math.min(pos1.getZ(), pos2.getZ());
            double maxZ = Math.max(pos1.getZ(), pos2.getZ());

            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
    }
}
