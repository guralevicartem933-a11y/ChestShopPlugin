package me.yourname.chestshop;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopManager {

    private final ChestShopPlugin plugin;
    private final Map<String, ShopData> shops = new HashMap<>();

    public ShopManager(ChestShopPlugin plugin) {
        this.plugin = plugin;
    }

    public String locToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public void createShop(Player owner, Block block) {
        String key = locToString(block.getLocation());
        ShopData data = new ShopData(owner.getUniqueId());
        shops.put(key, data);
        saveShopToConfig(key, data);
    }

    public ShopData getShop(Block block) {
        return shops.get(locToString(block.getLocation()));
    }

    public void saveShopToConfig(String key, ShopData data) {
        String path = "shops." + key;
        plugin.getShopsConfig().set(path + ".owner", data.getOwnerId().toString());
        plugin.getShopsConfig().set(path + ".isSetup", data.isSetup());
        plugin.getShopsConfig().set(path + ".product", data.getProduct());
        plugin.getShopsConfig().set(path + ".priceItem", data.getPriceItem());
        plugin.saveShopsConfig();
    }

    public void loadShops() {
        ConfigurationSection section = plugin.getShopsConfig().getConfigurationSection("shops");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            UUID owner = UUID.fromString(section.getString(key + ".owner"));
            ShopData data = new ShopData(owner);
            data.setSetup(section.getBoolean(key + ".isSetup"));
            data.setProduct(section.getItemStack(key + ".product"));
            data.setPriceItem(section.getItemStack(key + ".priceItem"));
            shops.put(key, data);
        }
    }

    public void saveShops() {
        for (Map.Entry<String, ShopData> entry : shops.entrySet()) {
            saveShopToConfig(entry.getKey(), entry.getValue());
        }
    }
}
