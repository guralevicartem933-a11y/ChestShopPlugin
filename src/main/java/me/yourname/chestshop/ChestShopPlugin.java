package me.yourname.chestshop;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ChestShopPlugin extends JavaPlugin {

    private File shopsFile;
    private FileConfiguration shopsConfig;
    private InlineShopManager shopManager;

    @Override
    public void onEnable() {
        shopsFile = new File(getDataFolder(), "shops.yml");
        if (!shopsFile.exists()) {
            saveResource("shops.yml", false);
        }
        shopsConfig = YamlConfiguration.loadConfiguration(shopsFile);

        shopManager = new InlineShopManager(this);
        shopManager.loadShops();

        // Реєструємо команду та івенти
        if (getCommand("createshop") != null) {
            getCommand("createshop").setExecutor(new CreateShopCommand(shopManager));
        }
        getServer().getPluginManager().registerEvents(new ShopListener(shopManager), this);

        getLogger().info("ChestShop успішно увімкнено!");
    }

    @Override
    public void onDisable() {
        if (shopManager != null) {
            shopManager.saveShops();
        }
        getLogger().info("ChestShop вимкнено.");
    }

    public FileConfiguration getShopsConfig() { return shopsConfig; }
    public void saveShopsConfig() {
        try {
            shopsConfig.save(shopsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// Перенесений менеджер, щоб уникнути помилок пошуку символів Maven
class InlineShopManager extends ShopManager {
    private final ChestShopPlugin plugin;
    private final Map<String, ShopData> shops = new HashMap<>();

    public InlineShopManager(ChestShopPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public String locToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    @Override
    public void createShop(Player owner, Block block) {
        String key = locToString(block.getLocation());
        ShopData data = new ShopData(owner.getUniqueId());
        shops.put(key, data);
        saveShopToConfig(key, data);
    }

    @Override
    public ShopData getShop(Block block) {
        return shops.get(locToString(block.getLocation()));
    }

    @Override
    public void saveShopToConfig(String key, ShopData data) {
        String path = "shops." + key;
        plugin.getShopsConfig().set(path + ".owner", data.getOwnerId().toString());
        plugin.getShopsConfig().set(path + ".isSetup", data.isSetup());
        plugin.getShopsConfig().set(path + ".product", data.getProduct());
        plugin.getShopsConfig().set(path + ".priceItem", data.getPriceItem());
        plugin.saveShopsConfig();
    }

    @Override
    public void loadShops() {
        ConfigurationSection section = plugin.getShopsConfig().getConfigurationSection("shops");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String ownerStr = section.getString(key + ".owner");
            if (ownerStr == null) continue;
            UUID owner = UUID.fromString(ownerStr);
            ShopData data = new ShopData(owner);
            data.setSetup(section.getBoolean(key + ".isSetup"));
            data.setProduct(section.getItemStack(key + ".product"));
            data.setPriceItem(section.getItemStack(key + ".priceItem"));
            shops.put(key, data);
        }
    }

    @Override
    public void saveShops() {
        for (Map.Entry<String, ShopData> entry : shops.entrySet()) {
            saveShopToConfig(entry.getKey(), entry.getValue());
        }
    }
}
