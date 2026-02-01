package com.pinotthecorsky.cgb.client;

import com.pinotthecorsky.cgb.compat.JeiCompat;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

public final class CgbClientEvents {
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        JeiCompat.onBadgesChanged();
    }

    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) {
            JeiCompat.onBadgesChanged();
        }
    }

    private CgbClientEvents() {
    }
}
