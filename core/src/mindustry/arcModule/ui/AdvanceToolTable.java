package mindustry.arcModule.ui;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import arc.struct.OrderedSet;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.arcModule.*;
import mindustry.arcModule.ui.dialogs.TeamSelectDialog;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.core.World;
import mindustry.entities.units.StatusEntry;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.input.DesktopInput;
import mindustry.input.MobileInput;
import mindustry.type.Item;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.payloads.BuildPayload;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.UnitPayload;

import java.util.Objects;

import static mindustry.Vars.*;
import static mindustry.arcModule.ARCVars.arcui;
import static mindustry.content.UnitTypes.*;
import static mindustry.ui.Styles.*;

public class AdvanceToolTable extends Table {
    private boolean show = false;
    private boolean showGameMode = false, showEntities = false, showTeamChange = false, showTimeControl = false, showCreator = false;

    private boolean enableRTSCode = false;

    //unitFactory
    private int unitCount = 1;
    private float unitRandDst = 1f;
    private Vec2 unitLoc = new Vec2(0, 0);
    private OrderedSet<StatusEntry> unitStatus = new OrderedSet<>();
    private final float[] statusTime = {10, 30f, 60f, 120f, 180f, 300f, 600f, 900f, 1200f, 1500f, 1800f, 2700f, 3600f, Float.MAX_VALUE};
    private Unit spawnUnit = UnitTypes.emanate.create(Team.sharded);
    private Boolean showUnitSelect = false;
    private Boolean showUnitPro = false;
    private Boolean showStatesEffect = false;
    private Boolean showItems = false;
    private Boolean showPayload = false;
    private boolean showSelectPayload = false;
    private Boolean showPayloadBlock = false;
    private boolean elevation = false;
    private float chatTime = 0;

    private final Vec2 initA = new Vec2(0, 0), initB = new Vec2(0, 0), finalA = new Vec2(0, 0);

    public AdvanceToolTable() {
        rebuild();
        Events.on(EventType.ResetEvent.class, e -> {
            if (!state.rules.editor) {
                Core.settings.put("worldCreator", false);
                Core.settings.put("forcePlacement", false);
                Core.settings.put("allBlocksReveal", false);
            }
        });
    }

