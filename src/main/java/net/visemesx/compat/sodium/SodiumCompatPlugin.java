package net.visemesx.compat.sodium;

import net.fabricmc.loader.api.*;

import net.visemesx.compat.CompatPlugin;

public class SodiumCompatPlugin extends CompatPlugin {

    @Override
    protected String getCompatModId() {
        return "sodium";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!super.shouldApplyMixin(targetClassName, mixinClassName)) {
            return false;
        }


        boolean hotMixin = mixinClassName.equals("net.visemesx.mixins.sodium.CubeMixinMixin");

        if (hotMixin) {
            return !this.isCurrentVersionOlderThanHot(mixinClassName);
        }


        return true;
    }

    private boolean isCurrentVersionOlderThanHot(String mixinName) {
        FabricLoader fabricLoader = FabricLoader.getInstance();
        ModContainer modContainer = fabricLoader.getModContainer(this.getCompatModId()).orElseThrow();

        Version currentVersion = modContainer.getMetadata().getVersion();
        Version hotVersion = this.getHotSodiumVersion();

        // <6.0.0 (currentOlder == true)
        // ModelPartMixinMixin

        // >=6.0.0 (currentOlder == false)
        // CubeMixin

        boolean bl = currentVersion.compareTo(hotVersion) < 0;

        return bl;
    }

    private Version getHotSodiumVersion() {
        try {
            return Version.parse("0.6.0+mc1.21.1");
        } catch (VersionParsingException e) {
            throw new RuntimeException(e);
        }
    }
}