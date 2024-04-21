package mindustryX.features.graphics

import arc.Core
import arc.Events
import arc.graphics.Color
import arc.graphics.Texture
import arc.graphics.g2d.Draw
import arc.graphics.gl.FrameBuffer
import arc.graphics.gl.Shader
import mindustry.game.EventType

object ScreenSampler {
    private var samplerBuffer = FrameBuffer()
    private var pingpong = FrameBuffer()

    private var activity = false

    private var baseShader: Shader? = null

    fun setup() {
        val fragmentShader = """
                
                uniform sampler2D u_texture;
                
                varying vec2 v_texCoords;
                
                void main() {
                    gl_FragColor.rgb = texture2D(u_texture, v_texCoords).rgb;
                    gl_FragColor.a = 1.0;
                }
                
                """.trimIndent()
        baseShader = Shader(Core.files.internal("shaders/screenspace.vert").readString(), fragmentShader)


        if (activity) throw RuntimeException("forbid setup sampler twice")

        Events.run(EventType.Trigger.preDraw) { flush(false) }
        Events.run(EventType.Trigger.postDraw) { end(false) }

        Events.run(EventType.Trigger.uiDrawBegin) { flush(true) }
        Events.run(EventType.Trigger.uiDrawEnd) { end(true) }
        activity = true
    }

    private fun end(blit: Boolean) {
        samplerBuffer.end()
        if (blit) samplerBuffer.blit(baseShader)
    }

    private fun flush(legacy: Boolean) {
        if (legacy) {
            getToBuffer(pingpong, true)
            val buffer = samplerBuffer
            samplerBuffer = pingpong
            pingpong = buffer //swap
        }

        samplerBuffer.resize(Core.graphics.width, Core.graphics.height)
        if (legacy) samplerBuffer.begin()
        else samplerBuffer.begin(Color.clear)
    }

    @JvmStatic
    val sampler: Texture
        /**获取当前屏幕纹理，纹理对象是当前屏幕纹理的引用或者映射，它会随渲染过程同步变化，请勿使用此对象暂存屏幕数据
         * @return 屏幕采样纹理的引用对象
         */
        get() {
            Draw.flush()
            return samplerBuffer.texture
        }

    /**将当前屏幕纹理转存到一个[帧缓冲区][FrameBuffer]，这将成为一份拷贝，可用于暂存屏幕内容
     *
     * @param target 用于转存屏幕纹理的目标缓冲区
     * @param clear 在转存之前是否清空帧缓冲区
     */
    fun getToBuffer(target: FrameBuffer, clear: Boolean) {
        if (clear) {
            target.begin(Color.clear)
        } else target.begin()

        samplerBuffer.blit(baseShader)
        target.end()
    }
}