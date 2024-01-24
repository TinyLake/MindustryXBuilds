package mindustry.editor;

import arc.*;
import arc.func.Intc;
import arc.func.Intp;
import arc.func.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.TextField.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;
import static mindustry.game.SpawnGroup.*;

public class WaveInfoDialog extends BaseDialog{
    private int start = 0, displayed = 20, graphSpeed = 1, maxGraphSpeed = 16;
    Seq<SpawnGroup> groups = new Seq<>();
    private @Nullable SpawnGroup expandedGroup;

    private Table table, iTable, eTable, uTable;
    private int search = -1, maxVisible = 30;
    private int filterHealth, filterBegin = -1, filterEnd = -1, filterAmount, filterAmountWave;
    private boolean expandPane = false, filterHealthMode = false, filterStrict = false;
    private @Nullable UnitType filterType;
    private StatusEffect filterEffect = StatusEffects.none;
    private Sort sort = Sort.begin;
    private boolean reverseSort = false;
    private float updateTimer, updatePeriod = 1f;
    private TextField amountField = new TextField();
    private boolean checkedSpawns;
    private WaveGraph graph = new WaveGraph();

    public WaveInfoDialog(){
        super("@waves.title");

        shown(() -> {
            checkedSpawns = false;
            setup();
        });
        hidden(() -> state.rules.spawns = groups);

        onResize(this::setup);
        addCloseButton();

        buttons.button("@waves.edit", Icon.edit, () -> {
            BaseDialog dialog = new BaseDialog("@waves.edit");
            dialog.addCloseButton();
            dialog.setFillParent(false);
            dialog.cont.table(Tex.button, t -> {
                var style = Styles.cleart;
                t.defaults().size(280f, 64f).pad(2f);

                t.button("@waves.copy", Icon.copy, style, () -> {
                    ui.showInfoFade("@waves.copied");
                    Core.app.setClipboardText(maps.writeWaves(groups));
                    dialog.hide();
                }).disabled(b -> groups == null || groups.isEmpty()).marginLeft(12f).row();

                t.button("@waves.load", Icon.download, style, () -> {
                    try{
                        groups = maps.readWaves(Core.app.getClipboardText());
                        buildGroups();
                    }catch(Exception e){
                        e.printStackTrace();
                        ui.showErrorMessage("@waves.invalid");
                    }
                    dialog.hide();
                }).disabled(Core.app.getClipboardText() == null || !Core.app.getClipboardText().startsWith("[")).marginLeft(12f).row();

                t.button("@clear", Icon.none, style, () -> ui.showConfirm("@confirm", "@settings.clear.confirm", () -> {
                    groups.clear();
                    buildGroups();
                    dialog.hide();
                })).marginLeft(12f).row();

                t.button("@settings.reset", Icon.refresh, style, () -> ui.showConfirm("@confirm", "@settings.clear.confirm", () -> {
                    groups = JsonIO.copy(waves.get());
                    buildGroups();
                    dialog.hide();
                })).marginLeft(12f);
            });

            dialog.show();
        }).size(250f, 64f);

        buttons.defaults().width(60f);

        buttons.button("<", () -> {}).update(t -> {
            if(t.getClickListener().isPressed()){
                shift(-graphSpeed);
            }
        });
        buttons.button(">", () -> {}).update(t -> {
            if(t.getClickListener().isPressed()){
                shift(graphSpeed);
            }
        });

        buttons.button("-", () -> {}).update(t -> {
            if(t.getClickListener().isPressed()){
                view(-graphSpeed);
            }
        });
        buttons.button("+", () -> {}).update(t -> {
            if(t.getClickListener().isPressed()){
                view(graphSpeed);
            }
        });

        if(experimental){
            buttons.button("x" + graphSpeed, () -> {
                graphSpeed *= 2;
                if(graphSpeed > maxGraphSpeed) graphSpeed = 1;
            }).update(b -> b.setText("x" + graphSpeed)).width(100f);

            buttons.button("Random", Icon.refresh, () -> {
                groups.clear();
                groups = Waves.generate(1f / 10f);
                buildGroups();
            }).width(200f);
        }
    }

    void view(int amount){
        updateTimer += Time.delta;
        if(updateTimer >= updatePeriod){
            displayed += amount;
            if(displayed < 5) displayed = 5;
            updateTimer = 0f;
            updateWaves();
        }
    }

