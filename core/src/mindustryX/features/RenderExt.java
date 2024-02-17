package mindustryX.features;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.logic.MessageBlock.*;

public class RenderExt{
    public static boolean bulletShow, showMineBeam, displayAllMessage;
    public static boolean arcChoiceUiIcon;
    public static boolean researchViewer;
    public static int hiddenItemTransparency;
    public static float overdriveZoneTransparency;

    public static void init(){
        Events.run(Trigger.update, () -> {
            bulletShow = Core.settings.getBool("bulletShow");
            showMineBeam = Core.settings.getBool("showminebeam");
            displayAllMessage = Core.settings.getBool("displayallmessage");
            arcChoiceUiIcon = Core.settings.getBool("arcchoiceuiIcon");
            researchViewer = Core.settings.getBool("researchViewer");
            hiddenItemTransparency = Core.settings.getInt("HiddleItemTransparency");
            overdriveZoneTransparency = Core.settings.getInt("overdrive_zone") / 100f;
        });
        Events.run(Trigger.draw, RenderExt::draw);
    }

    private static void draw(){

    }

    public static void onGroupDraw(Drawc t){
        if(!bulletShow && t instanceof Bulletc) return;
        if(!showMineBeam && t instanceof Minerc) return;
        t.draw();
        if(displayAllMessage && t instanceof MessageBuild build){
            Draw.draw(Layer.overlayUI - 0.1f, build::drawSelect);
        }
    }
}
