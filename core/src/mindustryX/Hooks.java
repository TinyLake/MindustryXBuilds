package mindustryX;

import arc.*;
import arc.util.*;
import mindustry.gen.*;
import mindustryX.features.*;

public class Hooks implements ApplicationListener{
    @Override
    public void init(){
        Log.infoTag("MindustryX", "Hooks.init");
        RenderExt.init();
        ArcOld.addSettings();
    }

    @Override
    public void update(){
        VarsX.update();
    }

    @SuppressWarnings("unused")//call before arc.util.Http$HttpRequest.block
    public static void onHttp(Http.HttpRequest req){
        if(Core.settings.getBool("githubMirror")){
            String url = req.url;
            if(url.contains("github.com") || url.contains("raw.githubusercontent.com")){
                url = "https://gh.tinylake.tech/" + url;
                req.url = url;
            }
        }
    }

    public static @Nullable String onHandleSendMessage(String message, @Nullable Player sender){
        if(message == null) return null;
        return message;
    }
}
