package io.github.bilektugrul.acconverter;

import me.despical.commons.configuration.ConfigUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import us.lynuxcraft.deadsilenceiv.advancedchests.chest.gui.page.NormalPage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
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

        if (!getConfig().getBoolean("enabled")) {
            return;
        }

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
        this.convertRunning = true;

        getLogger().info("Starting 1.21.1...");
        FileConfiguration file = ConfigUtils.getConfig(this, "converted");
        Connection connection = database.getConnection();

        for (String uuidKey : file.getKeys(false)) {
            ConfigurationSection section = file.getConfigurationSection(uuidKey);
            if (section == null) continue;

            UUID chestUUID = UUID.fromString(uuidKey);
            List<ChestPage<?>> pages = new ArrayList<>();

            for (String pageId : section.getKeys(false)) {
                List<String> base64Items = file.getStringList(uuidKey + "." + pageId);
                List<ItemStack> convertedItems = new ArrayList<>();
                for (String base64Item : base64Items) {
                    if (base64Item.equalsIgnoreCase("ZW1wdHkgcGFnZSBicm8gc2tpcCBpdA==")) {
                        pages.add(new ChestPage<>(chestUUID, Integer.parseInt(pageId), new ItemStack[0]));
                    } else {
                        ItemStack converted = deserializeFromBase64(base64Item);
                        if (converted == null) {
                            getLogger().warning("null item at " + uuidKey + " page " + pageId);
                            return;
                        }

                        convertedItems.add(converted);
                    }

                }

                ChestPage<?> page = new ChestPage<>(chestUUID, Integer.parseInt(pageId), convertedItems.toArray(new ItemStack[0]));
                pages.add(page);
            }

            for (ChestPage<?> page : pages) {
                if (page == null) {
                    getLogger().warning("null page at " + uuidKey);
                    return;
                }
            }

            byte[] pageArray = serializePages(pages.toArray(new ChestPage[0]));
            if (pageArray == null) {
                getLogger().warning(uuidKey + " pages are null");
                return;
            }

            getLogger().info("UUID: " + uuidKey + " - SIZE: " + pages.size());
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
            List<ChestPage<?>> base64Pages = new ArrayList<>();
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery(queryStr);
            while (result.next()) {
                int size = result.getInt("size");
                UUID chestUUID = UUID.fromString(result.getString("uuid"));
                Map<Integer, ChestPage<?>> pages = deserializePages(result.getBytes("pages"), size);
                if (pages.isEmpty()) continue;

                Iterator<ChestPage<?>> iterator = pages.values().iterator();
                while (iterator.hasNext()) {
                    ChestPage<?> page = iterator.next();
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

                    if (pageItems.isEmpty()) {
                        base64Pages.add(new ChestPage<>(page.id, chestUUID, Collections.singletonList(strSerializeToBase64("empty page bro skip it"))));
                    } else {
                        ChestPage<?> newPage = new ChestPage<>(page.id, chestUUID, pageItems);
                        base64Pages.add(newPage);
                    }
                }
            }

            for (ChestPage<?> page : base64Pages) {
                if (page.getBase64Items().isEmpty()) continue;

                file.set(page.chestUUID.toString() + "." + page.id, page.getBase64Items());
            }

            ConfigUtils.saveConfig(this, file,"converted");

        } catch (SQLException var5) {
            var5.printStackTrace();
        }

        getLogger().info("1.20.4 convert done.");
    }

    public String strSerializeToBase64(String str) {
        return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
    }

    public String strDeserializeFromBase64(String encoded) {
        return Arrays.toString(Base64.getDecoder().decode(encoded));
    }

    public String serializeToBase64(ItemStack item) {
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    public ItemStack deserializeFromBase64(String encoded) {
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
    }

    public byte[] serializePages(ChestPage<?>[] pages) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream bukkitOutputStream = new BukkitObjectOutputStream(outputStream);
            Map<Integer, Map<Integer, ItemStack>> items = new HashMap<>();

            for (ChestPage<?> page : pages) {
                Map<Integer, ItemStack> content = new HashMap<>();
                ItemStack[] contentArray = page.getItems();
                for (int i = 0; i < contentArray.length; i++){
                    ItemStack item = contentArray[i];
                    if (item != null) {
                        content.put(i, item);
                    }
                }
                items.put(page.getID(), content);
            }

            bukkitOutputStream.writeObject(items);
            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Map<Integer,ChestPage<?>> deserializePages(byte[] serializedPages, int totalChestSize) {
        Map<Integer,ChestPage<?>> pages = new HashMap<>();
        int totalPages = (int) Math.ceil((double) totalChestSize / (double) 45);

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(serializedPages);
            BukkitObjectInputStream bukkitInputStream = new BukkitObjectInputStream(inputStream);
            try {
                Map<Integer, Map<Integer, ItemStack>> items = (Map<Integer, Map<Integer, ItemStack>>) bukkitInputStream.readObject();
                for (int index : items.keySet()) {
                    Map<Integer, ItemStack> content = items.get(index);
                    int amountOfItems = 45;

                    if (index == totalPages - 1 && totalChestSize % 45 != 0){
                        amountOfItems = totalChestSize-(((int) Math.floor((double) totalChestSize / (double) 45)) *45);
                    }

                    ItemStack[] contentArray = new ItemStack[amountOfItems];
                    for (int i = 0; i < amountOfItems; i++) {
                        contentArray[i] = content.get(i);
                    }

                    pages.put(index, new ChestPage<>(index, contentArray));
                }
            } catch (Exception ignored) {}
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pages;
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }

}