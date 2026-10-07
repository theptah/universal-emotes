plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.20.1-fabric"

stonecutter parameters {
    val (_, loader) = current.project.split('-', limit = 2)

    properties {
        tags(node.metadata.version, loader)
    }

    constants {
        match(loader, "fabric", "forge", "neoforge")
    }

    replacements {
        string(current.parsed < "1.20") {
            replace("net.minecraft.client.gui.GuiGraphics", "com.ptah.client.compat.legacy.GuiGraphics")
        }

        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
            replace(".dimension().location()", ".dimension().identifier()")
            replace("net.minecraft.client.model.PlayerModel", "net.minecraft.client.model.player.PlayerModel")
            replace("net.minecraft.client.model.PlayerCapeModel", "net.minecraft.client.model.player.PlayerCapeModel")
        }
    }
}
