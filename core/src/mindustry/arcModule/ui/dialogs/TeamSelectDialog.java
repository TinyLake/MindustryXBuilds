package mindustry.arcModule.ui.dialogs;

import arc.func.Boolf;
import arc.func.Cons;
import arc.scene.ui.Dialog;
import mindustry.game.Team;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class TeamSelectDialog extends BaseDialog {
    public TeamSelectDialog(Cons<Team> cons, Team selectedTeam) {
        this(cons, team -> team == selectedTeam, true);
    }

    public TeamSelectDialog(Cons<Team> cons, Boolf<Team> checked, boolean autoHide) {
        super("队伍选择器");

        cont.pane(td -> {
            for (Team team : Team.all) {
                if (team.id % 10 == 6) {
                    td.row();
                    td.add("队伍：" + team.id + "~" + (team.id + 9));
                }
                td.button(Tex.whiteui, Styles.clearTogglei, 36f, () -> {
                    cons.get(team);
                    if (autoHide) hide();
                }).pad(3f).checked(b -> checked.get(team)).size(50f).with(b -> b.getStyle().imageUpColor = team.color);
            }
        });

        addCloseButton();
    }
}
