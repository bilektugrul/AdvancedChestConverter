package io.github.bilektugrul.acconverter;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public enum ChestAttribute {
    SIZE("inventory.size", (Object)null, Integer.class),
    INVENTORY_NAME("inventory.name", (Object)null, String.class),
    ICON((String)null, (Object)null, (Class)null),
    LOCATION((String)null, (Object)null, (Class)null),
    PAGES((String)null, (Object)null, (Class)null),
    CONFIG_TYPE((String)null, (Object)null, (Class)null),
    WHO_PLACED((String)null, (Object)null, (Class)null),
    DIRECTION((String)null, (Object)null, (Class)null),
    HOLOGRAM_ENABLED((String)null, (Object)null, (Class)null),
    CHEST_TYPE("chest-type", "NORMAL", (Class)null),
    PREVIOUS_PAGE_SLOT("previous-page-slot", 4, Integer.class),
    NEXT_PAGE_SLOT("next-page-slot", 6, Integer.class),
    UPGRADE("upgrades.next_upgrade", (Object)null, String.class),
    UPGRADE_PRICE("upgrades.price", (Object)null, Double.class),
    UPGRADE_AVAILABILITY("upgrades.enable", (Object)null, Boolean.class),
    UPGRADE_SLOT("upgrades.slot", 5, Integer.class),
    HOLOGRAM_TITLE_AVAILABILITY("hologram.enable", Boolean.FALSE, Boolean.class),
    HOLOGRAM_TITLE_CONTENT("hologram.title", new ArrayList(), List.class),
    SORTERS_AVAILABILITY("sorters.enable", Boolean.TRUE, Boolean.class),
    SORTERS_PRICE("sorters.price", 1000.0, Double.class),
    SORTERS_SLOT("sorters.slot", 9, Integer.class),
    SELLS_AVAILABILITY("sells.enable", Boolean.FALSE, Boolean.class),
    SELLS_MULTIPLIER("sells.multiplier", 1.0, Double.class),
    SELLS_SLOT("sells.slot", 1, Integer.class),
    AUTOSELLS_AVAILABILITY("autosells.enable", Boolean.FALSE, Boolean.class),
    AUTOSELLS_FREQUENCY("autosells.frequency", 2400, Integer.class),
    AUTOSELLS_MULTIPLIER("autosells.multiplier", 1.0, Double.class),
    AUTOSELLS_STATUS((String)null, (Object)null, (Class)null),
    AUTOSELLS_SESSION_OWNER((String)null, (Object)null, (Class)null),
    AUTOSELLS_TAX("autosells.tax", 0.0, Double.class),
    AUTOSELLS_SLOT("autosells.slot", 2, Integer.class),
    MONEY((String)null, (Object)null, (Class)null),
    HOPPERS_ALLOWED("allow-hoppers-use", Boolean.TRUE, Boolean.class),
    SHOP_PRICE("shop-price", -1.0, Double.class),
    CRAFTING_AVAILABILITY("crafting.enable", Boolean.FALSE, Boolean.class),
    CRAFTING_RECIPE((String)null, (Object)null, (Class)null),
    SMELTER_AVAILABILITY("smelter.enable", Boolean.FALSE, Boolean.class),
    SMELTER_PRICE("smelter.price", 0.0, Double.class),
    SMELTER_SLOT("smelter.slot", 8, Integer.class),
    COMPRESSOR_AVAILABILITY("compressor.enable", Boolean.FALSE, Boolean.class),
    COMPRESSOR_PRICE("compressor.price", 0.0, Double.class),
    COMPRESSOR_SLOT("compressor.slot", 7, Integer.class),
    DEPOSIT_AVAILABILITY("deposit.enable", Boolean.FALSE, Boolean.class),
    CONTAINER_TYPE("container.type", "CHEST", (Class)null),
    SEARCH_AVAILABILITY("search.enable", Boolean.FALSE, Boolean.class),
    SEARCH_SLOT("search.slot", 3, Integer.class),
    TRANSPORT_AVAILABILITY("transportation.enable", Boolean.FALSE, Boolean.class),
    TRANSPORT_ITEM_NAME("transportation.item.name", "Change this", String.class),
    TRANSPORT_ITEM_LORE("transportation.item.lore", new ArrayList(), List.class),
    SETTINGS_AVAILABILITY("settings.enable", Boolean.FALSE, Boolean.class),
    SETTINGS_SLOT("settings.slot", 1, Integer.class);

    private String a;
    private Object b;
    private Class c;

    private ChestAttribute(String var3, Object var4, Class var5) {
        this.a = var3;
        this.b = var4;
        this.c = var5;
    }

    public final Object getConfigValue(ConfigurationSection var1) {
        boolean var2 = this.b != null;
        if (this.c == String.class) {
            return !var2 ? var1.getString(this.a) : var1.getString(this.a, (String)this.b);
        } else if (this.c == Boolean.class) {
            return !var2 ? var1.getBoolean(this.a) : var1.getBoolean(this.a, (Boolean)this.b);
        } else if (this.c == Integer.class) {
            return !var2 ? var1.getInt(this.a) : var1.getInt(this.a, (Integer)this.b);
        } else if (this.c == Double.class) {
            return !var2 ? var1.getDouble(this.a) : var1.getDouble(this.a, (Double)this.b);
        } else {
            return this.c == List.class ? var1.getStringList(this.a) : null;
        }
    }

    public final String getConfigPath() {
        return this.a;
    }

    public final Object getDefaultConfigValue() {
        return this.b;
    }

    public final Class getConfigValueClass() {
        return this.c;
    }
}
