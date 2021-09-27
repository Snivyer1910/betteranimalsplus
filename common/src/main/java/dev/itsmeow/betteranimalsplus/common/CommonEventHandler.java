package dev.itsmeow.betteranimalsplus.common;

import dev.itsmeow.imdlib.entity.interfaces.IBucketable;
import dev.itsmeow.imdlib.entity.util.variant.IVariant;
import dev.itsmeow.betteranimalsplus.Ref;
import dev.itsmeow.betteranimalsplus.common.entity.*;
import dev.itsmeow.betteranimalsplus.common.entity.EntityFeralWolf.WolfVariant;
import dev.itsmeow.betteranimalsplus.common.entity.util.IHaveHunger;
import dev.itsmeow.betteranimalsplus.common.entity.util.abstracts.EntitySharkBase;
import dev.itsmeow.betteranimalsplus.init.ModEntities;
import dev.itsmeow.betteranimalsplus.init.ModItems;
import dev.itsmeow.betteranimalsplus.init.ModLootTables;
import dev.itsmeow.betteranimalsplus.init.ModTriggers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.RandomIntGenerator;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = Ref.MOD_ID)
public class CommonEventHandler {

    // TODO port this whole file

    public static final Set<Predicate<Entity>> NO_ATTACKED_DROPS = new HashSet<>();
    static {
        NO_ATTACKED_DROPS.add(e -> e instanceof EntityLamprey);
        NO_ATTACKED_DROPS.add(e -> e instanceof EntitySharkBase);
        NO_ATTACKED_DROPS.add(e -> e instanceof EntityBarracuda);
        NO_ATTACKED_DROPS.add(e -> e instanceof EntityPiranha);
        NO_ATTACKED_DROPS.add(e -> e instanceof EntityOctopus && ((EntityOctopus) e).friend == null);
    }

