package io.github.bilektugrul.acconverter;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class ChestPage {

    public int id;
    public UUID ownerUUID;
    public List<String> base64Items;
    public ItemStack[] items;

    public ChestPage(int i, ItemStack[] items) {
        this.id = i;
        this.items = items;
    }

    public ChestPage(UUID ownerUUID, int i, ItemStack[] items) {
        this.ownerUUID = ownerUUID;
        this.id = i;
        this.items = items;
    }

    public ChestPage(int i, UUID ownerUUID, List<String> base64Items) {
        this.id = i;
        this.ownerUUID = ownerUUID;
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