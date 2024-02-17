package mindustryX;

import arc.*;

public class VarsX{
    public static boolean arcChoiceUiIcon;

    public static void update(){
        arcChoiceUiIcon = Core.settings.getBool("arcchoiceuiIcon");
    }
}
