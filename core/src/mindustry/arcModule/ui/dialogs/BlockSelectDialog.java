package mindustry.arcModule.ui.dialogs;

import arc.Core;
import arc.func.Boolf;
import arc.func.Cons;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.arcModule.ARCVars;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import mindustry.world.meta.BlockGroup;

import static mindustry.Vars.*;

public class BlockSelectDialog extends BaseDialog {

    private String searchBlock = "";
    private Table blockTable = new Table();

    public BlockSelectDialog(Boolf<Block> condition, Cons<Block> cons, Boolf<Block> checked) {
        this(condition, cons, checked, true);
    }

    public BlockSelectDialog(Boolf<Block> condition, Cons<Block> cons, Boolf<Block> checked, boolean autoHide) {
        super("方块选择器");
        rebuild(condition, cons, checked, autoHide);
        cont.pane(td -> {
            td.field("",t->{
                searchBlock = t.length() > 0 ? t.toLowerCase() : "";
                rebuild(condition, cons, checked, autoHide);
            }).maxTextLength(50).growX().get().setMessageText("搜索...");
            td.row();
            td.add(blockTable);
        });
        addCloseButton();
    }

    private void rebuild(Boolf<Block> condition, Cons<Block> cons, Boolf<Block> checked, boolean autoHide){
        blockTable.clear();
        blockTable.table(td->{
            Seq<Block> blocks = content.blocks().select(block -> condition.get(block) && (searchBlock.length()==0 || block.name.contains(searchBlock) || block.localizedName.contains(searchBlock)) &&(block.privileged || Core.settings.getBool("allBlocksReveal") || !block.isHidden())).sort(block -> block.group.ordinal());
            Seq<BlockGroup> blockGroups = blocks.map(block -> block.group).distinct();
            blockGroups.each(blockGroup -> {
                td.row();
                td.add(blockGroup.toString()).row();
                td.image().color(ARCVars.getThemeColor()).fillX().row();
                td.table(ttd -> blocks.select(block1 -> block1.group == blockGroup).each(block1 -> {
                    ttd.button(new TextureRegionDrawable(block1.uiIcon), Styles.cleari, iconSmall, () -> {
                        cons.get(block1);
                        if (autoHide) hide();
                    }).tooltip(block1.localizedName).pad(3f).checked(b -> checked.get(block1)).size(50f);
                    if (ttd.getChildren().size % 10 == 0) ttd.row();
                }));
            });
        });
    }
}
