package mindustry.arcModule;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.arcModule.ui.dialogs.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.arcui;
import static mindustry.arcModule.RFuncs.getPrefix;

public class District{

    public static final String ShareType = "[violet]<District>";

    /**
     * 冷却时间
     */
    public static final float heatTime = 60f;
    /**
     * 滞留时间
     */
    public static final float retainTime = 1800f;

    public static final Seq<District.advDistrict> districtList = new Seq<>();

    public static advDistrict voidDistrict = new advDistrict();   //仅用于赋值，不实际处理

    static{
        Events.run(EventType.WorldLoadEvent.class, () -> {
            districtList.clear();
        });
    }

    public static void unitSpawnMenu(){
        BaseDialog disSet = new BaseDialog("ARC-区域规划中心");

        disSet.cont.table(t -> {
            t.table(tt -> tt.add("区域规划器-用于单机|服务器在地图上设置、管理和交流某个区域的用途"));
            t.row();
            t.add("坐标设置").color(ARCVars.getThemeColor()).colspan(4).pad(10).padTop(15).padBottom(4).row();
            t.image().color(ARCVars.getThemeColor()).fillX().height(3).colspan(4).padTop(0).padBottom(10).row();
            t.labelWrap("自动填入上个划定的选择区（即复制蓝图的区域）").growX().center().row();
            t.table(tt -> {
                tt.add("A点： ");
                tt.table(tx -> {
                    tx.add("x= ");
                    TextField Ax = tx.field(voidDistrict.districtA.x + "", text -> {
                        voidDistrict.districtA.x = Math.min(Math.max(Float.parseFloat(text), 0f), state.map.width);
                    }).valid(Strings::canParseFloat).maxTextLength(8).get();

                    tx.add("  ,y= ");
                    TextField Ay = tx.field(voidDistrict.districtA.y + "", text -> {
                        voidDistrict.districtA.y = Math.min(Math.max(Float.parseFloat(text), 0f), state.map.height);
                    }).valid(Strings::canParseFloat).maxTextLength(8).get();

                    tx.button(StatusEffects.blasted.emoji(), () -> {
                        if(Marker.markList.size == 0) return;
                        voidDistrict.districtA.set(World.toTile(Marker.markList.peek().markPos.x), World.toTile(Marker.markList.peek().markPos.y));
                        Ax.setText(voidDistrict.districtA.x + "");
                        Ay.setText(voidDistrict.districtA.y + "");
                    }).tooltip(Marker.markList.size == 0 ? "[red]未标记" : ("选择上个标记点：" + World.toTile(Marker.markList.peek().markPos.x) + "," + World.toTile(Marker.markList.peek().markPos.y))).height(50f);
                });
                tt.row();
                tt.add("B点： ");
                tt.table(tx -> {
                    tx.add("x= ");
                    TextField Bx = tx.field(voidDistrict.districtB.x + "", text -> {
                        voidDistrict.districtB.x = Float.parseFloat(text);
                    }).valid(Strings::canParseFloat).maxTextLength(8).get();

                    tx.add("  ,y= ");
                    TextField By = tx.field(voidDistrict.districtB.y + "", text -> {
                        voidDistrict.districtB.y = Float.parseFloat(text);
                    }).valid(Strings::canParseFloat).maxTextLength(8).get();

                    tx.button(StatusEffects.blasted.emoji(), () -> {
                        if(Marker.markList.size == 0) return;
                        voidDistrict.districtB.set(World.toTile(Marker.markList.peek().markPos.x), World.toTile(Marker.markList.peek().markPos.y));
                        Bx.setText(voidDistrict.districtB.x + "");
                        By.setText(voidDistrict.districtB.y + "");
                    }).tooltip(Marker.markList.size == 0 ? "[red]未标记" : ("选择上个标记点：" + World.toTile(Marker.markList.peek().markPos.x) + "," + World.toTile(Marker.markList.peek().markPos.y))).height(50f);
                });

            });
            t.row();
            t.add("持续时间").color(ARCVars.getThemeColor()).colspan(4).pad(10).padTop(15).padBottom(4).row();
            t.image().color(ARCVars.getThemeColor()).fillX().height(3).colspan(4).padTop(0).padBottom(10).row();
            t.table(tt -> {
                TextField sField = tt.field("" + 30, text -> {
                    voidDistrict.duration = Float.parseFloat(text);
                }).valid(Strings::canParseFloat).tooltip("规划区持续时间(单位：秒)").maxTextLength(10).get();

                tt.add("秒");
                Slider sSlider = tt.slider(30f, 300f, 30f, voidDistrict.duration, n -> {
                    if(voidDistrict.duration != n){//用一种神奇的方式阻止了反复更新
                        sField.setText(voidDistrict.duration + "");
                    }
                    voidDistrict.duration = n;
                }).get();
                sField.update(() -> sSlider.setValue(voidDistrict.duration));
            });

            t.row();
            t.add("规划项目").color(ARCVars.getThemeColor()).colspan(4).pad(10).padTop(15).padBottom(4).row();
            t.image().color(ARCVars.getThemeColor()).fillX().height(3).colspan(4).padTop(0).padBottom(10).row();
            t.table(tt -> {
                //tt.add("图标：");
                //tt.button(UnitTypes.gamma.emoji(),()->showNewIconTag(voidDistrict));
                tt.add("标签：");
                tt.field(voidDistrict.districtType.districtName, text -> {
                    voidDistrict.districtType.districtName = text;
                }).tooltip("规划区的标签条").maxTextLength(10).width(300f);
            });
            t.row();
            t.add("规划区设置").color(ARCVars.getThemeColor()).colspan(4).pad(10).padTop(15).padBottom(4).row();
            t.image().color(ARCVars.getThemeColor()).fillX().height(3).colspan(4).padTop(0).padBottom(10).row();
            t.button("发布规划区!", () -> {
                if(!Core.settings.getBool("cheating_mode") && (voidDistrict.districtA.x - voidDistrict.districtB.x > 100f || voidDistrict.districtA.y - voidDistrict.districtB.y > 100f)){
                    arcui.arcInfo("请勿发布无意义巨大规划区");
                }else Call.sendChatMessage(voidDistrict.toString());
            }).fillX();
        });

        disSet.addCloseButton();
        disSet.shown(District::setVoidDistrict);
        disSet.show();
    }