    void rebuild() {
        clear();
        if (mobile) {
            table(tBox -> {
                tBox.background(Tex.buttonEdge3);
                tBox.button("指挥", cleart, () -> {
                    control.input.commandMode = !control.input.commandMode;
                }).width(80f);

                tBox.button("取消", cleart, () -> {
                    if (control.input instanceof MobileInput input) input.arcClearPlans();
                }).width(80f);
            }).left().row();
        }
        if (!show) {
            table(t -> {
                t.background(Tex.buttonEdge3);
                t.button("[cyan]工具箱", cleart, () -> {
                    show = !show;
                    rebuild();
                }).left().width(70).expandX();

            }).left();
        } else {
            table(t -> {
                if (showEntities) {
                    t.table(tt -> {
                        tt.table(
                                tBox -> {
                                    tBox.background(Tex.pane);
                                    tBox.button(Items.copper.emoji() + "[acid]+", cleart, () -> {
                                        for (Item item : content.items())
                                            player.core().items.set(item, player.core().storageCapacity);
                                    }).width(40f).tooltip("[acid]填满核心的所有资源");
                                    tBox.button(Items.copper.emoji() + "[red]-", cleart, () -> {
                                        for (Item item : content.items()) player.core().items.set(item, 0);
                                    }).width(40f).tooltip("[acid]清空核心的所有资源");
                                }).left();

                        tt.table(tBox -> {
                            tBox.background(Tex.buttonEdge3);
                            tBox.button(UnitTypes.gamma.emoji() + "[acid]+", cleart, () -> {
                                Unit cloneUnit = cloneExactUnit(player.unit());
                                cloneUnit.set(player.x + Mathf.range(8f), player.y + Mathf.range(8f));
                                cloneUnit.add();
                            }).width(40f).tooltip("[acid]克隆");
                            tBox.button(UnitTypes.gamma.emoji() + "[red]×", cleart, () -> player.unit().kill()).width(40f).tooltip("[red]自杀");
                            tBox.button(Icon.waves, clearNonei, this::unitSpawnMenu).width(40f).tooltip("[acid]单位工厂-ARC");
                        }).left();
                    }).left().row();
                }

                if (showCreator) {
                    t.table(tt -> {
                        tt.table(
                                tBox -> {
                                    tBox.background(Tex.pane);
                                    tBox.button("创世神", flatToggleMenut, () -> {
                                        Core.settings.put("worldCreator", !Core.settings.getBool("worldCreator"));
                                    }).checked(b -> Core.settings.getBool("worldCreator")).size(70f, 30f);
                                    tBox.button("强制放置", flatToggleMenut, () -> {
                                        Core.settings.put("forcePlacement", !Core.settings.getBool("forcePlacement"));
                                    }).checked(b -> Core.settings.getBool("forcePlacement")).size(70f, 30f);
                                    tBox.button("解禁", flatToggleMenut, () -> {
                                        Core.settings.put("allBlocksReveal", !Core.settings.getBool("allBlocksReveal"));
                                        ui.hudfrag.blockfrag.rebuild();
                                    }).checked(b -> Core.settings.getBool("allBlocksReveal")).tooltip("[acid]显示并允许建造所有物品").size(50f, 30f);
                                    tBox.button("[orange]复制地形", flatToggleMenut, this::tileCopyer).tooltip("[acid]复制特定地形").size(70f, 30f).checked(jb -> false);
                                }).left();
                    }).left().row();
                }

                if (showTeamChange) {
                    t.table(tBox -> {
                        tBox.background(Tex.buttonEdge3);
                        tBox.add("队伍：").left();
                        for (Team team : Team.baseTeams) {
                            tBox.button(String.format("[#%s]%s", team.color, team.localized()), flatToggleMenut, () -> player.team(team))
                                    .checked(b -> player.team() == team).size(30f, 30f);
                        }
                        tBox.button("[violet]+", flatToggleMenut, () -> {
                            new TeamSelectDialog(team -> player.team(team), player.team()).show();
                        }).checked(b -> {
                            return !Seq.with(Team.baseTeams).contains(player.team());
                        }).tooltip("[acid]更多队伍选择").size(30f, 30f);

                    }).left().row();
                }

                if (showGameMode) {
                    t.table(tBox -> {
                        tBox.background(Tex.buttonEdge3);
                        tBox.add("规则：").left();

                        tBox.button("无限火力", flatToggleMenut, () -> {
                            player.team().rules().cheat = !player.team().rules().cheat;
                        }).checked(b -> player.team().rules().cheat).tooltip("[acid]开关自己队的无限火力").size(90f, 30f);

                        tBox.button("编辑器", flatToggleMenut, () -> {
                            state.rules.editor = !state.rules.editor;
                        }).checked(b -> state.rules.editor).size(70f, 30f);

                        tBox.button("沙盒", flatToggleMenut, () -> {
                            state.rules.infiniteResources = !state.rules.infiniteResources;
                        }).checked(b -> state.rules.infiniteResources).size(50f, 30f);


                    }).left().row();
                }

                t.row();
                t.table(mainBox -> {
                    mainBox.background(Tex.buttonEdge3);
                    mainBox.button("[red]工具箱", cleart, () -> {
                        show = !show;
                        rebuild();
                    }).width(70f);
                    mainBox.button(Items.copper.emoji() + UnitTypes.gamma.emoji(), cleart, () -> {
                        showEntities = !showEntities;
                        rebuild();
                    }).width(50f);

                    mainBox.button(Blocks.worldProcessor.emoji(), cleart, () -> {
                        showCreator = !showCreator;
                        rebuild();
                    }).width(50f);

                    mainBox.button(Blocks.overdriveProjector.emoji(), cleart, () -> {
                        showTimeControl = !showTimeControl;
                        rebuild();
                    }).width(50f);

                    mainBox.button((showGameMode ? "[cyan]" : "[acid]") + "规则", cleart, () -> {
                        showGameMode = !showGameMode;
                        rebuild();
                    }).width(50f);

                    mainBox.button((showTeamChange ? "[cyan]" : "[acid]") + "队伍", cleart, () -> {
                        showTeamChange = !showTeamChange;
                        rebuild();
                    }).width(50f);

                }).left();


            }).left();
        }
    }

