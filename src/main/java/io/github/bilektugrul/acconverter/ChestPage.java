package io.github.bilektugrul.acconverter;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class ChestPage<I> {

    public int id;
    public UUID chestUUID;
    public List<String> base64Items;
    public ItemStack[] items;

    public ChestPage(int id, ItemStack[] items) {
        this.id = id;
        this.items = items;
    }

    public ChestPage(UUID chestUIID, int id, ItemStack[] items) {
        this.chestUUID = chestUIID;
        this.id = id;
        this.items = items;
    }

    public ChestPage(int id, UUID chestUUID, List<String> base64Items) {
        this.id = id;
        this.chestUUID = chestUUID;
        this.base64Items = base64Items;
    }

    public int getID() {
        return id;
    }

    public ItemStack[] getItems() {
        return items;
    }

    public List<String> getBase64Items() {
        return base64Items;
    }

}