    public static void setVoidDistrict(){
        voidDistrict.districtA.x = control.input.lastSelection.x;
        voidDistrict.districtA.y = control.input.lastSelection.y;
        voidDistrict.districtB.x = control.input.lastSelection.x + control.input.lastSelection.width;
        voidDistrict.districtB.y = control.input.lastSelection.y + control.input.lastSelection.height;
    }

    public static boolean resolveMessage(String text){

        advDistrict resolveDistrict = new advDistrict();

        if(!text.contains(ShareType)){
            return false;
        }
        int Indexer = text.indexOf(ShareType) + ShareType.length();

        resolveDistrict.message = text;

        int districtTypeHand = text.indexOf('{', Indexer + 1);
        int districtTypeTail = text.indexOf('}', Indexer + 2);
        if(districtTypeHand == -1 || districtTypeTail + 1 < districtTypeHand) return false;
        resolveDistrict.districtType.districtName = text.substring(districtTypeHand + 1, districtTypeTail);

        Indexer = districtTypeTail;
        int locAHand = text.indexOf('(', Indexer + 1);
        int locATail = text.indexOf(')', Indexer + 2);
        Vec2 pos = Tmp.v1;
        try{
            pos.fromString(text.substring(locAHand, locATail + 1));
        }catch(Throwable e){
            Log.err("Cannot resolve position");
            return false;
        }
        resolveDistrict.districtA = new Vec2(pos);

        Indexer = locATail;
        int locBHand = text.indexOf('(', Indexer + 1);
        int locBTail = text.indexOf(')', Indexer + 2);

        try{
            pos.fromString(text.substring(locBHand, locBTail + 1));
        }catch(Throwable e){
            Log.err("Cannot resolve position");
            return false;
        }
        resolveDistrict.districtB = new Vec2(pos);

        districtList.add(new advDistrict(resolveDistrict));
        ARCVars.districtList = districtList;
        MessageDialog.addMsg(new MessageDialog.advanceMsg(MessageDialog.arcMsgType.district, text, resolveDistrict.center()));
        return true;
    }

    public static void drawDistrict(){
        if(districtList.size <= 0) return;
        for(advDistrict advDistrict : districtList){
            if(Time.time - advDistrict.time > advDistrict.duration * 60f) continue;
            advDistrict.draw();
        }
    }

    static void showNewIconTag(advDistrict district){
        new Dialog("arc-区域规划中心"){{
            closeOnBack();
            setFillParent(true);

            cont.pane(t -> {
                resized(true, () -> {
                    t.clearChildren();
                    t.marginRight(19f);
                    t.defaults().size(48f);

                    int cols = (int)Math.min(20, Core.graphics.getWidth() / Scl.scl(52f));

                    for(ContentType ctype : defaultContentIcons){
                        t.row();
                        t.image().colspan(cols).growX().width(Float.NEGATIVE_INFINITY).height(3f).color(Pal.accent);
                        t.row();

                        int i = 0;
                        for(UnlockableContent u : content.getBy(ctype).<UnlockableContent>as()){
                            if(!u.isHidden() && u.unlockedNow() && u.hasEmoji()){
                                t.button(new TextureRegionDrawable(u.uiIcon), Styles.flati, iconMed, () -> {
                                    district.districtType.districtType = u;

                                    hide();
                                });

                                if(++i % cols == 0) t.row();
                            }
                        }
                    }
                });
            });
            buttons.button("@back", Icon.left, this::hide).size(210f, 64f);
        }}.show();
    }

