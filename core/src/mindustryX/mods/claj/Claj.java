package mindustryX.mods.claj;

import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustryX.mods.claj.dialogs.*;

public class Claj extends Plugin{
    public JoinViaClajDialog joinViaClaj;
    public ManageRoomsDialog manageRooms;

    @Override
    public void init(){
        ClajIntegration.load();
        joinViaClaj = new JoinViaClajDialog();
        manageRooms = new ManageRoomsDialog();

        Table buttons = Vars.ui.join.buttons;
        buttons.button("通过claj代码加入游戏", Icon.play, joinViaClaj::show).self(cell -> {
            Seq<Cell> cells = buttons.getCells();
            cells.remove(cell);
            cells.insert(3, cell); // 插入到占位元素前
        });

        var pausedDialog = Vars.ui.paused;
        pausedDialog.shown(() -> {
            Table cont = pausedDialog.cont;

            int columns = cont.getColumns();

            cont.row();
            cont.button("管理claj房间", Icon.planet, () -> manageRooms.show()).size(Float.NEGATIVE_INFINITY, 60).colspan(columns).fill().disabled(b -> !Vars.net.server());
        });
    }
}