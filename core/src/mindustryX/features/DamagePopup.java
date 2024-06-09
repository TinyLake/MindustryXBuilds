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
import mindustryX.events.*;

import static arc.util.Tmp.*;

/**
 * 玩家子弹伤害跳字
 * Create by 2024/6/5
 */
public class DamagePopup{
    private static final Pool<Popup> popupPool = Pools.get(Popup.class, Popup::new);
    private static final ObjectMap<Sized, Popup> popups = new ObjectMap<>();

    // 跳字初始缩放限制
    public static final float minScale = 1f / 4 / Scl.scl(1);
    public static final float maxScale = 1f / 2 / Scl.scl(1);

    // 无持续攻击的消退时间
    public static float popupLifetime = 60f;

    // 设置
    public static boolean enable;
    public static boolean playerPopupOnly;
    public static float popupMinHealth;

    public static void init(){
        Events.on(BuildUnderDamagedEvent.class, e -> {
            if(enable && shouldPopup(e.bullet, e.build)){
                popup(e.bullet, e.build, e.damage);
            }
        });

        Events.on(UnitUnderDamagedEvent.class, e -> {
            if(enable && shouldPopup(e.bullet, e.unit)){
                popup(e.bullet, e.unit, e.damage);
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

            Rect cameraBounds = Core.camera.bounds(r1).grow(4 * Vars.tilesize);

            for(Popup popup : popups.values()){
                if(cameraBounds.contains(popup.entity.getX(), popup.entity.getY())){
                    popup.draw();
                }
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

    private static Entityc getSourceOwner(Ownerc source){
        Entityc current = source.owner();

        while(current instanceof Ownerc o && o.owner() != null){
            current = o.owner();
        }

        return current;
    }

    private static boolean shouldPopup(Ownerc source, Healthc damaged){
        if(damaged.maxHealth() < popupMinHealth){
            return false;
        }

        if(source == null || !playerPopupOnly) return true;

        Entityc owner = getSourceOwner(source);
        Unit playerUnit = Vars.player.unit();

        return owner == playerUnit
        || (playerUnit instanceof BlockUnitUnit blockUnit && owner == blockUnit.tile())
        || (Vars.control.input.commandMode &&
            (owner instanceof Unit unitOwner && Vars.control.input.selectedUnits.contains(unitOwner)
            || (owner instanceof Building buildOwner && Vars.control.input.commandBuildings.contains(buildOwner))));
    }

    private static void popup(Sized source, Sized damaged, float amount){
        if(Mathf.equal(amount, 0f)) return;

        float offsetY = Mathf.range(damaged.hitSize() * 0.1f);
        float scale = Mathf.clamp(damaged.hitSize() / 64 / Scl.scl(1), minScale, maxScale);
        Color color = amount > 0 ? Pal.health : Pal.heal;
        float rotation = source != null
        ? damaged.angleTo(source) + Mathf.random(5f) + (amount > 0 ? 180 : 0)
        : Mathf.random(20);
        float offsetLength = Mathf.random(8, 12);

        Popup popup = popups.get(damaged);
        if(popup == null){
            popup = popupPool.obtain().set(damaged, 0, offsetY, popupLifetime, Math.abs(amount), color, 1f, scale, rotation, offsetLength);
            popups.put(damaged, popup);
        }else{
            popup.superposeAmount(Math.abs(amount));
        }
    }

    private static class Popup implements Poolable{
        public static float maxAmountEffect = 5_000;
        public static int maxCountEffect = 50;
        public static float amountEffect = 3f;
        public static float countEffect = 2f;
        public static float fontScaleEffectScl = 8f;
        public static float minDecelerateAmount = 300;

        // data
        public Font font = Fonts.outline;
        public Sized entity;
        public float offsetX, offsetY;
        public float lifetime;
        public float alpha;
        public float scale;
        public float offsetLength;
        public float rotation; // deg
        public Color color;

        public float amount;
        public int count;

        private float lastAmount;
        private float timer;
        private float decelerateTime;

        public Popup set(Sized entity, float offsetX, float offsetY, float lifetime, float amount, Color color, float alpha, float scale, float rotation, float offsetLength){
            this.entity = entity;

            this.offsetX = offsetX;
            this.offsetY = offsetY;

            this.lifetime = lifetime;
            this.amount = amount;
            this.color = color;
            this.alpha = alpha;
            this.scale = scale;
            this.rotation = rotation;
            this.offsetLength = offsetLength;

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
            float scale = this.scale * easeOutDown * Math.max(effect() / fontScaleEffectScl, 1);

            float offsetLength = this.offsetLength * easeOutExpo;

            float fx = entity.getX() + offsetX + offsetLength * Mathf.sin(rotation * Mathf.degRad),
            fy = entity.getY() + offsetY + offsetLength * Mathf.cos(rotation * Mathf.degRad);

            c1.set(color).a(alpha);

            Draw.z(Layer.overlayUI);
            font.draw(Strings.autoFixed(amount, 1), fx, fy, c1, scale, false, Align.center);
            Draw.reset();
        }

        public boolean dead(){
            return timer >= lifetime;
        }

        public void update(){
            float deltaAmount = amount - lastAmount;

            timer += Time.delta * (decelerateTime > 0 ? 0.1f : 1f);

            if(deltaAmount >= minDecelerateAmount){
                decelerateTime = lifetime / 2;
                lastAmount = amount;
            }

            if(decelerateTime > 0){
                decelerateTime = Math.max(0, decelerateTime - Time.delta);
            }
        }

        public float effect(){
            float damageEffect = Popup.amountEffect * Math.min(amount / maxAmountEffect, 1);
            float countEffect = Popup.countEffect * Math.min(count / maxCountEffect, 1);
            return 1f + damageEffect + countEffect;
        }

        public void superposeAmount(float amount){
            this.amount += amount;
            count++;
        }

        @Override
        public void reset(){
            entity = null;
            offsetX = 0;
            offsetY = 0;

            lifetime = 0;
            alpha = 0;
            scale = 0;
            offsetLength = 0;
            rotation = 0;
            color = Color.white;

            lastAmount = 0f;
            amount = 0f;
            count = 0;
            timer = 0f;
        }
    }
}
