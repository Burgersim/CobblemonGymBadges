package com.pinotthecorsky.cgb.client;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import com.pinotthecorsky.cgb.client.resources.BadgeModelPack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

@EventBusSubscriber(modid = CobblemonGymBadges.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class CgbClientModEvents {
    private CgbClientModEvents() {
    }

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        BadgeModelPack.onAddPackFinders(event);
    }
}
