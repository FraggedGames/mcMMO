package com.gmail.nossr50.skills.axes;

import java.util.Map;

import com.gmail.nossr50.datatypes.skills.SubSkill;
import com.gmail.nossr50.util.skills.SubSkillActivationType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.inventory.ItemStack;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.SuperAbility;
import com.gmail.nossr50.datatypes.skills.PrimarySkill;
import com.gmail.nossr50.datatypes.skills.ToolType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.skills.SkillManager;
import com.gmail.nossr50.util.ItemUtils;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.skills.CombatUtils;
import com.gmail.nossr50.util.skills.ParticleEffectUtils;
import com.gmail.nossr50.util.skills.SkillUtils;

public class AxesManager extends SkillManager {
    public AxesManager(McMMOPlayer mcMMOPlayer) {
        super(mcMMOPlayer, PrimarySkill.AXES);
    }

    public boolean canUseAxeMastery() {
        return Permissions.isSubSkillEnabled(getPlayer(), SubSkill.AXES_AXE_MASTERY);
    }

    public boolean canCriticalHit(LivingEntity target) {
        return target.isValid() && Permissions.isSubSkillEnabled(getPlayer(), SubSkill.AXES_CRITICAL_HIT);
    }

    public boolean canImpact(LivingEntity target) {
        return target.isValid() && Permissions.isSubSkillEnabled(getPlayer(), SubSkill.AXES_ARMOR_IMPACT) && Axes.hasArmor(target);
    }

    public boolean canGreaterImpact(LivingEntity target) {
        return target.isValid() && Permissions.isSubSkillEnabled(getPlayer(), SubSkill.AXES_GREATER_IMPACT) && !Axes.hasArmor(target);
    }

    public boolean canUseSkullSplitter(LivingEntity target) {
        return target.isValid() && mcMMOPlayer.getAbilityMode(SuperAbility.SKULL_SPLITTER) && Permissions.skullSplitter(getPlayer());
    }

    public boolean canActivateAbility() {
        return mcMMOPlayer.getToolPreparationMode(ToolType.AXE) && Permissions.skullSplitter(getPlayer());
    }

    /**
     * Handle the effects of the Axe Mastery ability
     */
    public double axeMastery() {
        if (!SkillUtils.isActivationSuccessful(SubSkillActivationType.ALWAYS_FIRES, SubSkill.AXES_AXE_MASTERY, getPlayer(), null, 0, 0)) {
            return 0;
        }

        return Math.min(getSkillLevel() / (Axes.axeMasteryMaxBonusLevel / Axes.axeMasteryMaxBonus), Axes.axeMasteryMaxBonus);
    }

    /**
     * Handle the effects of the Critical Hit ability
     *
     * @param target The {@link LivingEntity} being affected by the ability
     * @param damage The amount of damage initially dealt by the event
     */
    public double criticalHit(LivingEntity target, double damage) {
        if (!SkillUtils.isActivationSuccessful(SubSkillActivationType.RANDOM_LINEAR_100_SCALE_WITH_CAP, SubSkill.AXES_CRITICAL_HIT, getPlayer(), this.skill, getSkillLevel(), activationChance)) {
            return 0;
        }

        Player player = getPlayer();

        if (mcMMOPlayer.useChatNotifications()) {
            player.sendMessage(LocaleLoader.getString("Axes.Combat.CriticalHit"));
        }

        if (target instanceof Player) {
            Player defender = (Player) target;

            if (UserManager.getPlayer(defender).useChatNotifications()) {
                defender.sendMessage(LocaleLoader.getString("Axes.Combat.CritStruck"));
            }

            damage = (damage * Axes.criticalHitPVPModifier) - damage;
        }
        else {
            damage = (damage * Axes.criticalHitPVEModifier) - damage;
        }

        return damage;
    }

    /**
     * Handle the effects of the Impact ability
     *
     * @param target The {@link LivingEntity} being affected by Impact
     */
    public void impactCheck(LivingEntity target) {
        int durabilityDamage = 1 + (getSkillLevel() / Axes.impactIncreaseLevel);

        for (ItemStack armor : target.getEquipment().getArmorContents()) {
            if (armor != null && ItemUtils.isArmor(armor)) {
                if (SkillUtils.isActivationSuccessful(SubSkillActivationType.RANDOM_STATIC_CHANCE, SubSkill.AXES_ARMOR_IMPACT, getPlayer(), this.skill, getSkillLevel(), activationChance)) {
                    SkillUtils.handleDurabilityChange(armor, durabilityDamage, Axes.impactMaxDurabilityModifier);
                }
            }
        }
    }

    /**
     * Handle the effects of the Greater Impact ability
     *
     * @param target The {@link LivingEntity} being affected by the ability
     */
    public double greaterImpact(LivingEntity target) {
        //static chance (3rd param)
        if (!SkillUtils.isActivationSuccessful(SubSkillActivationType.RANDOM_STATIC_CHANCE, SubSkill.AXES_GREATER_IMPACT, getPlayer(), this.skill, getSkillLevel(), activationChance)) {
            return 0;
        }

        Player player = getPlayer();

        ParticleEffectUtils.playGreaterImpactEffect(target);
        target.setVelocity(player.getLocation().getDirection().normalize().multiply(Axes.greaterImpactKnockbackMultiplier));

        if (mcMMOPlayer.useChatNotifications()) {
            player.sendMessage(LocaleLoader.getString("Axes.Combat.GI.Proc"));
        }

        if (target instanceof Player) {
            Player defender = (Player) target;

            if (UserManager.getPlayer(defender).useChatNotifications()) {
                defender.sendMessage(LocaleLoader.getString("Axes.Combat.GI.Struck"));
            }
        }

        return Axes.greaterImpactBonusDamage;
    }

    /**
     * Handle the effects of the Skull Splitter ability
     *
     * @param target The {@link LivingEntity} being affected by the ability
     * @param damage The amount of damage initially dealt by the event
     */
    public void skullSplitterCheck(LivingEntity target, double damage, Map<DamageModifier, Double> modifiers) {
        CombatUtils.applyAbilityAoE(getPlayer(), target, damage / Axes.skullSplitterModifier, modifiers, skill);
    }
}
