package net.visemesx.render;

import net.minecraft.client.model.ModelPart;
import net.minecraft.util.math.Direction;

import java.util.Set;

public class CustomCuboid extends ModelPart.Cuboid {




    public CustomCuboid(int u, int v, float x, float y, float z, float sizeX, float sizeY, float sizeZ, float extraX, float extraY, float extraZ, boolean mirror, float textureWidth, float textureHeight, Set<Direction> set) {
        super(u, v, x, y, z, sizeX, sizeY, sizeZ, extraX, extraY, extraZ, mirror, textureWidth, textureHeight, Set.of());
        float f = x + sizeX;
        float g = y + sizeY;
        float h = z + sizeZ;
        ModelPart.Vertex vertex = new ModelPart.Vertex(x, y, z, 0.0F, 0.0F);
        ModelPart.Vertex vertex2 = new ModelPart.Vertex(f, y, z, 0.0F, 8.0F);
        ModelPart.Vertex vertex3 = new ModelPart.Vertex(f, g, z, 8.0f, 8.0f);
        ModelPart.Vertex vertex4 = new ModelPart.Vertex(x, g, z, 8.0f, 0.0f);
        ModelPart.Vertex vertex5 = new ModelPart.Vertex(x, y, h, 0.0F, 0.0F);
        ModelPart.Vertex vertex6 = new ModelPart.Vertex(f, y, h, 0.0F, 8.0F);
        ModelPart.Vertex vertex7 = new ModelPart.Vertex(f, g, h, 8.0f, 8.0f);
        ModelPart.Vertex vertex8 = new ModelPart.Vertex(x, g, h, 8.0f, 0.0f);
        // here is important to pass an empty set(last argument) of the directions to avoid unnecessary side creating because
        // we will override them anyway, check your case

        // take the necessary vertex from the original code, you can copy them
        // For example we take top quad:
        ModelPart.Quad BackQuad = new ModelPart.Quad(new ModelPart.Vertex[]{vertex2, vertex, vertex4, vertex3}, 0, 0, 640, 640, textureWidth, textureHeight, mirror, Direction.SOUTH);
        // the important part here to change uv values for it
        // In your case it could be something like 0, 0, 640, 640

        // also remove final modifier from side field to override it here
        this.sides = new ModelPart.Quad[]{BackQuad};
    }
}