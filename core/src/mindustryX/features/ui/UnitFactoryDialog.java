package mindustryX.features.ui;

import arc.*;
import arc.flabel.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.actions.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.mod.Mods.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustryX.features.*;
import mindustryX.features.func.*;

import static mindustry.Vars.*;
import static mindustry.ui.Styles.*;

public class UnitFactoryDialog extends BaseDialog{
    private static final int maxCount = 50;

    UnitStack selected = UnitStack.vanillaStack;

    private int unitCount = 1;
    private float unitRandDst = 0;
    /** 仅作为部分数据的载体 */
    private Unit spawnUnit = UnitTypes.emanate.create(Team.sharded);
    /** 载荷独立存储 */
    private final Seq<Payload> unitPayloads = new Seq<>();

    private float chatTime = 0;
    private boolean lookingLocation;
    private Table selection, infoTable, posTable, countTable, itemTable, propertiesTable, teamTable, effectTable, payloadTable;

    public UnitFactoryDialog(){
        super("单位工厂");

        closeOnBack();
        addCloseButton();

        //Lazy build
        shown(() -> {
            if(cont.hasChildren()) return;

            setup();
        });
    }

    private void setup(){
        cont.top();

        selection = new Table();
        infoTable = new Table();
        posTable = new Table();
        countTable = new Table();
        itemTable = new Table();
        propertiesTable = new Table();
        teamTable = new Table();
        effectTable = new Table();
        payloadTable = new Table();

        setupCountTable();
        setupPosTable();
        rebuildTables();

        buttons.button("重置", Icon.refresh, () -> {
            resetUnit(spawnUnit);
        });
        buttons.button("[orange]生成！", Icon.modeAttack, this::spawn);

        rebuild();

        resized(this::rebuild);

        Events.run(Trigger.draw, () -> {
            if(!lookingLocation){
                return;
            }

            Draw.z(Layer.overlayUI);
            Draw.color(spawnUnit.team.color);
            Lines.circle(spawnUnit.x, spawnUnit.y, 10);
            Draw.rect(spawnUnit.type.uiIcon, spawnUnit.x, spawnUnit.y, 10, 10, Time.time % 360);
            Draw.reset();

            Vec2 mouse = Core.input.mouseWorld();
            Tile tile = world.tileWorld(mouse.x, mouse.y);
            if(tile != null){
                Drawf.dashRect(Pal.accent, tile.drawx() - tilesize/2f, tile.drawy() - tilesize/2f, tilesize, tilesize);

                Vec2 tilePos = Tmp.v1.set(tile.x, tile.y);
                Vec2 textPos = Tmp.v2.set(tile).sub(0, tilesize);
                FuncX.drawText(textPos, "" + tilePos, 1f, Pal.accent, Align.top, Fonts.outline);
                Draw.reset();
            }
        });

//        cont.button("[orange]生成(/js)", Icon.modeAttack, () -> {
//            if(chatTime > 0f){
//                ui.showInfoFade("为了防止因ddos被服务器ban，请勿太快操作", 5f);
//                return;
//            }
//            chatTime = 1f;
//            ui.showInfoFade("已生成单个单位。\n[gray]请不要短时多次使用本功能，否则容易因ddos被服务器ban", 5f);
//            Tmp.v1.rnd(Mathf.random(unitRandDst)).add(unitLoc.x, unitLoc.y).scl(tilesize);
//            sendFormatChat("/js u = UnitTypes.@.create(Team.get(@))",
//            spawnUnit.type.name,
//            spawnUnit.team.id
//            );
//            sendFormatChat("/js u.set(@,@)",
//            unitLoc.x * tilesize,
//            unitLoc.y * tilesize
//            );
//            if(spawnUnit.health != spawnUnit.type.health){
//                sendFormatChat("/js u.health = @", spawnUnit.health);
//                if(spawnUnit.health > spawnUnit.type.health){
//                    sendFormatChat("/js u.maxHealth = @", spawnUnit.health);
//                }
//            }
//            if(spawnUnit.shield != 0)
//                sendFormatChat("/js u.shield = @", spawnUnit.shield);
//            if(elevation)
//                sendFormatChat("/js u.elevation = 1");
//            if(!unitStatus.isEmpty()){
//                sendFormatChat("/js gs=(t,o,n)->{try{let f=t.getDeclaredField(n);f.setAccessible(true);return f.get(o)}catch(e){let s=t.getSuperclass();return s?gs(s,o,n):null}}");
//                sendFormatChat("/js statuses = gs(u.class,u,\"statuses\")");
//                unitStatus.each(entry -> {
//                    if(!entry.effect.reactive){
//                        sendFormatChat("/js {e = new StatusEntry().set(StatusEffects.@, @);statuses.add(e);statuses.size}", entry.effect.name, entry.time * 60f);
//                    }else sendFormatChat("/js u.apply(StatusEffects.@)", entry.effect.name);
//                });
//                sendFormatChat("/js delete statuses");
//            }
//            if(spawnUnit.hasItem()){
//                sendFormatChat("/js u.addItem(Items.@, @)", spawnUnit.stack.item.name, spawnUnit.stack.amount);
//            }
//            sendFormatChat("/js u.add()");
//            sendFormatChat("/js delete u");
//            Time.run(chatTime, () -> chatTime = 0f);
//            control.input.panCamera(Tmp.v1.set(unitLoc).scl(tilesize));
//        }).fillX().visible(() -> Core.settings.getBool("easyJS")).row();
    }

