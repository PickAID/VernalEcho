package org.pickaid.vernalecho.echo.client.render;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import org.pickaid.vernalecho.echo.data.EchoPose;

public final class EchoPoseRenderers {
    private static final Map<EchoPose, EchoPoseRenderer> RENDERERS = new EnumMap<>(EchoPose.class);
    private static final List<EchoRenderDecorator> DECORATORS = new ArrayList<>();

    static {
        registerDefaults();
    }

    private EchoPoseRenderers() {
    }

    public static void register(EchoPose pose, EchoPoseRenderer renderer) {
        RENDERERS.put(pose, renderer);
    }

    public static void registerDecorator(EchoRenderDecorator decorator) {
        DECORATORS.add(decorator);
    }

    public static EchoPoseRenderContext apply(EchoPose pose, EchoPoseRenderContext context) {
        RENDERERS.getOrDefault(pose, EchoPoseRenderers::idle).apply(context);
        for (EchoRenderDecorator decorator : DECORATORS) {
            decorator.decorate(context);
        }
        return context;
    }

    private static void registerDefaults() {
        register(EchoPose.IDLE, EchoPoseRenderers::idle);
        register(EchoPose.WALKING, EchoPoseRenderers::walking);
        register(EchoPose.CROUCHING, EchoPoseRenderers::crouching);
        register(EchoPose.GUARDING, EchoPoseRenderers::guarding);
        register(EchoPose.REACHING, EchoPoseRenderers::reaching);
        register(EchoPose.CROUCH_REACHING, EchoPoseRenderers::crouchReaching);
        register(EchoPose.READING, EchoPoseRenderers::reading);
        register(EchoPose.FALLEN, EchoPoseRenderers::fallen);
    }

    private static void idle(EchoPoseRenderContext context) {
        PlayerModel model = context.model();
        float idle = idle(context.gameTime());
        model.head.yRot = idle;
        model.rightArm.xRot = -0.12F + idle;
        model.leftArm.xRot = 0.12F - idle;
        addHeldGear(context);
    }

    private static void walking(EchoPoseRenderContext context) {
        PlayerModel model = context.model();
        float swing = Mth.sin(context.gameTime() * 0.23F) * 0.72F;
        model.head.yRot = idle(context.gameTime());
        model.rightArm.xRot = -swing;
        model.leftArm.xRot = swing;
        model.rightLeg.xRot = swing;
        model.leftLeg.xRot = -swing;
        addHeldGear(context);
    }

    private static void crouching(EchoPoseRenderContext context) {
        PlayerModel model = context.model();
        model.head.yRot = idle(context.gameTime());
        model.body.xRot = 0.34F;
        model.head.xRot = 0.16F;
        model.rightArm.xRot = -0.52F;
        model.leftArm.xRot = -0.48F;
        model.rightLeg.xRot = -0.72F;
        model.leftLeg.xRot = -0.68F;
        addHeldGear(context);
    }

    private static void guarding(EchoPoseRenderContext context) {
        PlayerModel model = context.model();
        model.head.yRot = idle(context.gameTime()) * 0.6F;
        model.rightArm.xRot = -1.18F;
        model.leftArm.xRot = -1.05F;
        model.rightArm.yRot = -0.24F;
        model.leftArm.yRot = 0.24F;
        model.rightLeg.xRot = 0.18F;
        model.leftLeg.xRot = -0.18F;
        addMainHand(context);
        ItemStack offHand = context.gearSnapshot().offHand();
        context.addProp(offHand.isEmpty() ? new ItemStack(Items.SHIELD) : offHand, EchoPropStyle.SHIELD, EchoPropAnchor.LEFT_HAND);
    }

    private static void reaching(EchoPoseRenderContext context) {
        PlayerModel model = context.model();
        model.head.yRot = idle(context.gameTime()) * 0.4F;
        model.rightArm.xRot = -1.46F;
        model.rightArm.yRot = -0.18F;
        model.leftArm.xRot = -0.35F;
        model.head.xRot = 0.2F;
        addMainHand(context);
    }

    private static void crouchReaching(EchoPoseRenderContext context) {
        PlayerModel model = context.model();
        model.body.xRot = 0.36F;
        model.head.xRot = 0.22F;
        model.rightArm.xRot = -1.52F;
        model.rightArm.yRot = -0.2F;
        model.leftArm.xRot = -0.58F;
        model.rightLeg.xRot = -0.74F;
        model.leftLeg.xRot = -0.7F;
        addMainHand(context);
    }

    private static void reading(EchoPoseRenderContext context) {
        PlayerModel model = context.model();
        model.head.xRot = 0.26F;
        model.rightArm.xRot = -1.12F;
        model.leftArm.xRot = -1.08F;
        model.rightArm.yRot = -0.28F;
        model.leftArm.yRot = 0.28F;
        ItemStack mainHand = context.gearSnapshot().mainHand();
        context.addProp(mainHand.isEmpty() ? new ItemStack(Items.BOOK) : mainHand, EchoPropStyle.BOOK, EchoPropAnchor.RIGHT_HAND);
    }

    private static void fallen(EchoPoseRenderContext context) {
        PlayerModel model = context.model();
        model.rightArm.xRot = -0.85F;
        model.leftArm.xRot = 0.42F;
        model.rightLeg.xRot = 0.26F;
        model.leftLeg.xRot = -0.18F;
        model.head.zRot = 0.22F;
        ItemStack mainHand = context.gearSnapshot().mainHand();
        context.addProp(mainHand, styleFor(mainHand), EchoPropAnchor.GROUND_RIGHT);
    }

    private static void addHeldGear(EchoPoseRenderContext context) {
        addMainHand(context);
        addOffHand(context);
    }

    private static void addMainHand(EchoPoseRenderContext context) {
        ItemStack mainHand = context.gearSnapshot().mainHand();
        context.addProp(mainHand, styleFor(mainHand), EchoPropAnchor.RIGHT_HAND);
    }

    private static void addOffHand(EchoPoseRenderContext context) {
        ItemStack offHand = context.gearSnapshot().offHand();
        context.addProp(offHand, styleFor(offHand), EchoPropAnchor.LEFT_HAND);
    }

    private static EchoPropStyle styleFor(ItemStack stack) {
        if (stack.getItem() instanceof ShieldItem) {
            return EchoPropStyle.SHIELD;
        }
        if (stack.is(Items.BOOK) || stack.is(Items.WRITABLE_BOOK) || stack.is(Items.WRITTEN_BOOK) || stack.is(Items.ENCHANTED_BOOK)) {
            return EchoPropStyle.BOOK;
        }
        return EchoPropStyle.ITEM;
    }

    private static float idle(long gameTime) {
        return Mth.sin(gameTime * 0.08F) * 0.08F;
    }
}
