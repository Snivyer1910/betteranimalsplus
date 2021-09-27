package dev.itsmeow.betteranimalsplus.common.entity.util.abstracts;

import dev.itsmeow.imdlib.entity.interfaces.IBucketable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;

public abstract class EntityWaterMobPathingWithTypesBucketable extends EntityWaterMobPathingWithTypes implements IBucketable {

    public EntityWaterMobPathingWithTypesBucketable(EntityType<? extends EntityWaterMobPathingWithTypesBucketable> entityType, Level worldIn) {
        super(entityType, worldIn);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.registerFromContainerKey();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        this.writeFromContainerToEntity(compound);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.readFromContainerToEntity(compound);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.isFromContainer() && despawn(distanceToClosestPlayer);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return this.isFromContainer();
    }

    @Override
    public void setContainerData(ItemStack bucket) {
        IBucketable.super.setContainerData(bucket);
        CompoundTag tag = bucket.getTag();
        if(bucket.getTag() == null) {
            tag = new CompoundTag();
        }
        tag.putString("BucketVariantTag", this.getVariantNameOrEmpty());
        bucket.setTag(tag);
    }

    protected abstract SoundEvent getFlopSound();

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.FISH_SWIM;
    }

    @Override
    public void aiStep() {
        if(!this.isInWater() && this.onGround && this.verticalCollision) {
            this.setDeltaMovement(this.getDeltaMovement().add((this.random.nextFloat() * 2.0F - 1.0F) * 0.05F, 0.4F, (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F));
            this.onGround = false;
            this.hasImpulse = true;
            this.playSound(this.getFlopSound(), this.getSoundVolume(), this.getVoicePitch());
        }

        super.aiStep();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if(this.processContainerInteract(player, hand)) {
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void readFromContainerTag(CompoundTag tag) {
        if(tag.contains("BucketVariantTag")) {
            this.setType(tag.getString("BucketVariantTag"));
        }
    }

}
