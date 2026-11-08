package de.lpchat.orbital;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Original geometric implementation; no code from OrbitalStrike+ is included. */
public enum StrikePattern {
    NUKE, COMPRESSED_NUKE, STAB, CLUSTER, CROSS, X_SHOT, RING, LINE,
    HEXAGON, PENTAGON, STAR, STAR_OF_DAVID, HEART, SCATTER, GRID,
    CIRCLE_STAB, CROSS_STAB, X_STAB, RING_STAB, TRENCH_STAB,
    HEXAGON_STAB, PENTAGON_STAB, STAR_STAB, STAR_OF_DAVID_STAB,
    HEART_STAB, SCATTER_STAB, GRID_STAB;

    public record Point(double x, double z, int band) {}
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public boolean stab() { return this == STAB || name().endsWith("_STAB"); }
    public boolean rings() { return this == NUKE || this == COMPRESSED_NUKE || this == CIRCLE_STAB; }
    public int defaultSize() { return this == STAB || this == CIRCLE_STAB ? 1 : stab() ? 3 : rings() ? 5 : 12; }
    public static StrikePattern parse(String value) {
        try { return valueOf(value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Unbekanntes Muster. /ob help"); }
    }

    public List<Point> points(int size, double yawDegrees) {
        if (size < 1 || size > (rings() ? 8 : 48))
            throw new IllegalArgumentException(rings() ? "Ringzahl: 1 bis 8." : "Groesse: 1 bis 48.");
        List<Point> result = new ArrayList<>();
        switch (this) {
            case STAB -> result.add(new Point(0, 0, 0));
            case NUKE, COMPRESSED_NUKE, CIRCLE_STAB -> {
                result.add(new Point(0, 0, 0));
                for (int ring = 1; ring <= size; ring++)
                    circle(result, ring * 6, ring * 12, ring, this == COMPRESSED_NUKE ? 0 : (ring % 2) * Math.PI / (ring * 12));
            }
            case RING, RING_STAB -> circle(result, size, Math.max(12, size * 2), 1, 0);
            case CROSS, CROSS_STAB, X_SHOT, X_STAB -> {
                double rotation = this == X_SHOT || this == X_STAB ? Math.PI / 4 : 0;
                result.add(new Point(0, 0, 0));
                for (int d = 3; d <= size; d += 3)
                    for (int arm = 0; arm < 4; arm++) result.add(polar(d, rotation + arm * Math.PI / 2, d));
            }
            case LINE, TRENCH_STAB -> {
                for (int d = -size; d <= size; d += 3) result.add(new Point(0, d, Math.abs(d)));
            }
            case HEXAGON, HEXAGON_STAB -> polygon(result, 6, size, false);
            case PENTAGON, PENTAGON_STAB -> polygon(result, 5, size, false);
            case STAR, STAR_STAB -> polygon(result, 5, size, true);
            case STAR_OF_DAVID, STAR_OF_DAVID_STAB -> {
                for (int tri = 0; tri < 2; tri++)
                    for (int i = 0; i < 3; i++)
                        edge(result, polar(size, (tri / 3.0 + i * 2.0 / 3) * Math.PI, 1),
                                polar(size, (tri / 3.0 + (i + 1) * 2.0 / 3) * Math.PI, 1));
            }
            case HEART, HEART_STAB -> {
                int count = Math.max(24, size * 4);
                for (int i = 0; i < count; i++) {
                    double a = 2 * Math.PI * i / count;
                    result.add(new Point(size * Math.pow(Math.sin(a), 3),
                            -size * (13 * Math.cos(a) - 5 * Math.cos(2 * a) - 2 * Math.cos(3 * a) - Math.cos(4 * a)) / 17, 1));
                }
            }
            case GRID, GRID_STAB -> {
                for (int x = -size; x <= size; x += 6)
                    for (int z = -size; z <= size; z += 6) result.add(new Point(x, z, Math.max(Math.abs(x), Math.abs(z))));
            }
            case SCATTER, SCATTER_STAB -> {
                // Golden-angle disk: reproducible preview, evenly spread without random hot spots.
                for (int i = 0; i < size * 3; i++) result.add(polar(size * Math.sqrt((i + .5) / (size * 3)), i * 2.399963229728653, i % 4));
            }
            case CLUSTER -> {
                for (int c = 0; c < 5; c++) {
                    Point centre = c == 0 ? new Point(0, 0, 0) : polar(size, (c - 1) * Math.PI / 2, 0);
                    result.add(centre);
                    for (int i = 0; i < 16; i++) {
                        Point offset = polar(4, i * Math.PI / 8, 1);
                        result.add(new Point(centre.x + offset.x, centre.z + offset.z, 1));
                    }
                }
            }
        }
        double yaw = Math.toRadians(yawDegrees), cos = Math.cos(yaw), sin = Math.sin(yaw);
        return result.stream().map(p -> new Point(p.x * cos - p.z * sin, p.x * sin + p.z * cos, p.band)).toList();
    }

    private static Point polar(double r, double a, int band) { return new Point(r * Math.cos(a), r * Math.sin(a), band); }
    private static void circle(List<Point> out, double radius, int count, int band, double phase) {
        for (int i = 0; i < count; i++) out.add(polar(radius, phase + 2 * Math.PI * i / count, band));
    }
    private static void polygon(List<Point> out, int sides, int size, boolean star) {
        int vertices = star ? sides * 2 : sides;
        for (int i = 0; i < vertices; i++) {
            Point a = polar(star && i % 2 == 1 ? size * .45 : size, 2 * Math.PI * i / vertices, 1);
            int next = (i + 1) % vertices;
            Point b = polar(star && next % 2 == 1 ? size * .45 : size, 2 * Math.PI * next / vertices, 1);
            edge(out, a, b);
        }
    }
    private static void edge(List<Point> out, Point a, Point b) {
        int steps = Math.max(1, (int) Math.ceil(Math.hypot(b.x - a.x, b.z - a.z) / 3));
        for (int i = 0; i < steps; i++) out.add(new Point(a.x + (b.x - a.x) * i / steps, a.z + (b.z - a.z) * i / steps, 1));
    }
}
