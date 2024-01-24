package mindustry.arcModule;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Scl;
import arc.util.Align;
import arc.util.Time;
import arc.util.pooling.Pools;
import mindustry.entities.Effect;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.ui.Fonts;
import mindustry.world.blocks.defense.MendProjector;
import mindustry.world.blocks.defense.OverdriveProjector;
import mindustry.world.blocks.defense.Radar;
import mindustry.world.blocks.defense.turrets.BaseTurret;
import mindustry.world.blocks.storage.CoreBlock;

import static arc.graphics.g2d.Draw.color;
import static arc.graphics.g2d.Lines.line;
import static arc.graphics.g2d.Lines.stroke;
import static mindustry.Vars.*;

public class DrawUtilities {
    private static Vec2 vector = new Vec2();

    public static float arcDrawText(String text, float scl, float dx, float dy, Color color, int halign) {
        Font font = Fonts.outline;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(scl);
        layout.setText(font, text);

        float height = layout.height;

        font.setColor(color);
        font.draw(text, dx, dy + layout.height + 1, halign);

        font.setUseIntegerPositions(ints);
        font.setColor(Color.white);
        font.getData().setScale(1f);
        Draw.reset();
        Pools.free(layout);

        return height;
    }

    public static float arcDrawText(String text, float scl, float dx, float dy, int halign) {
        Font font = Fonts.outline;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(scl);
        layout.setText(font, text);

        float height = layout.height;

        font.draw(text, dx + layout.width / 2, dy + layout.height / 2, halign);

        font.setUseIntegerPositions(ints);
        font.setColor(Color.white);
        font.getData().setScale(1f);
        Draw.reset();
        Pools.free(layout);

        return height;
    }

    public static void arcFillTextHead(String text, float x1, float x2, float y, float ratio) {

        Font font = Fonts.outline;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(Math.abs(x2 - x1) / Math.max(text.length(), 1f) / 2f / Scl.scl(1f) * ratio);
        layout.setText(font, text);

        //font.setColor(Color.goldenrod);
        float cx = (x1 + x2) / 2 * tilesize, cy = y * tilesize - 5f;
        font.draw(text, cx, cy, Align.center);
        font.setUseIntegerPositions(ints);
        font.setColor(Color.white);
        font.getData().setScale(1f);
        Draw.reset();
        Pools.free(layout);
    }

    public static void arcDrawTextMain(String text, int x, int y) {

        Color color = ARCVars.getThemeColor();
        Font font = Fonts.outline;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(1f / 3f / Scl.scl(1f));
        layout.setText(font, text);

        font.setColor(color);
        float dx = x * tilesize, dy = y * tilesize;
        font.draw(text, dx, dy + layout.height + 1, Align.center);
        dy -= 1f;
        Lines.stroke(2f, Color.darkGray);
        line(dx - layout.width / 2f - 2f, dy, dx + layout.width / 2f + 1.5f, dy);
        Lines.stroke(1f, color);
        line(dx - layout.width / 2f - 2f, dy, dx + layout.width / 2f + 1.5f, dy);

        font.setUseIntegerPositions(ints);
        font.setColor(Color.white);
        font.getData().setScale(1f);
        Draw.reset();
        Pools.free(layout);
    }

    public static Effect arcBuildEffect(Building build) {
        if (build == null || !Core.settings.getBool("arcPlacementEffect")) return new Effect();
        if (build.block instanceof BaseTurret baseTurret && build.health > Core.settings.getInt("blockbarminhealth"))
            return createBuildEffect(120f, baseTurret.range, build.team.color);
        else if (build.block instanceof MendProjector mendProjector)
            return createBuildEffect(120f, mendProjector.range, Pal.heal);
        else if (build.block instanceof OverdriveProjector overdriveProjector)
            return createBuildEffect(120f, overdriveProjector.range, overdriveProjector.baseColor);
        else if (build.block instanceof CoreBlock block)
            return createBuildEffect(180f, block.fogRadius * tilesize, build.team.color);
        else if (build.block instanceof Radar radar)
            return createBuildEffect(180f, radar.fogRadius * tilesize, build.team.color);
        else return new Effect();
    }

    public static Effect createBuildEffect(float lifeTime, float range, Color color) {
        return new Effect(lifeTime, e -> {
            color(color);
            stroke((1.5f - e.fin() * 1f) * (range / 100));
            if (e.fin() < 0.7f) Lines.circle(e.x, e.y, (float) ((1 - Math.pow((0.7f - e.fin()) / 0.7f, 2f)) * range));
            else {
                Draw.alpha((1 - e.fin()) * 5f);
                Lines.circle(e.x, e.y, range);
            }
        });
    }

    public static void arcDashCircling(float x, float y, float radius) {
        arcDashCircling(x, y, radius, 0.5f);
    }

    public static void arcDashCircling(float x, float y, float radius, float speed) {
        arcDashCircle(x, y, radius, Time.time * speed);
    }

    public static void arcDashCircle(float x, float y, float radius, float rotation) {
        float scaleFactor = 0.6f;
        int sides = 10 + (int) (radius * scaleFactor);
        if (sides % 2 == 1) sides++;

        vector.set(0, 0);

        for (int i = 0; i < sides; i += 2) {
            vector.set(radius, 0).rotate(360f / sides * i + 90 + rotation);
            float x1 = vector.x;
            float y1 = vector.y;

            vector.set(radius, 0).rotate(360f / sides * (i + 1) + 90 + rotation);

            line(x1 + x, y1 + y, vector.x + x, vector.y + y);
        }
    }

    public static void drawNSideRegion(float x, float y, int n, float range, float rotation, Color color, float fraction, TextureRegion region, boolean regionColor){
        Draw.z(Layer.effect - 2f);
        Draw.color(color);

        Lines.stroke(2f);

        for (int i = 0; i < n; i++) {
            float frac = 360f * (1 - fraction * n)/n/2;
            float rot = rotation + i * 360f / n + frac;
            if (!regionColor){
                Draw.color(color);
                Lines.arc(x, y, range, 0.25f, rot, (int) (50 + range / 10));
                Draw.color();
            }else{
                Lines.arc(x, y, range, 0.25f, rot, (int) (50 + range / 10));
            }
            Draw.rect(region, x + range * Mathf.cos((float) Math.toRadians(rot-frac)),  y + range * Mathf.sin((float) Math.toRadians(rot-frac)),12f,12f);
        }
        Draw.reset();
    }
}