    private void tileCopyer() {
        BaseDialog dialog = new BaseDialog("地块复制器");
        dialog.cont.table(t -> {
            t.table(tt -> {
                tt.add("复制区角A：x= ");
                TextField x = tt.field(Strings.autoFixed(initA.x, 2), text -> {
                    initA.x = Float.parseFloat(text);
                }).valid(Strings::canParseFloat).maxTextLength(8).get();

                tt.add("  ,y= ");
                TextField y = tt.field(Strings.autoFixed(initA.y, 2), text -> {
                    initA.y = Float.parseFloat(text);
                }).valid(Strings::canParseFloat).maxTextLength(8).get();

                tt.button(StatusEffects.blasted.emoji(), () -> {
                    if (Marker.markList.size == 0) return;
                    initA.set(World.toTile(Marker.markList.peek().markPos.x), World.toTile(Marker.markList.peek().markPos.y));
                    x.setText(World.toTile(Marker.markList.peek().markPos.x) + "");
                    y.setText(World.toTile(Marker.markList.peek().markPos.y) + "");
                }).tooltip(Marker.markList.size == 0 ? "[red]未标记" : ("选择上个标记点：" + World.toTile(Marker.markList.peek().markPos.x) + "," + World.toTile(Marker.markList.peek().markPos.y))).height(50f);
            }).row();
            t.table(tt -> {
                tt.add("复制区角B：x= ");
                TextField x = tt.field(Strings.autoFixed(initB.x, 2), text -> {
                    initB.x = Float.parseFloat(text);
                }).valid(Strings::canParseFloat).maxTextLength(8).get();

                tt.add("  ,y= ");
                TextField y = tt.field(Strings.autoFixed(initB.y, 2), text -> {
                    initB.y = Float.parseFloat(text);
                }).valid(Strings::canParseFloat).maxTextLength(8).get();

                tt.button(StatusEffects.blasted.emoji(), () -> {
                    if (Marker.markList.size == 0) return;
                    initB.set(World.toTile(Marker.markList.peek().markPos.x), World.toTile(Marker.markList.peek().markPos.y));
                    x.setText(World.toTile(Marker.markList.peek().markPos.x) + "");
                    y.setText(World.toTile(Marker.markList.peek().markPos.y) + "");
                }).tooltip(Marker.markList.size == 0 ? "[red]未标记" : ("选择上个标记点：" + World.toTile(Marker.markList.peek().markPos.x) + "," + World.toTile(Marker.markList.peek().markPos.y))).height(50f);
            }).row();
            t.table(tt -> {
                tt.add("粘贴区左下坐标：x= ");
                TextField x = tt.field(Strings.autoFixed(finalA.x, 2), text -> {
                    finalA.x = Float.parseFloat(text);
                }).valid(Strings::canParseFloat).maxTextLength(8).get();

                tt.add("  ,y= ");
                TextField y = tt.field(Strings.autoFixed(finalA.y, 2), text -> {
                    finalA.y = Float.parseFloat(text);
                }).valid(Strings::canParseFloat).maxTextLength(8).get();

                tt.button(StatusEffects.blasted.emoji(), () -> {
                    if (Marker.markList.size == 0) return;
                    finalA.set(World.toTile(Marker.markList.peek().markPos.x), World.toTile(Marker.markList.peek().markPos.y));
                    x.setText(World.toTile(Marker.markList.peek().markPos.x) + "");
                    y.setText(World.toTile(Marker.markList.peek().markPos.y) + "");
                }).tooltip(Marker.markList.size == 0 ? "[red]未标记" : ("选择上个标记点：" + World.toTile(Marker.markList.peek().markPos.x) + "," + World.toTile(Marker.markList.peek().markPos.y))).height(50f);
            }).row();
            t.button("复制！", () -> {
                arcui.arcInfo("复制蓝图中...\n[orange]测试中功能，请等待后续完善");
                Vec2 left2 = new Vec2(Math.min(initA.x, initB.x), Math.min(initA.y, initB.y));
                for (int x = 0; x <= Math.abs(initA.x - initB.x); x++) {
                    for (int y = 0; y <= Math.abs(initA.y - initB.y); y++) {
                        Tile copyTile = world.tile((int) (left2.x + x), (int) (left2.y + y));
                        Tile thisTile = world.tile((int) (finalA.x + x), (int) (finalA.y + y));
                        Tile.setFloor(thisTile, copyTile.floor(), copyTile.overlay());
                        if (copyTile.build == null)
                            thisTile.setBlock(copyTile.block());
                        else thisTile.setBlock(copyTile.block(), copyTile.build.team, copyTile.build.rotation);

                    }
                }

            }).height(50f).fillX();
        });
        dialog.addCloseButton();
        dialog.show();
    }

