package work.mgnet.tasbattle.packets;

import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.PacketListener;

import java.io.IOException;

public class ConfigurationPacket implements Packet {

    public String dimension;
    public boolean showTotems; // Needs to be
    public boolean showTrajectories; // Implemented

    @Override
    public void read(PacketByteBuf buf) throws IOException {

    }

    @Override
    public void write(PacketByteBuf buf) throws IOException {

    }

    @Override
    public void apply(PacketListener listener) {

    }

}
