package me.yourname.chestshop;

import org.bukkit.inventory.ItemStack;
import java.util.UUID;

public class ShopData {
    private final UUID ownerId;
    private boolean isSetup = false;
    private ItemStack product = null;
    private ItemStack priceItem = null;

    public ShopData(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public UUID getOwnerId() { return ownerId; }
    public boolean isSetup() { return isSetup; }
    public void setSetup(boolean setup) { isSetup = setup; }
    public ItemStack getProduct() { return product; }
    public void setProduct(ItemStack product) { this.product = product; }
    public ItemStack getPriceItem() { return priceItem; }
    public void setPriceItem(ItemStack priceItem) { this.priceItem = priceItem; }
}
