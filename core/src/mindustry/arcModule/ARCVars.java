package mindustry.arcModule;

import arc.*;
import arc.graphics.*;
import arc.struct.*;
import mindustry.*;
import mindustry.arcModule.ui.*;
import mindustry.core.*;
import mindustry.game.*;

import static arc.Core.settings;

public class ARCVars{
    public static ARCUI arcui = new ARCUI();
    public static final int minimapSize = 40;
    public static boolean unitHide = false;
    public static boolean limitUpdate = false;
    public static int limitDst = 0;

    /** ARC */
    public static String arcVersion = Version.mdtXBuild;
    public static String arcVersionPrefix = "<ARC~" + arcVersion + ">";
    public static int changeLogRead = 18;
    public static Seq<District.advDistrict> districtList = new Seq<>();
    /** 服务器远程控制允许或移除作弊功能 */
    public static Boolean arcCheatServer = false;

    public static Boolean arcInfoControl = false;

    public static int maxBuildPlans = 50;

    static{
        // 减少性能开销
        Events.run(EventType.Trigger.update, () -> {
            arcInfoControl = !arcCheatServer && (Core.settings.getBool("showOtherTeamState") ||
            Vars.player.team().id == 255 || Vars.state.rules.mode() != Gamemode.pvp);
        });
    }

    public static int getMaxSchematicSize(){
        int s = Core.settings.getInt("maxSchematicSize");
        return s == 501 ? Integer.MAX_VALUE : s;
    }

    public static int getMinimapSize(){
        return settings.getInt("minimapSize", minimapSize);
    }

    public static String getThemeColorCode(){
        return "[#" + getThemeColor() + "]";
    }

    public static Color getThemeColor(){
        try{
            return Color.valueOf(settings.getString("themeColor"));
        }catch(Exception e){
            return Color.valueOf("ffd37f");
        }
    }

    public static Color getPlayerEffectColor(){
        try{
            return Color.valueOf(settings.getString("playerEffectColor"));
        }catch(Exception e){
            return Color.valueOf("ffd37f");
        }
    }

    public static Boolean arcInfoControl(Team team){
        return team == Vars.player.team() || arcInfoControl;
    }
}
