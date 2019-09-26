package org.dynmap.griefprevention;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Settings {
    private Integer minZoom = 0;
    private Integer layerPriority = 10;
    private Integer maxDepth = 16;
    private Integer updatePeriod = 300;
    private Boolean hideByDefault = false;
    private Boolean use3dRegions = false;
    private String infoWindow = Config.DEF_INFOWINDOW;
    private String adminInfoWindow = Config.DEF_ADMININFOWINDOW;
    private AreaStyle areaStyle = null;
    private final Map<String, AreaStyle> ownerStyle = new HashMap<>();
    private final Set<String> visibleRegions = new HashSet<>();
    private final Set<String> hiddenRegions = new HashSet<>();

    public Settings(final FileConfiguration config) {
        minZoom = config.getInt("layer.minzoom", minZoom);
        layerPriority = config.getInt("layer.layerprio", layerPriority);
        maxDepth = config.getInt("layer.maxdepth", maxDepth);
        updatePeriod = config.getInt("update.period", updatePeriod) * 20;
        //
        hideByDefault = config.getBoolean("layer.hidebydefault", hideByDefault);
        use3dRegions = config.getBoolean("use3dregions", use3dRegions);
        //
        infoWindow = config.getString("infowindow", infoWindow);
        adminInfoWindow = config.getString("adminclaiminfowindow", adminInfoWindow);
        //
        areaStyle = new AreaStyle(config, "regionstyle");
        loadOwnerStyle(config);
        //
        visibleRegions.addAll(config.getStringList("visibleregions"));
        hiddenRegions.addAll(config.getStringList("hiddenregions"));
    }

    private void loadOwnerStyle(final FileConfiguration config) {
        final ConfigurationSection ownerStyleConfig = config.getConfigurationSection("ownerstyle");
        ownerStyleConfig.getKeys(false)
                .stream()
                .forEach(id -> ownerStyle.put(id.toLowerCase(), new AreaStyle(config, "ownerstyle." + id, areaStyle)));
    }

    public Integer getMinZoom() {
        return minZoom;
    }

    public Integer getLayerPriority() {
        return layerPriority;
    }

    public Integer getMaxDepth() {
        return maxDepth;
    }

    public Integer getUpdatePeriod() {
        return updatePeriod;
    }

    public Boolean getHideByDefault() {
        return hideByDefault;
    }

    public Boolean getUse3dRegions() {
        return use3dRegions;
    }

    public String getInfoWindow() {
        return infoWindow;
    }

    public String getAdminInfoWindow() {
        return adminInfoWindow;
    }

    public AreaStyle getAreaStyle() {
        return areaStyle;
    }

    public Map<String, AreaStyle> getOwnerStyle() {
        return ownerStyle;
    }

    public AreaStyle getStyle(final String owner) {
        if (ownerStyle.containsKey(owner)) {
            return ownerStyle.get(owner);
        }
        return areaStyle;
    }

    public Set<String> getVisibleRegions() {
        return visibleRegions;
    }

    public Set<String> getHiddenRegions() {
        return hiddenRegions;
    }

    public static class AreaStyle {
        String strokeColor;
        double strokeOpacity;
        int strokeWeight;
        String fillColor;
        double fillOpacity;
        String label;

        AreaStyle(final FileConfiguration cfg, final String path) {
            strokeColor = cfg.getString(path + ".strokeColor", "#FF0000");
            strokeOpacity = cfg.getDouble(path + ".strokeOpacity", 0.8);
            strokeWeight = cfg.getInt(path + ".strokeWeight", 3);
            fillColor = cfg.getString(path + ".fillColor", "#FF0000");
            fillOpacity = cfg.getDouble(path + ".fillOpacity", 0.35);
        }

        AreaStyle(final FileConfiguration cfg, final String path, final AreaStyle def) {
            strokeColor = cfg.getString(path + ".strokeColor", def.strokeColor);
            strokeOpacity = cfg.getDouble(path + ".strokeOpacity", def.strokeOpacity);
            strokeWeight = cfg.getInt(path + ".strokeWeight", def.strokeWeight);
            fillColor = cfg.getString(path + ".fillColor", def.fillColor);
            fillOpacity = cfg.getDouble(path + ".fillOpacity", def.fillOpacity);
            label = cfg.getString(path + ".label", null);
        }
    }
}
