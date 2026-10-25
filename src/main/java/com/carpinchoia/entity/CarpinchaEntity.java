package com.carpinchoia.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class CarpinchaEntity extends CarpinchoEntity {
    public CarpinchaEntity(EntityType<? extends CarpinchoEntity> type, Level level) { super(type, level); }
    @Override public boolean isFemale() { return true; }
}
