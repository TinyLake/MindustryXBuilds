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

    public static float LIFETIME = 90f;
    public static final float MIN_SCALE = 1f / 4 / Scl.scl(1);
    public static final float MAX_SCALE = 1f / 2 / Scl.scl(1);

    private static final ObjectMap<Sized, Popup> popups = new ObjectMap<>();

    public static void init(){
        Events.on(BuildDamageEvent.class, e -> {
            if(e.source.owner == Vars.player.unit()
            || Vars.player.unit() instanceof BlockUnitUnit blockUnit && e.source.owner == blockUnit.tile()){
                popupDamage(e.source, e.build, e.damage);
            }
        });

        Events.on(UnitDamageEvent.class, e -> {
            if(e.bullet.owner == Vars.player.unit()
            || Vars.player.unit() instanceof BlockUnitUnit blockUnit && e.bullet.owner == blockUnit.tile()){
                popupDamage(e.bullet, e.unit, e.damage);
            }
        });

        Events.run(Trigger.update, () -> {
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
            for(Popup popup : popups.values()){
                popup.draw();
            }
        });
    }

    private static void popupDamage(Sized source, Sized damaged, float damage){
        if(Mathf.equal(damage, 0f)) return;

        float x = damaged.getX(), y = damaged.getY() + Mathf.range(damaged.hitSize() * 0.25f);
        float scale = Mathf.clamp(damaged.hitSize() / 64 / Scl.scl(1), MIN_SCALE, MAX_SCALE);
        Color color = damage > 0 ? Pal.health : Pal.heal;
        float rotation = damaged.angleTo(source) + Mathf.random(5f) + (damage > 0 ? 180 : 0);
        float offset = Mathf.random(8, 12);

        Popup popup = popups.get(damaged);
        if(popup == null){
            popup = popupPool.obtain().set(x, y, LIFETIME, damage, color, 1f, scale, rotation, offset);
            popups.put(damaged, popup);
        }else{
            popup.damage += damage;
            popup.count ++;
        }
    }

    private static class Popup implements Poolable{
        public static final int MAX_DAMAGE_EFFECT = 7_000;
        public static final int MAX_COUNT_EFFECT = 40;

        // data
        public Font font = Fonts.outline;
        public float x, y;
        public float lifetime;
        public float damage;
        public float alpha;
        public float scale;
        public float offset;
        public float rotation; // deg
        public Color color;

        private float time;
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
            float fin = time / lifetime;

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
            float scale = this.scale * countEffect() / 2 * easeOutDown;

            float offset = this.offset * easeOutExpo;

            float fy = y + offset * Mathf.sin(rotation * Mathf.degRad),
            fx = x + offset * Mathf.cos(rotation * Mathf.degRad);

            c1.set(color).a(alpha);

            Draw.z(Layer.overlayUI);
            font.draw(Strings.autoFixed(damage, 1), fx, fy, c1, scale, false, Align.center);
            Draw.reset();
        }

        public boolean dead(){
            return time >= lifetime;
        }

        public void update(){
            time += Time.delta / countEffect();
        }

        public float countEffect(){
            return 1f + 2.5f * Math.min(Math.abs(damage), MAX_DAMAGE_EFFECT) / MAX_DAMAGE_EFFECT + 2f * Mathf.clamp(count, 1, MAX_COUNT_EFFECT) / MAX_COUNT_EFFECT;
        }

        @Override
        public void reset(){
            time = 0f;
            count = 0;
        }
    }
}
