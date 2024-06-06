package mindustryX.features;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import arc.util.pooling.*;
import arc.util.pooling.Pool.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

import static arc.util.Tmp.*;

/**
 * 玩家子弹伤害跳字
 * Create by 2024/6/5
 */
public class DamagePopup{
    private static final Pool<Popup> popupPool = Pools.get(Popup.class, Popup::new);
    private static final ObjectMap<Sized, Popup> popups = new ObjectMap<>();

    public static final float MIN_SCALE = 1f / 4 / Scl.scl(1);
    public static final float MAX_SCALE = 1f / 2 / Scl.scl(1);

    public static float popupLifetime = 50f;

    // 设置
    public static boolean enable;
    public static boolean playerPopupOnly;
    public static float popupMinHealth;

    public static void init(){
        Events.on(BuildDamageEvent.class, e -> {
            if(enable && shouldPopup(e.source, e.build)){
                popupDamage(e.source, e.build, e.damage);
            }
        });

        Events.on(UnitDamageEvent.class, e -> {
            if(enable && shouldPopup(e.bullet, e.unit)){
                popupDamage(e.bullet, e.unit, e.damage);
            }
        });

        Events.run(Trigger.update, () -> {
            updateSettings();

            if(Vars.state.isPaused()) return;
            if(popups.isEmpty()) return;

            Values<Popup> values = popups.values();

            while(values.hasNext()){
                Popup popup = values.next();

                if(popup.dead()){
                    popupPool.free(popup);
                    values.remove();
                }else{
                    popup.update();
                }
            }
        });

        Events.run(Trigger.draw, () -> {
            if(popups.isEmpty()) return;

            for(Popup popup : popups.values()){
                popup.draw();
            }
        });

        Events.on(ResetEvent.class, e -> {
            clearPopup();
        });
    }

    private static void updateSettings(){
        boolean enableSetting = Core.settings.getBool("damagePopup");
        playerPopupOnly = Core.settings.getBool("playerPopupOnly");
        popupMinHealth = Core.settings.getInt("popupMinHealth");

        if(enable != enableSetting){
            enable = enableSetting;

            // 关闭后保留已有跳字
            // if(!enable) clearPopup();
        }
    }

    public static void clearPopup(){
        Values<Popup> values = popups.values();
        popupPool.freeAll(values.toSeq());
        popups.clear();
    }

    private static Entityc getSourceOwner(Bullet bullet){
        Entityc current = bullet.owner;

        while(current instanceof Bullet b && b.owner != null){
            current = b.owner;
        }

        return current;
    }

    private static boolean shouldPopup(Bullet bullet, Healthc damaged){
        if(damaged.maxHealth() < popupMinHealth){
            return false;
        }

        if(!playerPopupOnly) return true;

        Entityc owner = getSourceOwner(bullet);
        Unit playerUnit = Vars.player.unit();

        return owner == playerUnit
        || (playerUnit instanceof BlockUnitUnit blockUnit && owner == blockUnit.tile())
        || (Vars.control.input.commandMode &&
            (owner instanceof Unit unitOwner && Vars.control.input.selectedUnits.contains(unitOwner)
            || (owner instanceof Building buildOwner && Vars.control.input.commandBuildings.contains(buildOwner))));
    }

    private static void popupDamage(Sized source, Sized damaged, float damage){
        if(Mathf.equal(damage, 0f)) return;

        float x = damaged.getX(), y = damaged.getY() + Mathf.range(damaged.hitSize() * 0.25f);
        float scale = Mathf.clamp(damaged.hitSize() / 32 / Scl.scl(1), MIN_SCALE, MAX_SCALE);
        Color color = damage > 0 ? Pal.health : Pal.heal;
        float rotation = damaged.angleTo(source) + Mathf.random(5f) + (damage > 0 ? 180 : 0);
        float offset = Mathf.random(8, 12);

        Popup popup = popups.get(damaged);
        if(popup == null){
            popup = popupPool.obtain().set(x, y, popupLifetime, damage, color, 1f, scale, rotation, offset);
            popups.put(damaged, popup);
        }else{
            popup.superposeDamage(damage);
        }
    }

    private static class Popup implements Poolable{
        public static float maxDamageEffect = 5_000;
        public static int maxCountEffect = 50;
        public static float damageEffectScl = 5f;
        public static float countEffectScl = 2f;

        // data
        public Font font = Fonts.outline;
        public float x, y;
        public float lifetime;
        public float alpha;
        public float scale;
        public float offset;
        public float rotation; // deg
        public Color color;

        public float damage;
        private float timer;
        public int count;

        public Popup set(float x, float y, float lifetime, float damage, Color color, float alpha, float scale, float rotation, float offset){
            this.x = x;
            this.y = y;
            this.lifetime = lifetime;
            this.damage = damage;
            this.color = color;
            this.alpha = alpha;
            this.scale = scale;
            this.rotation = rotation;
            this.offset = offset;

            return this;
        }

        public void draw(){
            float fin = timer / lifetime;

            float easeOutDown = Bezier.quadratic(v1, fin,
            v2.set(1f, 1f),
            v3.set(0.565f, 1f),
            v4.set(0.39f, 0.575f),
            v5.set(0.5f, 0.5f)).y;
            float easeOutExpo = Bezier.quadratic(v1, fin,
            v2.set(0f, 0f),
            v3.set(0.19f, 1f),
            v4.set(0.22f, 1f),
            v5.set(1f, 1f)).y;

            float alpha = this.alpha * easeOutDown;
            float scale = this.scale * easeOutDown * Math.max(1, effect() / 3);

            float offset = this.offset * easeOutExpo;

            float fy = y + offset * Mathf.sin(rotation * Mathf.degRad),
            fx = x + offset * Mathf.cos(rotation * Mathf.degRad);

            c1.set(color).a(alpha);

            Draw.z(Layer.overlayUI);
            font.draw(Strings.autoFixed(damage, 1), fx, fy, c1, scale, false, Align.center);
            Draw.reset();
        }

        public boolean dead(){
            return timer >= lifetime;
        }

        public void update(){
            timer += Time.delta / effect();
        }

        public float effect(){
            float damageEffect = damageEffectScl * Math.min(Math.abs(damage), maxDamageEffect) / maxDamageEffect;
            float countEffect = countEffectScl * (float)Mathf.clamp(count, 1, maxCountEffect) / maxCountEffect;
            return 1f + damageEffect + countEffect;
        }

        public void superposeDamage(float damage){
            this.damage += damage;
            count++;
        }

        @Override
        public void reset(){
            timer = 0f;
            damage = 0f;
            count = 0;
        }
    }
}
