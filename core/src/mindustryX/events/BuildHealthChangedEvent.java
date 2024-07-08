package mindustryX.events;

import arc.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustryX.features.*;

/**
 * Called when a building is damaged whatever bullet or not.
 * Provide real change amount.
 */
public class BuildHealthChangedEvent{
    public static final BuildHealthChangedEvent buildHealthChangedEvent = new BuildHealthChangedEvent();
    private static boolean autoReset = true;

    public Building build;
    // TODO: More source tracked.
    public @Nullable Sized source;
    public DamageType type;
    public float amount;

    private BuildHealthChangedEvent(){

    }

    public BuildHealthChangedEvent setSource(Sized source){
        this.source = source;
        return this;
    }

    public BuildHealthChangedEvent setType(DamageType type){
        this.type = type;
        return this;
    }

    public BuildHealthChangedEvent startWrap(){
        autoReset = false;
        return this;
    }

    public BuildHealthChangedEvent endWrap(){
        autoReset = true;
        reset();
        return this;
    }

    public BuildHealthChangedEvent fire(Building build, float amount){
        if(type == null){ // default normal
            type = DamageType.normal;
        }

        this.build = build;
        this.amount = amount;
        Events.fire(this);

        if(autoReset){
            reset();
        }
        return this;
    }

    public BuildHealthChangedEvent reset(){
        setSource(null);
        setType(DamageType.normal);
        return this;
    }
}
