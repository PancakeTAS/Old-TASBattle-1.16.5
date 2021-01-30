package work.mgnet.tasbattle;

import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.*;

public class InventoryManaging {

    public static void saveInventory(String name, ServerPlayerEntity p) throws IOException {
        File kitFile = new File("kits/" + name);
        if (!kitFile.exists()) kitFile.createNewFile();
        PrintWriter writer = new PrintWriter(kitFile);
        writer.println(""); // Empty File
        writer.close();
        DataOutputStream stream = new DataOutputStream(new FileOutputStream(kitFile));
        for (int i = 0; i < p.inventory.size(); i++) {
            ItemStack stack = p.inventory.getStack(i);
            if (stack == null) stack = new ItemStack(Blocks.BARRIER);
            NbtIo.write(stack.toTag(new CompoundTag()), stream);
        }
        NbtIo.write(p.inventory.armor.get(0).toTag(new CompoundTag()), stream);
        NbtIo.write(p.inventory.armor.get(1).toTag(new CompoundTag()), stream);
        NbtIo.write(p.inventory.armor.get(2).toTag(new CompoundTag()), stream);
        NbtIo.write(p.inventory.armor.get(3).toTag(new CompoundTag()), stream);
        NbtIo.write(p.inventory.offHand.get(0).toTag(new CompoundTag()), stream);
        stream.close();
    }

    public static void loadInventory(String name, ServerPlayerEntity p) throws IOException {
        File kitFile = new File("kits/" + name);
        DataInputStream reader = new DataInputStream(new FileInputStream(kitFile));
        p.inventory.clear();
        for (int i = 0; i < p.inventory.size(); i++) {
            CompoundTag tag = NbtIo.read(reader);
            ItemStack stack = ItemStack.fromTag(tag);
            p.inventory.setStack(i, stack);
        }
        reader.close();
    }

}
