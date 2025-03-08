package org.ual.spatialindex.spatialindex;

public class Region implements IShape
{
    public double[] low = null;
    public double[] high = null;

    public Region()
    {
    }

    public Region(final double[] pLow, final double[] pHigh)
    {
        if (pLow.length != pHigh.length) throw new IllegalArgumentException("Region: arguments have different number of dimensions.");

        low = new double[pLow.length];
        System.arraycopy(pLow, 0, low, 0, pLow.length);

        high = new double[pHigh.length];
        System.arraycopy(pHigh, 0, high, 0, pHigh.length);
    }

    public Region(final Point low, final Point high)
    {
        if (low.coords.length != high.coords.length) throw new IllegalArgumentException("Region: arguments have different number of dimensions.");

        this.low = new double[low.coords.length];
        System.arraycopy(low.coords, 0, this.low, 0, low.coords.length);
        this.high = new double[high.coords.length];
        System.arraycopy(high.coords, 0, this.high, 0, high.coords.length);
    }

    public Region(final Region r)
    {
        low = new double[r.low.length];
        System.arraycopy(r.low, 0, low, 0, r.low.length);
        high = new double[r.high.length];
        System.arraycopy(r.high, 0, high, 0, r.high.length);
    }

    public boolean equals(Object o)
    {
        if (o instanceof Region)
        {
            Region r = (Region) o;

            if (r.low.length != low.length) return false;

            for (int cIndex = 0; cIndex < low.length; cIndex++)
            {
                if (low[cIndex] < r.low[cIndex] - SpatialIndex.EPSILON || low[cIndex] > r.low[cIndex] + SpatialIndex.EPSILON ||
                        high[cIndex] < r.high[cIndex] - SpatialIndex.EPSILON || high[cIndex] > r.high[cIndex] + SpatialIndex.EPSILON)
                    return false;
            }
            return true;
        }
        return false;
    }

    //
    // Cloneable interface
    //

    public Object clone()
    {
        return new Region(low, high);
    }

    //
    // IShape interface
    //

    public boolean intersects(final IShape s)
    {
        if (s instanceof Region) return intersects((Region) s);

        if (s instanceof Point) return contains((Point) s);

        throw new IllegalStateException("intersects: Not implemented yet!");
    }

    public boolean contains(final IShape s)
    {
        if (s instanceof Region) return contains((Region) s);

        if (s instanceof Point) return contains((Point) s);

        throw new IllegalStateException("contains: Not implemented yet!");
    }

    public boolean touches(final IShape s)
    {
        if (s instanceof Region) return touches((Region) s);

        if (s instanceof Point) return touches((Point) s);

        throw new IllegalStateException("touches: Not implemented yet!");
    }

    public double[] getCenter()
    {
        double[] pCoords = new double[low.length];

        for (int cIndex = 0; cIndex < low.length; cIndex++)
        {
            pCoords[cIndex] = (low[cIndex] + high[cIndex]) / 2.0;
        }

        return pCoords;
    }

    public long getDimension()
    {
        return low.length;
    }

    public Region getMBR()
    {
        return new Region(low, high);
    }

    public double getArea()
    {
        double area = 1.0;

        for (int cIndex = 0; cIndex < low.length; cIndex++)
        {
            area *= high[cIndex] - low[cIndex];
        }

        return area;
    }

    public double getMinimumDistance(final IShape s)
    {
        if (s instanceof Region) return getMinimumDistance((Region) s);

        if (s instanceof Point) return getMinimumDistance((Point) s);

        throw new IllegalStateException("getMinimumDistance: Not implemented yet!");
    }

    public boolean intersects(final Region r)
    {
        if (low.length != r.low.length) throw new IllegalArgumentException("intersects: Shape has the wrong number of dimensions.");

        for (int cIndex = 0; cIndex < low.length; cIndex++)
        {
            if (low[cIndex] > r.high[cIndex] || high[cIndex] < r.low[cIndex]) return false;
        }
        return true;
    }

    public boolean contains(final Region r)
    {
        if (low.length != r.low.length) throw new IllegalArgumentException("contains: Shape has the wrong number of dimensions.");

        for (int cIndex = 0; cIndex < low.length; cIndex++)
        {
            if (low[cIndex] > r.low[cIndex] || high[cIndex] < r.high[cIndex]) return false;
        }
        return true;
    }

    public boolean touches(final Region r)
    {
        if (low.length != r.low.length) throw new IllegalArgumentException("touches: Shape has the wrong number of dimensions.");

        for (int cIndex = 0; cIndex < low.length; cIndex++)
        {
            if ((low[cIndex] > r.low[cIndex] - SpatialIndex.EPSILON && low[cIndex] < r.low[cIndex] + SpatialIndex.EPSILON) ||
                    (high[cIndex] > r.high[cIndex] - SpatialIndex.EPSILON && high[cIndex] < r.high[cIndex] + SpatialIndex.EPSILON))
                return true;
        }
        return false;
    }

    public double getMinimumDistance(final Region r)
    {
        if (low.length != r.low.length) throw new IllegalArgumentException("getMinimumDistance: Shape has the wrong number of dimensions.");

        double ret = 0.0;

        for (int cIndex = 0; cIndex < low.length; cIndex++)
        {
            double x = 0.0;

            if (r.high[cIndex] < low[cIndex])
            {
                x = Math.abs(r.high[cIndex] - low[cIndex]);
            }
            else if (high[cIndex] < r.low[cIndex])
            {
                x = Math.abs(r.low[cIndex] - high[cIndex]);
            }

            ret += x * x;
        }

        return Math.sqrt(ret);
    }

