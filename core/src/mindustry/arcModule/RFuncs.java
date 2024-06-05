package mindustry.arcModule;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.fragments.*;
import mindustry.world.*;
import mindustry.world.blocks.logic.*;
import mindustryX.features.ui.*;

import static arc.graphics.Color.RGBtoHSV;
import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.arcui;

public class RFuncs{
    private static final int msgSeperator = 145;

    public static void sendChatMsg(String msg){
        for(int i = 0; i < msg.length() / (float)msgSeperator; i++){
            Call.sendChatMessage(msg.substring(i * msgSeperator, Math.min(msg.length(), (i + 1) * msgSeperator)));
        }
    }

    public static void colorizeContent(){
        if(!Core.settings.getBool("colorizedContent")) return;
        content.items().each(c -> colorizeContent(c, c.color));
        content.liquids().each(c -> colorizeContent(c, c.color));
        content.statusEffects().each(c -> colorizeContent(c, c.color));
        content.planets().each(c -> colorizeContent(c, c.atmosphereColor));
        content.blocks().each(c -> {
            if(c.hasColor) colorizeContent(c, blockColor(c));
            else if(c.itemDrop != null) colorizeContent(c, c.itemDrop.color);
        });
    }

    private static void colorizeContent(UnlockableContent c, Color color){
        c.localizedName = "[#" + color + "]" + c.localizedName + "[]";
    }

    private static Color blockColor(Block block){
        Color bc = new Color(0, 0, 0, 1);
        Color bestColor = new Color(0, 0, 0, 1);
        int highestS = 0;
        if(!block.synthetic()){
            PixmapRegion image = Core.atlas.getPixmap(block.fullIcon);
            for(int x = 0; x < image.width; x++)
                for(int y = 0; y < image.height; y++){
                    bc.set(image.get(x, y));
                    int s = RGBtoHSV(bc)[1] * RGBtoHSV(bc)[1] + RGBtoHSV(bc)[2] + RGBtoHSV(bc)[2];
                    if(s > highestS){
                        highestS = s;
                        bestColor = bc;
                    }
                }
        }else{
            return block.mapColor.cpy().mul(1.2f);
        }
        return bestColor;
    }

    public static void shareWaveInfo(int waves){
        if(!state.rules.waves) return;
        StringBuilder builder = new StringBuilder(getPrefix("orange", "Wave"));
        builder.append("标记了第").append(waves).append("波");
        if(waves < state.wave){
            builder.append("。");
        }else{
            if(waves > state.wave){
                builder.append("，还有").append(waves - state.wave).append("波");
            }
            int timer = (int)(state.wavetime + (waves - state.wave) * state.rules.waveSpacing);
            builder.append("[").append(fixedTime(timer)).append("]。");
        }

        builder.append(arcWaveInfo(waves));
        sendChatMsg(builder.toString());
    }

    public static String calWaveTimer(){
        StringBuilder waveTimer = new StringBuilder();
        waveTimer.append("[orange]");
        int m = ((int)state.wavetime / 60) / 60;
        int s = ((int)state.wavetime / 60) % 60;
        int ms = (int)state.wavetime % 60;
        if(m > 0){
            waveTimer.append(m).append("[white]: [orange]");
            if(s < 10){
                waveTimer.append("0");
            }
            waveTimer.append(s).append("[white]min");
        }else{
            waveTimer.append(s).append("[white].[orange]").append(ms).append("[white]s");
        }
        return waveTimer.toString();
    }

    public static String arcWaveInfo(int waves){
        StringBuilder builder = new StringBuilder();
        if(state.rules.attackMode){
            int sum = Math.max(state.teams.present.sum(t -> t.team != player.team() ? t.cores.size : 0), 1) + Vars.spawner.countSpawns();
            builder.append("包含(×").append(sum).append(")");
        }else{
            builder.append("包含(×").append(Vars.spawner.countSpawns()).append("):");
        }
        for(SpawnGroup group : state.rules.spawns){
            if(group.getSpawned(waves - 1) > 0){
                builder.append((char)Fonts.getUnicode(group.type.name)).append("(");
                if(group.effect != StatusEffects.invincible && group.effect != StatusEffects.none && group.effect != null){
                    builder.append((char)Fonts.getUnicode(group.effect.name)).append("|");
                }
                if(group.getShield(waves - 1) > 0){
                    builder.append(FormatDefault.format(group.getShield(waves - 1))).append("|");
                }
                builder.append(group.getSpawned(waves - 1)).append(")");
            }
        }
        return builder.toString();
    }

    public static String arcColorTime(int timer){
        return arcColorTime(timer, true);
    }

    public static String arcColorTime(int timer, boolean units){
        StringBuilder str = new StringBuilder();
        String color = timer > 0 ? "[orange]" : "[acid]";
        timer = Math.abs(timer);
        str.append(color);
        int m = timer / 60 / 60;
        int s = timer / 60 % 60;
        int ms = timer % 60;
        if(m > 0){
            str.append(m).append("[white]: ").append(color);
            if(s < 10){
                str.append("0");
            }

            str.append(s);
            if(units) str.append("[white]min");
        }else{
            str.append(s).append("[white].").append(color).append(ms);
            if(units) str.append("[white]s");
        }
        return str.toString();
    }

    public static String fixedTime(int timer, boolean units){
        StringBuilder str = new StringBuilder();
        int m = timer / 60 / 60;
        int s = timer / 60 % 60;
        int ms = timer % 60;
        if(m > 0){
            str.append(m).append(": ");
            if(s < 10){
                str.append("0");
            }

            str.append(s);
            if(units) str.append("min");
        }else{
            str.append(s).append(".").append(ms);
            if(units) str.append('s');
        }
        return str.toString();
    }

    public static String fixedTime(int timer){
        return fixedTime(timer, true);
    }

    public static StringBuilder getPrefix(String color, String type){
        StringBuilder prefix = new StringBuilder();
        if(ui.chatfrag.mode == ChatFragment.ChatMode.team) prefix.append("/t ");
        prefix.append(ARCVars.arcVersionPrefix);
        prefix.append("[").append(color).append("]");
        prefix.append("<").append(type).append(">");
        prefix.append("[white]");
        return prefix;
    }

    public static void worldProcessor(){
        Log.info("当前地图:@", state.map.name());
        int[] data = new int[3];
        Groups.build.each(b -> {
            if(b instanceof LogicBlock.LogicBuild lb && lb.block.privileged){
                data[0] += 1;
                data[1] += lb.code.split("\n").length + 1;
                data[2] += lb.code.length();
            }
        });
        Log.info("地图共有@个世处，总共@行指令，@个字符", data[0], data[1], data[2]);
        ui.announce(Strings.format("地图共有@个世处，总共@行指令，@个字符", data[0], data[1], data[2]), 10);
    }

    public static void uploadToWeb(Fi f, Cons<String> result){
        uploadToWebID(f, l -> result.get("http://124.220.46.174/api/get?id=" + l));
    }

    public static void uploadToWebID(Fi f, Cons<String> result){
        arcui.arcInfo("上传中，请等待...");
        Http.HttpRequest post = Http.post("http://124.220.46.174/api/upload");
        post.contentStream = f.read();
        post.header("filename", f.name());
        post.header("size", String.valueOf(f.length()));
        post.header("token", "3ab6950d5970c57f938673911f42fd32");
        post.timeout = 10000;
        post.error(e -> Core.app.post(() -> arcui.arcInfo("发生了一个错误:" + e.toString())));
        post.submit(r -> result.get(r.getResultAsString()));
    }
}
