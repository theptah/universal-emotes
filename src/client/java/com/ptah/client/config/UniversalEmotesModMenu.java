package com.ptah.client.config;

import com.ptah.bundle.Rarity;
import com.ptah.client.network.ClientConfigNetwork;
import com.ptah.client.network.ClientVisibilityNetwork;
import com.ptah.config.ServerConfig;
import com.ptah.config.ServerConfigManager;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class UniversalEmotesModMenu implements ModMenuApi {
    private static final ClientConfig CLIENT_DEFAULTS = new ClientConfig();
    private static final ServerConfig SERVER_DEFAULTS = new ServerConfig();

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return UniversalEmotesModMenu::build;
    }

    private static boolean serverTabEditable(Minecraft mc) {
        if (mc.player == null || mc.level == null) return true;
        if (mc.hasSingleplayerServer()) return true;

        return ClientConfigNetwork.isEditAllowedByServer() || com.ptah.compat.ServerCompat.hasPermission(mc.player, 2);
    }

    private static Screen build(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        ClientConfig client = ClientConfigManager.get();
        boolean connected = mc.player != null;
        boolean serverEditable = serverTabEditable(mc);

        ServerConfig source = connected ? SyncedServerConfig.get() : ServerConfigManager.get();
        ServerConfig draft = source.copy();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.universal-emotes.title"))
                .setSavingRunnable(() -> {
                    ClientConfigManager.save();

                    ClientVisibilityNetwork.sendCurrent();
                    if (serverEditable) {
                        draft.sanitize();

                        if (connected && ClientConfigNetwork.sendUpdate(draft)) {
                            return;
                        }

                        ServerConfigManager.set(draft);
                        ServerConfigManager.save();
                    }
                });

        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory clientCategory =
                builder.getOrCreateCategory(Component.translatable("config.universal-emotes.category.client"));

        clientCategory.addEntry(entries
                .startBooleanToggle(Component.translatable("config.universal-emotes.client.tps"), client.turnToTpsAfterEveryEmote)
                .setDefaultValue(CLIENT_DEFAULTS.turnToTpsAfterEveryEmote)
                .setTooltip(Component.translatable("config.universal-emotes.client.tps.tooltip"))
                .setSaveConsumer(value -> client.turnToTpsAfterEveryEmote = value)
                .build());

        clientCategory.addEntry(entries
                .startBooleanToggle(Component.translatable("config.universal-emotes.client.mute"), client.muteEmoteSounds)
                .setDefaultValue(CLIENT_DEFAULTS.muteEmoteSounds)
                .setTooltip(Component.translatable("config.universal-emotes.client.mute.tooltip"))
                .setSaveConsumer(value -> client.muteEmoteSounds = value)
                .build());

        clientCategory.addEntry(entries
                .startBooleanToggle(Component.translatable("config.universal-emotes.client.dmca"), client.dmcaMode)
                .setDefaultValue(CLIENT_DEFAULTS.dmcaMode)
                .setTooltip(Component.translatable("config.universal-emotes.client.dmca.tooltip"))
                .setSaveConsumer(value -> client.dmcaMode = value)
                .build());

        clientCategory.addEntry(entries
                .startBooleanToggle(Component.translatable("config.universal-emotes.client.particles"), client.hideParticleEffects)
                .setDefaultValue(CLIENT_DEFAULTS.hideParticleEffects)
                .setTooltip(Component.translatable("config.universal-emotes.client.particles.tooltip"))
                .setSaveConsumer(value -> client.hideParticleEffects = value)
                .build());

        clientCategory.addEntry(entries
                .startBooleanToggle(Component.translatable("config.universal-emotes.client.hide_armor"), client.hideArmorDuringEmotes)
                .setDefaultValue(CLIENT_DEFAULTS.hideArmorDuringEmotes)
                .setTooltip(Component.translatable("config.universal-emotes.client.hide_armor.tooltip"))
                .setSaveConsumer(value -> client.hideArmorDuringEmotes = value)
                .build());

        clientCategory.addEntry(entries
                .startBooleanToggle(Component.translatable("config.universal-emotes.client.hide_held"), client.hideHeldItemsDuringEmotes)
                .setDefaultValue(CLIENT_DEFAULTS.hideHeldItemsDuringEmotes)
                .setTooltip(Component.translatable("config.universal-emotes.client.hide_held.tooltip"))
                .setSaveConsumer(value -> client.hideHeldItemsDuringEmotes = value)
                .build());

        clientCategory.addEntry(entries
                .startColorField(Component.translatable("config.universal-emotes.client.accent_color"), client.accentColor)
                .setDefaultValue(CLIENT_DEFAULTS.accentColor)
                .setTooltip(Component.translatable("config.universal-emotes.client.accent_color.tooltip"))
                .setSaveConsumer(value -> client.accentColor = value)
                .build());

        ConfigCategory serverCategory =
                builder.getOrCreateCategory(Component.translatable("config.universal-emotes.category.server"));

        if (serverEditable) {
            addEditableServerEntries(entries, serverCategory, draft);
        } else {
            addReadOnlyServerEntries(entries, serverCategory, source);
        }

        return builder.build();
    }

    private static void addEditableServerEntries(ConfigEntryBuilder entries, ConfigCategory category, ServerConfig draft) {
        category.addEntry(entries
                .startBooleanToggle(Component.translatable("config.universal-emotes.server.ignore_op.name"), draft.ignoreAnimationsOpLevel)
                .setDefaultValue(SERVER_DEFAULTS.ignoreAnimationsOpLevel)
                .setTooltip(Component.translatable("config.universal-emotes.server.ignore_op.tooltip"))
                .setSaveConsumer(value -> draft.ignoreAnimationsOpLevel = value)
                .build());

        category.addEntry(entries
                .startBooleanToggle(Component.translatable("config.universal-emotes.server.level_requirement.name"), draft.enableLevelRequirement)
                .setDefaultValue(SERVER_DEFAULTS.enableLevelRequirement)
                .setTooltip(Component.translatable("config.universal-emotes.server.level_requirement.tooltip"))
                .setSaveConsumer(value -> draft.enableLevelRequirement = value)
                .build());

        category.addEntry(entries
                .startBooleanToggle(Component.translatable("config.universal-emotes.server.damage_no_interrupt.name"), draft.disallowInterruptOnDamage)
                .setDefaultValue(SERVER_DEFAULTS.disallowInterruptOnDamage)
                .setTooltip(Component.translatable("config.universal-emotes.server.damage_no_interrupt.tooltip"))
                .setSaveConsumer(value -> draft.disallowInterruptOnDamage = value)
                .build());

        for (Rarity rarity : Rarity.values()) {
            Rarity captured = rarity;
            category.addEntry(entries
                    .startIntField(rarityLevelLabel(rarity), draft.levelFor(rarity))
                    .setDefaultValue(SERVER_DEFAULTS.levelFor(rarity))
                    .setMin(0)
                    .setTooltip(Component.translatable("config.universal-emotes.server.rarity_level.tooltip"))
                    .setSaveConsumer(value -> draft.setLevelFor(captured, value))
                    .build());
        }
    }

    private static void addReadOnlyServerEntries(ConfigEntryBuilder entries, ConfigCategory category, ServerConfig server) {
        category.addEntry(entries
                .startTextDescription(Component.translatable("config.universal-emotes.server.readonly"))
                .build());
        category.addEntry(entries
                .startTextDescription(readOnlyLine(
                        Component.translatable("config.universal-emotes.server.ignore_op.name"),
                        onOff(server.ignoreAnimationsOpLevel)))
                .build());
        category.addEntry(entries
                .startTextDescription(readOnlyLine(
                        Component.translatable("config.universal-emotes.server.level_requirement.name"),
                        onOff(server.enableLevelRequirement)))
                .build());
        category.addEntry(entries
                .startTextDescription(readOnlyLine(
                        Component.translatable("config.universal-emotes.server.damage_no_interrupt.name"),
                        onOff(server.disallowInterruptOnDamage)))
                .build());
        for (Rarity rarity : Rarity.values()) {
            category.addEntry(entries
                    .startTextDescription(readOnlyLine(
                            rarityLevelLabel(rarity),
                            Component.literal(Integer.toString(server.levelFor(rarity)))))
                    .build());
        }
    }

    private static Component rarityLevelLabel(Rarity rarity) {
        return Component.translatable("config.universal-emotes.server.rarity_level.name",
                Component.translatable("config.universal-emotes.rarity." + rarity.name().toLowerCase()));
    }

    private static Component readOnlyLine(Component label, Component value) {
        return Component.translatable("config.universal-emotes.server.readonly_line", label, value);
    }

    private static Component onOff(boolean value) {
        return Component.translatable(value ? "config.universal-emotes.on" : "config.universal-emotes.off");
    }
}
