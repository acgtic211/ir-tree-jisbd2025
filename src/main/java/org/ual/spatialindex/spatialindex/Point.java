package org.ual.spatialindex.spatialindex;

public class Point implements IShape, Cloneable {
    public double[] coords = null;

    public Point(double[] pCoords) {
        coords = new double[pCoords.length];
        System.arraycopy(pCoords, 0, coords, 0, pCoords.length);
    }

    public Point(final Point pt) {
        coords = new double[pt.coords.length];
        System.arraycopy(pt.coords, 0, coords, 0, pt.coords.length);
    }

    public boolean equals(Object o) {
        if (o instanceof Point) {
            Point pt = (Point) o;

            if (pt.coords.length != coords.length)
                return false;

            for (int cIndex = 0; cIndex < coords.length; cIndex++) {
                if (coords[cIndex] < pt.coords[cIndex] - SpatialIndex.EPSILON
                        || coords[cIndex] > pt.coords[cIndex] + SpatialIndex.EPSILON)
                    return false;
            }

            return true;
        }

        return false;
    }

    //
    // Cloneable interface
    //

    public Object clone() {
        return new Point(coords);
    }

    //
    // IShape interface
    //

    public boolean intersects(final IShape s) {
        if (s instanceof Region)
            return ((Region) s).contains(this);

        return false;
    }

    public boolean contains(final IShape s) {
        return false;
    }

    public boolean touches(final IShape s) {
        if (s instanceof Point && this.equals(s))
            return true;

        if (s instanceof Region)
            return ((Region) s).touches(this);

        return false;
    }

    public double[] getCenter() {
        double[] pCoords = new double[coords.length];
        System.arraycopy(coords, 0, pCoords, 0, coords.length);
        return pCoords;
    }

    public long getDimension() {
        return coords.length;
    }

    public Region getMBR() {
        return new Region(coords, coords);
    }

    public double getArea() {
        return 0.0;
    }

    public double getMinimumDistance(final IShape s) {
        if (s instanceof Region)
            return ((Region) s).getMinimumDistance(this);

        if (s instanceof Point)
            return getMinimumDistance((Point) s);

        throw new IllegalStateException("getMinimumDistance: Not implemented yet!");
    }

    double getMinimumDistance(final Point p) {
        if (coords.length != p.coords.length)
            throw new IllegalArgumentException("getMinimumDistance: Shape has the wrong number of dimensions.");

        double ret = 0.0f;

        for (int cIndex = 0; cIndex < coords.length; cIndex++) {
            ret += Math.pow(coords[cIndex] - p.coords[cIndex], 2.0);
        }

        return Math.sqrt(ret);
    }

    public double getCoord(int index) throws IndexOutOfBoundsException {
        if (index >= coords.length)
            throw new IndexOutOfBoundsException("" + index);
        return coords[index];
    }

    @Override
    public String toString() {
        return "(" + (int) coords[0] + ", " + (int) coords[1] + ")";
    }
}
