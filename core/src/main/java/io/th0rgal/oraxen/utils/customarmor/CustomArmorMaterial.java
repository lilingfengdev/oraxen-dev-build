package io.th0rgal.oraxen.utils.customarmor;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;

public enum CustomArmorMaterial {
    CHAINMAIL, NETHERITE, DIAMOND, GOLDEN, IRON, LEATHER;

    public static CustomArmorMaterial fromMaterial(Material material) {
        String materialPrefix = StringUtils.substringBefore(material.name().toUpperCase(), "_");
        try {
            return CustomArmorMaterial.valueOf(materialPrefix);
        } catch (IllegalArgumentException e) {
            Logs.logError("Invalid custom armor material: " + material);
            Logs.logError("Defaulting to NONE.");
            return CHAINMAIL;
        }
    }

    public static CustomArmorMaterial fromString(String material) {
        try {
            return CustomArmorMaterial.valueOf(material.toUpperCase());
        } catch (IllegalArgumentException e) {
            Logs.logError("Invalid custom armor material: " + material);
            Logs.logError("Defaulting to NONE.");
            return CHAINMAIL;
        }
    }
}
