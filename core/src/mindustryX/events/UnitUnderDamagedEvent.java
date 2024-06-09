package mindustryX.events;

import arc.*;
import arc.util.*;
import mindustry.gen.*;

/**
 * Called when a unit is damaged whatever bullet or not.
 * Provide real damage amount.
 */
public class UnitUnderDamagedEvent{
    private static final UnitUnderDamagedEvent damagedEvent = new UnitUnderDamagedEvent();

    public Unit unit;
    public @Nullable Bullet bullet;
    public float damage;

    private UnitUnderDamagedEvent(){

    }

    public static void setBullet(Bullet bullet){
        damagedEvent.bullet = bullet;
    }

    public static void fire(Unit unit, float damage){
        damagedEvent.unit = unit;
        damagedEvent.damage = damage;
        Events.fire(damagedEvent);
        setBullet(null);
    }
}
