package me.yourname.chestshop;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;

public final class ChestShopPlugin extends JavaPlugin {

    private File shopsFile;
    private FileConfiguration shopsConfig;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        shopsFile = new File(getDataFolder(), "shops.yml");
        if (!shopsFile.exists()) {
            saveResource("shops.yml", false);
        }
        shopsConfig = YamlConfiguration.loadConfiguration(shopsFile);

        shopManager = new ShopManager(this);
        shopManager.loadShops();

        getCommand("createshop").setExecutor(new CreateShopCommand(shopManager));
        getServer().getPluginManager().registerEvents(new ShopListener(shopManager), this);

        getLogger().info("ChestShop успішно увімкнено!");
    }

    @Override
    public void onDisable() {
        shopManager.saveShops();
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
