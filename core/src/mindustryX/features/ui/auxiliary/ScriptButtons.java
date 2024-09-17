package mindustryX.features.ui.auxiliary;

import arc.*;
import arc.func.*;
import arc.scene.style.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustryX.features.*;
import mindustryX.features.ui.*;

import static mindustry.Vars.*;

public class ScriptButtons extends AuxiliaryTools.Table{
    public ScriptButtons(){
        super(UnitTypes.gamma.uiIcon);
        defaults().size(40);

        button(new TextureRegionDrawable(Blocks.buildTower.uiIcon), RStyles.clearLineNonei, iconMed, () -> player.buildDestroyedBlocks()).tooltip("在建造列表加入被摧毁建筑");
        button(new TextureRegionDrawable(Items.copper.uiIcon), RStyles.clearLineNonei, iconMed, () -> player.dropItems()).tooltip("一键放置");
        addSettingButton(Icon.modeAttack, "autotarget", "自动攻击", null);
        addSettingButton(new TextureRegionDrawable(UnitTypes.vela.uiIcon), "forceBoost", "强制助推", null);
        addSettingButton(Icon.eyeSmall, "viewMode", "视角脱离玩家", s -> {
            if(s){
                if(control.input instanceof DesktopInput desktopInput){
                    desktopInput.panning = true;
                }
            }else{
                Core.camera.position.set(player);
            }
        });
    }

    protected void addSettingButton(Drawable icon, String settingName, String description, Boolc onClick){
        button(icon, RStyles.clearLineNoneTogglei, iconMed, () -> {
            boolean setting = Core.settings.getBool(settingName);

            Core.settings.put(settingName, !setting);
            UIExt.announce("已" + (setting ? "取消" : "开启") + description);

            if(onClick != null) onClick.get(!setting);
        }).tooltip(description).checked(b -> Core.settings.getBool(settingName));
    }
}
