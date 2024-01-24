package mindustry.arcModule;

import arc.struct.ObjectMap;
import mindustry.world.meta.*;

public class ArcStat extends Stat {
    public static final ObjectMap<String, ArcStat> all = new ObjectMap<>();

    public ArcStat(String name, StatCat category) {
        super(name, category);
    }

    public static ArcStat get(String name, StatCat category) {
        if (!all.containsKey(name)) {
            all.put(name, new ArcStat(name, category));
        }
        return all.get(name);
    }

    @Override
    public String localized() {
        return name;
    }
}
