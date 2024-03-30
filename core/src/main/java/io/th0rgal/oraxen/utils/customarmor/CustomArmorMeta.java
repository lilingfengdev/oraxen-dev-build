package io.th0rgal.oraxen.utils.customarmor;

import io.th0rgal.oraxen.config.settings.Settings;
import net.kyori.adventure.key.Key;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.configuration.ConfigurationSection;

public class CustomArmorMeta {
    private final CustomArmorType type;
    //private final CustomArmorMaterial material;

    private final Key firstLayer;
    private final Key secondLayer;
    public CustomArmorMeta(ConfigurationSection section) {
        if (section == null) new EmptyCustomArmor();
        this.type = CustomArmorType.fromString(section.getString("type", Settings.CUSTOM_ARMOR_DEFAULT_TYPE.toString()));

        String armorPrefix = StringUtils.substringBeforeLast(section.getParent().getName(), "_");
        this.firstLayer = Key.key(section.getString("first_layer", armorPrefix + "_armor_layer_1"));
        this.secondLayer = Key.key(section.getString("second_layer", armorPrefix + "_armor_layer_2"));
    }

    public CustomArmorType type() {
        return type;
    }

    public Key firstLayer() {
        return firstLayer;
    }

    public Key secondLayer() {
        return secondLayer;
    }
}