    private void spawn(){
        if(spawnUnit instanceof Payloadc payloadUnit){
            payloadUnit.payloads().set(unitPayloads);
        }

        for(int n = 0; n < unitCount; n++){
            Unit unit = cloneUnit(spawnUnit);

            Vec2 pos = Tmp.v1.set(unit);
            Vec2 offset = Tmp.v2.rnd(Mathf.random(unitRandDst * tilesize));
            unit.set(pos.add(offset));

            unit.add();
        }

//        control.input.panCamera(pos);
    }

    private void rebuild(){
        Table main = new Table();

        cont.clearChildren();
        main.defaults().growX().expandY().top();

        if(Core.graphics.isPortrait()){
            cont.pane(noBarPane, main).growX();
        }else{
            float width = Core.scene.getWidth() * (Core.scene.getWidth() > 1500 ? 0.6f : 0.9f) / Scl.scl();
            cont.add(main).width(width).growY();
        }

        if(!selection.hasChildren()){
            rebuildUnitSelection();
        }

        // 等table分配大小后重构
        Core.app.post(this::rebuildUnitSelection);

        Table rightTable = new Table();

        rightTable.top();
        rightTable.defaults().growX();

        rightTable.add(infoTable).row();

        rightTable.defaults().padTop(12f);

        rightTable.add(posTable).row();
        rightTable.add(countTable).row();
        rightTable.table(randDstTable -> {
            randDstTable.add("生成范围：");
            randDstTable.field(Strings.autoFixed(unitRandDst, 3), text -> {
                unitRandDst = Float.parseFloat(text);
            }).valid(Strings::canParsePositiveFloat).tooltip("在目标点附近的这个范围内随机生成").maxTextLength(6).padLeft(4f);
            randDstTable.add("格").expandX().left();
        }).row();
        rightTable.add(itemTable).row();
        rightTable.add(propertiesTable).row();

        rightTable.add(teamTable).row();

        rightTable.table(bottomTable -> {
            bottomTable.left();
            bottomTable.defaults().top().left();

            bottomTable.add(effectTable);

            bottomTable.defaults().padLeft(16f);
            bottomTable.add(payloadTable);
        }).padTop(8f).fillY();

        Cell<?> unitPropsTable, selectionCell = main.add(selection);

        if(Core.graphics.isPortrait()){
            main.row();
        }

        if(Core.graphics.isPortrait()){
            unitPropsTable = main.add(rightTable);
        }else{
            unitPropsTable = main.pane(Styles.noBarPane, rightTable).scrollX(false);
        }

        if(Core.graphics.isPortrait()){
            selectionCell.maxHeight(10 * 64f);
            selectionCell.padBottom(8f);
        }else{
            selectionCell.padRight(8f);
            selectionCell.expand(1, 1).fill();
            unitPropsTable.expand(4, 1).fill();
        }
    }

