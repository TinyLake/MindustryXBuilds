package mindustry.arcModule.ui.auxilliary;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import mindustry.arcModule.*;
import mindustry.arcModule.toolpack.arcScanner;
import mindustry.content.*;
import mindustry.gen.*;

import static arc.Core.settings;
import static mindustry.Vars.*;
import static mindustry.arcModule.ui.RStyles.*;

public class MobileToolTable extends BaseToolsTable{

    public MobileToolTable(){
        super(UnitTypes.emanate.uiIcon);

        if(mobile){
            toggle();
        }
    }

    @Override
    protected void setup(){
        defaults().size(40);

        toolButton(Icon.unitsSmall, "指挥模式", () -> {
            control.input.commandMode = !control.input.commandMode;
        }, b -> control.input.commandMode);

        toolButton(Icon.pause, "暂停建造", () -> {
            control.input.isBuilding = !control.input.isBuilding;
        }, b -> control.input.isBuilding);

        toolButton(StatusEffects.unmoving.uiIcon, "原地静止", () -> {
            boolean view = settings.getBool("viewMode");
            if(view) Core.camera.position.set(player);
            settings.put("viewMode", !view);
        }, b -> settings.getBool("viewMode"));

        toolButton(Icon.up, "捡起载荷", () -> {
            control.input.tryPickupPayload();
        });

        toolButton(Icon.down, "丢下载荷", () -> {
            control.input.tryDropPayload();
        });

        toolButton(Blocks.payloadConveyor.uiIcon, "进入传送带", () -> {
            Building build = world.buildWorld(player.unit().x, player.unit().y);
            if(build != null && player.unit().team() == build.team && build.canControlSelect(player.unit())){
                Call.unitBuildingControlSelect(player.unit(), build);
            }
        });

        toolButton(Blocks.radar.uiIcon, "雷达扫描", () -> {
            arcScanner.mobileRadar = !arcScanner.mobileRadar;
        });
    }

    private void toolButton(TextureRegion region, String describe, Runnable runnable){
        toolButton(new TextureRegionDrawable(region), describe, runnable);
    }

    private void toolButton(Drawable icon, String describe, Runnable runnable){
        ImageButton button = button(icon, clearAccentNonei, 30, runnable).get();
        ElementUtils.tooltip(button, describe);
    }

    private void toolButton(TextureRegion region, String describe, Runnable runnable, Boolf<ImageButton> checked){
        toolButton(new TextureRegionDrawable(region), describe, runnable, checked);
    }

    private void toolButton(Drawable icon, String describe, Runnable runnable, Boolf<ImageButton> checked){
        ImageButton button = button(icon, clearLineNoneTogglei, 30, runnable).checked(checked).get();

        ElementUtils.tooltip(button, describe);
    }

}
