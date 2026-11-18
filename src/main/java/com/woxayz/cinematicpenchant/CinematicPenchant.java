package com.woxayz.cinematicpenchant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;

import com.woxayz.cinematicpenchant.client.SlotBookEvents;
import com.woxayz.cinematicpenchant.network.CinematicPenchantNetworking;

@Mod(CinematicPenchant.MOD_ID)
public class CinematicPenchant {
    public static final String MOD_ID = "cinematicpenchant";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public CinematicPenchant(IEventBus modBus, ModContainer container) {
        LOGGER.info("Cinematic Interactions: Penchant is present");
        modBus.addListener(CinematicPenchantNetworking::register);
        if (FMLEnvironment.dist == Dist.CLIENT) SlotBookEvents.register();
    }
}
