package org.dynmap.griefprevention;

import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.dynmap.markers.AreaMarker;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class GriefPreventionUpdate implements Runnable {

    public void run() {
        // Delete Marker & clear them from the HashMap
        DynmapGriefPreventionPlugin.MAP_MARKER.values().forEach(AreaMarker::deleteMarker);
        DynmapGriefPreventionPlugin.MAP_MARKER.clear();
        // Get Claims from GriefPrevention
        final Collection<Claim> claims = DynmapGriefPreventionPlugin.GRIEF_PREVENTION.dataStore.getClaims();
        // Recreate Marker & HashMap
        DynmapGriefPreventionPlugin.MAP_MARKER = claims.stream()
                .flatMap(c -> Stream.concat(Stream.of(c), c.children.stream()))
                .filter(this::isVisible)
                .map(this::handleClaim)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean isVisible(final Claim claim) {
        if (match(DynmapGriefPreventionPlugin.SETTINGS.getHiddenRegions(), claim)) {
            return false;
        }
        if (DynmapGriefPreventionPlugin.SETTINGS.getVisibleRegions().size() == 0) {
            return true;
        }
        return match(DynmapGriefPreventionPlugin.SETTINGS.getVisibleRegions(), claim);
    }

    private boolean match(final Set<String> regions, final Claim claim) {
        final String ownerName = claim.getOwnerName();
        final String worldName = claim.getLesserBoundaryCorner().getWorld().getName();
        return regions.stream()
                .anyMatch(s -> ownerName.equalsIgnoreCase(s)
                        || ("world:" + worldName).equalsIgnoreCase(s)
                        || (worldName + "/" + ownerName).equalsIgnoreCase(s));
    }

    private Map.Entry<String, AreaMarker> handleClaim(final Claim claim) {
        final String markerId = "GP_" + Long.toHexString(claim.getID());
        final Location locationOne = claim.getLesserBoundaryCorner();
        final Location locationTwo = claim.getGreaterBoundaryCorner();
        final double[] x = createCoordListX(locationOne, locationTwo);
        final double[] z = createCoordListZ(locationOne, locationTwo);
        final String owner = claim.isAdminClaim() ? Config.ADMIN_ID : claim.getOwnerName();
        final AreaMarker marker = DynmapGriefPreventionPlugin.MARKER_SET.createAreaMarker(markerId, owner, false, locationOne.getWorld().getName(), x, z, false);
        // If 3D?
        if (DynmapGriefPreventionPlugin.SETTINGS.getUse3dRegions()) {
            marker.setRangeY(locationTwo.getY() + 1.0, locationOne.getY());
        }
        // Set line and fill properties
        addStyle(owner, marker);
        //Build & Set popup
        marker.setDescription(formatInfoWindow(claim, marker));
        return new AbstractMap.SimpleEntry<>(markerId, marker);
    }

    private double[] createCoordListX(final Location locationOne, final Location locationTwo) {
        final double[] coordList = new double[4];
        coordList[0] = locationOne.getX();
        coordList[1] = locationOne.getX();
        coordList[2] = locationTwo.getX() + 1.0;
        coordList[3] = locationTwo.getX() + 1.0;
        return coordList;
    }

    private double[] createCoordListZ(final Location locationOne, final Location locationTwo) {
        final double[] coordList = new double[4];
        coordList[0] = locationOne.getZ();
        coordList[1] = locationTwo.getZ() + 1.0;
        coordList[2] = locationTwo.getZ() + 1.0;
        coordList[3] = locationOne.getZ();
        return coordList;
    }

    private void addStyle(final String owner, final AreaMarker marker) {
        final Settings.AreaStyle style = DynmapGriefPreventionPlugin.SETTINGS.getStyle(owner);
        int strokeColor = 0xFF0000;
        int fillColor = 0xFF0000;
        try {
            strokeColor = Integer.parseInt(style.strokeColor.substring(1), 16);
            fillColor = Integer.parseInt(style.fillColor.substring(1), 16);
        } catch (final NumberFormatException nfx) {
            Log.Error(nfx.getMessage());
        }
        marker.setLineStyle(style.strokeWeight, style.strokeOpacity, strokeColor);
        marker.setFillStyle(style.fillOpacity, fillColor);
        if (style.label != null) {
            marker.setLabel(style.label);
        }
    }

    private String formatInfoWindow(final Claim claim, final AreaMarker m) {
        final String popup = String.format("<div class=\"regioninfo\">%s</div>",
                claim.isAdminClaim()
                        ? DynmapGriefPreventionPlugin.SETTINGS.getAdminInfoWindow()
                        : DynmapGriefPreventionPlugin.SETTINGS.getInfoWindow());
        final ArrayList<String> builders = new ArrayList<>();
        final ArrayList<String> containers = new ArrayList<>();
        final ArrayList<String> accessors = new ArrayList<>();
        final ArrayList<String> managers = new ArrayList<>();
        claim.getPermissions(builders, containers, accessors, managers);
        return popup.replace("%owner%", claim.isAdminClaim() ? Config.ADMIN_ID : claim.getOwnerName())
                .replace("%area%", Integer.toString(claim.getArea()))
                /* Build builders list */
                .replace("%builders%", builders.stream().map(this::getPlayerName).collect(joining(", ")))
                /* Build containers list */
                .replace("%containers%", containers.stream().map(this::getPlayerName).collect(joining(", ")))
                /* Build accessors list */
                .replace("%accessors%", accessors.stream().map(this::getPlayerName).collect(joining(", ")))
                /* Build managers list */
                .replace("%managers%", managers.stream().map(this::getPlayerName).collect(joining(", ")));
    }

    private String getPlayerName(final String uuid) {
        return Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
    }
}
