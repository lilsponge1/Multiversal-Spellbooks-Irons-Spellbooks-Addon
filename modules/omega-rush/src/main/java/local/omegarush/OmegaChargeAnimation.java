package local.omegarush;
import dev.kosmx.playerAnim.core.data.*;
import dev.kosmx.playerAnim.core.util.Ease;
/** A double-biceps power-up, with bent elbows rather than crossed hands. */
public final class OmegaChargeAnimation {
    public static KeyframeAnimation create(int chargeTicks){
        var b=new KeyframeAnimation.AnimationBuilder(AnimationFormat.JSON_EMOTECRAFT);
        int intro=Math.max(1,Math.min(8,chargeTicks/3));b.beginTick=0;b.endTick=Math.max(20,chargeTicks);b.stopTick=b.endTick+1;b.returnTick=intro;b.isLooped=true;
        b.setName("omega_form_flex").setAuthor("Sponge");
        key(b.body.pitch,-4,intro,b.endTick);key(b.head.pitch,-3,intro,b.endTick);
        key(b.rightArm.pitch,-10,intro,b.endTick);key(b.leftArm.pitch,-10,intro,b.endTick);
        key(b.rightArm.yaw,-12,intro,b.endTick);key(b.leftArm.yaw,12,intro,b.endTick);
        key(b.rightArm.roll,85,intro,b.endTick);key(b.leftArm.roll,-85,intro,b.endTick);
        key(b.rightArm.bend,135,intro,b.endTick);key(b.leftArm.bend,135,intro,b.endTick);
        key(b.rightArm.bendDirection,90,1,b.endTick);key(b.leftArm.bendDirection,-90,1,b.endTick);
        return b.build();
    }
    private static void key(KeyframeAnimation.StateCollection.State state,float degrees,int intro,int end){
        state.setEnabled(true);state.addKeyFrame(0,state.defaultValue,Ease.INOUTSINE);
        state.addKeyFrame(intro,(float)Math.toRadians(degrees),Ease.INOUTSINE);state.addKeyFrame(end,(float)Math.toRadians(degrees),Ease.LINEAR);
    }
    private OmegaChargeAnimation(){}
}
