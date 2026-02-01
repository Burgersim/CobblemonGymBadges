package com.pinotthecorsky.cgb.client;

import com.pinotthecorsky.cgb.compat.EmiCompat;
import com.pinotthecorsky.cgb.compat.JeiCompat;
import com.pinotthecorsky.cgb.compat.ReiCompat;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

public final class CgbClientEvents {
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        Minecraft.getInstance().execute(() -> {
            JeiCompat.onRecipesUpdated();
            EmiCompat.onRecipesUpdated();
            ReiCompat.onRecipesUpdated();
        });
    }

    public static void onTagsUpdated(TagsUpdatedEvent event) {
        Minecraft.getInstance().execute(() -> {
            JeiCompat.onRecipesUpdated();
            EmiCompat.onRecipesUpdated();
            ReiCompat.onRecipesUpdated();
        });
    }

    private CgbClientEvents() {
    }
}
