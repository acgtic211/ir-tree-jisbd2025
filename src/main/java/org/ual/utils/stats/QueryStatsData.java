package org.ual.utils.stats;

import org.ual.utils.main.QueryLogic;

public class QueryStatsData {
    public String queryType;
    public String value;

    public long totalTime;
    public int averageTime;
    public double averageNodesVisited; //numOfVisitedNodes
    public double averageSpatialCost;
    public double averageIRCost;
}
