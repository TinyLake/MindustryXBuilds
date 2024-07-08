package mindustry.entities.comp;

import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustryX.*;

@Component
abstract class HealthComp implements Entityc, Posc{
    static final float hitDuration = 9f;

    float health;
    transient float hitTime;
    transient float maxHealth = 1f;
    transient boolean dead;

    boolean isValid(){
        return !dead && isAdded();
    }

    float healthf(){
        return health / maxHealth;
    }

    @Override
    public void update(){
        hitTime -= Time.delta / hitDuration;
    }

    void killed(){
        //implement by other components
    }

    void kill(){
        if(dead) return;

        health = Math.min(health, 0);
        dead = true;
        killed();
        remove();
    }

    void heal(){
        dead = false;
        health = maxHealth;
    }

    boolean damaged(){
        return health < maxHealth - 0.001f;
    }

    /** Damage and pierce armor. */
    void damagePierce(float amount, boolean withEffect){
        damage(amount, withEffect);
    }

    /** Damage and pierce armor. */
    void damagePierce(float amount){
        damagePierce(amount, true);
    }

    void damage(float amount){
        health -= amount;

        if(amount != 0){
            onDamaged(amount);
        }

        hitTime = 1f;
        if(health <= 0 && !dead){
            kill();
        }
    }

    void damage(float amount, boolean withEffect){
        float pre = hitTime;

        damage(amount);

        if(!withEffect){
            hitTime = pre;
        }
    }

    void damageContinuous(float amount){
        damage(amount * Time.delta, hitTime <= -10 + hitDuration);
    }

    void damageContinuousPierce(float amount){
        damagePierce(amount * Time.delta, hitTime <= -20 + hitDuration);
    }

    @MindustryXApi
    void onDamaged(float damage){

    }

    @MindustryXApi
    void onHealed(float amount){

    }

    void clampHealth(){
        health = Math.min(health, maxHealth);
    }

    /** Heals by a flat amount. */
    void heal(float amount){
        float lastHealth = health;

        health += amount;
        clampHealth();

        float healAmount = health - lastHealth;
        if(healAmount != 0){
            onHealed(healAmount);
        }
    }

    /** Heals by a 0-1 fraction of max health. */
    void healFract(float amount){
        heal(amount * maxHealth);
    }
}
