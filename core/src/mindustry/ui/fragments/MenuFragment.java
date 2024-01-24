package mindustry.ui.fragments;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.service.*;
import mindustry.ui.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.arcui;
import static mindustry.gen.Tex.*;
import static mindustry.ui.Styles.cleart;

public class MenuFragment{
    private Table container, submenu;
    private Button currentMenu;
    private MenuRenderer renderer;
    private Seq<MenuButton> customButtons = new Seq<>();
    Label textLabel;
    float tx, ty, base;
    String[] labels = {"[yellow]学术端!"};
    float period = 75f;
    float varSize = 0.8f;
    String text = labels[0];

    Fi arcBackground;
    String arcBackgroundPath = Core.settings.getString("arcBackgroundPath");
    Seq<Fi> arcBGList;

    Image img = new Image();

    int arcBackgroundIndex = 0;

    public void build(Group parent){
        renderer = new MenuRenderer();

        if(!Core.settings.getBool("arcDisableModWarning")){
            arcui.aboutcn_arc.show();
        }
        Group group = new WidgetGroup();
        group.setFillParent(true);
        group.visible(() -> !ui.editor.isShown());
        parent.addChild(group);

        WidgetGroup textGroup = new WidgetGroup();
        parent.addChild(textGroup);

        parent = group;

        if(arcBackgroundPath != null && Core.files.absolute(arcBackgroundPath).exists() && Core.files.absolute(arcBackgroundPath).list().length >= 1){
            arcBackgroundIndex = (int)(Math.random() * Core.files.absolute(arcBackgroundPath).list().length);
            nextBackGroundImg();
            if(arcBGList.size == 0){
                parent.fill((x, y, w, h) -> renderer.render());
            }else{
                group.addChild(img);
                img.setFillParent(true);
            }
        }else{
            parent.fill((x, y, w, h) -> renderer.render());
        }

        parent.fill(c -> {
            c.pane(Styles.noBarPane, cont -> {
                container = cont;
                cont.name = "menu container";

                if(!mobile){
                    c.left();
                    buildDesktop();
                    Events.on(ResizeEvent.class, event -> buildDesktop());
                }else{
                    buildMobile();
                    Events.on(ResizeEvent.class, event -> buildMobile());
                }
            }).with(pane -> {
                pane.setOverscroll(false, false);
            }).grow();
        });

        parent.fill(c -> c.bottom().right().button(Icon.discord, new ImageButtonStyle(){{
            up = discordBanner;
        }}, ui.discord::show).marginTop(9f).marginLeft(10f).tooltip("@discord").size(84, 45).name("discord"));

        parent.fill(c -> c.bottom().right().button("检查更新", Icon.refresh, () -> {
            ui.loadfrag.show();
            becontrol.checkUpdate(result -> {
                ui.loadfrag.hide();
                becontrol.BeControlTable();
            });
        }).size(200, 60).name("检查更新").update(t -> {
            t.getLabel().setColor(becontrol.isUpdateAvailable() ? Tmp.c1.set(Color.white).lerp(Pal.accent, Mathf.absin(5f, 1f)) : Color.white);
        }));

        parent.fill(c -> c.bottom().left().table(t -> {
            t.background(Tex.buttonEdge3);
            t.button("\uE83D", cleart, this::nextBackGroundImg).width(50f);
        }).visible(() -> Core.settings.getString("arcBackgroundPath", "").length() != 0).left().width(100));
        String versionText = ((Version.build == -1) ? "[#fc8140aa]" : "[#ffffffba]") + Version.combined();
        parent.fill((x, y, w, h) -> {
            TextureRegion logo = Core.atlas.find("logo");
            float width = Core.graphics.getWidth(), height = Core.graphics.getHeight() - Core.scene.marginTop;
            float logoscl = Scl.scl(1) * logo.scale;
            float logow = Math.min(logo.width * logoscl, Core.graphics.getWidth() - Scl.scl(20));
            float logoh = logow * (float)logo.height / logo.width;

            float fx = (int)(width / 2f);
            float fy = (int)(height - 6 - logoh) + logoh / 2 - (Core.graphics.isPortrait() ? Scl.scl(30f) : 0f);
            if(Core.settings.getBool("macnotch") ){
                fy -= Scl.scl(macNotchHeight);
            }

            tx = width / 2f + logow * 0.35f;
            ty = fy - logoh / 2f - Scl.scl(2f) + logoh * 0.15f;
            base = logoh * 0.03f;

            Draw.color();
            Draw.rect(logo, fx, fy, logow, logoh);

            Fonts.outline.setColor(Color.white);
            Fonts.outline.draw(versionText, fx, fy - logoh/2f - Scl.scl(2f), Align.center);
        }).touchable = Touchable.disabled;

        textGroup.setTransform(true);
        textGroup.setRotation(20);
        textGroup.addChild(textLabel = new Label(""));
        textGroup.visible(() -> Core.settings.getBool("menuFloatText", true));
        textLabel.setAlignment(Align.center);
        textGroup.update(() -> {
            textGroup.x = tx;
            textGroup.y = ty;
            textLabel.setFontScale((base == 0 ? 1f : base) * Math.abs(Time.time % period / period - 0.5f) * varSize + 1);
            textLabel.setText(text);
        });
        labels = Core.files.internal("labels").readString("UTF-8").replace("\r", "").replace("\\n", "\n").replace("/n", "\n").split("\n");
        randomLabel();
    }