    private void rebuildTables(){
        rebuildInfoTable(spawnUnit, infoTable);
        rebuildPropertiesTable(spawnUnit, propertiesTable);
        rebuildItemTable(spawnUnit, itemTable);
        rebuildTeamTable(spawnUnit, teamTable);
        rebuildEffectsTable(spawnUnit, effectTable);
        rebuildPayloadTable(spawnUnit, payloadTable);
    }

    private void setSpawnUnitType(UnitType unitType){
        spawnUnit.type = unitType;
        spawnUnit = cloneUnit(spawnUnit);
        spawnUnit.health = unitType.health;

        if(spawnUnit instanceof PayloadUnit payloadUnit){
            payloadUnit.payloads = unitPayloads;
        }

        rebuildTables();
    }

    private void rebuildUnitSelection(){
        selection.top();
        selection.clearChildren();

        Table unitSelectTable = new Table(Tex.whiteui);
        unitSelectTable.setColor(Pal.gray);

        selection.table(Tex.whiteui, stackSelectTable -> {
            stackSelectTable.left();
            stackSelectTable.setColor(Pal.gray);

            final int[] i = {0};
            int rows = Math.max(1, (int)((selection.getWidth() - 16f) / (184f + 8f) / Scl.scl()));
            UnitStack.getUnitStacks().each(stack -> {
                stackSelectTable.button(b -> {
                    TextureRegion region = stack.icon();
                    Drawable icon = region == null ? Tex.nomap : new TextureRegionDrawable(region);

                    b.image(icon).scaling(Scaling.fit).size(48f).pad(8f);

                    b.add(stack.name()).width(128f);
                }, clearTogglei, () -> {
                    selected = stack;
                    rebuildSelectTable(selected, unitSelectTable);
                }).height(64f).pad(8f).checked(b -> selected == stack);

                if(++i[0] % rows == 0){
                    stackSelectTable.row();
                }
            });
        }).fillX().row();

        selection.pane(noBarPane, unitSelectTable).scrollX(false).pad(8).padTop(0).growX();
        rebuildSelectTable(selected, unitSelectTable);

        selection.row();
    }

    private void rebuildSelectTable(UnitStack stack, Table table){
        final int[] i = {0};
        int rows = Math.max(1, (int)((selection.getWidth() - 4f * 2) / (64f + 4f) / Scl.scl()));

        table.clearChildren();

        stack.units.each(unit -> {
            // block单位只会造成崩溃
            if(unit == UnitTypes.block) return;

            table.button(new TextureRegionDrawable(unit.uiIcon), clearTogglei, 32f, () -> {
                setSpawnUnitType(unit);
            }).margin(3).size(64f).pad(4f).checked(b -> spawnUnit.type == unit);

            if(++i[0] % rows == 0){
                table.row();
            }
        });
    }

    private void setupPosTable(){
        posTable.add("生成位置:");

        posTable.label(() -> {
            int tileX = World.toTile(spawnUnit.x);
            int tileY = World.toTile(spawnUnit.y);
            return "" + Tmp.v1.set(tileX, tileY);
        }).left().expandX().padLeft(4);

        posTable.defaults().size(48);
        posTable.button(Icon.pickSmall, clearNonei, () -> {
            lookingLocation = true;
            hide();

            UIExt.hitter(true, (cx, cy, hider) -> {
                Vec2 v = Core.camera.unproject(cx, cy);
                spawnUnit.set(v);

                hider.run();

                // 给个时间反应一下
                Timer.schedule(() -> {
                    show();
                    lookingLocation = false;
                }, 0.5f);
            }, "[green]点击屏幕采集坐标");
        });

        posTable.button(Icon.eyeSmall, clearNonei, () -> {
            lookingLocation = true;
            hide();

            Core.camera.position.set(spawnUnit);
            if(control.input instanceof DesktopInput input){
                input.panning = true;
            }

            UIExt.hitter(false, (cx, cy, hider) -> {
                hider.run();

                show();

                lookingLocation = false;
            }, "[green]点击屏幕返回");
        }).padLeft(4);

        posTable.button(new TextureRegionDrawable(UnitTypes.gamma.uiIcon), clearNonei, 24, () -> {
            spawnUnit.set(player.unit());
        }).padLeft(4);
    }

