package org.wso2.carbon.sp.jobmanager.core.allocation;

import java.util.LinkedList;

public class BranchAndBoundNode implements Comparable<BranchAndBoundNode> {
    private double upperBound;
    private double throughputCost;
    private int level;
    private LinkedList<PartialSiddhiApp> partialSiddhiAppsOfNode;

    public BranchAndBoundNode(double upperBound, double throughputCost, LinkedList<PartialSiddhiApp> partialSiddhiAppsOfNode){
        this.setUpperBound(upperBound);
        this.setThroughputCost(throughputCost);
        this.setPartialSiddhiAppsOfNode(partialSiddhiAppsOfNode);
    }

    @Override
    public int compareTo(BranchAndBoundNode branchAndBoundNode) {
        return 0;
    }


    public double getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(double upperBound) {
        this.upperBound = upperBound;
    }

    public double getThroughputCost() {
        return throughputCost;
    }

    public void setThroughputCost(double throughputCost) {
        this.throughputCost = throughputCost;
    }

    public LinkedList<PartialSiddhiApp> getPartialSiddhiAppsOfNode() {
        return partialSiddhiAppsOfNode;
    }

    public void setPartialSiddhiAppsOfNode(LinkedList<PartialSiddhiApp> partialSiddhiAppsOfNode) {
        this.partialSiddhiAppsOfNode = partialSiddhiAppsOfNode;
    }

}