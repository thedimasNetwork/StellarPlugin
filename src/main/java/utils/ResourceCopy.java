package utils;

import arc.files.Fi;

public class ResourceCopy {
    public static void export(String name) {
        Fi.get(name).copyTo(Fi.get("config/mods/thedimasPlugin/" + name));
    }
}
