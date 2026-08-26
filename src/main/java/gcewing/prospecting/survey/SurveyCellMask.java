package gcewing.prospecting.survey;

/** Applies a grid-anchored visual fade without moving or expanding a coarse survey cell. */
final class SurveyCellMask {

    private SurveyCellMask() {}

    static int roundedCornerAlpha(int baseAlpha, int cellSize, int localX, int localZ, int cornerRadius,
        int featherWidth, int minimumAlphaPercent) {
        if (baseAlpha <= 0 || baseAlpha > 255)
            throw new IllegalArgumentException("Base alpha must be between 1 and 255");
        if (cellSize <= 0) throw new IllegalArgumentException("Cell size must be positive");
        if (localX < 0 || localX >= cellSize || localZ < 0 || localZ >= cellSize)
            throw new IllegalArgumentException("Local coordinates are outside the survey cell");
        if (cornerRadius < 0 || cornerRadius > cellSize / 2)
            throw new IllegalArgumentException("Corner radius is outside the survey cell");
        if (minimumAlphaPercent <= 0 || minimumAlphaPercent > 100)
            throw new IllegalArgumentException("Minimum alpha percent must be between 1 and 100");
        if (cornerRadius == 0) return baseAlpha;
        if (featherWidth <= 0 || featherWidth > cornerRadius)
            throw new IllegalArgumentException("Corner feather width must be between 1 and the corner radius");

        double halfSize = cellSize / 2.0;
        double straightHalfSize = halfSize - cornerRadius;
        double cornerX = Math.max(Math.abs(localX + 0.5 - halfSize) - straightHalfSize, 0.0);
        double cornerZ = Math.max(Math.abs(localZ + 0.5 - halfSize) - straightHalfSize, 0.0);
        if (cornerX == 0.0 || cornerZ == 0.0) return baseAlpha;

        double distance = Math.sqrt(cornerX * cornerX + cornerZ * cornerZ);
        double fadeStart = cornerRadius - featherWidth;
        if (distance <= fadeStart) return baseAlpha;

        double maximumCornerDistance = Math.sqrt(2.0) * (cornerRadius - 0.5);
        double fadeProgress = Math.min(1.0, (distance - fadeStart) / (maximumCornerDistance - fadeStart));
        double minimumScale = minimumAlphaPercent / 100.0;
        double alphaScale = 1.0 - fadeProgress * (1.0 - minimumScale);
        return Math.max(1, (int) Math.round(baseAlpha * alphaScale));
    }
}