    private void unitSpawnMenu() {
        BaseDialog unitFactory = new BaseDialog("单位工厂-ARC");

        Table table = unitFactory.cont;
        Runnable[] rebuild = {null};
        rebuild[0] = () -> {
            table.clear();
            /* Unit */
            table.label(() -> "目标单位：" + spawnUnit.type.emoji() + spawnUnit.type.localizedName);

            table.row();

            Table r = table.table().center().bottom().get();
            r.add("数量：");
            r.field("" + unitCount, text -> {
                unitCount = Integer.parseInt(text);
            }).valid(Strings::canParsePositiveInt).maxTextLength(4).get();

            table.row();

            r.add("生成范围：");
            r.field(Strings.autoFixed(unitRandDst, 3), text -> {
                unitRandDst = Float.parseFloat(text);
            }).valid(Strings::canParsePositiveFloat).tooltip("在目标点附近的这个范围内随机生成").maxTextLength(8).get();
            r.add("格");
            table.row();

            table.table(t -> {
                t.add("坐标：x= ");
                t.field(Strings.autoFixed(unitLoc.x, 2), text -> {
                    unitLoc.x = Float.parseFloat(text);
                }).valid(Strings::canParseFloat).maxTextLength(8).get();

                t.add("  ,y= ");
                t.field(Strings.autoFixed(unitLoc.y, 2), text -> {
                    unitLoc.y = Float.parseFloat(text);
                }).valid(Strings::canParseFloat).maxTextLength(8).get();

                t.button(gamma.emoji(), () -> {
                    unitLoc.set(player.tileX(), player.tileY());
                    rebuild[0].run();
                }).tooltip("选择玩家当前位置：" + player.tileX() + "," + player.tileY()).height(50f);

                t.button(StatusEffects.blasted.emoji(), () -> {
                    if (Marker.markList.size == 0) return;

                    unitLoc.set(World.toTile(Marker.markList.peek().markPos.x), World.toTile(Marker.markList.peek().markPos.y));
                    rebuild[0].run();
                }).tooltip(Marker.markList.size == 0 ? "[red]未标记" : ("选择上个标记点：" + World.toTile(Marker.markList.peek().markPos.x) + "," + World.toTile(Marker.markList.peek().markPos.y))).height(50f);
            });

            table.row();

            table.button(Blocks.tetrativeReconstructor.emoji() + "[cyan]单位状态重构厂", () -> unitFabricator(spawnUnit)).fillX();

            table.row();

            table.button("[orange]生成！", Icon.modeAttack, () -> {
                for (var n = 0; n < unitCount; n++) {
                    Tmp.v1.rnd(Mathf.random(unitRandDst * tilesize));
                    Unit unit = cloneUnit(spawnUnit);
                    if (elevation) unit.elevation = 1f;
                    unit.set(unitLoc.x * tilesize + Tmp.v1.x, unitLoc.y * tilesize + Tmp.v1.y);
                    unitStatus.each(e -> {
                        unit.addEntry(e.effect, e.time * 60f);
                    });
                    unit.add();
                }
                if (control.input instanceof DesktopInput input) {
                    input.panning = true;
                }
                Core.camera.position.set(unitLoc.x * tilesize, unitLoc.y * tilesize);
                unitFactory.closeOnBack();
            }).fillX();

            if (Core.settings.getBool("easyJS")) {
                table.row();
                table.button("[orange] 生成！(/js)", Icon.modeAttack, () -> {
                    if (chatTime > 0f) {
                        arcui.arcInfo("为了防止因ddos被服务器ban，请勿太快操作", 5f);
                        return;
                    }
                    chatTime = 1f;
                    arcui.arcInfo("已生成单个单位。\n[gray]请不要短时多次使用本功能，否则容易因ddos被服务器ban", 5f);
                    Tmp.v1.rnd(Mathf.random(unitRandDst)).add(unitLoc.x, unitLoc.y).scl(tilesize);
                    sendFormatChat("/js u = UnitTypes.@.create(Team.get(@))",
                            spawnUnit.type.name,
                            spawnUnit.team.id
                            );
                    sendFormatChat("/js u.set(@,@)",
                            unitLoc.x * tilesize,
                            unitLoc.y * tilesize
                    );
                    if (spawnUnit.health != spawnUnit.type.health) {
                        sendFormatChat("/js u.health = @", spawnUnit.health);
                        if (spawnUnit.health > spawnUnit.type.health) {
                            sendFormatChat("/js u.maxHealth = @", spawnUnit.health);
                        }
                    }
                    if (spawnUnit.shield != 0)
                        sendFormatChat("/js u.shield = @", spawnUnit.shield);
                    if (elevation)
                        sendFormatChat("/js u.elevation = 1");
                    if (!unitStatus.isEmpty()) {
                        sendFormatChat("/js gs=(t,o,n)=>{try{let f=t.getDeclaredField(n);f.setAccessible(true);return f.get(o)}catch(e){let s=t.getSuperclass();return s?gs(s,o,n):null}}");
                        sendFormatChat("/js statuses = gs(u.class,u,\"statuses\")");
                        unitStatus.each(entry -> {
                            if (!entry.effect.reactive) {
                                sendFormatChat("/js {e = new StatusEntry().set(StatusEffects.@, @);statuses.add(e);statuses.size}", entry.effect.name, entry.time * 60f);
                            }
                            else sendFormatChat("/js u.apply(StatusEffects.@)", entry.effect.name);
                        });
                        sendFormatChat("/js delete statuses");
                    }
                    if (spawnUnit.hasItem()) {
                        sendFormatChat("/js u.addItem(Items.@, @)", spawnUnit.stack.item.name, spawnUnit.stack.amount);
                    }
                    sendFormatChat("/js u.add()");
                    sendFormatChat("/js delete u");
                    Time.run(chatTime, () -> chatTime = 0f);
                    if (control.input instanceof DesktopInput input) {
                        input.panning = true;
                    }
                    Core.camera.position.set(unitLoc.x * tilesize, unitLoc.y * tilesize);
                    unitFactory.closeOnBack();
                }).fillX();
            }
            if (enableRTSCode) {
                table.row();
                table.button("RTS价格代码生成", Icon.logic, this::generateRTSCode).fillX();
            }
        };
        rebuild[0].run();

        unitFactory.addCloseButton();
        unitFactory.show();
    }

