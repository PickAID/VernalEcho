package org.pickaid.vernalecho.data;

import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class VernalEchoDataGenerators {
    private VernalEchoDataGenerators() {
    }

    public static void gatherServerData(GatherDataEvent.Server event) {
        event.createDatapackRegistryObjects(VernalEchoWorldgen.registrySetBuilder());
    }
}
