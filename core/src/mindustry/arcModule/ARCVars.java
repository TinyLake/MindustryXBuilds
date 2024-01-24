package mindustry.arcModule;

import arc.*;
import arc.graphics.*;
import mindustry.*;
import mindustry.arcModule.ui.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.graphics.*;

import static arc.Core.settings;

public class ARCVars{
    public static final ARCUI arcui = new ARCUI();
    public static final int maxBuildPlans = 100;
    public static final String arcVersionPrefix = "<ARC~" + Version.mdtXBuild + ">";

    private static Boolean arcInfoControl = false;

    static{
        // 减少性能开销
        Events.run(EventType.Trigger.update, () -> {
            arcInfoControl = Core.settings.getBool("showOtherTeamState");
        });
    }

    public static Color getPlayerEffectColor(){
        try{
            return Color.valueOf(settings.getString("playerEffectColor"));
        }catch(Exception e){
            return Pal.accent;
        }
    }

    public static Boolean arcInfoControl(Team team){
        return team == Vars.player.team() || arcInfoControl ||
        Vars.player.team().id == 255 || Vars.state.rules.mode() != Gamemode.pvp;
    }
}
