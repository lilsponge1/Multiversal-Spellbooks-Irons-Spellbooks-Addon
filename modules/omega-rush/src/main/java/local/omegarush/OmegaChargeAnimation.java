package local.omegarush;
import dev.kosmx.playerAnim.core.data.*;
import dev.kosmx.playerAnim.core.util.Ease;
/** Upright levitating power-up: one raised thigh with a folded knee. */
public final class OmegaChargeAnimation {
    public static KeyframeAnimation create(int chargeTicks){
        var b=new KeyframeAnimation.AnimationBuilder(AnimationFormat.JSON_EMOTECRAFT);
        int intro=Math.max(1,(int)(chargeTicks*0.18));b.beginTick=0;b.endTick=Math.max(20,chargeTicks);b.stopTick=b.endTick+1;b.returnTick=intro;b.isLooped=true;
        b.setName("omega_form_levitation").setAuthor("Sponge");
        key(b.body.pitch,-4,intro,b.endTick);key(b.head.pitch,-3,intro,b.endTick);
        key(b.rightArm.pitch,-10,intro,b.endTick);key(b.leftArm.pitch,-10,intro,b.endTick);
        key(b.rightArm.yaw,-12,intro,b.endTick);key(b.leftArm.yaw,12,intro,b.endTick);
        key(b.rightArm.roll,85,intro,b.endTick);key(b.leftArm.roll,-85,intro,b.endTick);
        key(b.rightArm.bend,135,intro,b.endTick);key(b.leftArm.bend,135,intro,b.endTick);
        key(b.rightArm.bendDirection,90,1,b.endTick);key(b.leftArm.bendDirection,-90,1,b.endTick);
        key(b.leftLeg.pitch,-72,intro,b.endTick);key(b.leftLeg.roll,-7,intro,b.endTick);
        key(b.leftLeg.bend,108,intro,b.endTick);key(b.leftLeg.bendDirection,0,1,b.endTick);
        key(b.rightLeg.pitch,12,intro,b.endTick);key(b.rightLeg.roll,5,intro,b.endTick);
        key(b.rightLeg.bend,12,intro,b.endTick);key(b.rightLeg.bendDirection,0,1,b.endTick);
        return b.build();
    }
    private static void key(KeyframeAnimation.StateCollection.State state,float degrees,int intro,int end){
        state.setEnabled(true);state.addKeyFrame(0,state.defaultValue,Ease.INOUTSINE);
        state.addKeyFrame(intro,(float)Math.toRadians(degrees),Ease.INOUTSINE);state.addKeyFrame(end,(float)Math.toRadians(degrees),Ease.LINEAR);
    }
    private OmegaChargeAnimation(){}
}
