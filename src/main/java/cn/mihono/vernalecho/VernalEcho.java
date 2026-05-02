package cn.mihono.vernalecho;

import net.minecraft.resources.Identifier;
import net.neoforged.fml.common.Mod;

@Mod(VernalEcho.MOD_ID)
public final class VernalEcho {
    public static final String MOD_ID = "vernalecho";

    public VernalEcho() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
