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
            UUID chestUUID = UUID.fromString(uuidKey);
            ConfigurationSection section = file.getConfigurationSection(uuidKey);
            if (section == null) continue;

            for (String pageId : section.getKeys(false)) {
                List<String> base64Items = file.getStringList(uuidKey + "." + pageId);
                List<ItemStack> convertedItems = new ArrayList<>();
                for (String base64Item : base64Items) {
                    convertedItems.add(deserializeFromBase64(base64Item));
                }

                ChestPage page = new ChestPage(chestUUID, Integer.parseInt(pageId), convertedItems.toArray(new ItemStack[0]));
                pages.add(page);
            }

            byte[] pageArray = serializePages(pages.toArray(new ChestPage[0]));
            if (pageArray == null) {
                getLogger().warning(uuidKey + " pages are null");
                continue;
            }

            String statementStr = "UPDATE " + tableName + " SET pages=(?)" + " WHERE uuid='" + uuidKey + "';";

            try {
                PreparedStatement pstmt = connection.prepareStatement(statementStr);
                pstmt.setObject(1, pageArray);
                pstmt.executeUpdate();
                converted++;
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        getLogger().info("1.21 Convert done. Converted data: " + converted);
        convertRunning = false;
    }

    public void convertAllChestsToBase64() {
        getLogger().info("Starting 1.20.4...");

        FileConfiguration file = ConfigUtils.getConfig(this, "converted");
        String queryStr = "SELECT * FROM " + tableName;
        Connection connection = database.getConnection();

        try {
            List<ChestPage> base64Pages = new ArrayList<>();
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery(queryStr);
            while (result.next()) {
                int size = result.getInt("size");
                UUID chestUUID = UUID.fromString(result.getString("uuid"));
                Map<Integer, ChestPage> pages = deserializePages(result.getBytes("pages"), size);
                if (pages.isEmpty()) continue;

                Iterator<ChestPage> iterator = pages.values().iterator();
                while (iterator.hasNext()) {
                    ChestPage page = iterator.next();
                    if (page == null) {
                        iterator.remove();
                        continue;
                    }

                    List<String> pageItems = new ArrayList<>();
                    for (ItemStack item : page.getItems()) {
                        if (item == null) continue;

                        String itemBase64 = serializeToBase64(item);
                        pageItems.add(itemBase64);
                    }

                    ChestPage newPage = new ChestPage(page.id, chestUUID, pageItems);
                    base64Pages.add(newPage);
                }
            }

            for (ChestPage page : base64Pages) {
                if (page.getBase64Items().isEmpty()) continue;

                file.set(page.chestUUID.toString() + "." + page.id, page.getBase64Items());
            }

            ConfigUtils.saveConfig(this, file,"converted");

        } catch (SQLException var5) {
            var5.printStackTrace();
        }

        getLogger().info("1.20.4 convert done.");
    }

    public String serializeToBase64(ItemStack item) {
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    public ItemStack deserializeFromBase64(String encoded) {
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
    }

    public byte[] serializePages(ChestPage[] pages) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try {
                BukkitObjectOutputStream bukkitObjectOutputStream = new BukkitObjectOutputStream(outputStream);

                try {
                    HashMap var4 = new HashMap();

                    for (ChestPage page : pages) {
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
                    } else {
                        getLogger().warning("bukkitObjectOutputStream did not close.");
                    }

                }
            } finally {
                if (Collections.singletonList(outputStream).get(0) != null) {
                    outputStream.close();
                } else {
                    getLogger().warning("outputStream did not close.");
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

                    for (Object o : (var6 = (Map) var5.readObject()).keySet()) {
                        int id = (Integer) o;
                        Map var9 = (Map) var6.get(id);
                        int var10 = 45;
                        if (id == var4 - 1 && var2 % 45 != 0) {
                            var10 = var2 - (int) Math.floor((double) var2 / 45.0) * 45;
                        }

                        ItemStack[] items = new ItemStack[var10];

                        for (int var12 = 0; var12 < var10; ++var12) {
                            items[var12] = (ItemStack) var9.get(var12);
                        }

                        var3.put(id, new ChestPage(id, items));
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                } finally {
                    if (Collections.singletonList(var5).get(0) != null) {
                        var5.close();
                    } else {
                        getLogger().warning("var5 did not close.");
                    }

                }
            } finally {
                if (Collections.singletonList(var25).get(0) != null) {
                    var25.close();
                } else {
                    getLogger().warning("var25 did not close.");
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