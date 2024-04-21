package mindustryX.features.graphics

import arc.Core
import arc.Events
import arc.scene.style.Drawable
import arc.scene.style.TextureRegionDrawable
import mindustry.Vars
import mindustry.game.EventType
import mindustry.gen.Tex
import mindustry.ui.Styles
import mindustry.ui.fragments.LoadingFragment
import mindustry.ui.fragments.MenuFragment

object NewUI {
    var uiBlur: Blur = Blur(*Blur.DEf_B)

    lateinit var BLUR3: Drawable
    lateinit var BLUR5: Drawable
    lateinit var BLUR6: Drawable
    lateinit var BLUR8: Drawable
    lateinit var BLUR9: Drawable

    private var lastBlurLevel = Core.settings.getInt("blurLevel", 1)
    private var lastBlurSpace = Core.settings.getInt("backBlurLen", 120) * 0.01f
    private var lastEnableBlur = Core.settings.getBool("enableBlur", true)
    private var loaded = false

    @JvmStatic
    fun init() {
        ScreenSampler.setup()

        BLUR3 = object : TextureRegionDrawable(Core.atlas.white()) {
            override fun draw(x: Float, y: Float, width: Float, height: Float) {
                uiBlur.directDraw { super.draw(x, y, width, height) }
                Styles.black3.draw(x, y, width, height)
            }

            override fun draw(x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float) {
                uiBlur.directDraw { super.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation) }
                Styles.black3.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation)
            }
        }

        BLUR5 = object : TextureRegionDrawable(Core.atlas.white()) {
            override fun draw(x: Float, y: Float, width: Float, height: Float) {
                uiBlur.directDraw { super.draw(x, y, width, height) }
                Styles.black5.draw(x, y, width, height)
            }

            override fun draw(x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float) {
                uiBlur.directDraw { super.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation) }
                Styles.black5.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation)
            }
        }

        BLUR6 = object : TextureRegionDrawable(Core.atlas.white()) {
            override fun draw(x: Float, y: Float, width: Float, height: Float) {
                uiBlur.directDraw { super.draw(x, y, width, height) }
                Styles.black6.draw(x, y, width, height)
            }

            override fun draw(x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float) {
                uiBlur.directDraw { super.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation) }
                Styles.black6.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation)
            }
        }

        BLUR8 = object : TextureRegionDrawable(Core.atlas.white()) {
            override fun draw(x: Float, y: Float, width: Float, height: Float) {
                uiBlur.directDraw { super.draw(x, y, width, height) }
                Styles.black8.draw(x, y, width, height)
            }

            override fun draw(x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float) {
                uiBlur.directDraw { super.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation) }
                Styles.black8.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation)
            }
        }

        BLUR9 = object : TextureRegionDrawable(Core.atlas.white()) {
            override fun draw(x: Float, y: Float, width: Float, height: Float) {
                uiBlur.directDraw { super.draw(x, y, width, height) }
                Styles.black9.draw(x, y, width, height)
            }

            override fun draw(x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float) {
                uiBlur.directDraw { super.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation) }
                Styles.black9.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation)
            }
        }

        Events.run(EventType.Trigger.update) {
            val blurLevel = Core.settings.getInt("blurLevel", 1)
            val blurSpace = Core.settings.getInt("backBlurLen", 120) * 0.01f
            val enableBlur = Core.settings.getBool("enableBlur", true)

            if (blurLevel != lastBlurLevel || blurSpace != lastBlurSpace || enableBlur != lastEnableBlur || !loaded) {
                uiBlur.blurScl = blurLevel
                uiBlur.blurSpace = blurSpace

                Styles.defaultDialog.stageBackground = if (enableBlur) BLUR5 else Styles.black9

                Styles.defaultb.up = if (enableBlur) BLUR5 else Tex.button
                Styles.defaultb.down = if (enableBlur) BLUR9 else Tex.buttonDown
                Styles.defaultb.disabled = if (enableBlur) BLUR6 else Tex.buttonDisabled

                Styles.defaultt.over = if (enableBlur) BLUR6 else Tex.button
                Styles.defaultt.up = if (enableBlur) BLUR5 else Tex.button
                Styles.defaultt.down = if (enableBlur) BLUR9 else Tex.buttonDown
                Styles.defaultt.disabled = if (enableBlur) BLUR6 else Tex.buttonDisabled

                MenuFragment.background = if (enableBlur) BLUR6 else Styles.black6
                LoadingFragment.background = if (enableBlur) BLUR6 else Styles.black8

                Vars.ui.join.style.over = if (enableBlur) BLUR8 else Styles.flatOver
                //Vars.ui.join.style.up = enableBlur ? BLUR5 : Styles.black5;  for lag
                Vars.ui.join.style.down = if (enableBlur) BLUR8 else Styles.flatOver
                Vars.ui.chatfrag.chatfield.style.background = if (enableBlur) BLUR5 else Tex.underline

                lastBlurLevel = blurLevel
                lastBlurSpace = blurSpace
                lastEnableBlur = enableBlur
            }
        }
    }
}