    void shift(int amount){
        updateTimer += Time.delta;
        if(updateTimer >= updatePeriod){
            start += amount;
            if(start < 0) start = 0;
            updateTimer = 0f;
            updateWaves();
        }
    }

    void setup(){
        groups = JsonIO.copy(state.rules.spawns.isEmpty() ? waves.get() : state.rules.spawns);
        if(groups == null) groups = new Seq<>();

        cont.clear();
        cont.stack(new Table(Tex.clear, main -> {
            main.table(s -> {
                s.image(Icon.zoom).padRight(8);
                s.field(search < 0 ? "" : (search + 1) + "", TextFieldFilter.digitsOnly, text -> {
                    search = groups.any() ? Strings.parseInt(text, 0) - 1 : -1;
                    start = Math.max(search - (displayed / 2) - (displayed % 2), 0);
                    buildGroups();
                }).growX().maxTextLength(8).get().setMessageText("@waves.search");
                s.button(Icon.units, Styles.emptyi, () -> showUnits(type -> filterType = type, true)).size(46f).tooltip("@waves.filter")
                .update(b -> b.getStyle().imageUp = filterType != null ? new TextureRegionDrawable(filterType.uiIcon) : Icon.filter);
            }).growX().pad(6f).row();
            main.pane(t -> table = t).grow().padRight(8f).scrollX(false).row();

            main.table(f -> {
                f.button("@add", () -> {
                    showUnits(type -> groups.add(expandedGroup = new SpawnGroup(type)), false);
                    buildGroups();
                    clearFilter();
                }).growX().height(70f);
                f.button(Icon.filter, () -> {
                    BaseDialog dialog = new BaseDialog("@waves.sort");
                    dialog.setFillParent(false);
                    dialog.cont.table(Tex.button, t -> {
                        for(Sort s : Sort.all){
                            t.button("@waves.sort." + s, Styles.cleart, () -> {
                                sort = s;
                                dialog.hide();
                                buildGroups();
                            }).size(150f, 60f).checked(s == sort);
                        }
                    }).row();
                    dialog.cont.check("@waves.sort.reverse", b -> {
                        reverseSort = b;
                        buildGroups();
                    }).padTop(4).checked(reverseSort).padBottom(8f);
                    dialog.addCloseButton();
                    dialog.show();
                    buildGroups();
                }).size(64f, 70f).padLeft(6f);
            }).growX();
        }), new Label("@waves.none"){{
            visible(() -> groups.isEmpty());
            this.touchable = Touchable.disabled;
            setWrap(true);
            setAlignment(Align.center, Align.center);
        }}).width(390f).growY();

        cont.add(graph = new WaveGraph()).grow();

        buildGroups();
    }

