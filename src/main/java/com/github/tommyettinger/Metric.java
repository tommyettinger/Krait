package com.github.tommyettinger;

/**
 * Created by Tommy Ettinger on 2/23/2016.
 */
public enum Metric {
    /**
     * Each orthogonal step counts as 1 unit of distance. Diagonal steps count as 2 orthogonal steps iff there is a
     * viable space orthogonally adjacent to both diagonally adjacent cells. Also called taxicab distance; can be
     * compared to a rook's movement in chess if limited to one square per move. The king's's movement is Chebyshev.
     * <br>
     * It can also be described as Minkowski distance with order 1, if you're trying to impress somebody.
     */
    MANHATTAN,

    /**
     * "Realistic" measurement applied to an n-dimensional grid, using the Pythagorean formula for distance in n
     * dimensions, only sometimes computing a square root. This metric is somewhat permissive with distance measurement,
     * calculating distances that are slightly (up to 1/3 of a cell) longer than a maximum distance as not greater than
     * that maximum distance.
     * <br>
     * It can also be described as Minkowski distance with order 2, if you're trying to impress somebody.
     */
    EUCLIDEAN,

    /**
     * "Realistic" measurement applied to an n-dimensional grid, using the Pythagorean formula for distance in n
     * dimensions, only sometimes computing a square root. This metric is strict with distance measurement, calculating
     * distances that are any fraction of a cell longer than a maximum distance as greater than that maximum distance.
     * <br>
     * It can also be described as Minkowski distance with order 2, if you're trying to impress somebody.
     */
    EUCLIDEAN_STRICT,

    /**
     * Each orthogonal or diagonal step, regardless of how many dimensions are changing in value, counts as 1 unit of
     * distance. Also called chessboard distance, Tchebychev distance, or maximum metric; can be compared to a king's
     * movement in chess. The rook's movement, if limited to 1-square steps, is Manhattan.
     * <br>
     * It can also be described as Minkowski distance with infinite order, if you're trying to impress somebody, or
     * Chevy Chase distance, if you really like Caddyshack.
     */
    CHEBYSHEV;

    /**
     * Gets the distance from the origin to the points specified by coordinates, using this metric, as a double.
     * Requires a square root calculation for both Euclidean metrics, and returns identical results for those metrics.
     * @param coordinates the n-dimensional point as a double array or varargs
     * @return the exact distance, using this metric, as a double
     */
    public double exactFromOrigin(double... coordinates)
    {
        double dist = 0.0;
        if(coordinates == null)
            return dist;
        switch (this)
        {
            case MANHATTAN:
                for (int i = 0; i < coordinates.length; i++) {
                    dist += Math.abs(coordinates[i]);
                }
                return dist;
            case CHEBYSHEV:
                for (int i = 0; i < coordinates.length; i++) {
                    dist = Math.max(dist, Math.abs(coordinates[i]));
                }
                return dist;
            default:
                for (int i = 0; i < coordinates.length; i++) {
                    dist += coordinates[i] * coordinates[i];
                }
                return Math.sqrt(dist);
        }
    }

    /**
     * Gets the distance from point a to point b, where each point is a double array and the lesser dimension count will
     * be used. Returns a double. Requires a square root calculation for both Euclidean metrics, and returns identical
     * results for those metrics.
     * @param a a point as a double array
     * @param b another point as a double array
     * @return the distance between a and b, using this metric, as a double
     */
    public double exact(double[] a, double[] b)
    {
        double dist = 0.0;
        if(a == null || b == null)
            return dist;
        switch (this)
        {
            case MANHATTAN:
                for (int i = 0; i < a.length && i < b.length; i++) {
                    dist += Math.abs(a[i] - b[i]);
                }
                return dist;
            case CHEBYSHEV:
                for (int i = 0; i < a.length && i < b.length; i++) {
                    dist = Math.max(dist, Math.abs(a[i] - b[i]));
                }
                return dist;
            default:
                for (int i = 0; i < a.length && i < b.length; i++) {
                    dist += (a[i] - b[i]) * (a[i] - b[i]);
                }
                return Math.sqrt(dist);
        }
    }

