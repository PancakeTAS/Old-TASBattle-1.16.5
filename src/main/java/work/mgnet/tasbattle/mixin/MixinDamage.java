package work.mgnet.tasbattle.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.EntityDamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.mgnet.tasbattle.TASBattle;

import java.io.IOException;

@Mixin(LivingEntity.class)
public abstract class MixinDamage extends Entity {

    @Shadow
    public DamageSource lastDamageSource;
    @Shadow
    public long lastDamageTime;
    @Shadow
    public float knockbackVelocity;
    @Shadow
    public int playerHitTimer;
    @Shadow
    public int despawnCounter;
    @Shadow
    public int maxHurtTime;
    @Shadow
    public PlayerEntity attackingPlayer;
    @Shadow
    public float lastDamageTaken;
    @Shadow
    public float limbDistance;
    @Shadow
    public int hurtTime;

    public MixinDamage(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(at = @At(value = "HEAD"), method = "damage", cancellable = true)
    public void redodamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> ci) throws IOException {
        try {
            if (!TASBattle.isRunning || !TASBattle.players.contains((ServerPlayerEntity) (Object) this)) {
                ci.setReturnValue(false);
                return;
            }
        } catch (Exception e) {
            // Something happened, that is just because I'm lazy
        }
        if (this.isInvulnerableTo(source)) {
            ci.setReturnValue(false);
        } else if (this.world.isClient) {
            ci.setReturnValue(false);
        } else if (this.isDead()) {
            ci.setReturnValue(false);
        } else if (source.isFire() && this.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
            ci.setReturnValue(false);
        } else {
            if (this.isSleeping() && !this.world.isClient) {
                this.wakeUp();
            }

            this.despawnCounter = 0;
            float f = amount;
            if ((source == DamageSource.ANVIL || source == DamageSource.FALLING_BLOCK) && !this.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
                this.getEquippedStack(EquipmentSlot.HEAD).damage((int)(amount * 4.0F + this.random.nextFloat() * amount * 2.0F), (LivingEntity) (Object)this, (livingEntityx) -> {
                    livingEntityx.sendEquipmentBreakStatus(EquipmentSlot.HEAD);
                });
                amount *= 0.75F;
            }

            boolean bl = false;
            float g = 0.0F;
            if (amount > 0.0F && this.blockedByShield(source)) {
                this.damageShield(amount);
                g = amount;
                amount = 0.0F;
                if (!source.isProjectile()) {
                    Entity entity = source.getSource();
                    if (entity instanceof LivingEntity) {
                        this.takeShieldHit((LivingEntity)entity);
                    }
                }

                bl = true;
            }

            this.limbDistance = 1.5F;
            boolean bl2 = true;
            if ((float)this.timeUntilRegen > 10.0F) {
                if (amount <= this.lastDamageTaken) {
                    ci.setReturnValue(false);
                }

                this.applyDamage(source, amount - this.lastDamageTaken);
                this.lastDamageTaken = amount;
                bl2 = false;
            } else {
                this.lastDamageTaken = amount;
                this.timeUntilRegen = 20;
                this.applyDamage(source, amount);
                this.maxHurtTime = 10;
                this.hurtTime = this.maxHurtTime;
            }

            this.knockbackVelocity = 0.0F;
            Entity entity2 = source.getAttacker();
            if (entity2 != null) {
                if (entity2 instanceof LivingEntity) {
                    this.setAttacker((LivingEntity)entity2);
                }

                if (entity2 instanceof PlayerEntity) {
                    this.playerHitTimer = 100;
                    this.attackingPlayer = (PlayerEntity)entity2;
                } else if (entity2 instanceof WolfEntity) {
                    WolfEntity wolfEntity = (WolfEntity)entity2;
                    if (wolfEntity.isTamed()) {
                        this.playerHitTimer = 100;
                        LivingEntity livingEntity = wolfEntity.getOwner();
                        if (livingEntity != null && livingEntity.getType() == EntityType.PLAYER) {
                            this.attackingPlayer = (PlayerEntity)livingEntity;
                        } else {
                            this.attackingPlayer = null;
                        }
                    }
                }
            }

            if (bl2) {
                if (bl) {
                    this.world.sendEntityStatus(this, (byte)29);
                } else if (source instanceof EntityDamageSource && ((EntityDamageSource)source).isThorns()) {
                    this.world.sendEntityStatus(this, (byte)33);
                } else {
                    byte e;
                    if (source == DamageSource.DROWN) {
                        e = 36;
                    } else if (source.isFire()) {
                        e = 37;
                    } else if (source == DamageSource.SWEET_BERRY_BUSH) {
                        e = 44;
                    } else {
                        e = 2;
                    }

                    this.world.sendEntityStatus(this, e);
                }

                if (source != DamageSource.DROWN && (!bl || amount > 0.0F)) {
                    this.scheduleVelocityUpdate();
                }

                if (entity2 != null) {
                    double h = entity2.getX() - this.getX();

                    double i;
                    for(i = entity2.getZ() - this.getZ(); h * h + i * i < 1.0E-4D; i = (Math.random() - Math.random()) * 0.01D) {
                        h = (Math.random() - Math.random()) * 0.01D;
                    }

                    this.knockbackVelocity = (float)(MathHelper.atan2(i, h) * 57.2957763671875D - (double)this.yaw);
                    this.takeKnockback(0.4F, h, i);
                } else {
                    this.knockbackVelocity = (float)((int)(Math.random() * 2.0D) * 180);
                }
            }

            if (this.isDead()) {
                if (!this.tryUseTotem(source)) {
                    SoundEvent soundEvent = this.getDeathSound();
                    if (bl2 && soundEvent != null) {
                        this.playSound(soundEvent, this.getSoundVolume(), this.getSoundPitch());
                    }

                    double x = getPos().x;
                    double y = getPos().y;
                    double z = getPos().z;

                    this.onDeath(source);
                    if ((Object) this instanceof ServerPlayerEntity) {
                        TASBattle.playerDeath((ServerPlayerEntity) (Object) this, x, y, z);
                    }
                }
            } else if (bl2) {
                this.playHurtSound(source);
            }

            boolean bl3 = !bl || amount > 0.0F;
            if (bl3) {
                this.lastDamageSource = source;
                this.lastDamageTime = this.world.getTime();
            }

            if ((Object) this instanceof ServerPlayerEntity) {
                Criteria.ENTITY_HURT_PLAYER.trigger((ServerPlayerEntity)(Object)this, source, f, amount, bl);
                if (g > 0.0F && g < 3.4028235E37F) {
                    ((ServerPlayerEntity)(Object)this).increaseStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(g * 10.0F));
                }
            }

            if (entity2 instanceof ServerPlayerEntity) {
                Criteria.PLAYER_HURT_ENTITY.trigger((ServerPlayerEntity)entity2, this, source, f, amount, bl);
            }

            ci.setReturnValue(bl3);
        }
    }
    @Shadow
    public abstract void applyDamage(DamageSource source, float amount);
    @Shadow
    public abstract ItemStack getEquippedStack(EquipmentSlot head);
    @Shadow
    public abstract boolean blockedByShield(DamageSource source);
    @Shadow
    public abstract void setAttacker(LivingEntity entity2);
    @Shadow
    public abstract void takeShieldHit(LivingEntity entity);
    @Shadow
    public abstract void takeKnockback(float v, double h, double i);
    @Shadow
    public abstract boolean tryUseTotem(DamageSource source);
    @Shadow
    public abstract void damageShield(float amount);
    @Shadow
    public abstract float getSoundPitch();
    @Shadow
    public abstract float getSoundVolume();
    @Shadow
    public abstract SoundEvent getDeathSound();
    @Shadow
    public abstract void onDeath(DamageSource source);
    @Shadow
    public abstract boolean isSleeping();
    @Shadow
    public abstract void wakeUp();
    @Shadow
    public abstract boolean isDead();
    @Shadow
    public abstract void playHurtSound(DamageSource source);
    @Shadow
    public abstract boolean hasStatusEffect(StatusEffect fireResistance);

}
