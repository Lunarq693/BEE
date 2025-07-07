package com.lunar.raid;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;

public class ExampleCondition implements IEntityCondition {

    public ExampleCondition(MythicLineConfig config) {

    }

    @Override
    public boolean check(AbstractEntity target) {
        return target.isPlayer();
    }
}

