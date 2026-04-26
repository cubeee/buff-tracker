package com.github.cubeee.bufftracker.model;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

import java.util.BitSet;

import static com.github.cubeee.bufftracker.model.BuffCondition.varbit;
import static com.github.cubeee.bufftracker.model.BuffCondition.varp;
import static com.github.cubeee.bufftracker.model.BuffIcon.item;
import static com.github.cubeee.bufftracker.model.BuffIcon.sprite;

/**
 * Every trackable buff. Groups are saved by constant name, so renaming a constant drops it from saved groups.
 */
@Getter
public enum Buff {
    // Potions
    STAMINA("Stamina", varbit(VarbitID.STAMINA_ACTIVE), item(ItemID._4DOSESTAMINA)),
    // Negative while immune to poison; antivenom pushes it below the venom immunity cutoff
    ANTIPOISON("Antipoison", varp(VarPlayerID.POISON, value -> value < 0), item(ItemID._4DOSEANTIPOISON)),
    ANTIVENOM("Antivenom", varp(VarPlayerID.POISON, value -> value < Buff.VENOM_IMMUNITY_CUTOFF), item(ItemID.ANTIVENOM4)),
    ANTIFIRE("Antifire", varbit(VarbitID.ANTIFIRE_POTION), item(ItemID._4DOSE1ANTIDRAGON)),
    SUPER_ANTIFIRE("Super antifire", varbit(VarbitID.SUPER_ANTIFIRE_POTION), item(ItemID._4DOSE3ANTIDRAGON)),
    MENAPHITE_REMEDY("Menaphite remedy", varbit(VarbitID.STATRENEWAL_POTION_TIMER), item(ItemID._4DOSESTATRENEWAL)),
    DIVINE_SUPER_ATTACK("Divine super attack", varbit(VarbitID.DIVINEATTACK_POTION_TIME), item(ItemID._4DOSEDIVINEATTACK)),
    DIVINE_SUPER_STRENGTH("Divine super strength", varbit(VarbitID.DIVINESTRENGTH_POTION_TIME), item(ItemID._4DOSEDIVINESTRENGTH)),
    DIVINE_SUPER_DEFENCE("Divine super defence", BuffCondition.varbits(Buff::isDivineDefenceActive,
            VarbitID.DIVINEDEFENCE_POTION_TIME,
            VarbitID.DIVINECOMBAT_POTION_TIME,
            VarbitID.DIVINEBASTION_POTION_TIME,
            VarbitID.DIVINEBATTLEMAGE_POTION_TIME,
            VarbitID.MOONLIGHT_POTION_TIME
    ), item(ItemID._4DOSEDIVINEDEFENCE)),
    DIVINE_SUPER_COMBAT("Divine super combat", varbit(VarbitID.DIVINECOMBAT_POTION_TIME), item(ItemID._4DOSEDIVINECOMBAT)),
    DIVINE_RANGING("Divine ranging", varbit(VarbitID.DIVINERANGE_POTION_TIME), item(ItemID._4DOSEDIVINERANGE)),
    DIVINE_MAGIC("Divine magic", varbit(VarbitID.DIVINEMAGIC_POTION_TIME), item(ItemID._4DOSEDIVINEMAGIC)),
    DIVINE_BASTION("Divine bastion", varbit(VarbitID.DIVINEBASTION_POTION_TIME), item(ItemID._4DOSEDIVINEBASTION)),
    DIVINE_BATTLEMAGE("Divine battlemage", varbit(VarbitID.DIVINEBATTLEMAGE_POTION_TIME), item(ItemID._4DOSEDIVINEBATTLEMAGE)),
    MOONLIGHT_POTION("Moonlight potion", varbit(VarbitID.MOONLIGHT_POTION_TIME), item(ItemID._4DOSEMOONLIGHTPOTION)),
    GOADING_POTION("Goading potion", varbit(VarbitID.GOADING_POTION_TIMER), item(ItemID._4DOSEGOADING)),
    PRAYER_REGENERATION("Prayer regeneration", varbit(VarbitID.PRAYER_REGENERATION_POTION_TIMER), item(ItemID._4DOSE1PRAYER_REGENERATION)),
    // The timer is the cooldown before another dose works; present while it runs
    SURGE_POTION("Surge potion", varbit(VarbitID.SURGE_POTION_TIMER), item(ItemID._4DOSESURGE)),
    // Nightmare Zone, Chambers of Xeric and Deadman overloads each have their own varbit
    OVERLOAD("Overload", BuffCondition.anyVarbit(
            VarbitID.NZONE_OVERLOAD_POTION_EFFECTS,
            VarbitID.RAIDS_OVERLOAD_TIMER,
            VarbitID.DEADMAN_OVERLOAD_POTION_EFFECTS
    ), item(ItemID.NZONE4DOSEOVERLOADPOTION)),
    // Tombs of Amascut
    SMELLING_SALTS("Smelling salts", varbit(VarbitID.TOA_MIDRAIDLOOT_STATS_TIMER), item(ItemID.TOA_SUPPLY_STATS_2)),

