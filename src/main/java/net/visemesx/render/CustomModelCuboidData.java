package net.visemesx.render;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelCuboidData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class CustomModelCuboidData extends ModelCuboidData {

    public CustomModelCuboidData(@Nullable String name, float textureX, float textureY, float offsetX, float offsetY, float offsetZ, float sizeX, float sizeY, float sizeZ, Dilation extra, boolean mirror, float textureScaleX, float textureScaleY, Set<Direction> directions) {
        super(name, textureX, textureY, offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, extra, mirror, textureScaleX, textureScaleY, directions);
    }

    @Override
    public ModelPart.Cuboid createCuboid(int textureWidth, int textureHeight) {
        // Also use AW to get fields from this class to pass it into your CustomCuboid
        return new CustomCuboid((int)this.textureUV.getX(), (int)this.textureUV.getY(), this.offset.x(), this.offset.y(), this.offset.z(), this.dimensions.x(), this.dimensions.y(), this.dimensions.z(), this.extraSize.radiusX, this.extraSize.radiusY, this.extraSize.radiusZ, this.mirror, (float)textureWidth * this.textureScale.getX(), (float)textureHeight * this.textureScale.getY(), this.directions);
    }
}