    private void nextBackGroundImg(){
        arcBGList = Core.files.absolute(arcBackgroundPath).findAll(f -> !f.isDirectory() && (f.extEquals("png") || f.extEquals("jpg") || f.extEquals("jpeg")));
        if(arcBGList.size == 0) return;
        arcBackgroundPath = Core.settings.getString("arcBackgroundPath");
        arcBackgroundIndex += 1;
        arcBackgroundIndex = arcBackgroundIndex % arcBGList.size;
        new Thread(() -> {
            try{
                arcBackground = arcBGList.get(arcBackgroundIndex);
                Core.app.post(() -> img.setDrawable(new TextureRegion(new Texture(arcBackground))));
            }catch(Exception e){
                Core.app.post(() -> ui.showException("背景图片无效:" + arcBGList.get(arcBackgroundIndex).path(), e));
            }
        }).start();
    }

    private void randomLabel(){
        Timer.schedule(() -> text = "[yellow]" + labels[new Rand().random(0, labels.length - 1)], 0.11f);
    }

    private void buildMobile(){
        container.clear();
        container.name = "buttons";
        container.setSize(Core.graphics.getWidth(), Core.graphics.getHeight());

        float size = 120f;
        container.defaults().size(size).pad(5).padTop(4f);

        initAchievement();

        MobileButton
            play = new MobileButton(Icon.play, "@campaign", () -> checkPlay(ui.planet::show)),
            custom = new MobileButton(Icon.rightOpenOut, "@customgame", () -> checkPlay(ui.custom::show)),
            maps = new MobileButton(Icon.download, "@loadgame", () -> checkPlay(ui.load::show)),
            join = new MobileButton(Icon.add, "@joingame", () -> checkPlay(ui.join::show)),
            editor = new MobileButton(Icon.terrain, "@editor", () -> checkPlay(ui.maps::show)),
            tools = new MobileButton(Icon.settings, "@settings", ui.settings::show),
            mods = new MobileButton(Icon.book, "@mods", ui.mods::show),
            exit = new MobileButton(Icon.exit, "@quit", () -> Core.app.exit()),
            cn_arc = new MobileButton(Icon.info, "@aboutcn_arc.button", arcui.aboutcn_arc::show),
            //mindustrywiki = new MobileButton(Icon.book, "@mindustrywiki.button", ui.mindustrywiki::show),
            database = new MobileButton(Icon.book, "@database", ui.database::show),
            achievements = new MobileButton(Icon.star, "@achievements", arcui.achievements::show);

        play.clicked(this::randomLabel);
        custom.clicked(this::randomLabel);
        maps.clicked(this::randomLabel);
        join.clicked(this::randomLabel);
        editor.clicked(this::randomLabel);
        tools.clicked(this::randomLabel);
        mods.clicked(this::randomLabel);
        cn_arc.clicked(this::randomLabel);
        database.clicked(this::randomLabel);
        achievements.clicked(this::randomLabel);

        Seq<MobileButton> customs = customButtons.map(b -> new MobileButton(b.icon, b.text, b.runnable == null ? () -> {} : b.runnable));

        if(!Core.graphics.isPortrait()){
            container.marginTop(60f);
            container.add(play);
            container.add(join);
            container.add(custom);
            container.add(maps);
            // add odd custom buttons
            for(int i = 1; i < customs.size; i += 2){
                customs.get(i).clicked(this::randomLabel);
                container.add(customs.get(i));
            }
            container.row();

            container.add(editor);
            container.add(tools);
            container.add(mods);
            container.add(achievements);
            // add even custom buttons (before the exit button)
            for(int i = 0; i < customs.size; i += 2){
                customs.get(i).clicked(this::randomLabel);
                container.add(customs.get(i));
            }
            container.row();
            container.add(cn_arc);
            container.add(database);
            if(!ios) container.add(exit);
        }else{
            container.marginTop(0f);
            container.add(play);
            container.add(maps);
            container.row();
            container.add(custom);
            container.add(join);
            container.row();
            container.add(editor);
            container.add(tools);
            container.row();
            container.add(mods);
            // add custom buttons
            for(int i = 0; i < customs.size; i++){
                customs.get(i).clicked(this::randomLabel);
                container.add(customs.get(i));
                if(i % 2 == 0) container.row();
            }
            if(!ios) container.add(exit);
            container.row();
            container.add(cn_arc);
            container.add(database);
            container.row();
            container.add(achievements);
        }
    }

