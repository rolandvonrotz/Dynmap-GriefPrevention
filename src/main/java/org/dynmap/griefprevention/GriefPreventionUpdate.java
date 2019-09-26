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
        updateClaims();
    }

    private void updateClaims() {
        DynmapGriefPreventionPlugin.MAP_MARKER.values().forEach(AreaMarker::deleteMarker);
        DynmapGriefPreventionPlugin.MAP_MARKER.clear();

        final Collection<Claim> claims = DynmapGriefPreventionPlugin.GP.dataStore.getClaims();

        DynmapGriefPreventionPlugin.MAP_MARKER = claims.stream()
                .flatMap(c -> Stream.concat(Stream.of(c), c.children.stream()))
                .filter(this::isVisible)
                .map(this::handleClaim)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean isVisible(final Claim claim) {
        if (isHidden(claim)) {
            return false;
        }
        if (DynmapGriefPreventionPlugin.SETTINGS.getVisibleRegions().size() == 0) {
            return true;
        }
        return match(DynmapGriefPreventionPlugin.SETTINGS.getVisibleRegions(), claim);
    }

    private boolean isHidden(final Claim claim) {
        return match(DynmapGriefPreventionPlugin.SETTINGS.getHiddenRegions(), claim);
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
        double[] x = null;
        double[] z = null;
        final Location l0 = claim.getLesserBoundaryCorner();
        final Location l1 = claim.getGreaterBoundaryCorner();
        final String wname = l0.getWorld().getName();
        final String owner = claim.isAdminClaim() ? Config.ADMIN_ID : claim.getOwnerName();
        /* Handle areas */
        /* Make outline */
        x = new double[4];
        z = new double[4];
        x[0] = l0.getX();
        z[0] = l0.getZ();
        x[1] = l0.getX();
        z[1] = l1.getZ() + 1.0;
        x[2] = l1.getX() + 1.0;
        z[2] = l1.getZ() + 1.0;
        x[3] = l1.getX() + 1.0;
        z[3] = l0.getZ();
        final Long id = claim.getID();
        final String markerid = "GP_" + Long.toHexString(id);
        AreaMarker marker = DynmapGriefPreventionPlugin.MAP_MARKER.remove(markerid); /* Existing area? */
        if (marker == null) {
            marker = DynmapGriefPreventionPlugin.MARKER_SET.createAreaMarker(markerid, owner, false, wname, x, z, false);
        } else {
            marker.setCornerLocations(x, z); /* Replace corner locations */
            marker.setLabel(owner);   /* Update label */
        }
        if (DynmapGriefPreventionPlugin.SETTINGS.getUse3dRegions()) { /* If 3D? */
            marker.setRangeY(l1.getY() + 1.0, l0.getY());
        }
        /* Set line and fill properties */
        addStyle(owner, wname, marker, claim);

        /* Build popup */
        final String desc = formatInfoWindow(claim, marker);

        marker.setDescription(desc); /* Set popup */

        return new AbstractMap.SimpleEntry<>(markerid, marker);
    }

    private void addStyle(final String owner, final String worldid, final AreaMarker m, final Claim claim) {
        final Settings.AreaStyle style = DynmapGriefPreventionPlugin.SETTINGS.getStyle(owner);
        int sc = 0xFF0000;
        int fc = 0xFF0000;
        try {
            sc = Integer.parseInt(style.strokeColor.substring(1), 16);
            fc = Integer.parseInt(style.fillColor.substring(1), 16);
        } catch (final NumberFormatException nfx) {
        }
        m.setLineStyle(style.strokeWeight, style.strokeOpacity, sc);
        m.setFillStyle(style.fillOpacity, fc);
        if (style.label != null) {
            m.setLabel(style.label);
        }
    }

    private String formatInfoWindow(final Claim claim, final AreaMarker m) {
        String v = "<div class=\"regioninfo\">" + DynmapGriefPreventionPlugin.SETTINGS.getInfoWindow() + "</div>";
        if (claim.isAdminClaim()) {
            v = "<div class=\"regioninfo\">" + DynmapGriefPreventionPlugin.SETTINGS.getAdminInfoWindow() + "</div>";
        }

        final ArrayList<String> builders = new ArrayList<>();
        final ArrayList<String> containers = new ArrayList<>();
        final ArrayList<String> accessors = new ArrayList<>();
        final ArrayList<String> managers = new ArrayList<>();
        claim.getPermissions(builders, containers, accessors, managers);
        return v.replace("%owner%", claim.isAdminClaim() ? Config.ADMIN_ID : claim.getOwnerName())
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