    @SubscribeEvent
    public static void onDeathOfEntity(LivingDeathEvent e) {
        if(e.getSource().getDirectEntity() instanceof EntityBoar && ModEntities.BOAR.getCustomConfiguration().getBoolean("nerf_options/breed_from_kill")) {
            EntityBoar boar = (EntityBoar) e.getSource().getDirectEntity();
            boar.setInLove(null);
            BlockPos p = boar.blockPosition();
            boar.level.addParticle(ParticleTypes.HEART, p.getX(), p.getY(), p.getZ(), 0.0F, 0.05F, 0.0F);
        } else if(e.getSource().getDirectEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) e.getSource().getDirectEntity();
            if(e.getEntity() instanceof EntityBear || e.getEntity() instanceof PolarBear) {
                if(player.getMainHandItem().isEmpty()) {
                    ModTriggers.PUNCH_BEAR_DEATH.trigger(player);
                }
            } else if(e.getEntity() instanceof EntitySquirrel && !player.getAdvancements().getOrStartProgress(player.server.getAdvancements().getAdvancement(new ResourceLocation("betteranimalsplus:squirrel_kill_100"))).isDone()) {
                CompoundTag pTag = player.getPersistentData();
                if(!pTag.contains("betteranimalsplus", Constants.NBT.TAG_COMPOUND)) {
                    pTag.put("betteranimalsplus", new CompoundTag());
                }
                CompoundTag bTag = pTag.getCompound("betteranimalsplus");
                if(bTag.contains("squirrel_kills", Constants.NBT.TAG_INT)) {
                    int newVal = bTag.getInt("squirrel_kills") + 1;
                    bTag.putInt("squirrel_kills", newVal);
                    if(ModTriggers.SQUIRREL_KILL_TRIGGERS.containsKey(newVal)) {
                        ModTriggers.SQUIRREL_KILL_TRIGGERS.get(newVal).trigger(player);
                    }
                    if(newVal >= 100) {
                        bTag.remove("squirrel_kills");
                    }
                } else {
                    bTag.putInt("squirrel_kills", 1);
                    if(ModTriggers.SQUIRREL_KILL_TRIGGERS.containsKey(1)) {
                        ModTriggers.SQUIRREL_KILL_TRIGGERS.get(1).trigger(player);
                    }
                }
            }
        } else if(e.getSource().getEntity() instanceof EntityOctopus) {
            EntityOctopus octo = (EntityOctopus) e.getSource().getEntity();
            if(octo.friend != null) {
                Player p = octo.level.getPlayerByUUID(octo.friend);
                if(p instanceof ServerPlayer && p.getKillCredit() == e.getEntityLiving()) {
                    ModTriggers.OCTOPUS_SAVE_PLAYER.trigger((ServerPlayer) octo.level.getPlayerByUUID(octo.friend));
                }
            }
        }
        if(e.getSource().getDirectEntity() instanceof IHaveHunger) {
            ((IHaveHunger<?>) e.getSource().getDirectEntity()).resetHunger();
        }
    }
    
    @SubscribeEvent
    public static void onBlockActivate(PlayerInteractEvent.RightClickBlock event) {
        if(event.getWorld().getBlockState(event.getPos()).getBlock() == Blocks.JUKEBOX && event.getPlayer() instanceof ServerPlayer) {
            BlockEntity te = event.getWorld().getBlockEntity(event.getPos());
            if(te instanceof JukeboxBlockEntity) {
                JukeboxBlockEntity box = (JukeboxBlockEntity) te;
                Item held = event.getPlayer().getItemInHand(event.getHand()).getItem();
                Item boxItem = box.getRecord().getItem();
                boolean added = box.getRecord().isEmpty();
                onDiskUse(added, (ServerPlayer) event.getPlayer(), added ? held : boxItem);
            }
        }
    }

    @SubscribeEvent
    public static void onItemUsed(PlayerInteractEvent.RightClickItem event) {
        ResourceLocation reg = event.getItemStack().getItem().getRegistryName();
        if(reg != null && reg.getPath().equals("portable_jukebox") && event.getPlayer() instanceof ServerPlayer) {
            if(event.getItemStack().getTagElement("Disc") != null) {
                Item item = ItemStack.of(event.getItemStack().getTagElement("Disc")).getItem();
                onDiskUse(event.getPlayer().isCrouching(), (ServerPlayer) event.getPlayer(), item);
            }
        }
    }

    private static void onDiskUse(boolean added, ServerPlayer player, Item item) {
        if(item == ModItems.RECORD_CRAB_RAVE.get()) {
            if(added) {
                List<EntityCrab> crabs = player.getCommandSenderWorld().getEntitiesOfClass(EntityCrab.class, player.getBoundingBox().inflate(50));
                ModTriggers.USE_CRAB_DISK.trigger(player);
                for(EntityCrab crab : crabs) {
                    crab.crabRave();
                }

            } else {
                List<EntityCrab> crabs = player.getCommandSenderWorld().getEntitiesOfClass(EntityCrab.class, player.getBoundingBox().inflate(100));
                for(EntityCrab crab : crabs) {
                    crab.unCrabRave();
                }
            }
        } else if(item == ModItems.RECORD_WALRUS.get() && added) {
            ModTriggers.USE_WALRUS_DISK.trigger(player);
        }
    }
    
    @SubscribeEvent
    public static void onLootLoad(LootTableLoadEvent event) {
        RandomIntGenerator simple_one = new RandomIntGenerator() {
            @Override
            public int getInt(Random rand) {
                return 1;
            }

            @Override
            public ResourceLocation getType() {
                return RandomIntGenerator.CONSTANT;
            }
        };
        if(event.getName().equals(EntityType.WOLF.getDefaultLootTable())) {
            IVariant v = ModEntities.FERAL_WOLF.getVariantForName("snowy");
            if(v instanceof WolfVariant) {
                WolfVariant variant = (WolfVariant) v;
                event.getTable().addPool(LootPool.lootPool().setRolls(simple_one).name("snowy_pelt").add(LootTableReference.lootTableReference(variant.getLootTable())).build());
            }
        } else if(event.getName().equals(EntityType.SQUID.getDefaultLootTable())) {
            event.getTable().addPool(LootPool.lootPool().setRolls(simple_one).name("bap_calamari").add(LootTableReference.lootTableReference(ModLootTables.SQUID)).build());
        }
    }
    
    @SubscribeEvent
    public static void onPlayerDamage(AttackEntityEvent event) {
        if(event.getPlayer() instanceof ServerPlayer && (event.getTarget() instanceof EntityBear || event.getTarget() instanceof PolarBear)) {
            if(event.getPlayer().getMainHandItem().isEmpty()) {
                ModTriggers.PUNCH_BEAR.trigger((ServerPlayer) event.getPlayer());
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.Clone event) {
        if(event.getOriginal().getPersistentData().contains("betteranimalsplus", Constants.NBT.TAG_COMPOUND)) {
            event.getPlayer().getPersistentData().put("betteranimalsplus", event.getOriginal().getPersistentData().getCompound("betteranimalsplus"));
        }
    }
    
    @SubscribeEvent
    public static void onLivingDrop(LivingDropsEvent event) {
        if(event.getSource().getEntity() != null && !(event.getEntity() instanceof Player) && NO_ATTACKED_DROPS.stream().anyMatch(predicate -> predicate.test(event.getSource().getEntity())) && (!(event.getSource().getEntity() instanceof IBucketable) || !((IBucketable) event.getSource().getEntity()).isFromContainer())) {
            event.getDrops().clear();
        }
    }

    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinWorldEvent event) {
        if(event.getEntity() instanceof IronGolem) {
            GoalSelector targetSelector = ((IronGolem) event.getEntity()).targetSelector;
            targetSelector.addGoal(3, new NearestAttackableTargetGoal<>((IronGolem) event.getEntity(), EntityFeralWolf.class, 5, false, false, e -> !((EntityFeralWolf) e).isTame() && (e instanceof EntityCoyote ? !((EntityCoyote) e).isDaytime() : true)));
        }
    }

}