    /**
     * Gets the approximate distance from the origin to the points specified by coordinates, using this metric, as an
     * int measuring grid steps. Requires a square root calculation for both Euclidean metrics, but will get the ceiling
     * of the distance for EUCLIDEAN_STRICT, and will reduce the distance by just over 1/3 before getting its ceiling
     * for EUCLIDEAN (resulting in distances no more than 1/3 greater than an integer being rounded down).
     * @param coordinates the n-dimensional point as an int array or varargs
     * @return the grid distance, using this metric, as an int
     */
    public int gridFromOrigin(int... coordinates)
    {
        int dist = 0;
        if(coordinates == null)
            return dist;
        switch (this)
        {
            case MANHATTAN:
                for (int i = 0, j = coordinates[i]; i < coordinates.length; ++i, j = coordinates[i]) {
                    dist += Math.abs(j);
                }
                return dist;
            case CHEBYSHEV:
                for (int i = 0, j = coordinates[i]; i < coordinates.length; ++i, j = coordinates[i]) {
                    dist = Math.max(dist, Math.abs(j));
                }
                return dist;
            case EUCLIDEAN_STRICT:
                for (int i = 0, j = coordinates[i]; i < coordinates.length; ++i, j = coordinates[i]) {
                    dist += j * j;
                }
                return (int)Math.ceil(Math.sqrt(dist));
            default:
                for (int i = 0, j = coordinates[i]; i < coordinates.length; ++i, j = coordinates[i]) {
                    dist += j * j;
                }
                return (int)Math.ceil(Math.sqrt(dist) - 0.333334);
        }
    }

    /**
     * Gets the distance from point a to point b, where each point is an int array and the lesser dimension count will
     * be used. Returns an int measuring grid steps. Requires a square root calculation for both Euclidean metrics, but
     * will get the ceiling of the distance for EUCLIDEAN_STRICT, and will reduce the distance by just over 1/3 before
     * getting its ceiling for EUCLIDEAN (resulting in distances no more than 1/3 greater than an integer being rounded
     * down).
     * @param a a point as an int array
     * @param b another point as an int array
     * @return the distance between a and b, using this metric, as an int
     */
    public int grid(int[] a, int[] b)
    {
        int dist = 0;
        if(a == null || b == null)
            return dist;
        switch (this)
        {
            case MANHATTAN:
                for (int i = 0; i < a.length && i < b.length; i++) {
                    dist += Math.abs(a[i] - b[i]);
                }
                return dist;
            case CHEBYSHEV:
                for (int i = 0; i < a.length && i < b.length; i++) {
                    dist = Math.max(dist, Math.abs(a[i] - b[i]));
                }
                return dist;
            case EUCLIDEAN_STRICT:
                for (int i = 0; i < a.length && i < b.length; i++) {
                    dist += (a[i] - b[i]) * (a[i] - b[i]);
                }
                return (int)Math.ceil(Math.sqrt(dist));
            default:
                for (int i = 0; i < a.length && i < b.length; i++) {
                    dist += (a[i] - b[i]) * (a[i] - b[i]);
                }
                return (int)Math.ceil(Math.sqrt(dist) - 0.333334);
        }
    }

    /**
     * Given a distance and a point as an int array or varargs, finds if the point has no more than the specified
     * distance from the origin using this metric. Returns a boolean, true if the point is within the distance. Does not
     * calculate a square root for any metrics, and approximates the 1/3 cell difference for EUCLIDEAN by finding 1/3 of
     * the difference of the squares for the specified distance and the distance one unit longer than it. This may cause
     * slight differences between this calculation and the other methods.
     * @param distance the distance to see if the point is within
     * @param coordinates the point as an int array or varargs
     * @return true if the point is within distance, using this metric
     */
    public boolean withinGridDistance(int distance, int... coordinates)
    {
        if(coordinates == null)
            return false;
        int dist = 0, square = distance * distance;
        switch (this)
        {
            case MANHATTAN:
                for (int i = 0, j = coordinates[i]; i < coordinates.length; ++i, j = coordinates[i]) {
                    if((dist += Math.abs(j)) > distance)
                        return false;
                }
                return true;
            case CHEBYSHEV:
                for (int i = 0, j = coordinates[i]; i < coordinates.length; ++i, j = coordinates[i]) {
                    dist = Math.max(dist, Math.abs(j));
                }
                return dist <= distance;
            case EUCLIDEAN:
                square += ((distance + 1) * (distance + 1) - square) / 3;
                // not using break here, using modified square value in permissive but not strict
            default:
                for (int i = 0, j = coordinates[i]; i < coordinates.length; ++i, j = coordinates[i]) {
                    dist += j * j;
                }
                return dist <= square;
        }
    }
}
