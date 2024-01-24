package mindustry.arcModule.draw;

import arc.Core;
import arc.Events;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import mindustry.game.EventType;
import mindustry.gen.Unit;

import static mindustry.Vars.*;

public class ARCUnits {
    public static float unitTrans = 1f;
    public static float unitDrawMinHealth = 1f, unitBarDrawMinHealth = 1f;

    public static float unitWeaponRangeAlpha = 1f, unitWeaponRange = 1f;

    public static boolean selectedUnitsFlyer = false, selectedUnitsLand = false;

    private static boolean canHitPlayer = false, canHitCommand = false, canHitPlans = false, canHitMouse = false;

    static {
        // 减少性能开销
        Events.run(EventType.Trigger.update, () -> {
            unitTrans = Core.settings.getInt("unitTransparency") / 100f;
            unitDrawMinHealth = Core.settings.getInt("unitDrawMinHealth");
            unitBarDrawMinHealth = Core.settings.getInt("unitBarDrawMinHealth");

            unitWeaponRange = Core.settings.getInt("unitWeaponRange") * tilesize;
            unitWeaponRangeAlpha = Core.settings.getInt("unitWeaponRangeAlpha") / 100f;

            selectedUnitsFlyer = control.input.selectedUnits.contains(unit -> unit.isFlying());
            selectedUnitsLand = control.input.selectedUnits.contains(unit -> !unit.isFlying());
        });
    }

    public static void drawWeaponRange(Unit unit) {
        if (unitWeaponRange == 0 || unitWeaponRangeAlpha == 0) return;
        if (unitWeaponRange == 30) {
            drawWeaponRange(unit, unitWeaponRangeAlpha);
        } else if (unit.team != player.team()) {
            canHitPlayer = !player.unit().isNull() && player.unit().hittable() && (player.unit().isFlying() ? unit.type.targetAir : unit.type.targetGround)
                    && unit.within(player.unit().x, player.unit().y, unit.type.maxRange + unitWeaponRange);
            canHitCommand = control.input.commandMode && ((selectedUnitsFlyer && unit.type.targetAir) || (selectedUnitsLand && unit.type.targetGround));
            canHitPlans = (control.input.block != null || control.input.selectPlans.size > 0) && unit.type.targetGround;
            canHitMouse = unit.within(Core.input.mouseWorldX(), Core.input.mouseWorldY(), unit.type.maxRange + unitWeaponRange);
            if (canHitPlayer || (canHitMouse && (canHitCommand || canHitPlans)))
                drawWeaponRange(unit, unitWeaponRangeAlpha);
        }
    }

    public static void drawWeaponRange(Unit unit, float alpha) {
        Draw.color(unit.team.color);
        Draw.alpha(alpha);
        Lines.dashCircle(unit.x, unit.y, unit.type.maxRange);
    }

}
