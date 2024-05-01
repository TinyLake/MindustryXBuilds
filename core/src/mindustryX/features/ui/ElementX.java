package mindustryX.features.ui;

import arc.flabel.*;
import arc.graphics.*;
import arc.scene.ui.layout.*;
import mindustry.graphics.*;
import mindustry.ui.*;

public class ElementX{
    public static void setLoading(Table table){
        table.clearChildren();
        table.add(new FLabel("@alphaLoading")).style(Styles.outlineLabel).expand().center();
    }

    public static void setLoadFailed(Table table){
        table.clearChildren();
        table.add(new FLabel("@alphaLoadFailed")).style(Styles.outlineLabel).expand().center();
    }

    public static void cardShadow(Table table){
        cardShadow(table, 8f, Pal.darkerGray);
    }

    /**
     * 为单行表格添加阴影
     * 由于表格没有rowspan所以只能为单行表格添加
     * @param table 添加阴影的表格
     * @param size 阴影大小
     * @param color 阴影的颜色
     */
    public static void cardShadow(Table table, float size, Color color){
        table.image().width(size).color(color).growY().right();

        table.row();

        Cell<?> horizontalLine = table.image().height(size).color(color).growX();
        horizontalLine.colspan(table.getColumns());
    }

}