    void buildGroups(){
        table.clear();
        table.top();
        table.margin(10f);

        if(groups != null){
            groups.sort(Structs.comps(Structs.comparingFloat(sort.sort), Structs.comparingFloat(sort.secondary)));
            if(reverseSort) groups.reverse();

            for(SpawnGroup group : groups){
                if(group.effect == StatusEffects.none) group.effect = null;
                if((search >= 0 && group.getSpawned(search) <= 0)
                || (filterHealth != 0 && !(filterHealthMode ? group.type.health * (search >= 0 ? group.getSpawned(search) : 1) > filterHealth : group.type.health * (search >= 0 ? group.getSpawned(search) : 1) < filterHealth))
                || (filterBegin >= 0 && !(filterStrict ? group.begin == filterBegin : group.begin - 2 <= filterBegin && group.begin + 2 >= filterBegin))
                || (filterEnd >= 0 && !(filterStrict ? group.end == filterEnd : group.end - 2 <= filterEnd && group.end + 2 >= filterEnd))
                || (filterAmount != 0 && !(filterStrict ? group.getSpawned(filterAmountWave) == filterAmount : filterAmount - 5 <= group.getSpawned(filterAmountWave) && filterAmount + 5 >= group.getSpawned(filterAmountWave)))
                || (filterEffect != StatusEffects.none && group.effect != filterEffect)
                || (filterType != null && group.type != filterType)) continue;

                table.table(Tex.button, t -> {
                    t.margin(0).defaults().pad(3).padLeft(5f).growX().left();
                    t.button(b -> {
                        b.left();
                        b.image(group.type.uiIcon).size(32f).padRight(3).scaling(Scaling.fit);
                        if(group.effect != null && group.effect != StatusEffects.none) b.image(group.effect.uiIcon).size(20f).padRight(3).scaling(Scaling.fit);
                        b.add(group.type.localizedName).color(Pal.accent);

                        b.add().growX();

                        b.label(() -> (group.begin + 1) + "").color(Color.lightGray).minWidth(45f).labelAlign(Align.left).left();

                        b.button(Icon.settingsSmall, Styles.emptyi, () -> {
                            BaseDialog dialog = new BaseDialog("@waves.group");
                            dialog.setFillParent(false);
                            dialog.cont.table(Tex.button, a -> iTable = a).row();
                            dialog.cont.table(c -> {
                                c.defaults().size(210f, 64f).pad(2f);
                                c.button("@waves.duplicate", Icon.copy, () -> {
                                    SpawnGroup newGroup = group.copy();
                                    groups.add(newGroup);
                                    expandedGroup = newGroup;
                                    buildGroups();
                                    dialog.hide();
                                });
                                c.button("@settings.resetKey", Icon.refresh, () -> ui.showConfirm("@confirm", "@settings.clear.confirm", () -> {
                                    group.effect = StatusEffects.none;
                                    group.payloads = Seq.with();
                                    group.items = null;
                                    buildGroups();
                                    dialog.hide();
                                }));
                            });
                            buildGroups();
                            updateIcons(group);
                            dialog.addCloseButton();
                            dialog.show();
                        }).pad(-6).size(46f);
                        b.button(Icon.copySmall, Styles.emptyi, () -> {
                            groups.insert(groups.indexOf(group) + 1, expandedGroup = group.copy());
                            buildGroups();
                        }).pad(-6).size(46f).tooltip("@editor.copy");
                        b.button(group.effect != null ?
                            new TextureRegionDrawable(group.effect.uiIcon) :
                            Icon.logicSmall,
                        Styles.emptyi, () -> showEffects(group)).pad(-6).size(46f).scaling(Scaling.fit).tooltip(group.effect != null ? group.effect.localizedName : "@none");
                        b.button(Icon.unitsSmall, Styles.emptyi, () -> showUnits(type -> group.type = type, false)).pad(-6).size(46f).tooltip("@stat.unittype");
                        b.button(Icon.cancel, Styles.emptyi, () -> {
                            if(expandedGroup == group) expandedGroup = null;
                            groups.remove(group);
                            if(expandedGroup == group) expandedGroup = null;
                            table.getCell(t).pad(0f);
                            t.remove();
                            buildGroups();
                        }).pad(-6).size(46f).padRight(-12f).tooltip("@waves.remove");
                        b.clicked(KeyCode.mouseMiddle, () -> {
                            groups.insert(groups.indexOf(group) + 1, expandedGroup = group.copy());
                            buildGroups();
                        });
                    }, () -> {
                        expandedGroup = expandedGroup == group ? null : group;
                        buildGroups();
                    }).height(46f).pad(-6f).padBottom(0f).row();

                    if(expandedGroup == group){
                        t.table(spawns -> {
                            spawns.field("" + (group.begin + 1), TextFieldFilter.digitsOnly, text -> {
                                if(Strings.canParsePositiveInt(text)){
                                    group.begin = Strings.parseInt(text) - 1;
                                    updateWaves();
                                }
                            }).width(100f);
                            spawns.add("@waves.to").padLeft(4).padRight(4);
                            spawns.field(group.end == never ? "" : (group.end + 1) + "", TextFieldFilter.digitsOnly, text -> {
                                if(Strings.canParsePositiveInt(text)){
                                    group.end = Strings.parseInt(text) - 1;
                                    updateWaves();
                                }else if(text.isEmpty()){
                                    group.end = never;
                                    updateWaves();
                                }
                            }).width(100f).get().setMessageText("∞");
                        }).row();

                        t.table(p -> {
                            p.add("@waves.every").padRight(4);
                            p.field(group.spacing + "", TextFieldFilter.digitsOnly, text -> {
                                if(Strings.canParsePositiveInt(text) && Strings.parseInt(text) > 0){
                                    group.spacing = Strings.parseInt(text);
                                    updateWaves();
                                }
                            }).width(100f);
                            p.add("@waves.waves").padLeft(4);
                        }).row();

                        t.table(a -> {
                            a.field(group.unitAmount + "", TextFieldFilter.digitsOnly, text -> {
                                if(Strings.canParsePositiveInt(text)){
                                    group.unitAmount = Strings.parseInt(text);
                                    updateWaves();
                                }
                            }).width(80f);

                            a.add(" + ");
                            a.field(Strings.fixed(Math.max((Mathf.zero(group.unitScaling) ? 0 : 1f / group.unitScaling), 0), 2), TextFieldFilter.floatsOnly, text -> {
                                if(Strings.canParsePositiveFloat(text)){
                                    group.unitScaling = 1f / Strings.parseFloat(text);
                                    updateWaves();
                                }
                            }).width(80f);
                            a.add("@waves.perspawn").padLeft(4);
                        }).row();

                        t.table(a -> {
                            a.field(group.max + "", TextFieldFilter.digitsOnly, text -> {
                                if(Strings.canParsePositiveInt(text)){
                                    group.max = Strings.parseInt(text);
                                    updateWaves();
                                }
                            }).width(80f);

                            a.add("@waves.max").padLeft(5);
                        }).row();

                        t.table(a -> {
                            a.field((int)group.shields + "", TextFieldFilter.digitsOnly, text -> {
                                if(Strings.canParsePositiveInt(text)){
                                    group.shields = Strings.parseInt(text);
                                    updateWaves();
                                }
                            }).width(80f);

                            a.add(" + ");
                            a.field((int)group.shieldScaling + "", TextFieldFilter.digitsOnly, text -> {
                                if(Strings.canParsePositiveInt(text)){
                                    group.shieldScaling = Strings.parseInt(text);
                                    updateWaves();
                                }
                            }).width(80f);
                            a.add("@waves.shields").padLeft(4);
                        }).row();

                        t.check("@waves.guardian", b -> {
                            group.effect = (b ? StatusEffects.boss : null);
                            buildGroups();
                        }).padTop(4).update(b -> b.setChecked(group.effect == StatusEffects.boss)).padBottom(8f).row();

                        t.table(a -> {
                            a.add("@waves.spawn").padRight(8);

                            a.button("", () -> {
                                if(!checkedSpawns){
                                    //recalculate waves when changed
                                    Vars.spawner.reset();
                                    checkedSpawns = true;
                                }

                                BaseDialog dialog = new BaseDialog("@waves.spawn.select");
                                dialog.cont.pane(p -> {
                                    p.background(Tex.button).margin(10f);
                                    int i = 0;
                                    int cols = 4;
                                    int max = 20;

                                    if(spawner.getSpawns().size >= max){
                                        p.add("[lightgray](first " + max + ")").colspan(cols).padBottom(4).row();
                                    }

                                    for(var spawn : spawner.getSpawns()){
                                        p.button(spawn.x + ", " + spawn.y, Styles.flatTogglet, () -> {
                                            group.spawn = Point2.pack(spawn.x, spawn.y);
                                            dialog.hide();
                                        }).size(110f, 45f).checked(spawn.pos() == group.spawn);

                                        if(++i % cols == 0){
                                            p.row();
                                        }

                                        //only display first 20 spawns, you don't need to see more.
                                        if(i >= 20){
                                            break;
                                        }
                                    }

                                    if(spawner.getSpawns().isEmpty()){
                                        p.add("@waves.spawn.none");
                                    }else{
                                        p.button("@waves.spawn.all", Styles.flatTogglet, () -> {
                                            group.spawn = -1;
                                            dialog.hide();
                                        }).size(110f, 45f).checked(-1 == group.spawn);
                                    }
                                }).grow();
                                dialog.setFillParent(false);
                                dialog.addCloseButton();
                                dialog.show();
                            }).width(160f).height(36f).get().getLabel().setText(() -> group.spawn == -1 ? "@waves.spawn.all" : Point2.x(group.spawn) + ", " + Point2.y(group.spawn));

                        }).padBottom(8f).row();
                    }
                }).width(340f).pad(8);

                table.row();
            }

            if(table.getChildren().isEmpty() && groups.any()){
                table.add("@none.found");
            }
        }else{
            table.add("@editor.default");
        }

        updateWaves();
    }

