package net.baizhi.client.injection.forge;

import net.baizhi.client.injection.transformers.AbstractJavaLinkerHandler;
import net.baizhi.client.injection.transformers.ForgeNetworkTransformer;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.util.Map;

public class TransformerLoader implements IFMLLoadingPlugin {

    public TransformerLoader() {
        makeProcessDpiAware();
        MixinBootstrap.init();
        Mixins.addConfiguration("client.forge.mixins.json");
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
    }

    /**
     * Opt in to per-monitor DPI awareness BEFORE the LWJGL window is created.
     * Without this, Windows DPI-virtualizes the whole game surface on scaled
     * displays (e.g. 110%/125%) and upscales the rendered bitmap, blurring
     * every anti-aliased edge in the ImGui UI.
     * JNA is already on the 1.8.9 classpath (net.java.dev.jna:jna:3.4.0).
     */
    private static void makeProcessDpiAware() {
        try {
            Object user32 = com.sun.jna.Native.loadLibrary("user32", DpiUser32.class);
            ((DpiUser32) user32).SetProcessDPIAware();
        } catch (Throwable ignored) {
        }
    }

    public interface DpiUser32 extends com.sun.jna.Library {
        boolean SetProcessDPIAware();
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{ForgeNetworkTransformer.class.getName(), AbstractJavaLinkerHandler.class.getName()};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