    void initAchievement(){
        if(service.enabled()) return;
        service = new GameService(){
            @Override
            public boolean enabled(){
                return true;
            }

            @Override
            public void completeAchievement(String name){
                Core.settings.put("achievement." + name, true);
                //TODO draw the sprite of the achievement
                ui.hudfrag.showToast(Core.atlas.getDrawable("error"), Core.bundle.get("achievement.unlocked") + "\n" + Core.bundle.get("achievement." + name + ".name"));
            }

            @Override
            public boolean isAchieved(String name){
                return Core.settings.getBool("achievement." + name, false);
            }

            @Override
            public int getStat(String name, int def){
                return Core.settings.getInt("achievementstat." + name, def);
            }

            @Override
            public void setStat(String name, int amount){
                Core.settings.put("achievementstat." + name, amount);
            }
        };

        service.init();
    }

    private void buildDesktop(){
        container.clear();
        container.setSize(Core.graphics.getWidth(), Core.graphics.getHeight());

        float width = 230f;
        Drawable background = Styles.black6;

        container.left();
        container.add().width(Core.graphics.getWidth() / 10f);
        container.table(background, t -> {
            t.defaults().width(width).height(70f);
            t.name = "buttons";

            buttons(t,
            new MenuButton("@play", Icon.play,
            new MenuButton("@campaign", Icon.play, () -> checkPlay(ui.planet::show)),
            new MenuButton("@joingame", Icon.add, () -> checkPlay(ui.join::show)),
            new MenuButton("@customgame", Icon.terrain, () -> checkPlay(ui.custom::show)),
            new MenuButton("@loadgame", Icon.download, () -> checkPlay(ui.load::show)),
            new MenuButton("@editor", Icon.terrain, () -> checkPlay(ui.maps::show)), steam ? new MenuButton("@workshop", Icon.steam, platform::openWorkshop) : null
            ),
            new MenuButton("@database.button", Icon.menu,
            new MenuButton("@schematics", Icon.paste, ui.schematics::show),
            new MenuButton("@database", Icon.book, ui.database::show),
            new MenuButton("@about.button", Icon.info, ui.about::show)
            ),

            new MenuButton("@achievements", Icon.star, arcui.achievements::show),
            new MenuButton("@mods", Icon.book, ui.mods::show),
            new MenuButton("@settings", Icon.settings, ui.settings::show),
            new MenuButton("@aboutcn_arc.button", Icon.info, arcui.aboutcn_arc::show)
            );
            buttons(t, customButtons.toArray(MenuButton.class));
            buttons(t, new MenuButton("@quit", Icon.exit, Core.app::exit));
        }).width(width).growY();

        container.table(background, t -> {
            submenu = t;
            t.name = "submenu";
            t.color.a = 0f;
            t.top();
            t.defaults().width(width).height(70f);
            t.visible(() -> !t.getChildren().isEmpty());

        }).width(width).growY();
    }