    void showUnits(Cons<UnitType> cons, boolean reset){
        BaseDialog dialog = new BaseDialog(reset ? "@waves.filter" : "");
        dialog.cont.pane(p -> {
            p.defaults().pad(2).fillX();
            if(reset){
                p.button(t -> {
                    t.left();
                    t.image(Icon.none).size(8 * 4).scaling(Scaling.fit).padRight(2f);
                    t.add("@settings.resetKey");
                }, () -> {
                    cons.get(null);
                    dialog.hide();
                    buildGroups();
                }).margin(12f);
            }
            int i = reset ? 1 : 0;
            for(UnitType type : content.units()){
                if(type.isHidden()) continue;
                p.button(t -> {
                    t.left();
                    t.image(type.uiIcon).size(8 * 4).scaling(Scaling.fit).padRight(2f);
                    t.add(type.localizedName);
                }, () -> {
                    cons.get(type);
                    dialog.hide();
                    buildGroups();
                }).margin(12f);
                if(++i % 3 == 0) p.row();
            }
        }).growX().scrollX(false);
        dialog.addCloseButton();
        dialog.show();
    }

    void showUpdate(SpawnGroup group, boolean payloads){
        BaseDialog dialog = new BaseDialog("");
        dialog.setFillParent(true);
        if(payloads && group.payloads == null) group.payloads = Seq.with();
        if(payloads) dialog.cont.table(e -> {
            uTable = e;
            updateIcons(group);
        }).padBottom(6f).row();
        dialog.cont.pane(p -> {
            int i = 0;
            for(UnitType type : content.units()){
                if(type.isHidden()) continue;
                p.button(t -> {
                    t.left();
                    t.image(type.uiIcon).size(8 * 4).scaling(Scaling.fit).padRight(2f);
                    t.add(type.localizedName);
                }, () -> {
                    if(payloads){
                        group.payloads.add(type);
                        updateIcons(group);
                    }else{
                        group.type = type;
                        dialog.hide();
                    }
                    if(group.payloads != null && group.type.payloadCapacity <= 8) group.payloads.clear();
                    if(group.items != null) group.items.amount = Mathf.clamp(group.items.amount, 0, group.type.itemCapacity);
                    buildGroups();
                }).pad(2).margin(12f).fillX();
                if(++i % 3 == 0) p.row();
            }
        });
        dialog.addCloseButton();
        dialog.show();
    }
    void showEffects(SpawnGroup group){
        BaseDialog dialog = new BaseDialog("");
        dialog.cont.pane(p -> {
            p.defaults().pad(2).fillX();
            p.button(t -> {
                t.left();
                t.image(Icon.none).size(8 * 4).scaling(Scaling.fit).padRight(2f);
                t.add("@settings.resetKey");
            }, () -> {
                group.effect = null;
                dialog.hide();
                buildGroups();
            }).margin(12f);
            int i = 1;
            for(StatusEffect effect : content.statusEffects()){
                if(effect.isHidden() || effect.reactive) continue;
                p.button(t -> {
                    t.left();
                    t.image(effect.uiIcon).size(8 * 4).scaling(Scaling.fit).padRight(2f);
                    t.add(effect.localizedName);
                }, () -> {
                    group.effect = effect;
                    dialog.hide();
                    buildGroups();
                }).margin(12f);
                if(++i % 3 == 0) p.row();
            }
        }).growX().scrollX(false);
        dialog.addCloseButton();
        dialog.show();
    }
    void showEffect(SpawnGroup group){
        BaseDialog dialog = new BaseDialog("");
        dialog.setFillParent(true);
        dialog.cont.pane(p -> {
            int i = 0;
            for(StatusEffect effect : content.statusEffects()){
                if(effect != StatusEffects.none && effect.reactive) continue;

                p.button(t -> {
                    t.left();
                    if(effect.uiIcon != null && effect != StatusEffects.none){
                        t.image(effect.uiIcon).size(8 * 4).scaling(Scaling.fit).padRight(2f);
                    }else{
                        t.image(Icon.none).size(8 * 4).scaling(Scaling.fit).padRight(2f);
                    }

                    if(effect != StatusEffects.none){
                        t.add(effect.localizedName);
                    }else{
                        t.add("@settings.resetKey");
                    }
                }, () -> {
                    if(group == null){
                        filterEffect = effect;
                    }else{
                        group.effect = effect;
                    }
                    updateIcons(group);
                    dialog.hide();
                    buildGroups();
                }).pad(2).margin(12f).fillX();
                if(++i % 3 == 0) p.row();
            }
        });
        dialog.addCloseButton();
        dialog.show();
    }
    void showItems(SpawnGroup group){
        BaseDialog dialog = new BaseDialog("");
        dialog.setFillParent(true);
        dialog.cont.table(items -> {
            items.add(Core.bundle.get("filter.option.amount") + ":");
            amountField = items.field(group.items != null ? group.items.amount + "" : "", TextFieldFilter.digitsOnly, text -> {
                if(Strings.canParsePositiveInt(text) && group.items != null){
                    group.items.amount = Strings.parseInt(text) <= 0 ? group.type.itemCapacity : Mathf.clamp(Strings.parseInt(text), 0, group.type.itemCapacity);
                }
            }).width(120f).pad(2).margin(12f).maxTextLength((group.type.itemCapacity + "").length() + 1).get();
            amountField.setMessageText(group.type.itemCapacity + "");
        }).padBottom(6f).row();
        dialog.cont.pane(p -> {
            int i = 1;
            p.defaults().pad(2).margin(12f).minWidth(200f).fillX();
            p.button(icon -> {
                icon.left();
                icon.image(Icon.none).size(8 * 4).scaling(Scaling.fit).padRight(2f);
                icon.add("@settings.resetKey");
            }, () -> {
                group.items = null;
                updateIcons(group);
                dialog.hide();
                buildGroups();
            });
            for(Item item : content.items()){
                p.button(t -> {
                    t.left();
                    if(item.uiIcon != null) t.image(item.uiIcon).size(8 * 4).scaling(Scaling.fit).padRight(2f);
                    t.add(item.localizedName);
                }, () -> {
                    group.items = new ItemStack(item, Strings.parseInt(amountField.getText()) <= 0 ? group.type.itemCapacity : Mathf.clamp(Strings.parseInt(amountField.getText()), 0, group.type.itemCapacity));
                    updateIcons(group);
                    dialog.hide();
                    buildGroups();
                });
                if(++i % 3 == 0) p.row();
            }
        });
        dialog.addCloseButton();
        dialog.show();
    }

