package de.lpchat.orbital;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Pure planning: bounded entity count and trajectories before any world mutation. */
public final class StrikePlan {
    public enum Order {
        SIMULTANEOUS, INWARD, OUTWARD;
        public static Order parse(String value) {
            try { return valueOf(value.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException e) { throw new IllegalArgumentException("Reihenfolge: simultaneous, inward oder outward."); }
        }
    }
    public record Payload(double x, double y, double z, double vx, double vy, double vz, int fuse, boolean gravity) {}
    private StrikePlan() {}

    public static List<Payload> create(StrikePattern pattern, int size, Order order, double yaw,
                                       double targetY, int minY, int maxY, int maxTnt) {
        if (!Double.isFinite(targetY) || targetY < minY || targetY >= maxY)
            throw new IllegalArgumentException("Ziel liegt ausserhalb der Welthoehe.");
        List<StrikePattern.Point> points = pattern.points(size, yaw);
        List<Payload> out = new ArrayList<>();
        int lastBand = points.stream().mapToInt(StrikePattern.Point::band).max().orElse(0);
        if (pattern.stab()) {
            double top = Math.min(maxY - 1, targetY + 70);
            for (StrikePattern.Point p : points) {
                int depth = 0;
                for (double y = top; y >= minY; y -= 1) {
                    add(out, new Payload(p.x(), y, p.z(), 0, 0, 0, 1, false), maxTnt);
                    depth++;
                }
            }
        } else {
            // Vanilla TNT: gravity is applied before movement, then velocity is damped by .98.
            int flight = 50;
            while (flight > 6 && targetY + 1 - fallDistance(flight) >= maxY - 1) flight--;
            double startY = targetY + 1 - fallDistance(flight);
            if (startY >= maxY - 1) throw new IllegalArgumentException("Zu wenig Platz oberhalb des Ziels.");
            double travel = horizontalTravel(flight);
            for (StrikePattern.Point p : points) {
                int delay = switch (order) {
                    case SIMULTANEOUS -> 0;
                    case INWARD -> (lastBand - p.band()) * 2;
                    case OUTWARD -> p.band() * 2;
                };
                // All TNT starts in a compact disk and fans out into concentric rings on descent.
                // Compressed nuke instead drops the already arranged circular formation.
                boolean compressed = pattern == StrikePattern.COMPRESSED_NUKE;
                double scale = compressed ? 1 : .015;
                add(out, new Payload(p.x() * scale, startY, p.z() * scale,
                        p.x() * (1 - scale) / travel, -.2, p.z() * (1 - scale) / travel,
                        flight + delay, true), maxTnt);
            }
        }
        return List.copyOf(out);
    }

    static double horizontalTravel(int ticks) { return (1 - Math.pow(.98, ticks)) / .02; }
    static double fallDistance(int ticks) {
        double velocity = -.2, distance = 0;
        for (int i = 0; i < ticks; i++) { velocity -= .04; distance += velocity; velocity *= .98; }
        return distance;
    }
    private static void add(List<Payload> out, Payload payload, int max) {
        if (out.size() >= max) throw new IllegalArgumentException("Zu viele TNT-Entities fuer einen Schuss. Kleinere Groesse waehlen.");
        out.add(payload);
    }
}