    private void setupCountTable(){
        countTable.add("生成数量:");

        Cons<Integer>[] changeCount = new Cons[1];

        TextField field = countTable.field("" + unitCount, text -> {
            changeCount[0].get(Math.min(maxCount, Strings.parseInt(text)));
        }).left().expandX().width(80).valid(Strings::canParseInt).get();

        countTable.button(b -> b.add("MAX"), clearNonei, () -> {
            changeCount[0].get(maxCount);
        }).size(48f).padLeft(4);

        Slider slider = countTable.slider(1, maxCount, 1, 1, n -> {
            changeCount[0].get((int)n);
        }).width(128).padLeft(4).get();

        changeCount[0] = n -> {
            unitCount = n;
            field.setText("" + n);
            slider.setValue(n);
        };
    }

    private void rebuildInfoTable(Unit unit, Table infoTable){
        infoTable.clearChildren();

        infoTable.table(Tex.pane, imageTable -> {
            imageTable.image(unit.type.uiIcon).size(112).scaling(Scaling.fit);
        }).top();

        infoTable.table(null, rightTable -> {
            rightTable.table(Tex.pane, labelTable -> {
                String name = unit.type.name;
                String localizedName = unit.type.localizedName;
                boolean hasLocalized = !name.equals(localizedName);
                String text = localizedName +
                (hasLocalized ? "(" + name + ")" : "") +
                "[" + unit.type.id + "]";

                Label label = labelTable.add(text).labelAlign(Align.left).left().expandX().get();

                labelTable.table(null, buttons -> {
                    buttons.button(Icon.copySmall, clearNonei, 16, () -> {
                        Core.app.setClipboardText("" + label.getText());

                        ui.announce("复制成功:" + label.getText(), 3);
                    }).size(28);

                    buttons.button("i", () -> {
                        ui.content.show(unit.type);
                    }).size(28).padLeft(4);
                }).right();
            }).growX().row();

            rightTable.pane(noBarPane, describeTable -> {
                describeTable.background(Tex.pane);

                describeTable.label(() -> unit.type.description).labelAlign(Align.left).grow().wrap();
            }).grow().maxHeight(80).padTop(4).scrollX(false);
        }).grow().padLeft(4);
    }

    private void rebuildPropertiesTable(Unit unit, Table propertiesTable){
        propertiesTable.clearChildren();
        propertiesTable.defaults().expandX().left();

        propertiesTable.table(t -> {
            t.check("飞行模式    [orange]生成的单位会飞起来", unit.elevation > 0, a -> {
                unit.elevation = a ? 1 : 0;
            }).padBottom(5f).padRight(10f)
            .checked(b -> unit.elevation > 0);
        });

        propertiesTable.row();

        propertiesTable.table(healthTable -> {
            healthTable.add("[red]血量：");
            healthTable.field(Strings.autoFixed(unit.health, 1), text -> {
                unit.health = Float.parseFloat(text);
            }).valid(Strings::canParsePositiveFloat).padLeft(4f);
        });

        propertiesTable.table(shieldTable -> {
            shieldTable.add("[yellow]护盾：");
            shieldTable.field(Strings.autoFixed(unit.shield, 1), text -> {
                unit.shield = Float.parseFloat(text);
            }).valid(Strings::canParsePositiveFloat).padLeft(4f);
        });
    }

