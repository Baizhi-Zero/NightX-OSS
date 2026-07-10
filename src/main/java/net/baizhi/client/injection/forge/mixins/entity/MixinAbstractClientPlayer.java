package net.baizhi.client.injection.forge.mixins.entity;

import net.baizhi.client.Launch;
import net.baizhi.client.features.module.impl.visual.Cape;
import net.baizhi.client.features.module.impl.visual.Interface;
import net.baizhi.client.utils.MinecraftInstance;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer extends MixinEntityPlayer {

    @Inject(method = "getLocationCape", at = @At("HEAD"), cancellable = true)
    private void getCape(CallbackInfoReturnable<ResourceLocation> callbackInfoReturnable) {
        final Cape cape = Objects.requireNonNull(Launch.moduleManager.getModule(Cape.class));
        if (cape.getCustomCape().get() && getGameProfile().getName().equalsIgnoreCase(MinecraftInstance.mc.thePlayer.getGameProfile().getName()))
            callbackInfoReturnable.setReturnValue(cape.getCapeLocation(cape.getStyleValue().get()));
    }

    @Inject(method = "getFovModifier", at = @At("HEAD"), cancellable = true)
    private void getFovModifier(CallbackInfoReturnable<Float> callbackInfoReturnable) {
        final Interface anInterface = Objects.requireNonNull(Launch.moduleManager.getModule(Interface.class));
        float newFov = anInterface.getCustomFovModifier().getValue();
        newFov *= 1.0f;
        if (anInterface.getCustomFov().get() && anInterface.getState()) {
            callbackInfoReturnable.setReturnValue(newFov);
        }
    }
}