    private void unitFabricator(Unit unit) {
        BaseDialog rebuildFabricatorTable = new BaseDialog("单位加工车间");
        Table table = new Table();

        ScrollPane pane = new ScrollPane(table);

        Runnable[] rebuildTable = {null};
        rebuildTable[0] = () -> {
            table.clear();

            table.button("加工单位：" + unit.type.emoji(), showUnitSelect ? Icon.upOpen : Icon.downOpen, Styles.togglet, () -> {
                showUnitSelect = !showUnitSelect;
                rebuildTable[0].run();
            }).fillX().minWidth(400f).row();

            if (showUnitSelect) {
                table.table(list -> {
                    int i = 0;
                    for (UnitType units : content.units()) {
                        if (i++ % 8 == 0) list.row();
                        list.button(units.emoji() + (enableRTSCode ? getPrice(units) : ""), cleart, () -> {
                            if (unit.type != units) {
                                changeUnitType(unit, units);
                                rebuildTable[0].run();
                            }
                            showUnitSelect = !showUnitSelect;
                            rebuildTable[0].run();
                        }).tooltip(units.localizedName).width(enableRTSCode ? 100f : 50f).height(50f);
                    }
                }).row();
            }

            table.button("[#" + unit.team.color + "]单位属性", showUnitPro ? Icon.upOpen : Icon.downOpen, Styles.togglet, () -> {
                showUnitPro = !showUnitPro;
                rebuildTable[0].run();
            }).fillX().row();
            if (showUnitPro) {
                table.table(t -> {
                    t.table(tt -> {
                        tt.add("[red]血：");
                        tt.field(Strings.autoFixed(unit.health, 1), text -> unit.health = Float.parseFloat(text)).valid(Strings::canParsePositiveFloat);
                        tt.add("[yellow]盾：");
                        tt.field(Strings.autoFixed(unit.shield, 1), text -> unit.shield = Float.parseFloat(text)).valid(Strings::canParsePositiveFloat);
                    }).row();
                    t.table(tt -> {
                        tt.add("队伍：");
                        tt.field(String.valueOf(unit.team.id), text -> {
                            unit.team = Team.get(Integer.parseInt(text));
                        }).valid(text -> {
                            return Strings.canParsePositiveInt(text) && Integer.parseInt(text) < Team.all.length;
                        }).maxTextLength(4).get();
                        for (Team team : Team.baseTeams) {
                            tt.button("[#" + team.color + "]" + team.localized(), flatToggleMenut, () -> {
                                unit.team = team;
                                rebuildTable[0].run();
                            }).checked(b -> unit.team == team).size(30, 30);
                        }
                        tt.button("[violet]+", flatToggleMenut, () -> {
                            new TeamSelectDialog(team -> {
                                unit.team = team;
                                rebuildTable[0].run();
                            }, unit.team).show();
                        }).checked(b -> {
                            return !Seq.with(Team.baseTeams).contains(unit.team);
                        }).tooltip("[acid]更多队伍选择").center().width(50f).row();
                    }).row();
                    t.table(list -> {
                        list.check("飞行模式    [orange]生成的单位会飞起来", elevation, a -> elevation = !elevation).center().padBottom(5f).padRight(10f);
                        /*
                        if (Core.settings.getBool("developMode")) {
                            TextField sField = list.field(elevation + "", text -> elevation = Float.parseFloat(text)).
                                    valid(text -> Strings.canParseFloat(text)).tooltip("单位层级").maxTextLength(10).get();
                            list.add("层");
                            Slider sSlider = list.slider(-10, 10, 0.05f, 0, n -> {
                                if (elevation != n) {
                                    sField.setText(n + "");
                                }
                                elevation = n;
                            }).get();
                            sField.update(() -> sSlider.setValue(elevation));
                        }*/
                    });
                }).row();
            }

            StringBuilder unitStatusText = new StringBuilder("单位状态 ");
            for (var entry : unitStatus) {
                unitStatusText.append(entry.effect.emoji());
            }
            table.button(unitStatusText.toString(), showStatesEffect ? Icon.upOpen : Icon.downOpen, Styles.togglet, () -> {
                showStatesEffect = !showStatesEffect;
                rebuildTable[0].run();
            }).fillX().row();

            if (showStatesEffect) {
                table.table(t -> {
                    t.table(list -> {
                        int i = 0;
                        for (StatusEffect effects : content.statusEffects()) {
                            if (effects == StatusEffects.none) continue;
                            if (i++ % 8 == 0) list.row();
                            list.button(effects.emoji(), squareTogglet, () -> {
                                unitStatus.add(new StatusEntry().set(effects, unitStatus.isEmpty() ? 600f : unitStatus.orderedItems().peek().time));
                                rebuildTable[0].run();
                            }).size(50f).color(unitStatus.select(e -> e.effect == effects).isEmpty() ? Color.gray : Color.white).tooltip(effects.localizedName);
                        }
                    }).top().center();

                    t.row();

                    float[] status = {1f, 1f, 1f, 1f};
                    unitStatus.each(s -> {
                        status[0] *= s.effect.healthMultiplier;
                        status[1] *= s.effect.damageMultiplier;
                        status[2] *= s.effect.reloadMultiplier;
                        status[3] *= s.effect.speedMultiplier;
                    });
                    Table statusText = UnitType.getStatustext(status[0], status[1], status[2], status[3]);
                    if (statusText != null) {
                        t.add(statusText).row();
                    }
                    t.table(list -> {
                        for (var entry : unitStatus) {
                            list.add(entry.effect.emoji() + entry.effect.localizedName + " ");

                            if (entry.effect.permanent) {
                                list.add("<永久状态>");
                            } else if(entry.effect.reactive) {
                                list.add("<瞬间状态>");
                            } else {
                                list.table(et -> {
                                    TextField sField = et.field(checkInf(entry.time), text -> {
                                        entry.time = Objects.equals(text, "Inf") ? Float.MAX_VALUE : Float.parseFloat(text);
                                    }).valid(text -> Objects.equals(text, "Inf") || Strings.canParsePositiveFloat(text)).tooltip("buff持续时间(单位：秒)").maxTextLength(10).get();

                                    et.add("秒");

                                    Slider sSlider = et.slider(0f, statusTime.length - 1f, 1f, statusTimeIndex(entry.time), n -> {
                                        if (statusTimeIndex(entry.time) != n) {
                                            sField.setText(checkInf(statusTime[(int) n]));
                                        }
                                        entry.time = statusTime[(int) n];
                                    }).get();

                                    sField.update(() -> sSlider.setValue(statusTimeIndex(entry.time)));
                                });
                            }

                            list.button(Icon.cancel, () -> {
                                unitStatus.remove(entry);
                                rebuildTable[0].run();
                            });
                            list.row();
                        }
                    });
                }).fillX().row();
            }

            String unitItemText = "单位物品 ";
            if (unit.stack.amount > 0 && !showItems) {
                unitItemText += unit.stack.item.emoji() + " " + unit.stack.amount;
            }
            table.button(unitItemText, showItems ? Icon.upOpen : Icon.downOpen, Styles.togglet, () -> {
                showItems = !showItems;
                rebuildTable[0].run();
            }).fillX().row();
            if (showItems) {
                table.table(pt -> {
                    pt.table(ptt -> {
                        int i = 0;
                        for (Item item : content.items()) {
                            ptt.button(item.emoji(), cleart, () -> {
                                unit.stack.item = item;
                                if (unit.stack.amount == 0) {
                                    unit.stack.amount = unit.itemCapacity();
                                }
                                rebuildTable[0].run();
                            }).size(50f).left().tooltip(item.localizedName);
                            if (++i % 6 == 0) ptt.row();
                        }
                    });
                    if (unit.stack.amount > 0) {
                        pt.row();
                        pt.table(ptt -> {
                            ptt.add(unit.stack.item.emoji() + " 数量：");
                            ptt.field(String.valueOf(unit.stack.amount), text -> {
                                unit.stack.amount = Integer.parseInt(text);
                            }).valid(value -> {
                                if (!Strings.canParsePositiveInt(value)) return false;
                                int val = Integer.parseInt(value);
                                return 0 < val && val <= unit.type.itemCapacity;
                            }).maxTextLength(4);
                            ptt.add("/ " + unit.type.itemCapacity + " ");
                            ptt.button(Icon.up, cleari, () -> {
                                unit.stack.amount = unit.type.itemCapacity;
                                rebuildTable[0].run();
                            }).tooltip("设置物品数量为单位最大容量");
                            ptt.button(Icon.down, cleari, () -> {
                                unit.stack.amount = 0;
                                rebuildTable[0].run();
                            }).tooltip("清空单位物品");
                        });
                    }
                }).row();
            }

            if (unit instanceof Payloadc pay) {
                StringBuilder unitPayloadText = new StringBuilder("单位背包 ");
                for (Payload payload : pay.payloads()) {
                    unitPayloadText.append(payload.content().emoji());
                }
                table.button(unitPayloadText.toString(), showPayload ? Icon.upOpen : Icon.downOpen, Styles.togglet, () -> {
                    showPayload = !showPayload;
                    rebuildTable[0].run();
                }).fillX().checked(showPayload).row();
            }

            if (showPayload) {
                table.table(p -> {
                    if (unit instanceof Payloadc pay) {
                        p.table(pt -> {
                            pay.payloads().each(payload -> {
                                if (payload instanceof Payloadc payloadUnit) {
                                    pt.button(payload.content().emoji() + "[red]*", squareTogglet, () -> {
                                        pay.payloads().remove(payload);
                                        rebuildTable[0].run();
                                    }).color(payloadUnit.team().color).size(50f).left();
                                } else {
                                    pt.button(payload.content().emoji(), squareTogglet, () -> {
                                        pay.payloads().remove(payload);
                                        rebuildTable[0].run();
                                    }).size(50f).left();
                                }
                                if (pay.payloads().indexOf(payload) % 8 == 7) pt.row();
                            });
                        }).row();

                        p.button("载入单位 " + UnitTypes.mono.emoji(), showSelectPayload ? Icon.upOpen : Icon.downOpen, Styles.togglet, () -> {
                            showSelectPayload = !showSelectPayload;
                            rebuildTable[0].run();
                        }).width(300f).row();

                        if (showSelectPayload) {
                            p.table(list -> {
                                int i = 0;
                                for (UnitType units : content.units()) {
                                    list.button(units.emoji(), () -> {
                                        pay.addPayload(new UnitPayload(units.create(unit.team)));
                                        rebuildTable[0].run();
                                    }).size(50f).tooltip(units.localizedName);
                                    if (++i % 8 == 0) list.row();
                                }
                            });
                            p.row();
                            p.table(pt -> {
                                pt.button("[cyan]自递归", () -> {
                                    pay.pickup(cloneUnit(unit));
                                    rebuildTable[0].run();
                                }).width(200f);
                                pt.button("?", () -> ui.showInfo("""
                                        使用说明：携带的单位存在一个序列，每个单位可以具备特定的属性。
                                        [cyan]自递归[white]是指根据当前的配置生成一个单位，并储存到载荷序列上
                                        这一单位具备所有目前设置的属性，包括buff、物品和载荷。
                                        合理使用自递归可以发掘无限的可能性
                                        [orange][警告]可能导致地图损坏！请备份地图后再使用！""")).size(50f);
                            }).row();
                        }

                        p.button("载入建筑 " + Blocks.surgeWallLarge.emoji(), showPayloadBlock ? Icon.upOpen : Icon.downOpen, Styles.togglet, () -> {
                            showPayloadBlock = !showPayloadBlock;
                            rebuildTable[0].run();
                        }).width(300f).row();

                        if (showPayloadBlock) {
                            p.table(list -> {
                                int i = 0;
                                for (Block payBlock : content.blocks()) {
                                    if (!payBlock.isVisible() || !payBlock.isAccessible() || payBlock.isFloor())
                                        continue;
                                    list.button(payBlock.emoji(), () -> {
                                        pay.addPayload(new BuildPayload(payBlock, unit.team));
                                        rebuildTable[0].run();
                                    }).size(50f).tooltip(payBlock.localizedName);
                                    if (++i % 8 == 0) list.row();
                                }
                            });
                        }
                    }
                }).fillX().row();
            }

            table.row();
            table.button("[red]重置出厂状态", () -> {
                resetUnitType(unit, unit.type);
                rebuildTable[0].run();
                rebuildFabricatorTable.closeOnBack();
            }).fillX().row();
            //table.add("[orange]单位加工车间。 [white]Made by [violet]Lucky Clover\n").width(400f);
        };


        rebuildTable[0].run();
        rebuildFabricatorTable.cont.add(pane);
        rebuildFabricatorTable.addCloseButton();
        rebuildFabricatorTable.show();
    }

