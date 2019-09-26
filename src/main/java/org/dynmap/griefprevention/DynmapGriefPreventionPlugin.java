package org.dynmap.griefprevention;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DynmapGriefPreventionPlugin extends JavaPlugin {
    public static DynmapGriefPreventionPlugin INSTANCE;
    public static DynmapAPI DYNMAP_API;
    public static MarkerAPI MARKER_API;
    public static MarkerSet MARKER_SET;
    public static GriefPrevention GP;
    public static Settings SETTINGS;
    public static Map<String, AreaMarker> MAP_MARKER = new HashMap<>();

    @Override
    public void onEnable() {
        Log.Info("initializing");
        saveDefaultConfig();
        INSTANCE = this;
        SETTINGS = new Settings(getConfig());
        //
        loadDynmapApi();
        loadGriefPrevention();
        //
        initializeMarkerSet();
        initializeMetrics();
        //
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new GriefPreventionUpdate(), 40, SETTINGS.getUpdatePeriod());

        Log.Info("version " + this.getDescription().getVersion() + " is activated");
    }

    @Override
    public void onDisable() {
        if (MARKER_SET != null) {
            MARKER_SET.deleteMarkerSet();
            MARKER_SET = null;
        }
    }

    private void loadDynmapApi() {
        final Plugin dynmap = getServer().getPluginManager().getPlugin("dynmap");
        if (dynmap instanceof DynmapAPI && dynmap.isEnabled()) {
            DYNMAP_API = (DynmapAPI) dynmap;
            MARKER_API = DYNMAP_API.getMarkerAPI();
            return;
        }
    }

    private void loadGriefPrevention() {
        final Plugin gp = getServer().getPluginManager().getPlugin("GriefPrevention");
        if (gp instanceof GriefPrevention && gp.isEnabled()) {
            GP = (GriefPrevention) gp;
            return;
        }
    }

    private void initializeMarkerSet() {
        MARKER_SET = MARKER_API.getMarkerSet("griefprevention.markerset");
        final String layerName = Config.getString("layer.name", "GriefPrevention");
        if (MARKER_SET == null) {
            MARKER_SET = MARKER_API.createMarkerSet("griefprevention.markerset", layerName, null, false);
        } else {
            MARKER_SET.setMarkerSetLabel(layerName);
        }
        MARKER_SET.setMinZoom(SETTINGS.getMinZoom());
        MARKER_SET.setLayerPriority(SETTINGS.getLayerPriority());
        MARKER_SET.setHideByDefault(SETTINGS.getHideByDefault());
    }

    private void initializeMetrics() {
        try {
            final MetricsLite ml = new MetricsLite(this);
            ml.start();
        } catch (final IOException iox) {
            Log.Error(iox.getMessage());
        }
    }

}