    private void checkPlay(Runnable run){
        if(!mods.hasContentErrors()){
            run.run();
        }else{
            ui.showInfo("@mod.noerrorplay");
        }
    }

    private void fadeInMenu(){
        submenu.clearActions();
        submenu.actions(Actions.alpha(1f, 0.15f, Interp.fade));
    }

    private void fadeOutMenu(){
        //nothing to fade out
        if(submenu.getChildren().isEmpty()){
            return;
        }

        submenu.clearActions();
        submenu.actions(Actions.alpha(1f), Actions.alpha(0f, 0.2f, Interp.fade), Actions.run(() -> submenu.clearChildren()));
    }

    private void buttons(Table t, MenuButton... buttons){
        for(MenuButton b : buttons){
            if(b == null) continue;
            Button[] out = {null};
            out[0] = t.button(b.text, b.icon, Styles.flatToggleMenut, () -> {
                if(currentMenu == out[0]){
                    currentMenu = null;
                    fadeOutMenu();
                }else{
                    if(b.submenu != null){
                        currentMenu = out[0];
                        submenu.clearChildren();
                        fadeInMenu();
                        //correctly offset the button
                        submenu.add().height((Core.graphics.getHeight() - Core.scene.marginTop - Core.scene.marginBottom - out[0].getY(Align.topLeft)) / Scl.scl(1f));
                        submenu.row();
                        buttons(submenu, b.submenu);
                    }else{
                        currentMenu = null;
                        fadeOutMenu();
                        randomLabel();
                        b.runnable.run();
                    }
                }
            }).marginLeft(11f).get();
            out[0].update(() -> out[0].setChecked(currentMenu == out[0]));
            t.row();
        }
    }

    /** Adds a custom button to the menu. */
    public void addButton(String text, Drawable icon, Runnable callback){
        addButton(new MenuButton(text, icon, callback));
    }

    /** Adds a custom button to the menu. */
    public void addButton(String text, Runnable callback){
        addButton(text, Styles.none, callback);
    }

    /**
     * Adds a custom button to the menu.
     * If {@link MenuButton#submenu} is null or the player is on mobile, {@link MenuButton#runnable} is invoked on click.
     * Otherwise, {@link MenuButton#submenu} is shown.
     */
    public void addButton(MenuButton button){
        customButtons.add(button);
    }

    /** Represents a menu button definition. */
    public static class MenuButton{
        public final Drawable icon;
        public final String text;
        /** Runnable ran when the button is clicked. Ignored on desktop if {@link #submenu} is not null. */
        public final Runnable runnable;
        /** Submenu shown when this button is clicked. Used instead of {@link #runnable} on desktop. */
        public final @Nullable MenuButton[] submenu;

        /** Constructs a simple menu button, which behaves the same way on desktop and mobile. */
        public MenuButton(String text, Drawable icon, Runnable runnable){
            this.icon = icon;
            this.text = text;
            this.runnable = runnable;
            this.submenu = null;
        }

        /** Constructs a button that runs the runnable when clicked on mobile or shows the submenu on desktop. */
        public MenuButton(String text, Drawable icon, Runnable runnable, MenuButton... submenu){
            this.icon = icon;
            this.text = text;
            this.runnable = runnable;
            this.submenu = submenu;
        }

        /** Comstructs a desktop-only button; used internally. */
        MenuButton(String text, Drawable icon, MenuButton... submenu){
            this.icon = icon;
            this.text = text;
            this.runnable = () -> {
            };
            this.submenu = submenu;
        }
    }
}