    private void rebuildItemTable(Unit unit, Table itemTable){
        itemTable.clearChildren();

        int itemCapacity = unit.itemCapacity();

        if(itemCapacity == 0) return;

        itemTable.add("携带物品:");

        Cons<Item>[] changeItem = new Cons[1];
        Cons<Integer>[] changeItemCount = new Cons[1];

        Image image = new Image(getItemIcon(unit.item()));
        itemTable.button(b -> {
            b.add(image).size(48f).scaling(Scaling.fit).padLeft(8f).get();
        }, clearNonei, () -> {
            UIExt.contentSelector.select(content.items(), item -> item != unit.item(), item -> {
                changeItem[0].get(item);
                return true;
            });
        }).size(48f).padLeft(8f);

        TextField field = itemTable.field("" + unit.stack.amount, text -> {
            changeItemCount[0].get(Math.min(itemCapacity, Strings.parseInt(text)));
        }).padLeft(8f).expandX().left().width(80).valid(Strings::canParsePositiveInt).get();

        itemTable.button(b -> b.add("MAX"), clearNonei, () -> {
            changeItemCount[0].get(itemCapacity);
        }).size(48f).padLeft(4);

        itemTable.button(Icon.none, clearNonei, () -> {
            changeItem[0].get(null);
        }).size(48f).padLeft(4);

        Slider slider = itemTable.slider(0, itemCapacity, 1, unit.stack.amount, n -> {
            changeItemCount[0].get((int)n);
        }).width(128f).padLeft(4).get();

        changeItem[0] = item -> {
            unit.stack.item = item;
            image.setDrawable(getItemIcon(item));
        };

        changeItemCount[0] = n -> {
            unit.stack.amount = n;
            field.setText("" + n);
            slider.setValue(n);
        };

        if(unit.stack.amount > itemCapacity){
            changeItemCount[0].get(itemCapacity);
        }
    }

    private void rebuildTeamTable(Unit unit, Table teamTable){
        teamTable.clearChildren();

        teamTable.add("生成队伍:");

        teamTable.label(() -> {
            Team team = unit.team;
            return "[#" + team.color + "]" + team.localized();
        }).minWidth(128f).padLeft(8f);

        Image image = new Image();
        image.setColor(unit.team.color);

        teamTable.add(image).size(48).left().expandX();

        Cons<Team>[] changeTeam = new Cons[1];

        for(Team team : Team.baseTeams){
            teamTable.button(b -> {
                b.image().grow().color(team.color);
            }, clearNonei, () -> {
                changeTeam[0].get(team);
            }).size(36).pad(8);
        }

        teamTable.add("队伍ID:").padLeft(4);

        TextField field = teamTable.field("" + unit.team.id, text -> {
            int id = Mathf.clamp(Strings.parseInt(text), 0, Team.all.length - 1);
            changeTeam[0].get(Team.all[id]);
        }).width(72).valid(Strings::canParseInt).get();

        changeTeam[0] = team -> {
            unit.team = team;
            field.setText("" + team.id);
            image.addAction(Actions.color(team.color, 0.25f));
        };
    }

