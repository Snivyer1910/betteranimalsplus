package dev.itsmeow.betteranimalsplus.client;

import dev.itsmeow.betteranimalsplus.Ref;
import dev.itsmeow.betteranimalsplus.common.item.ItemModeledArmor;
import dev.itsmeow.betteranimalsplus.init.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderingRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

public class BetterAnimalsPlusClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleHandler.clientInit();
        ModItems.getModeledArmor().values().forEach(registrySupplier -> {
            ItemModeledArmor armor = registrySupplier.get();
            ArmorRenderingRegistry.registerModel((entity, stack, slot, defaultModel) -> {
                HumanoidModel<LivingEntity> model = armor.getArmorModel(entity, stack, slot, defaultModel);
                if(!Minecraft.getInstance().isPaused()) {
                    float g = Minecraft.getInstance().getFrameTime();
                    float h = Mth.rotLerp(g, entity.yBodyRotO, entity.yBodyRot);
                    float j = Mth.rotLerp(g, entity.yHeadRotO, entity.yHeadRot);
                    float k = j - h;
                    float o;
                    if (entity.isPassenger() && entity.getVehicle() instanceof LivingEntity) {
                        LivingEntity livingEntity2 = (LivingEntity) entity.getVehicle();
                        h = Mth.rotLerp(g, livingEntity2.yBodyRotO, livingEntity2.yBodyRot);
                        k = j - h;
                        o = Mth.wrapDegrees(k);
                        if (o < -85.0F) {
                            o = -85.0F;
                        }

                        if (o >= 85.0F) {
                            o = 85.0F;
                        }

                        h = j - o;
                        if (o * o > 2500.0F) {
                            h += o * 0.2F;
                        }

                        k = j - h;
                    }

                    float m = Mth.lerp(g, entity.xRotO, entity.xRot);
                    float p;
                    if (entity.getPose() == Pose.SLEEPING) {
                        Direction direction = entity.getBedOrientation();
                        if (direction != null) {
                            p = entity.getEyeHeight(Pose.STANDING) - 0.1F;
                        }
                    }

                    o = (float) entity.tickCount + g;
                    p = 0.0F;
                    float q = 0.0F;
                    if (!entity.isPassenger() && entity.isAlive()) {
                        p = Mth.lerp(g, entity.animationSpeedOld, entity.animationSpeed);
                        q = entity.animationPosition - entity.animationSpeed * (1.0F - g);
                        if (entity.isBaby()) {
                            q *= 3.0F;
                        }

                        if (p > 1.0F) {
                            p = 1.0F;
                        }
                    }
                    model.setupAnim(entity, q, p, o, k, m);
                }
                return model;
            }, armor);
            ArmorRenderingRegistry.registerSimpleTexture(new ResourceLocation(Ref.MOD_ID, armor.getMaterial().getName()), armor);
        });
    }
}
