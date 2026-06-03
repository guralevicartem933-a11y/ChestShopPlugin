package me.yourname.chestshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ShopListener implements Listener {

    private final ShopManager shopManager;
    private final Map<UUID, ShopData> settingUpShops = new HashMap<>();
    private final Map<UUID, PurchaseSession> pendingPurchases = new HashMap<>();

    public ShopListener(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        ShopData shop = shopManager.getShop(block);
        if (shop == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        boolean isOwner = shop.getOwnerId().equals(player.getUniqueId());

        if (isOwner && !shop.isSetup()) {
            openSetupGUI(player, shop);
        } else if (shop.isSetup()) {
            openCustomerGUI(player, shop, isOwner);
        } else {
            player.sendMessage(Component.text("Цей магазин ще не налаштований власником!", NamedTextColor.RED));
        }
    }

    private void openSetupGUI(Player player, ShopData shop) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("Налаштування магазину"));
        ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            placeholder.setItemMeta(meta);
        }
        
        for (int i = 0; i < 27; i++) gui.setItem(i, placeholder);

        gui.setItem(11, null);
        gui.setItem(15, null);
        
        ItemStack confirmButton = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta = confirmButton.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.displayName(Component.text("ЗБЕРЕГТИ НАЛАШТУВАННЯ", NamedTextColor.GREEN));
            confirmButton.setItemMeta(confirmMeta);
        }
        gui.setItem(22, confirmButton);

        settingUpShops.put(player.getUniqueId(), shop);
        player.openInventory(gui);
    }

    private void openCustomerGUI(Player player, ShopData shop, boolean isOwner) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("Магазин"));
        ItemStack product = shop.getProduct().clone();
        ItemMeta productMeta = product.getItemMeta();
        
        if (productMeta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("----------------", NamedTextColor.GRAY));
            lore.add(Component.text("Ціна: ", NamedTextColor.YELLOW)
                    .append(Component.text(shop.getPriceItem().getAmount() + "x ", NamedTextColor.WHITE))
                    .append(Component.text(shop.getPriceItem().getType().name(), NamedTextColor.AQUA)));
            
            if (isOwner) {
                lore.add(Component.text("ПКМ: Змінити ціну/товар", NamedTextColor.GOLD));
            } else {
                lore.add(Component.text("Клікніть, щоб КУПИТИ", NamedTextColor.GREEN));
            }
            
            productMeta.lore(lore);
            product.setItemMeta(productMeta);
        }
        
        gui.setItem(13, product);
        settingUpShops.put(player.getUniqueId(), shop);
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ShopData shop = settingUpShops.get(player.getUniqueId());
        if (shop == null) return;

        if (event.getView().title().equals(Component.text("Налаштування магазину"))) {
            int slot = event.getRawSlot();
            if (slot == 22) {
                event.setCancelled(true);
                ItemStack product = event.getInventory().getItem(11);
                ItemStack price = event.getInventory().getItem(15);
                
                if (product == null || price == null) {
                    player.sendMessage(Component.text("Ви повинні покласти і товар, і ціну!", NamedTextColor.RED));
                    return;
                }
                
                shop.setProduct(product.clone());
                shop.setPriceItem(price.clone());
                shop.setSetup(true);
                shopManager.saveShopToConfig(shopManager.locToString(player.getLocation()), shop);
                
                player.closeInventory();
                player.sendMessage(Component.text("Магазин успішно налаштовано!", NamedTextColor.GREEN));
            } else if (slot != 11 && slot != 15 && slot < 27) {
                event.setCancelled(true);
            }
        } else if (event.getView().title().equals(Component.text("Магазин"))) {
            event.setCancelled(true);
            if (event.getRawSlot() != 13) return;

            boolean isOwner = shop.getOwnerId().equals(player.getUniqueId());

            if (isOwner && event.isRightClick()) {
                player.closeInventory();
                openSetupGUI(player, shop);
                return;
            }

            if (isOwner) {
                player.sendMessage(Component.text("Ви не можете купувати у самого себе!", NamedTextColor.RED));
                return;
            }

            player.closeInventory();
            pendingPurchases.put(player.getUniqueId(), new PurchaseSession(shop));
            
            Component message = Component.text("Ви впевнені, що хочете купити ", NamedTextColor.YELLOW)
                    .append(Component.text(shop.getProduct().getAmount() + "x " + shop.getProduct().getType().name(), NamedTextColor.AQUA))
                    .append(Component.text(" за ", NamedTextColor.YELLOW))
                    .append(Component.text(shop.getPriceItem().getAmount() + "x " + shop.getPriceItem().getType().name(), NamedTextColor.AQUA))
                    .append(Component.text("? Напишіть у чат ", NamedTextColor.YELLOW))
                    .append(Component.text("так", NamedTextColor.GREEN))
                    .append(Component.text(" або ", NamedTextColor.YELLOW))
                    .append(Component.text("ні", NamedTextColor.RED));

            player.sendMessage(message);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        settingUpShops.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        PurchaseSession session = pendingPurchases.get(player.getUniqueId());
        if (session == null) return;

        event.setCancelled(true);
        
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).toLowerCase().trim();

        if (text.contains("так")) {
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("ChestShop"), () -> {
                executeTransaction(player, session.getShop());
                pendingPurchases.remove(player.getUniqueId());
            });
        } else if (text.contains("ні")) {
            player.sendMessage(Component.text("Покупку скасовано.", NamedTextColor.RED));
            pendingPurchases.remove(player.getUniqueId());
        } else {
            player.sendMessage(Component.text("Будь ласка, напишіть 'так' або 'ні'.", NamedTextColor.GOLD));
        }
    }

    private void executeTransaction(Player buyer, ShopData shop) {
        Inventory inv = buyer.getInventory();
        ItemStack price = shop.getPriceItem();
        ItemStack product = shop.getProduct();

        if (!inv.containsAtLeast(new ItemStack(price.getType()), price.getAmount())) {
            buyer.sendMessage(Component.text("У вас недостатньо предметів для оплати!", NamedTextColor.RED));
            return;
        }

        inv.removeItem(price);
        Map<Integer, ItemStack> leftOver = inv.addItem(product);
        if (!leftOver.isEmpty()) {
            for (ItemStack drop : leftOver.values()) {
                buyer.getWorld().dropItemNaturally(buyer.getLocation(), drop);
            }
            buyer.sendMessage(Component.text("Частина предметів не помістилася та випала на землю!", NamedTextColor.GOLD));
        }

        buyer.sendMessage(Component.text("Успішна покупка!", NamedTextColor.GREEN));
        
        Player owner = Bukkit.getPlayer(shop.getOwnerId());
        if (owner != null && owner.isOnline()) {
            owner.sendMessage(Component.text("Гравець " + buyer.getName() + " купив ваш товар!", NamedTextColor.GREEN));
        }
    }

    private static class PurchaseSession {
        private final ShopData shop;
        public PurchaseSession(ShopData shop) { this.shop = shop; }
        public ShopData getShop() { return shop; }
    }
}