    private void rebuildEffectsTable(Unit unit, Table effectTable){
        effectTable.clearChildren();

        Seq<StatusEntry> unitStatus = UnitExt.getStatuses(unit);
        Table settingTable = new Table(Tex.whiteui);
        settingTable.setColor(Pal.gray);

        effectTable.defaults().growX();

        effectTable.table(Tex.whiteui, topTable -> {
            topTable.top();
            topTable.setColor(Pal.gray);
            topTable.defaults().expandX().fillY();

            topTable.table(leftTable -> {
                leftTable.top();

                leftTable.table(statusTable -> {
                    statusTable.image(StatusEffects.burning.uiIcon).size(64).scaling(Scaling.fit).expandX().left();

                    statusTable.button(Icon.refresh, cleari, 48f, () -> {
                        unitStatus.clear();
                        rebuildEffectSettingTable(unitStatus, settingTable);
                    }).size(64f).pad(8f);
                }).growX();

                leftTable.row();

                leftTable.table(grayPanel, effectInfo -> {
                    effectInfo.defaults().padTop(4f).fillX();

                    Runnable rebuildInfo = () -> {
                        effectInfo.clearChildren();

                        float[] status = {1f, 1f, 1f, 1f};
                        unitStatus.each(s -> {
                            status[0] *= s.effect.healthMultiplier;
                            status[1] *= s.effect.damageMultiplier;
                            status[2] *= s.effect.reloadMultiplier;
                            status[3] *= s.effect.speedMultiplier;
                        });

                        effectInfo.add("[acid]血量").pad(4f);
                        effectInfo.add(FormatDefault.format(status[0])).minWidth(64f).expandX().right();
                        effectInfo.row();

                        effectInfo.add("[red]伤害").pad(4f);
                        effectInfo.add(FormatDefault.format(status[1])).minWidth(64f).expandX().right();
                        effectInfo.row();

                        effectInfo.add("[violet]攻速").pad(4f);
                        effectInfo.add(FormatDefault.format(status[2])).minWidth(64f).expandX().right();
                        effectInfo.row();

                        effectInfo.add("[cyan]移速").pad(4f);
                        effectInfo.add(FormatDefault.format(status[3])).minWidth(64f).expandX().right();
                    };

                    int lastSize = -1;
                    effectInfo.update(() -> {
                        if(unitStatus.size != lastSize){
                            rebuildInfo.run();
                        }
                    });
                }).fill().pad(8f);
            });

            topTable.pane(noBarPane, selection -> {
                int i = 0;
                for(StatusEffect effect : content.statusEffects()){
                    selection.button(new TextureRegionDrawable(effect.uiIcon), cleari, 32f, () -> {
                        float time = unitStatus.isEmpty() ? 600f : unitStatus.peek().time;
                        unitStatus.add(new StatusEntry().set(effect, time));
                        rebuildEffectSettingTable(unitStatus, settingTable);
                    }).size(48f).pad(4f);

                    if(++i % 3 == 0){
                        selection.row();
                    }
                }
            }).pad(8f).maxHeight(48f * 4);
        });

        effectTable.row();

        effectTable.add(settingTable).fillY();
        rebuildEffectSettingTable(unitStatus, settingTable);
    }

    private void rebuildEffectSettingTable(Seq<StatusEntry> unitStatus, Table table){
        table.clearChildren();

        unitStatus.each(entry -> {
            StatusEffect effect = entry.effect;

            table.table(Tex.whiteui, t -> {
                t.setColor(Pal.lightishGray);

                t.image(effect.uiIcon).pad(4f).size(40).scaling(Scaling.fit);
                t.add(effect.localizedName).ellipsis(true).width(64f).padLeft(6);

                if(entry.effect.permanent){
                    t.add("<永久状态>").expandX();
                }else if(entry.effect.reactive){
                    t.add("<瞬间状态>").expandX();
                }else{
                    t.table(bottom -> {
                        Cons<Float>[] changeTime = new Cons[1];

                        TextField field = bottom.field(Float.isInfinite(entry.time) ? "∞" : "" + entry.time, text -> {
                            changeTime[0].get(Strings.parseFloat(text));
                        }).width(100f).valid(text -> Strings.canParsePositiveFloat(text.replaceAll("∞", "Infinity"))).get();

                        bottom.add("秒");

                        bottom.button(b -> {
                            b.add(new FLabel("{rainbow}∞"));
                        }, clearNonei, () -> {
                            changeTime[0].get(Float.POSITIVE_INFINITY);
                        }).size(32f).padLeft(8).expandX().right();

                        changeTime[0] = time -> {
                            entry.time = time;

                            String text = Float.isInfinite(time) ? "∞" : "" + time;
                            field.setText(text);
                        };
                    }).padTop(8f).expandX().left();
                }

                t.button(Icon.copySmall, clearNonei, 32, () -> {
                    StatusEntry copied = new StatusEntry().set(entry.effect, entry.time);
                    unitStatus.add(copied);

                    rebuildEffectSettingTable(unitStatus, table);
                }).size(32);

                t.button(Icon.cancelSmall, clearNonei, 32, () -> {
                    unitStatus.remove(entry, true);

                    rebuildEffectSettingTable(unitStatus, table);
                }).size(32);
            }).pad(8).growX();

            table.row();
        });
    }

