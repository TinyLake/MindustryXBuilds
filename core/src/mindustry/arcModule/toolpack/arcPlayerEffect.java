package mindustry.arcModule.toolpack;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.util.*;
import mindustry.arcModule.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.dialogs.*;

import java.lang.reflect.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.arcui;
import static mindustry.arcModule.DrawUtilities.drawNSideRegion;
import static mindustry.arcModule.ElementUtils.*;

public class arcPlayerEffect{
    private static Effect playerEffect = Fx.none;
    private static float effectChance = 0;

    private static int effectTime = 0;

    private static int effectCooldown = 1;
    private static Effect refEffect = Fx.none;

    private static boolean show = true;
    private static int curCounter = 0;

    static{
        Events.run(EventType.Trigger.update, () -> {
            curCounter += 1;
            if(curCounter >= effectCooldown){
                curCounter = 0;
                show = true;
            }else show = false;
        });
        Events.on(EventType.WorldLoadEvent.class, event -> readConfig());
    }

    public static void arcPlayerEffectSetting(){
        Dialog efTable = new BaseDialog("玩家特效设置");
        arcSliderTable(efTable.cont, "特效概率", effectChance, 0, 1, 0.05f, i -> (int)(i * 100) + "%", s -> effectChance = s);
        arcSliderTableInt(efTable.cont, "特效间隔", effectCooldown, 1, 30, 1, i -> "每" + i + "帧", s -> effectCooldown = s);
        arcSliderTableInt(efTable.cont, "特效时长", (int)playerEffect.lifetime, 0, 300, 5, i -> i + "帧", s -> {
            playerEffect.lifetime = s;
            effectTime = s;
        });
        efTable.cont.pane(tt -> {
            for(Field field : Fx.class.getDeclaredFields()){
                if(field.getGenericType() != Effect.class) continue;
                Effect thisEffect;
                try{
                    thisEffect = (Effect)field.get(null);
                }catch(IllegalAccessException e){
                    throw new RuntimeException(e);
                }
                tt.check(field.getName(), refEffect == thisEffect, a -> {
                    playerEffect = thisEffect;
                    refEffect = thisEffect;
                    playerEffect.lifetime = effectTime;
                }).left().row();
            }
        });
        efTable.addCloseButton();
        efTable.buttons.button("保存", arcPlayerEffect::saveConfig);
        efTable.buttons.button("读取", arcPlayerEffect::readConfig);
        efTable.show();
    }

    private static void saveConfig(){
        Core.settings.put("arcPlayerEffectPro", effectChance);
        Core.settings.put("arcPlayerEffectTime", playerEffect.lifetime);
        Core.settings.put("arcPlayerEffectType", refEffect.id);
        Core.settings.put("arcPlayerEffectCooldown", effectCooldown);
        arcui.arcInfo("[cyan]已保存设置");
    }

    private static void readConfig(){
        effectChance = Core.settings.getFloat("arcPlayerEffectPro");
        refEffect = Effect.get(Core.settings.getInt("arcPlayerEffectType"));
        playerEffect = refEffect;
        playerEffect.lifetime = Core.settings.getFloat("arcPlayerEffectTime");
        effectTime = (int)playerEffect.lifetime;
        effectCooldown = Core.settings.getInt("arcPlayerEffectCooldown");
        arcui.arcInfo("[cyan]已读取设置");
    }

    public static void drawPlayerEffect(Unit unit){
        Color effectColor = unit.controller() == player ? ARCVars.getPlayerEffectColor() : unit.team.color;

        boolean drawCircle = (unit.controller() == player && Core.settings.getInt("superUnitEffect") != 0) || (unit.controller() instanceof Player && Core.settings.getInt("superUnitEffect") == 2);
        if(drawCircle){
            // 射程圈
            float curStroke = (float)Core.settings.getInt("playerEffectCurStroke") / 10f;

            Lines.stroke(Lines.getStroke() * curStroke);

            Draw.z(Layer.effect - 2f);
            Draw.color(effectColor);

            Tmp.v1.trns(unit.rotation - 90, unit.x, unit.y).add(unit.x, unit.y);

            if(curStroke > 0){
                for(int i = 0; i < 5; i++){
                    float rot = unit.rotation + i * 360f / 5 + Time.time * 0.5f;
                    Lines.arc(unit.x, unit.y, unit.type.maxRange, 0.14f, rot, (int)(50 + unit.type.maxRange / 10));
                }
            }
        }
        // 武器圈
        if(Core.settings.getInt("unitTargetType") > 0){
            Draw.z(Layer.effect);
            Draw.color(effectColor, 0.8f);
            Lines.stroke(1f);
            Lines.line(unit.x, unit.y, unit.aimX, unit.aimY);
            switch(Core.settings.getInt("unitTargetType")){
                case 1:
                    Lines.dashCircle(unit.aimX, unit.aimY, 8);
                    break;
                case 2:
                    Drawf.target(unit.aimX, unit.aimY, 6f, 0.7f, effectColor);
                    break;
                case 3:
                    Drawf.target2(unit.aimX, unit.aimY, 6f, 0.7f, effectColor);
                    break;
                case 4:
                    Drawf.targetc(unit.aimX, unit.aimY, 6f, 0.7f, effectColor);
                    break;
                case 5:
                    Drawf.targetd(unit.aimX, unit.aimY, 6f, 0.7f, effectColor);
                    break;
            }
        }

        // 特效轨迹
        if(show && Mathf.chanceDelta(effectChance)) playerEffect.at(unit.x, unit.y, effectColor);
        Draw.reset();

        //玩家专属特效
        if(unit.controller() == player){
            detailBuildMode();
        }
    }

    private static void detailBuildMode(){
        if(!Core.settings.getBool("arcBuildInfo")) return;
        if(control.input.droppingItem){
            Color color = player.within(Core.input.mouseWorld(control.input.getMouseX(), control.input.getMouseY()), itemTransferRange) ? Color.gold : Color.red;
            drawNSideRegion(player.unit().x, player.unit().y, 3, player.unit().type.buildRange, player.unit().rotation, color, 0.25f, player.unit().stack.item.uiIcon, false);
        }else if(control.input.isBuilding || control.input.selectedBlock() || !player.unit().plans().isEmpty()){
            drawNSideRegion(player.unit().x, player.unit().y, 3, player.unit().type.buildRange, player.unit().rotation, Pal.heal, 0.25f, Icon.wrench.getRegion(), true);
        }
    }

    static void drawControlTurret(){
        if(player.unit() instanceof BlockUnitc unitc){
            unitc.tile().drawSelect();
        }
    }
}