package org.pickaid.vernalecho.echo.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.world.item.ItemStack;
import org.pickaid.vernalecho.echo.data.EchoRecord;
import org.pickaid.vernalecho.echo.data.GearSnapshot;

public final class EchoPoseRenderContext {
    private final EchoRecord record;
    private final PlayerModel model;
    private final long gameTime;
    private final List<EchoPropRenderPlan> props = new ArrayList<>();

    public EchoPoseRenderContext(EchoRecord record, PlayerModel model, long gameTime) {
        this.record = record;
        this.model = model;
        this.gameTime = gameTime;
    }

    public EchoRecord record() {
        return this.record;
    }

    public PlayerModel model() {
        return this.model;
    }

    public long gameTime() {
        return this.gameTime;
    }

    public GearSnapshot gearSnapshot() {
        return this.record.gearSnapshot();
    }

    public void addProp(ItemStack stack, EchoPropStyle style, EchoPropAnchor anchor) {
        if (!stack.isEmpty()) {
            this.props.add(new EchoPropRenderPlan(stack, style, anchor));
        }
    }

    public List<EchoPropRenderPlan> props() {
        return List.copyOf(this.props);
    }
}