    private void rebuildPayloadTable(Unit unit, Table payloadTable){
        payloadTable.clearChildren();

        if(!(unit instanceof PayloadUnit payloadUnit)) return;
        Seq<Payload> payloads = payloadUnit.payloads;

        Table settingTable = new Table(Tex.whiteui);
        settingTable.setColor(Pal.gray);

        payloadTable.defaults().growX();

        payloadTable.table(Tex.whiteui, topTable -> {
            topTable.top();
            topTable.setColor(Pal.gray);

            topTable.image(Blocks.payloadLoader.uiIcon).size(64).scaling(Scaling.fit).expandX().left();

            topTable.button(Icon.refresh, cleari, 48f, () -> {
                payloads.clear();
                rebuildPayloadSettingTable(payloads, settingTable);
            }).size(64f).pad(8f);

            topTable.row();

            topTable.table(Styles.grayPanel, buttons -> {
                buttons.defaults().size(48f).pad(8f);

                buttons.button(new TextureRegionDrawable(Blocks.siliconSmelter.uiIcon), Styles.cleari, 32, () -> {
                    UIExt.contentSelector.select(content.blocks(), block -> true, block -> {
                        BuildPayload payload = new BuildPayload(block, payloadUnit.team);
                        payloads.add(payload);
                        rebuildPayloadSettingTable(payloads, settingTable);
                        return true;
                    });
                });

                buttons.button(new TextureRegionDrawable(UnitTypes.alpha.uiIcon), Styles.cleari, 32f, () -> {
                    UIExt.contentSelector.select(content.units(), unitType -> true, unitType -> {
                        UnitPayload payload = new UnitPayload(unitType.create(payloadUnit.team));
                        payloads.add(payload);
                        rebuildPayloadSettingTable(payloads, settingTable);
                        return true;
                    });
                });

                buttons.button("装载自己", cleart, () -> {
                    payloads.add(new UnitPayload(cloneUnit(payloadUnit)));
                    rebuildPayloadSettingTable(payloads, settingTable);
                }).width(72f);
            }).pad(8f).colspan(2);
        });

        payloadTable.row();

        payloadTable.pane(noBarPane, settingTable).fillY();

        rebuildPayloadSettingTable(payloads, settingTable);
    }

    private void rebuildPayloadSettingTable(Seq<Payload> payloads, Table table){
        table.clearChildren();

        for(Payload payload : payloads){
            table.table(Tex.whiteui, t -> {
                t.setColor(Pal.lightishGray);

                t.image(payload.content().uiIcon).pad(4f).size(48f).scaling(Scaling.fit);
                t.add(payload.content().localizedName).ellipsis(true).width(64f).padLeft(6).expandX().left();

                t.defaults().size(32).pad(4f);

                if(payload instanceof UnitPayload unitPayload){
                    t.button(Icon.editSmall, clearNonei, 24f, () -> {
                        simpleFactory(unitPayload.unit);
                    });
                }

                t.button(Icon.copySmall, clearNonei, 24f, () -> {
                    Payload copied = clonePayload(payload);
                    payloads.add(copied);

                    rebuildPayloadSettingTable(payloads, table);
                });

                t.button(Icon.cancelSmall, clearNonei, 24f, () -> {
                    payloads.remove(payload, true);

                    rebuildPayloadSettingTable(payloads, table);
                });
            }).pad(8).growX();

            table.row();
        }
    }

    private void simpleFactory(Unit unit){
        BaseDialog dialog = new BaseDialog("单位工厂");

        Table main = new Table();

        float width = Core.scene.getWidth() * (Core.scene.getWidth() > 1500 ? 0.6f : 0.9f) / Scl.scl(1);
        dialog.cont.pane(Styles.noBarPane, main).scrollX(false).width(width).growY();

        main.top();
        main.defaults().growX();

        main.table(infoTable -> {
            rebuildInfoTable(unit, infoTable);
        }).row();

        main.defaults().padTop(12f);

        main.table(itemTable -> {
            rebuildItemTable(unit, itemTable);
        }).row();
        main.table(propertiesTable -> {
            rebuildPropertiesTable(unit, propertiesTable);
        }).row();

        main.table(teamTable -> {
            rebuildTeamTable(unit, teamTable);
        }).row();

        main.table(bottomTable -> {
            bottomTable.left();
            bottomTable.defaults().top().left();

            bottomTable.table(effectTable -> {
                rebuildEffectsTable(unit, effectTable);
            });

            bottomTable.defaults().padLeft(16f);
            bottomTable.table(payloadTable -> {
                rebuildPayloadTable(unit, payloadTable);
            });
        }).padTop(8f).fillY();

        dialog.addCloseButton();
        dialog.buttons.button("重置", Icon.refresh, () -> {
            resetUnit(unit);
        });

        dialog.show();
    }