    public static void districtSettingDialog(){
        Dialog disSet = new BaseDialog("规划区设置");
        Runnable[] rebuild = {null};
        Table t = disSet.cont;
        rebuild[0] = () -> {
            t.clear();
            if(districtList.size <= 0) return;
            t.add("区域名称");
            t.add("坐标");
            t.row();
            for(advDistrict advDistrict : districtList){
                t.add(advDistrict.districtType.getName());
                t.add(advDistrict.getLoc());
                t.button("[red]×", () -> {
                    districtList.remove(advDistrict);
                    rebuild[0].run();
                });
                t.row();
            }
        };
        rebuild[0].run();
        disSet.addCloseButton();
        disSet.show();
    }

    public static class advDistrict{
        public District.districtType districtType = new districtType("");
        public String message;
        public Float time;
        public Float duration = 300f;
        public String creator;
        public Vec2 districtA = new Vec2();
        public Vec2 districtB = new Vec2();

        public advDistrict(District.districtType districtType, String message, Float time, String creator, Vec2 districtA, Vec2 districtB){
            this.districtType = districtType;
            this.message = message;
            this.time = time;
            this.creator = creator;
            this.districtA = new Vec2().set(districtA);
            this.districtB = new Vec2().set(districtB);
        }

        public advDistrict(District.districtType districtType, String message, String creator, Vec2 districtA, Vec2 districtB){
            this(districtType, message, Time.time, creator, districtA, districtB);
        }

        public advDistrict(District.districtType districtType, String message, Vec2 districtA, Vec2 districtB){
            this(districtType, message, null, districtA, districtB);
        }

        public advDistrict(advDistrict voidDistrict){
            this.districtType = voidDistrict.districtType;
            this.message = voidDistrict.message;
            this.duration = voidDistrict.duration;
            this.time = Time.time;
            this.creator = voidDistrict.creator;
            this.districtA = new Vec2().set(voidDistrict.districtA);
            this.districtB = new Vec2().set(voidDistrict.districtB);
        }

        public advDistrict(){
        }

        public String toString(){
            return getPrefix("violet", "District") + "{" + districtType.getName() + "}" +
            "[white]：" +
            "(" + (int)districtA.x + "," + (int)districtA.y + ")~" +
            "(" + (int)districtB.x + "," + (int)districtB.y + ")"
            ;
        }

        public Vec2 center(){
            return new Vec2((districtA.x + districtB.x) / 2f, (districtA.y + districtB.y) / 2f);
        }

        public void draw(){
            if(districtA.x == districtB.x || districtA.y == districtB.y) return;

            //arcDrawTextMain(districtType.districtName,(districtA.x+districtB.x)/2, Math.max(districtA.y,districtB.y)-2);
            Draw.reset();
            float width = (districtB.x - districtA.x) * tilesize, height = (districtB.y - districtA.y) * tilesize;

            if(districtType.districtType != null){
                Draw.alpha(0.5f);
                Draw.rect(districtType.districtType.fullIcon, districtA.x * tilesize, districtA.y * tilesize, width, height);
            }
            if(districtType.districtName != null){
                float textX = (districtA.x + districtB.x) / 2;
                float textY = Math.max(districtA.y, districtB.y);
                DrawUtilities.drawText(districtType.getName(), 1f / 3f / Scl.scl(), textX, textY, Align.top);
            }

            Draw.color(Pal.stat, 0.7f);
            Draw.z(Layer.effect - 1f);
            Lines.stroke(Math.min(Math.abs(width), Math.abs(height)) / tilesize / 10f);
            Lines.rect(districtA.x * tilesize, districtA.y * tilesize, width, height);
            Draw.reset();
        }

        public String getLoc(){
            return "(" + (int)districtA.x + "," + (int)districtA.y + ") ~ (" + (int)districtB.x + "," + (int)districtB.y + ")";
        }

    }

    public static class districtType{
        public UnlockableContent districtType = null;
        public String districtName = null;

        districtType(UnlockableContent districtType, String districtName){
            this.districtType = districtType;
            this.districtName = districtName;
        }

        districtType(UnlockableContent districtType){
            this.districtType = districtType;
        }

        districtType(String districtName){
            this.districtName = districtName;
        }

        public String getName(){
            if(districtName == null) return districtType.emoji();
            else if(districtType == null) return districtName;
            else return districtType.emoji() + " " + districtName;
        }
    }

}
