package io.github.bilektugrul.acconverter;

import me.despical.commons.configuration.ConfigUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.*;
import java.util.Base64;

public class AdvancedChestConverter extends JavaPlugin {

    public String tableName;
    public MySQLDatabase database;
    public boolean convertRunning = false;
    public int converted;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.tableName = getConfig().getString("table-name", "chests");
        this.database = new MySQLDatabase(this);
        if (getServer().getMinecraftVersion().equalsIgnoreCase("1.20.4")) {
            convertAllChestsToBase64();
        } else {
            getServer().getScheduler().runTaskAsynchronously(this, this::convertAllChestsToChestPage);
            getServer().getScheduler().runTaskTimer(this, (runnable) -> {
                if (convertRunning) {
                    getLogger().info("Convert still running...");
                    getLogger().info("Converted player data: " + converted);
                } else {
                    runnable.cancel();
                }
            }, 0, 20);
        }

    }

    public void convertAllChestsToChestPage() {
        getLogger().info("Starting 1.21.1...");
        this.convertRunning = true;

        FileConfiguration file = ConfigUtils.getConfig(this, "converted");

        List<ChestPage> pages = new ArrayList<>();
        Connection connection = database.getConnection();

        for (String uuidKey : file.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidKey);
            ConfigurationSection section = file.getConfigurationSection(uuidKey);
            if (section == null) continue;

            for (String pageId : section.getKeys(false)) {
                List<String> base64Items = file.getStringList(uuidKey + "." + pageId);
                List<ItemStack> convertedItems = new ArrayList<>();
                for (String base64Item : base64Items) {
                    convertedItems.add(deserializeFromBase64(base64Item));
                }

                ChestPage page = new ChestPage(uuid, Integer.parseInt(pageId), convertedItems.toArray(new ItemStack[0]));
                pages.add(page);
            }

            byte[] pageArray = serializePages(pages.toArray(new ChestPage[0]));
            String statementStr = "UPDATE " + tableName + " SET pages=(?)" + " WHERE uuid='" + uuidKey + "';";

            try {
                PreparedStatement pstmt = connection.prepareStatement(statementStr);
                InputStream inputStream = new ByteArrayInputStream(pageArray);
                pstmt.setBinaryStream(1, inputStream);
                pstmt.executeUpdate();
                converted++;
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        getLogger().info("Convert done. Converted data: " + converted);
        convertRunning = false;
    }

    public void convertAllChestsToBase64() {
        getLogger().info("Starting 1.20.4...");

        FileConfiguration file = ConfigUtils.getConfig(this, "converted");
        String var3 = "SELECT * FROM " + tableName;
        Connection connection = database.getConnection();

        try {
            List<ChestPage> base64Pages = new ArrayList<>();
            Statement statement = connection.createStatement();
            ResultSet var6 = statement.executeQuery(var3);
            while (var6.next()) {
                int size = var6.getInt("size");
                UUID ownerUUID = UUID.fromString(var6.getString("uuid"));
                Map<Integer, ChestPage> pages = deserializePages(var6.getBytes("pages"), size);

                for (ChestPage page : pages.values()) {
                    List<String> pageItems = new ArrayList<>();
                    for (ItemStack item : page.getItems()) {
                        if (item == null) continue;

                        String itemBase64 = serializeToBase64(item);
                        pageItems.add(itemBase64);
                    }

                    ChestPage newPage = new ChestPage(page.id, ownerUUID, pageItems);
                    base64Pages.add(newPage);
                }
            }

            for (ChestPage page : base64Pages) {
                if (page.getBase64Items().isEmpty()) continue;

                file.set(page.ownerUUID.toString() + "." + page.id, page.getBase64Items());
            }

            ConfigUtils.saveConfig(this, file, "converted");

        } catch (SQLException var5) {
            var5.printStackTrace();
        }
    }

    public String serializeToBase64(ItemStack item) {
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    public ItemStack deserializeFromBase64(String encoded) {
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
    }

    public byte[] serializePages(ChestPage[] var1) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try {
                BukkitObjectOutputStream bukkitObjectOutputStream = new BukkitObjectOutputStream(outputStream);

                try {
                    HashMap var4 = new HashMap();

                    for (ChestPage page : var1) {
                        HashMap var8 = new HashMap();
                        ItemStack[] pageItems = page.getItems();

                        for (int var10 = 0; var10 < pageItems.length; ++var10) {
                            ItemStack var11;
                            if ((var11 = pageItems[var10]) != null) {
                                var8.put(var10, var11);
                            }
                        }

                        var4.put(page.getID(), var8);
                    }

                    bukkitObjectOutputStream.writeObject(var4);
                    return outputStream.toByteArray();
                } finally {
                    if (Collections.singletonList(bukkitObjectOutputStream).get(0) != null) {
                        bukkitObjectOutputStream.close();
                    }

                }
            } finally {
                if (Collections.singletonList(outputStream).get(0) != null) {
                    outputStream.close();
                }

            }
        } catch (Exception var20) {
            var20.printStackTrace();
            return null;
        }
    }

    public Map<Integer, ChestPage> deserializePages(byte[] var1, int var2) {
        HashMap var3 = new HashMap();
        int var4 = (int) Math.ceil((double) var2 / 45.0);

        try {
            ByteArrayInputStream var25 = new ByteArrayInputStream(var1);

            try {
                BukkitObjectInputStream var5 = new BukkitObjectInputStream(var25);

                try {
                    Map var6;
                    Iterator var7 = (var6 = (Map) var5.readObject()).keySet().iterator();

                    while (var7.hasNext()) {
                        int var8 = (Integer) var7.next();
                        Map var9 = (Map) var6.get(var8);
                        int var10 = 45;
                        if (var8 == var4 - 1 && var2 % 45 != 0) {
                            var10 = var2 - (int) Math.floor((double) var2 / 45.0) * 45;
                        }

                        ItemStack[] var11 = new ItemStack[var10];

                        for (int var12 = 0; var12 < var10; ++var12) {
                            var11[var12] = (ItemStack) var9.get(var12);
                        }

                        var3.put(var8, new ChestPage(var8, var11));
                    }
                } catch (Exception var21) {
                } finally {
                    if (Collections.singletonList(var5).get(0) != null) {
                        var5.close();
                    }

                }
            } finally {
                if (Collections.singletonList(var25).get(0) != null) {
                    var25.close();
                }

            }
        } catch (Exception var24) {
            var24.printStackTrace();
        }

        return var3;
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }

}