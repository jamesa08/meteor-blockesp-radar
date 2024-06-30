package com.jamesa08.blockespradar;

import com.jamesa08.blockespradar.hud.BlockESPRadar;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import org.slf4j.Logger;

public class BlockESPRadarAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final HudGroup HUD_GROUP = new HudGroup("BlockESP Hud");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor BlockESP Radar");

        // HUD
        Hud.get().register(BlockESPRadar.INFO);
    }

    @Override
    public String getPackage() {
        return "com.jamesa08.blockespradar";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("jamesa08", "meteor-blockesp-radar");
    }
}
