package local.kischooltest.mixin;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.DisplayData;
import java.util.OptionalInt;
import java.util.function.IntSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Test-only hidden render context; no desktop input or screen capture. */
@Mixin(value=Window.class,priority=2000)
abstract class HiddenWindow {
    @ModifyVariable(method="<init>",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static DisplayData size(DisplayData original) {
        return Boolean.getBoolean("ki.review")?new DisplayData(1600,900,OptionalInt.empty(),OptionalInt.empty(),false):original;
    }
    @ModifyArg(method="<init>",at=@At(value="INVOKE",target="Lnet/minecraftforge/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J"),index=0,remap=false)
    private IntSupplier hidden(IntSupplier width) {
        return () -> {
            if(Boolean.getBoolean("ki.review")) {
                try {
                    var glfw=Class.forName("org.lwjgl.glfw.GLFW");
                    var hint=glfw.getMethod("glfwWindowHint",int.class,int.class);
                    hint.invoke(null,0x20004,0);hint.invoke(null,0x20001,0);
                    System.out.println("KI_HIDDEN_WINDOW_REQUESTED");
                } catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            }
            return width.getAsInt();
        };
    }
}