    void showFilter(){
        BaseDialog dialog = new BaseDialog("@waves.filter");
        dialog.setFillParent(false);
        dialog.cont.defaults().size(210f, 64f);
        dialog.cont.add(Core.bundle.get("waves.sort.health") + ":");
        dialog.cont.table(filter -> {
            filter.button(">", Styles.cleart, () -> {
                filterHealthMode = !filterHealthMode;
                buildGroups();
            }).update(b -> b.setText(filterHealthMode ? ">" : "<")).size(40f).padRight(4f);
            filter.defaults().width(170f);
            numField("", filter, f -> filterHealth = f, () -> filterHealth, 15);
        }).row();

        dialog.cont.add("@waves.filter.begin");
        dialog.cont.table(filter -> {
            filter.defaults().maxWidth(120f);
            numField("", filter, f -> filterBegin = f - 1, () -> filterBegin + 1, 8);
            numField("@waves.to", filter, f -> filterEnd = f - 1, () -> filterEnd + 1, 8);
        }).row();

        dialog.cont.add(Core.bundle.get("waves.filter.amount") + ":");
        dialog.cont.table(filter -> {
            filter.defaults().maxWidth(120f);
            numField("", filter, f -> filterAmount = f, () -> filterAmount, 12);
            numField("@waves.filter.onwave", filter, f -> filterAmountWave = f, () -> filterAmountWave, 8);
        }).row();

        dialog.cont.table(t -> {
            eTable = t;
            updateIcons(null);
        }).row();
        dialog.row();
        dialog.check("@waves.filter.strict", b -> {
            filterStrict = b;
            buildGroups();
        }).checked(filterStrict).padBottom(10f).row();

        dialog.table(p -> {
            p.defaults().size(210f, 64f).padLeft(4f).padRight(4f);
            p.button("@back", Icon.left, dialog::hide);
            p.button("@clear", Icon.refresh, () -> {
                clearFilter();
                buildGroups();
                dialog.hide();
            });
        });
        dialog.addCloseListener();
        dialog.show();
    }

