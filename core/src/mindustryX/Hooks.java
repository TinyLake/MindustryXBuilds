package mindustryX;

import arc.*;
import arc.util.*;
import mindustry.*;
import mindustry.arcModule.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustryX.features.Settings;
import mindustryX.features.*;

import java.net.*;

import static arc.Core.*;

public class Hooks implements ApplicationListener{
    /** invoke before `Vars.init`. Note that may be executed from `Vars.loadAsync` */
    public static void beforeInit(){
        Log.infoTag("MindustryX", "Hooks.beforeInit");
        Settings.baseSettings();
        DebugUtil.init();//this is safe, and better at beforeInit,
    }

    /** invoke after loading, just before `Mod::init` */
    @Override
    public void init(){
        Log.infoTag("MindustryX", "Hooks.init");
        RenderExt.init();
        ArcOld.addSettings();
        Settings.addSettings();
        if(AutoUpdate.INSTANCE.getActive())
            AutoUpdate.INSTANCE.checkUpdate();
        if(!Vars.headless){
            TimeControl.init();
            UIExt.init();
        }
    }

    @SuppressWarnings("unused")//call before arc.util.Http$HttpRequest.block
    public static void onHttp(Http.HttpRequest req){
        if(Core.settings.getBool("githubMirror")){
            try{
                String url = req.url;
                String host = new URL(url).getHost();
                if(host.contains("github.com") || host.contains("raw.githubusercontent.com")){
                    url = "https://gh.tinylake.tech/" + url;
                    req.url = url;
                }
            }catch(Exception e){
                //ignore
            }
        }
    }

    public static @Nullable String onHandleSendMessage(String message, @Nullable Player sender){
        if(message == null) return null;
        if(Vars.ui != null){
            if(MarkerType.resolveMessage(message)) return message;
            try{
                ARCVars.arcui.MessageDialog.resolveMsg(message, sender);
                if(sender != null){
                    message = (sender.unit().isNull() ? Iconc.alphaaaa : sender.unit().type.emoji()) + " " + message;
                }
            }catch(Exception e){
                Log.err(e);
            }
        }
        return message;
    }

    @Override
    public void update(){
        pollKeys();
    }

    public static void pollKeys(){
        if(Core.scene.hasField()) return;
        if(Core.input.keyTap(Binding.toggle_unit)){
            RenderExt.unitHide = !RenderExt.unitHide;
        }
        if(Core.input.keyTap(Binding.lockonLastMark)){
            MarkerType.lockOnLastMark();
        }
        if(Core.input.keyTap(Binding.point)){
            MarkerType.selected.markWithMessage(Core.input.mouseWorld());
        }
        if(input.keyTap(Binding.toggle_block_render)){
            settings.put("blockRenderLevel", (RenderExt.blockRenderLevel + 1) % 3);
        }
    }
}
