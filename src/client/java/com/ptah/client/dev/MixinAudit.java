package com.ptah.client.dev;

import com.ptah.UniversalEmotesMod;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.spongepowered.asm.mixin.MixinEnvironment;

public final class MixinAudit {
    private static final String PROPERTY = "universalemotes.mixinAudit";
    private static final int DELAY_TICKS = 200;
    private static int ticks;

    private MixinAudit() { }

    public static void registerIfRequested() {
        if (!Boolean.getBoolean(PROPERTY)) return;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (++ticks != DELAY_TICKS) return;
            boolean ok = true;
            try {
                MixinEnvironment.getCurrentEnvironment().audit();
            } catch (Throwable throwable) {
                ok = false;
                UniversalEmotesMod.LOGGER.error("[MixinAudit] Audit threw", throwable);
            }
            UniversalEmotesMod.LOGGER.info("[MixinAudit] RESULT {}", ok ? "OK" : "FAILED");
            client.stop();
        });
    }
}
