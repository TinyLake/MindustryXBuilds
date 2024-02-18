package mindustryX;

import arc.*;

public class VarsX{
    public static boolean
    arcChoiceUiIcon,
    researchViewer;
    ;

    public static void update(){
        arcChoiceUiIcon = Core.settings.getBool("arcchoiceuiIcon");
        researchViewer = Core.settings.getBool("researchViewer");
    }
}
