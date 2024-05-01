package mindustryX.features.ui;

import mindustry.graphics.*;
import mindustryX.features.ui.Card.*;

/**
 * @author minri2
 * Create by 2024/4/13
 */
public class StylesX{
    public static CardStyle grayOuterDark, accentOutBack;

    public static void load(){
        grayOuterDark = new CardStyle(){{
            shadowStyle = CardShadowStyle.outer;
            shadowDark = Pal.darkerGray;
        }};

        accentOutBack = new Card.CardStyle(){{
            shadowStyle = CardShadowStyle.outer;
            shadowDark = Pal.accentBack;
        }};
    }
}
