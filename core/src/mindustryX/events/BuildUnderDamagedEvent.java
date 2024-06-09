package mindustryX.events;

import arc.*;
import arc.util.*;
import mindustry.gen.*;

/**
 * Called when a building is damaged whatever bullet or not.
 * Provide damage amount.
 */
public class BuildUnderDamagedEvent{
    private static final BuildUnderDamagedEvent damagedEvent = new BuildUnderDamagedEvent();

    public Building build;
    public @Nullable Bullet bullet;
    public float damage;

    private BuildUnderDamagedEvent(){

    }

    public static void setBullet(Bullet bullet){
        damagedEvent.bullet = bullet;
    }

    public static void fire(Building build, float damage){
        damagedEvent.build = build;
        damagedEvent.damage = damage;
        Events.fire(damagedEvent);
        setBullet(null);
    }
}