    void updateIcons(SpawnGroup group){
        if(iTable != null && group != null){
            iTable.clear();
            iTable.defaults().size(200f, 60f).pad(2f);
            iTable.button(icon -> {
                if(group.effect != null && group.effect != StatusEffects.none){
                    icon.image(group.effect.uiIcon).padRight(6f);
                }else{
                    icon.image(Icon.logic).padRight(6f);
                }
                icon.add("@waves.group.effect");
            }, Styles.cleart, () -> showEffect(group));
            iTable.button("@waves.group.payloads", Icon.defense, Styles.cleart, () -> showUpdate(group, true)).disabled(c -> group.type.payloadCapacity <= 8);
            iTable.button(icon -> {
                if(group.items != null){
                    icon.image(group.items.item.uiIcon).padRight(6f);
                }else{
                    icon.image(Icon.effect).padRight(6f);
                }
                icon.add("@waves.group.items");
            }, Styles.cleart, () -> showItems(group));
        }

        if(eTable != null){
            eTable.clear();
            eTable.add(Core.bundle.get("waves.filter.effect") + ":");
            eTable.button(filterEffect != null && filterEffect != StatusEffects.none ?
                    new TextureRegionDrawable(filterEffect.uiIcon) :
                    Icon.logic, () -> showEffect(null)).padLeft(30f).size(60f);
        }

        if(uTable != null && group != null && group.payloads != null){
            uTable.clear();
            uTable.left();
            uTable.defaults().pad(3);
            uTable.table(units -> {
                int i = 0;
                for(UnitType payl : group.payloads){
                    if(i < maxVisible || expandPane) units.table(Tex.button, s -> {
                        s.image(payl.uiIcon).size(45f);
                        s.button(Icon.cancelSmall, Styles.emptyi, () -> {
                            group.payloads.remove(payl);
                            updateIcons(group);
                            buildGroups();
                        }).size(20f).padRight(-9f).padLeft(-6f);
                    }).pad(2).margin(12f).fillX();
                    if(++i % 10 == 0) units.row();
                }
            });
            uTable.table(b -> {
                b.defaults().pad(2);
                if(group.payloads.size > 1) b.button(Icon.cancel, () -> {
                    group.payloads.clear();
                    updateIcons(group);
                    buildGroups();
                }).tooltip("@clear").row();
                if(group.payloads.size > maxVisible) b.button(expandPane ? Icon.eyeSmall : Icon.eyeOffSmall, () -> {
                    expandPane = !expandPane;
                    updateIcons(group);
                }).size(45f).tooltip(expandPane ? "@server.shown" : "@server.hidden");
            }).padLeft(6f);
        }
    }

