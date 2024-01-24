package mindustry.arcModule.ui.auxilliary;

import arc.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.arcModule.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.editor.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.dialogs.*;

import static arc.Core.settings;
import static mindustry.Vars.state;
import static mindustry.arcModule.ui.RStyles.*;
import static mindustry.ui.Styles.cleart;

public class WaveInfoTable extends BaseToolsTable{
    private final float fontScl = 0.8f;

    private int waveOffset = 0;

    private final Table waveInfo = new Table();

    private final arcWaveInfoDialog arcWaveInfoDialog = new arcWaveInfoDialog();

    private int showWaves = 0;
    private final int maxWavesShow = 8;

    public WaveInfoTable(){
        super(Icon.waves);

        Events.on(WorldLoadEvent.class, e -> {
            waveOffset = 0;
            rebuildWaveInfo();
        });

        Events.on(WaveEvent.class, e -> {
            rebuildWaveInfo();
        });
    }

    @Override
    protected void setup(){
        button(Icon.waves, clearAccentNonei, () -> {
            arcWaveInfoDialog.show();
        }).size(40).tooltip("波次信息");

        table(buttons -> {
            buttons.defaults().size(40);

            buttons.button("<", clearLineNonet, () -> {
                waveOffset -= 1;
                if(state.wave + waveOffset - 1 < 0) waveOffset = -state.wave + 1;
                rebuildWaveInfo();
            });

            buttons.button("O", clearLineNonet, () -> {
                waveOffset = 0;
                rebuildWaveInfo();
            });

            buttons.button(">", clearLineNonet, () -> {
                waveOffset += 1;
                rebuildWaveInfo();
            });

            buttons.button("Go", clearLineNonet, () -> {
                state.wave += waveOffset;
                waveOffset = 0;
                rebuildWaveInfo();
            });

            buttons.button("♐", clearLineNonet, () -> {
                String message = RFuncs.arcShareWaveInfo(state.wave + waveOffset);
                int seperator = 145;
                for(int i = 0; i < message.length() / (float)seperator; i++){
                    Call.sendChatMessage(message.substring(i * seperator, Math.min(message.length(), (i + 1) * seperator)));
                }
            }).get().setDisabled(() -> !state.rules.waves && !settings.getBool("arcShareWaveInfo"));

        }).left().row();

        table(setWave -> {
            setWave.label(() -> "" + (state.wave + waveOffset)).get().setFontScale(fontScl);

            setWave.button("∧", cleart, () -> {
                showWaves = Math.min(showWaves + 5, Math.max(0, state.rules.spawns.size - maxWavesShow));
                rebuildWaveInfo();
            }).size(30f);
            setWave.row();

            setWave.button(Icon.settingsSmall, clearAccentNonei, 30, () -> {
                Dialog lsSet = new BaseDialog("波次设定");
                lsSet.cont.add("设定查询波次").padRight(5f).left();
                TextField field = lsSet.cont.field(state.wave + waveOffset + "", text -> {
                    waveOffset = Integer.parseInt(text) - state.wave;
                }).size(320f, 54f).valid(Strings::canParsePositiveInt).maxTextLength(100).get();
                lsSet.cont.row();
                lsSet.cont.slider(1, new arcWaveInfoDialog().calWinWave(), 1, res -> {
                    waveOffset = (int)res - state.wave;
                    field.setText((int)res + "");
                });
                lsSet.addCloseButton();
                lsSet.show();
            });

            setWave.button("∨", cleart, () -> {
                showWaves = Math.max(0, showWaves - 5);
                rebuildWaveInfo();
            }).size(30f);

        });

        add(waveInfo).left();
    }

    private void rebuildWaveInfo(){
        waveInfo.clear();
        waveInfo.table(wt -> {
            int waveIndex = 0;
            int curInfoWave = state.wave - 1 + waveOffset;
            for(SpawnGroup group : state.rules.spawns){
                int amount = group.getSpawned(curInfoWave);

                if(amount > 0){
                    waveIndex += 1;
                    if(waveIndex < showWaves || waveIndex >= showWaves + maxWavesShow) continue;
                    float shield = group.getShield(curInfoWave);
                    StatusEffect effect = group.effect;

                    wt.table(groupT -> {
                        groupT.image(group.type.uiIcon).size(20).row();

                        groupT.add("" + amount, fontScl).center().row();

                        groupT.add((shield > 0 ? UI.formatAmount((long)shield) : ""), fontScl).center().row();

                        if(effect != null && effect != StatusEffects.none){
                            groupT.image(effect.uiIcon).size(20);
                        }
                    }).padLeft(4).top();
                }
            }

        }).maxWidth(300f);


    }


}
