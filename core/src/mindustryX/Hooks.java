package mindustryX;

import arc.*;
import arc.util.*;
import mindustryX.features.*;

public class Hooks implements ApplicationListener{
    @Override
    public void init(){
        Log.infoTag("MindustryX", "Hooks.init");
        RenderExt.init();
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
}