    public boolean contains(final Point p)
    {
        if (low.length != p.coords.length) throw new IllegalArgumentException("contains: Shape has the wrong number of dimensions.");

        for (int cIndex = 0; cIndex < low.length; cIndex++)
        {
            if (low[cIndex] > p.coords[cIndex] || high[cIndex] < p.coords[cIndex]) return false;
        }
        return true;
    }

    public boolean touches(final Point p)
    {
        if (low.length != p.coords.length) throw new IllegalArgumentException("touches: Shape has the wrong number of dimensions.");

        for (int cIndex = 0; cIndex < low.length; cIndex++)
        {
            if ((low[cIndex] > p.coords[cIndex] - SpatialIndex.EPSILON && low[cIndex] < p.coords[cIndex] + SpatialIndex.EPSILON) ||
                    (high[cIndex] > p.coords[cIndex] - SpatialIndex.EPSILON && high[cIndex] < p.coords[cIndex] + SpatialIndex.EPSILON))
                return true;
        }
        return false;
    }

    public double getMinimumDistance(final Point p) {
        if (low.length != p.coords.length) throw new IllegalArgumentException("getMinimumDistance: Shape has the wrong number of dimensions.");

        double ret = 0.0;

        for (int index = 0; index < low.length; index++) {
            if (p.coords[index] < low[index]) {
                ret += Math.pow(low[index] - p.coords[index], 2);
            } else if (p.coords[index] > high[index]) {
                ret += Math.pow(p.coords[index] - high[index], 2);
            }
        }

        return Math.sqrt(ret);
    }

    public double getIntersectingArea(final Region r)
    {
        if (low.length != r.low.length) throw new IllegalArgumentException("getIntersectingArea: Shape has the wrong number of dimensions.");

        int cIndex;

        // check for intersection.
        // marioh: avoid function call since this is called billions of times.
        for (cIndex = 0; cIndex < low.length; cIndex++)
        {
            if (low[cIndex] > r.high[cIndex] || high[cIndex] < r.low[cIndex]) return 0.0f;
        }

        double ret = 1.0;
        double f1, f2;

        for (cIndex = 0; cIndex < low.length; cIndex++)
        {
            f1 = Math.max(low[cIndex], r.low[cIndex]);
            f2 = Math.min(high[cIndex], r.high[cIndex]);
            ret *= f2 - f1;
        }

        return ret;
    }

    public Region combinedRegion(final Region r)
    {
        if (low.length != r.low.length) throw new IllegalArgumentException("combinedRegion: Shape has the wrong number of dimensions.");

        double[] mn = new double[low.length];
        double[] mx = new double[low.length];

        for (int cIndex = 0; cIndex < low.length; cIndex++)
        {
            mn[cIndex] = Math.min(low[cIndex], r.low[cIndex]);
            mx[cIndex] = Math.max(high[cIndex], r.high[cIndex]);
        }

        return new Region(mn, mx);
    }

    public static Region combinedRegion(Region[] pRegions)
    {
        double[] mn = new double[pRegions[0].low.length];
        double[] mx = new double[pRegions[0].low.length];

        for (int cDim = 0; cDim < pRegions[0].low.length; cDim++)
        {
            mn[cDim] = Double.POSITIVE_INFINITY;
            mx[cDim] = Double.NEGATIVE_INFINITY;

            for (int cIndex = 0; cIndex < pRegions.length; cIndex++)
            {
                mn[cDim] = Math.min(mn[cDim], pRegions[cIndex].low[cDim]);
                mx[cDim] = Math.max(mx[cDim], pRegions[cIndex].high[cDim]);
            }
        }

        return new Region(mn, mx);
    }

    // Modifies the first argument to include the second.
    public static void combinedRegion(Region pToModify, final Region pConst)
    {
        if (pToModify.low.length != pConst.low.length) throw new IllegalArgumentException("combineRegion: Shape has the wrong number of dimensions.");

        for (int cIndex = 0; cIndex < pToModify.low.length; cIndex++)
        {
            pToModify.low[cIndex] = Math.min(pToModify.low[cIndex], pConst.low[cIndex]);
            pToModify.high[cIndex] = Math.max(pToModify.high[cIndex], pConst.high[cIndex]);
        }
    }

    // Returns the margin of a region. It is calcuated as the sum of  2^(d-1) * width in each dimension.
    public double getMargin()
    {
        double mul = Math.pow(2.0, (low.length) - 1.0);
        double margin = 0.0;

        for (int cIndex = 0; cIndex < low.length; cIndex++)
        {
            margin += (high[cIndex] - low[cIndex]) * mul;
        }

        return margin;
    }

    public double getLow(int index) throws IndexOutOfBoundsException
    {
        if (index >= low.length) throw new IndexOutOfBoundsException("" + index);
        return low[index];
    }

    public double getHigh(int index) throws IndexOutOfBoundsException
    {
        if (index >= low.length) throw new IndexOutOfBoundsException("" + index);
        return high[index];
    }

    public String toString()
    {
        String s = "";

        for (int cIndex = 0; cIndex < low.length; cIndex++) s += low[cIndex] + " ";

        s += ": ";

        for (int cIndex = 0; cIndex < high.length; cIndex++) s += high[cIndex] + " ";

        return s;
    }
}
