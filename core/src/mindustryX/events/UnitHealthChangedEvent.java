package mindustryX.events;

import arc.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustryX.features.*;

/**
 * Called when a unit is damaged whatever bullet or not.
 * Provide real change amount.
 */
public class UnitHealthChangedEvent{
    public static final UnitHealthChangedEvent unitHealthChangedEvent = new UnitHealthChangedEvent();
    private static boolean autoReset = true;

    public Unit unit;
    // TODO: More source tracked.
    public @Nullable Sized source;
    public DamageType type;
    public float amount;

    private UnitHealthChangedEvent(){

    }

    public UnitHealthChangedEvent setSource(Sized source){
        this.source = source;
        return this;
    }

    public UnitHealthChangedEvent setType(DamageType type){
        this.type = type;
        return this;
    }

    public UnitHealthChangedEvent startWrap(){
        autoReset = false;
        return this;
    }

    public UnitHealthChangedEvent endWrap(){
        autoReset = true;
        reset();
        return this;
    }

    public UnitHealthChangedEvent fire(Unit unit, float amount){
        if(type == null){ // default normal
            type = DamageType.normal;
        }

        this.unit = unit;
        this.amount = amount;
        Events.fire(this);

        if(autoReset){
            reset();
        }
        return this;
    }

    public UnitHealthChangedEvent reset(){
        setSource(null);
        setType(DamageType.normal);
        return this;
    }
}