    private static Drawable getItemIcon(@Nullable Item item){
        return item == null ? Icon.none : new TextureRegionDrawable(item.uiIcon);
    }

    private static Unit cloneUnit(Unit unit){
        Unit cloned = unit.type.create(unit.team);
        cloned.health = unit.health;
        cloned.shield = unit.shield;
        cloned.stack.set(unit.stack.item, unit.stack.amount);
        cloned.elevation = unit.elevation;
        cloned.set(unit);

        if(unit instanceof Payloadc payloadUnit && cloned instanceof Payloadc clonedPayloadUnit){
            payloadUnit.payloads().each(p -> {
                Payload copied = clonePayload(p);

                if(copied != null){
                    clonedPayloadUnit.addPayload(copied);
                }
            });
        }

        Seq<StatusEntry> status = UnitExt.getStatuses(unit).map(UnitFactoryDialog::cloneStatus);
        UnitExt.getStatuses(cloned).set(status);
        return cloned;
    }

    private void sendFormatChat(String format, Object... args){
        for(int i = 0; i < args.length; i++){
            if(args[i] instanceof Float f){
                args[i] = Strings.autoFixed(f, 1);
            }
        }
        Time.run(chatTime, () -> Call.sendChatMessage(Strings.format(format, args)));
        chatTime = chatTime + 10f;
    }

    private static void resetUnit(Unit unit){
        unit.setType(unit.type);
        UnitExt.getStatuses(unit).clear();
        if(unit instanceof Payloadc pay){
            pay.payloads().clear();
        }
    }

    private static StatusEntry cloneStatus(StatusEntry entry){
        return new StatusEntry().set(entry.effect, entry.time);
    }

    private static Payload clonePayload(Payload payload){
        if(payload instanceof BuildPayload buildPayload){
            Building build = buildPayload.build;
            return new BuildPayload(build.block, build.team);
        }else if(payload instanceof UnitPayload unitPayload){
            Unit unit = cloneUnit(unitPayload.unit);
            return new UnitPayload(unit);
        }

        // impossible
        return null;
    }

    private static class UnitStack{
        private static Seq<UnitStack> classedUnits;
        public static UnitStack vanillaStack = new UnitStack(null);

        public @Nullable LoadedMod mod;
        public Seq<UnitType> units = new Seq<>();

        public UnitStack(LoadedMod mod){
            this.mod = mod;
        }

        public String name(){
            return mod == null ? Core.bundle.get("vanilla", "原版") : mod.meta.displayName;
        }

        public TextureRegion icon(){
            return mod == null ? Blocks.duo.uiIcon :
            mod.iconTexture != null ? new TextureRegion(mod.iconTexture) : null;
        }

        private static Seq<UnitStack> getUnitStacks(){
            if(classedUnits == null){
                initClassedUnits();
            }

            return classedUnits;
        }

        private static void initClassedUnits(){
            classedUnits = new Seq<>();

            classedUnits.add(vanillaStack);

            content.units().each(unit -> {
                if(unit.isVanilla()){
                    vanillaStack.units.add(unit);
                }else{
                    LoadedMod mod = unit.minfo.mod;
                    UnitStack stack = classedUnits.find(s -> s.mod == mod);

                    if(stack == null){
                        stack = new UnitStack(mod);
                        classedUnits.add(stack);
                    }

                    stack.units.add(unit);
                }
            });
        }
    }
}