    // Arceuus spells
    THRALL("Thrall", varbit(VarbitID.ARCEUUS_RESURRECTION_ACTIVE), sprite(SpriteID.MagicNecroOn.RESURRECT_GREATER_ZOMBIE)),
    SHADOW_VEIL("Shadow Veil", varbit(VarbitID.ARCEUUS_SHADOW_VEIL_ACTIVE), sprite(SpriteID.MagicNecroOn.SHADOW_VEIL)),
    // Set when cast, cleared when it restores special attack energy
    DEATH_CHARGE("Death Charge", varbit(VarbitID.ARCEUUS_DEATH_CHARGE_ACTIVE), sprite(SpriteID.MagicNecroOn.DEATH_CHARGE)),

    // Lunar spells
    // Set when cast, cleared when it rebounds damage
    VENGEANCE("Vengeance", varbit(VarbitID.VENGEANCE_REBOUND), sprite(SpriteID.LunarMagicOn.VENGEANCE));

    private static final int VENOM_IMMUNITY_CUTOFF = -38;

    private static final BitSet TRACKED_VARBITS = new BitSet();
    private static final BitSet TRACKED_VARPS = new BitSet();

    static {
        for (Buff buff : values()) {
            for (int id : buff.condition.getVarbitIds()) {
                TRACKED_VARBITS.set(id);
            }
            for (int id : buff.condition.getVarpIds()) {
                TRACKED_VARPS.set(id);
            }
        }
    }

    private final String name;
    private final BuffCondition condition;
    private final BuffIcon icon;

    Buff(String name, BuffCondition condition, BuffIcon icon) {
        this.name = name;
        this.condition = condition;
        this.icon = icon;
    }

    public boolean isActive(Client client) {
        return condition.isActive(client);
    }

    /**
     * Whether a change to the var could change any buff's state. Varbit changes also pass their parent varp.
     */
    public static boolean isTracked(int varbitId, int varpId) {
        return varbitId >= 0 && TRACKED_VARBITS.get(varbitId) || varpId >= 0 && TRACKED_VARPS.get(varpId);
    }

    /**
     * The moonlight potion also sets the divine defence timer, roughly in sync with its own. A divine combination
     * potion lasting at least as long as the defence timer means the defence boost is divine.
     */
    private static boolean isDivineDefenceActive(Client client) {
        int defence = client.getVarbitValue(VarbitID.DIVINEDEFENCE_POTION_TIME);
        if (defence <= 0) {
            return false;
        }
        boolean fromCombination = client.getVarbitValue(VarbitID.DIVINECOMBAT_POTION_TIME) >= defence
                || client.getVarbitValue(VarbitID.DIVINEBASTION_POTION_TIME) >= defence
                || client.getVarbitValue(VarbitID.DIVINEBATTLEMAGE_POTION_TIME) >= defence;
        int moonlight = client.getVarbitValue(VarbitID.MOONLIGHT_POTION_TIME);
        boolean fromMoonlight = moonlight > 0 && moonlight + 1 >= defence;
        return fromCombination || !fromMoonlight;
    }

    /**
     * Returns null for names that no longer exist, e.g. from saved groups.
     */
    public static Buff fromId(String id) {
        if (id == null) {
            return null;
        }
        try {
            return valueOf(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
