package local.grandexplosiontest;

import local.ironsultimateexplosion.*;

/** Real native buffer round trips; excluded from all deliverables. */
public final class SaiyanPacketCheck {
    public static int run() throws Exception {
        var stateEncode=SaiyanStatePacket.class.getDeclaredMethod("encode",SaiyanStatePacket.class,net.minecraft.network.FriendlyByteBuf.class);stateEncode.setAccessible(true);
        var stateDecode=SaiyanStatePacket.class.getDeclaredMethod("decode",net.minecraft.network.FriendlyByteBuf.class);stateDecode.setAccessible(true);
        var combatEncode=SaiyanCombatPacket.class.getDeclaredMethod("encode",SaiyanCombatPacket.class,net.minecraft.network.FriendlyByteBuf.class);combatEncode.setAccessible(true);
        var combatDecode=SaiyanCombatPacket.class.getDeclaredMethod("decode",net.minecraft.network.FriendlyByteBuf.class);combatDecode.setAccessible(true);
        int count=0;
        for(int form=0;form<4;form++) for(int target=0;target<4;target++) {
            var packet=new SaiyanStatePacket(java.util.UUID.randomUUID(),42,"minecraft:overworld",123,456,form,target,299,300,-1,60,SaiyanStatePacket.BURST,12,1,.35f);
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                stateEncode.invoke(null,packet,buffer);if(buffer.readableBytes()>128)throw new AssertionError("State packet grew unexpectedly");
                if(!packet.equals(stateDecode.invoke(null,buffer)) || buffer.isReadable())throw new AssertionError("State round trip");
            } finally {buffer.release();}
            System.out.println("SAIYAN_PASS state_packet_round_trip_"+form+"_"+target);count++;
        }
        for(byte kind=0;kind<4;kind++) {
            var packet=new SaiyanCombatPacket(java.util.UUID.randomUUID(),"minecraft:overworld",new net.minecraft.world.phys.Vec3(12.5,80,-9),kind);
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                combatEncode.invoke(null,packet,buffer);if(buffer.readableBytes()>80)throw new AssertionError("Combat packet grew unexpectedly");
                if(!packet.equals(combatDecode.invoke(null,buffer)) || buffer.isReadable())throw new AssertionError("Combat round trip");
            } finally {buffer.release();}
            System.out.println("SAIYAN_PASS combat_packet_round_trip_"+kind);count++;
        }
        for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1,0,1.49}) {
            if(SaiyanRules.drainThird(bad,1.5).form()!=SaiyanRules.Form.BASE)throw new AssertionError("Invalid or insufficient SSJ3 mana");
            System.out.println("SAIYAN_PASS third_mana_guard_"+bad);count++;
        }
        if(SaiyanRules.release(SaiyanRules.Form.SUPER_SAIYAN_3,5,20,60)!=SaiyanRules.Release.CANCEL)throw new AssertionError("Fourth form allowed");
        System.out.println("SAIYAN_PASS third_form_has_no_fourth_ascension");return count+1;
    }
    private SaiyanPacketCheck() {}
}
