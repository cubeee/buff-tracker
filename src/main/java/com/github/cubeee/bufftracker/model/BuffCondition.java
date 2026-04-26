package com.github.cubeee.bufftracker.model;

import lombok.Getter;
import net.runelite.api.Client;

import java.util.function.*;

/**
 * Decides whether a buff is active from client variables. Evaluated on the client thread.
 */
public final class BuffCondition {

    private final Predicate<Client> active;
    // Every var the condition reads, so unrelated var changes can be ignored
    @Getter
    private final int[] varbitIds;
    @Getter
    private final int[] varpIds;

    private BuffCondition(Predicate<Client> active, int[] varbitIds, int... varpIds) {
        this.active = active;
        this.varbitIds = varbitIds;
        this.varpIds = varpIds;
    }

    public boolean isActive(Client client) {
        return active.test(client);
    }

    static BuffCondition varbit(int varbitId) {
        return varbit(varbitId, value -> value > 0);
    }

    static BuffCondition varbit(int varbitId, IntPredicate active) {
        return new BuffCondition(client -> active.test(client.getVarbitValue(varbitId)), new int[]{varbitId});
    }

    static BuffCondition varp(int varPlayerId, IntPredicate active) {
        return new BuffCondition(client -> active.test(client.getVarpValue(varPlayerId)), new int[0], varPlayerId);
    }

    /**
     * Active when any of the varbits is positive, e.g. variants of one buff tracked in different places.
     */
    static BuffCondition anyVarbit(int... varbitIds) {
        int[] ids = varbitIds.clone();
        return new BuffCondition(client -> {
            for (int id : ids) {
                if (client.getVarbitValue(id) > 0) {
                    return true;
                }
            }
            return false;
        }, ids);
    }

    /**
     * A condition with its own logic; it must only read the given varbits.
     */
    static BuffCondition varbits(Predicate<Client> active, int... varbitIds) {
        return new BuffCondition(active, varbitIds.clone());
    }
}