    private Unit cloneExactUnit(Unit unit) {
        Unit reUnit = unit.type.create(unit.team);
        reUnit.health = unit.health;
        reUnit.shield = unit.shield;
        reUnit.stack = unit.stack;

        for (StatusEffect effects : content.statusEffects()) {
            if (unit.getDuration(effects) > 0f) reUnit.apply(effects, unit.getDuration(effects));
        }

        if (unit instanceof Payloadc pay && reUnit instanceof Payloadc rePay) {
            pay.payloads().each(rePay::addPayload);
        }
        return reUnit;
    }

    private Unit cloneUnit(Unit unit) {
        Unit reUnit = unit.type.create(unit.team);
        reUnit.health = unit.health;
        reUnit.shield = unit.shield;
        reUnit.stack = unit.stack;

        if (unit instanceof Payloadc pay && reUnit instanceof Payloadc rePay) {
            pay.payloads().each(rePay::addPayload);
        }
        return reUnit;
    }

    private void resetUnitType(Unit unit, UnitType unitType) {
        elevation = false;
        unit.type = unitType;
        unit.health = unitType.health;
        unit.shield = 0;
        unit.stack.amount = 0;
        if (unit instanceof Payloadc pay) {
            pay.payloads().clear();
        }
        unitStatus.clear();
    }