    void numField(String text, Table t, Intc cons, Intp prov, int maxLength){
        if(!text.isEmpty()) t.add(text);
        t.field(prov.get() + "", TextFieldFilter.digitsOnly, input -> {
            if(Strings.canParsePositiveInt(input)){
                cons.get(!input.isEmpty() ? Strings.parseInt(input) : 0);
                buildGroups();
            }
        }).maxTextLength(maxLength);
    }

    void clearFilter(){
        filterHealth = filterAmount = filterAmountWave = 0;
        filterStrict = filterHealthMode = false;
        filterBegin = filterEnd = -1;
        filterEffect = StatusEffects.none;
    }

    enum Sort{
        begin(g -> g.begin, g -> g.type.id),
        health(g -> g.type.health),
        type(g -> g.type.id);

        static final Sort[] all = values();

        final Floatf<SpawnGroup> sort, secondary;

        Sort(Floatf<SpawnGroup> sort){
            this(sort, g -> g.begin);
        }

        Sort(Floatf<SpawnGroup> sort, Floatf<SpawnGroup> secondary){
            this.sort = sort;
            this.secondary = secondary;
        }
    }

    void updateWaves(){
        graph.groups = groups;
        graph.from = start;
        graph.to = start + displayed;
        graph.rebuild();
    }
}