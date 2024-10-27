package io.github.bilektugrul.acconverter;

import org.bukkit.configuration.ConfigurationSection;
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
    public MysqlDatabase database;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.tableName = getConfig().getString("table", "chest");
        this.database = new MysqlDatabase(getLogger());
        if (getServer().getMinecraftVersion().equalsIgnoreCase("1.20.4")) {
            convertAllChestsToBase64();
        } else {
            convertAllChestsToChestPage();
        }

    }

    public void convertAllChestsToChestPage() {
        System.out.println("1.21.1 starting...");
        List<ChestPage> pages = new ArrayList<>();

        for (String uuidKey : getConfig().getKeys(false)) {
            UUID uuid = UUID.fromString(uuidKey);
            ConfigurationSection section = getConfig().getConfigurationSection(uuidKey);
            if (section == null) continue;

            for (String pageId : section.getKeys(false)) {
                List<String> base64Items = getConfig().getStringList(uuidKey + "." + pageId);
                List<ItemStack> convertedItems = new ArrayList<>();
                for (String base64Item : base64Items) {
                    convertedItems.add(deserializeFromBase64(base64Item));
                }

                ChestPage page = new ChestPage(uuid, Integer.parseInt(pageId), convertedItems.toArray(new ItemStack[0]));
                pages.add(page);
                getServer().createPlayerProfile()
            }

            String var3 = "SELECT (uuid,item) FROM chests";

            //serializePages(pages.toArray(new ChestPage[0]))

            byte[] pageArray = serializePages(pages.toArray(new ChestPage[0]));
            String statementStr = "UPDATE chests SET pages=(?)" + " WHERE uuid='" + uuidKey + "';";
            Connection connection = database.getConnection();
            try {
                PreparedStatement pstmt = connection.prepareStatement(statementStr);
                InputStream inputStream = new ByteArrayInputStream(pageArray);
                pstmt.setBinaryStream(1, inputStream);
                pstmt.executeUpdate();
            } catch (Exception exception) {
                exception.printStackTrace();
            }

        }

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

    public void convertAllChestsToBase64() {
        System.out.println("starting");

        String var3 = "SELECT * FROM chests";
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

                getConfig().set(page.ownerUUID.toString() + "." + page.id, page.getBase64Items());
            }

            saveConfig();

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

    /*int var7 = var6.getInt("size");
            var2.put(ChestAttribute.LOCATION, var6.getString("location"));
            var2.put(ChestAttribute.SIZE, var7);
            ChestType var8 = this.a.getType(var6.getString("chest_type"));
            var2.put(ChestAttribute.PAGES, var8.getMysqlService().deserializePages(var6.getBytes("pages"), var7));
            var2.put(ChestAttribute.CONFIG_TYPE, var6.getString("config_type"));
            var2.put(ChestAttribute.CHEST_TYPE, var8);
            var2.put(ChestAttribute.AUTOSELLS_STATUS, var6.getBoolean("autosells_status"));
            var2.put(ChestAttribute.AUTOSELLS_SESSION_OWNER, var6.getString("autosells_session_owner"));
            var2.put(ChestAttribute.MONEY, var6.getDouble("money"));
            var2.put(ChestAttribute.WHO_PLACED, var6.getString("placed_by"));
            var2.put(ChestAttribute.CONTAINER_TYPE, var6.getString("container_type"));
            var2.put(ChestAttribute.DIRECTION, var6.getString("direction"));
            var2.put(ChestAttribute.HOLOGRAM_ENABLED, var6.getBoolean("hologram_enabled"));*/

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

    /*public byte[] serializePages(ChestPage<?>[] var1) {
        try {
            ByteArrayOutputStream var2 = new ByteArrayOutputStream();

            try {
                BukkitObjectOutputStream var3 = new BukkitObjectOutputStream(var2);

                try {
                    HashMap var4 = new HashMap();
                    int var5 = ((Object[])(var1 = var1)).length;

                    for(int var6 = 0; var6 < var5; ++var6) {
                        Object var7 = ((Object[])var1)[var6];
                        HashMap var8 = new HashMap();
                        ItemStack[] var9 = ((NormalPage)var7).getItems();

                        for(int var10 = 0; var10 < var9.length; ++var10) {
                            ItemStack var11;
                            if ((var11 = var9[var10]) != null) {
                                var8.put(var10, var11);
                            }
                        }

                        var4.put(((ChestPage)var7).getId(), var8);
                    }

                    var3.writeObject(var4);
                    return var2.toByteArray();
                } finally {
                    if (Collections.singletonList(var3).get(0) != null) {
                        var3.close();
                    }

                }
            } finally {
                if (Collections.singletonList(var2).get(0) != null) {
                    var2.close();
                }

            }
        } catch (Exception var20) {
            var20.printStackTrace();
            return null;
        }
    }*/