    private void changeUnitType(Unit unit, UnitType unitType) {
        unit.type = unitType;
        unit.health = unitType.health;
        unit.shield = 0;
        if (unit.stack.amount > unit.itemCapacity()) {
            unit.stack.amount = unit.itemCapacity();
        }
        unitStatus.clear();
    }

    private String checkInf(float value) {
        if (value == Float.MAX_VALUE) {
            return "Inf";
        }
        return Strings.autoFixed(value, 1);
    }

    private int statusTimeIndex(float time) {
        for (int i = statusTime.length - 1; i >= 0; i--) {
            if (statusTime[i] <= time) {
                return i;
            }
        }
        return 0;
    }
    private void sendFormatChat(String format, Object... args) {
        for (int i = 0;i < args.length;i++) {
            if (args[i] instanceof Float f) {
                args[i] = Strings.autoFixed(f, 1);
            }
        }
        Time.run(chatTime, () -> Call.sendChatMessage(Strings.format(format, args)));
        chatTime = chatTime + 10f;
    }

    private int getPrice(UnitType unitType) {
        return (int) (unitType.health * (1 + unitType.range / 8 / 50) / 20);
    }

    private void generateRTSCode() {
        StringBuilder code = new StringBuilder();
        Vars.content.units().each(unitType -> {
            code.append("set 单位 @").append(unitType.name).append("\n");
            code.append("set 价格 ").append(getPrice(unitType)).append("\n");
            code.append("set 工厂 ");
            if (unitType.flying) {
                if (unitType.health < 400) code.append("空1");
                else code.append("空2");
            } else {
                if (unitType.allowLegStep || unitType.naval) code.append("海爬");
                else code.append("陆");
                if (unitType.health < 720) code.append("1");
                else code.append("2");
            }
            code.append("\n");
            code.append("set 名称 \"").append(unitType.emoji()).append(" ").append(unitType.localizedName).append(" ").append(unitType.name).append("\"\n");
            code.append("set @counter c返回").append("\n");

        });
        Core.app.setClipboardText(code.toString());
    }
}