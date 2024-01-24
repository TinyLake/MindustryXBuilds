package mindustry.ui.dialogs;

import arc.Core;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;

import static mindustry.Vars.*;

public class UpdateDialog extends BaseDialog{
    Seq<String> updatelog = new Seq<>();

    public UpdateDialog() {
        super("@updatedialog.button");

        shown(() -> {
            updatelog = Seq.with(Core.files.internal("updatelog").readString("UTF-8").split("\n"));
            Core.app.post(this::setup);
        });

        shown(this::setup);
        onResize(this::setup);

    }

    void setup(){
        cont.clear();
        buttons.clear();

        Table about = new Table();
        ScrollPane pane = new ScrollPane(about);

        cont.add("[cyan]CN-ARC Client by PVP学术交流群").row();
        cont.add("[acid]游戏版本更新日志及解读").row();
        for(String log : updatelog){
            about.add(log).left();
            about.row();
        }
        cont.add(pane).growX();
        cont.row();

        addCloseButton();
    }
}