package mindustryX.features.graphics;

import arc.Core;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import mindustry.Vars;
import mindustry.ui.Styles;
import mindustry.ui.fragments.LoadingFragment;
import mindustry.ui.fragments.MenuFragment;

import static mindustry.gen.Tex.*;

public class NewUI {

    public static Blur uiBlur = new Blur(Blur.DEf_B);

    public static void init(){

        ScreenSampler.setup();

        Drawable BLUR3 = new TextureRegionDrawable(Core.atlas.white()) {
            @Override
            public void draw(float x, float y, float width, float height) {
                uiBlur.directDraw(() -> super.draw(x, y, width, height));
                Styles.black3.draw(x, y, width, height);
            }

            @Override
            public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
                uiBlur.directDraw(() -> super.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation));
                Styles.black3.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation);
            }
        };

        Drawable BLUR5 = new TextureRegionDrawable(Core.atlas.white()) {
            @Override
            public void draw(float x, float y, float width, float height) {
                uiBlur.directDraw(() -> super.draw(x, y, width, height));
                Styles.black5.draw(x, y, width, height);
            }

            @Override
            public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
                uiBlur.directDraw(() -> super.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation));
                Styles.black5.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation);
            }
        };

        Drawable BLUR6 = new TextureRegionDrawable(Core.atlas.white()) {
            @Override
            public void draw(float x, float y, float width, float height) {
                uiBlur.directDraw(() -> super.draw(x, y, width, height));
                Styles.black5.draw(x, y, width, height);
            }

            @Override
            public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
                uiBlur.directDraw(() -> super.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation));
                Styles.black5.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation);
            }
        };

        Drawable BLUR8 = new TextureRegionDrawable(Core.atlas.white()) {
            @Override
            public void draw(float x, float y, float width, float height) {
                uiBlur.directDraw(() -> super.draw(x, y, width, height));
                Styles.black8.draw(x, y, width, height);
            }

            @Override
            public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
                uiBlur.directDraw(() -> super.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation));
                Styles.black8.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation);
            }
        };

        Drawable BLUR9 = new TextureRegionDrawable(Core.atlas.white()) {
            @Override
            public void draw(float x, float y, float width, float height) {
                uiBlur.directDraw(() -> super.draw(x, y, width, height));
                Styles.black9.draw(x, y, width, height);
            }

            @Override
            public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
                uiBlur.directDraw(() -> super.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation));
                Styles.black9.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation);
            }
        };

        UpdatePool.receive("syncUIBlurCfg", () -> {
            uiBlur.blurScl = Core.settings.getInt("blurLevel", 1);
            uiBlur.blurSpace = Core.settings.getInt("backBlurLen", 120) * 0.01f;

            Styles.defaultDialog.stageBackground = Core.settings.getBool("enableBlur", true)? BLUR5: Styles.black9;

            Styles.defaultb.up = Core.settings.getBool("enableBlur", true)? BLUR5: button;
            Styles.defaultb.down = Core.settings.getBool("enableBlur", true)? BLUR9: buttonDown;
            Styles.defaultb.disabled = Core.settings.getBool("enableBlur", true)? BLUR6: buttonDisabled;

            Styles.defaultt.over = Core.settings.getBool("enableBlur", true)? BLUR6: button;
            Styles.defaultt.up = Core.settings.getBool("enableBlur", true)? BLUR5: button;
            Styles.defaultt.down = Core.settings.getBool("enableBlur", true)? BLUR9: buttonDown;
            Styles.defaultt.disabled = Core.settings.getBool("enableBlur", true)? BLUR6: buttonDisabled;

            MenuFragment.background = Core.settings.getBool("enableBlur", true)? BLUR6: Styles.black6;
            LoadingFragment.background = Core.settings.getBool("enableBlur", true)? BLUR6: Styles.black8;

            Vars.ui.join.style.over = Core.settings.getBool("enableBlur", true) ? BLUR8 : Styles.flatOver;
            //Vars.ui.join.style.up = Core.settings.getBool("enableBlur", true) ? BLUR5 : Styles.black5;  for lag
            Vars.ui.join.style.down = Core.settings.getBool("enableBlur", true) ? BLUR8 : Styles.flatOver;

            Vars.ui.chatfrag.chatfield.getStyle().background = Core.settings.getBool("enableBlur", true)? BLUR5: underline;
        });
    }
}