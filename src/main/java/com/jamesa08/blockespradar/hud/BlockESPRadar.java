package com.jamesa08.blockespradar.hud;

import com.jamesa08.blockespradar.BlockESPRadarAddon;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPBlockData;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPChunk;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.BlockESP;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPBlock;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BlockESPRadar extends HudElement {
    public static final HudElementInfo<BlockESPRadar> INFO = new HudElementInfo<>(BlockESPRadarAddon.HUD_GROUP, "block-esp-radar", "Displays BlockESP blocks on a radar.", BlockESPRadar::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private Field chunksField;

    private Method getBlockDataMethod;


    private final Setting<SettingColor> backgroundColor = sgGeneral.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color of the radar background.")
        .defaultValue(new SettingColor(0, 0, 0, 64))
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale of the radar.")
        .defaultValue(1)
        .min(0.1)
        .sliderRange(0.1, 5)
        .onChanged(aDouble -> calculateSize())
        .build()
    );

    private final Setting<Double> zoom = sgGeneral.add(new DoubleSetting.Builder()
        .name("zoom")
        .description("Radar zoom level.")
        .defaultValue(1)
        .min(0.1)
        .sliderRange(0.1, 3)
        .build()
    );


    public BlockESPRadar() {
        super(INFO);
        calculateSize();
        initReflection();
    }

    private void initReflection() {
        try {
            chunksField = BlockESP.class.getDeclaredField("chunks");
            chunksField.setAccessible(true);
            getBlockDataMethod = BlockESP.class.getDeclaredMethod("getBlockData", Block.class);
            getBlockDataMethod.setAccessible(true);
        } catch (NoSuchFieldException | NoSuchMethodException ignored) {}
    }

    @SuppressWarnings("unchecked")
    private Long2ObjectMap<ESPChunk> getChunks(BlockESP blockESP) {
        try {
            return (Long2ObjectMap<ESPChunk>) chunksField.get(blockESP);
        } catch (IllegalAccessException e) {
            return null;
        }
    }



    private void calculateSize() {
        setSize(200 * scale.get(), 200 * scale.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        double width = getWidth();
        double height = getHeight();

        // Render background
        renderer.quad(x, y, width, height, backgroundColor.get());

        if (mc.player == null || mc.world == null) return;

        BlockESP blockESP = Modules.get().get(BlockESP.class);
        if (blockESP == null || chunksField == null) return;

        Long2ObjectMap<ESPChunk> chunks = getChunks(blockESP);
        if (chunks == null) return;

        // Pre-calculate constants
        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();
        double scaleZoom = scale.get() * zoom.get();
        double halfWidth = width / 2;
        double halfHeight = height / 2;

        int range = (int) (halfWidth / scaleZoom);
        int minX = (int) (playerX - range);
        int maxX = (int) (playerX + range);
        int minZ = (int) (playerZ - range);
        int maxZ = (int) (playerZ + range);

        // Iterate through chunks
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                long chunkKey = ChunkPos.toLong(chunkX, chunkZ);
                ESPChunk espChunk = chunks.get(chunkKey);

                if (espChunk != null) {
                    for (ESPBlock block : espChunk.blocks.values()) {
                        BlockPos pos = new BlockPos(block.x, block.y, block.z);

                        if (pos.getX() >= minX && pos.getX() <= maxX && pos.getZ() >= minZ && pos.getZ() <= maxZ) {
                            double xPos = ((pos.getX() - playerX) * scaleZoom + halfWidth);
                            double zPos = ((pos.getZ() - playerZ) * scaleZoom + halfHeight);
                            Block mcBlock = mc.world.getBlockState(pos).getBlock();
                            if (xPos >= 0 && zPos >= 0 && xPos <= width && zPos <= height) {
                                SettingColor blockColor = null;
                                try {
                                    ESPBlockData bData = (ESPBlockData) getBlockDataMethod.invoke(blockESP, mcBlock);
                                    blockColor = bData.lineColor.copy().toSetting();
                                    blockColor.a = 255;  // copy and set alpha to 255
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    return;
                                }
                                renderer.text("*", x + xPos, y + zPos, blockColor, false);
                            }
                        }
                    }
                }
            }
        }
        renderer.text("*", x + halfWidth, y+ halfHeight, SettingColor.WHITE, false);
    }

}
