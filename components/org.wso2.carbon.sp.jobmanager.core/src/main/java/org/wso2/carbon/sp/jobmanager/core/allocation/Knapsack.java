package org.wso2.carbon.sp.jobmanager.core.allocation;

import org.wso2.carbon.sp.jobmanager.core.model.ResourceNode;

import java.util.LinkedList;

/**
 * A class that represents a Knapsack.
 */
public class Knapsack {

    private double cpuCapacity;
    private double memoryCapacity;
    private double startcpuUsage;
    private double startmemoryUsage;
    private ResourceNode resourceNode;
    private LinkedList<PartialSiddhiApp> partialSiddhiApps;

    /**
     * Constructor that creates a new knapsack with a capacity, resourceNode and a startcpuUsage latency.
     * @param cpuCapacity
     * @param memoryCapacity
     * @param resourceNode
     */
    public Knapsack(double cpuCapacity, double memoryCapacity, ResourceNode resourceNode) {
        this.cpuCapacity = cpuCapacity;
        this.memoryCapacity = memoryCapacity;
        this.resourceNode = resourceNode;
        this.startcpuUsage = cpuCapacity;
        this.startmemoryUsage = memoryCapacity;
        partialSiddhiApps = new LinkedList<>();
    }

    /**
     * Copy constructor which copies a knapsack object and creates a new identical one.
     * @param knapsack
     */
    public Knapsack(Knapsack knapsack) {
        this.cpuCapacity = knapsack.getCPUCapacity();
        this.memoryCapacity = knapsack.getMemoryCapacity();
        this.startcpuUsage = knapsack.getStartcpuUsage();
        this.startmemoryUsage = knapsack.getStartmemoryUsage();
        this.resourceNode = knapsack.getresourceNode();
        this.partialSiddhiApps = new LinkedList<>(knapsack.getpartialSiddhiApps());
    }

    /**
     * Adds an item doubleo the item-list and updates the capacity so it's up to date.
     * @param item
     */

    public void addPartialSiddhiApps(PartialSiddhiApp item) {
        if(item != null) {
            partialSiddhiApps.add(item);
            cpuCapacity = cpuCapacity - item.getcpuUsage();
            memoryCapacity = memoryCapacity - item.getMemoryUsage();
        }
    }

    /**
     * Stes the capacity to the initial latency of the knapsack.
     */

    public void resetcapacity() {
        cpuCapacity = startcpuUsage;
        memoryCapacity = startmemoryUsage;
    }

    /**
     * Sets the capacity to the latency provided to the method.
     * @param cpuCapacity
     */

    public void setCPUCapacity(double cpuCapacity) {
        this.cpuCapacity = cpuCapacity;
    }

    /**
     * Method that returns the knapsack's startcpuUsage
     * @return
     */
    public double getStartcpuUsage() {
        return startcpuUsage;
    }

    public  void  setStartcpuUsage(double startcpuUsage){
        this.startcpuUsage = startcpuUsage;
    }

    public void setStartmemoryUsage(double startmemoryUsage) { this.startmemoryUsage = startmemoryUsage; }

    /**
     * Method that returns the knapsack's startmemoryUsage
     * @return
     */

    public double getStartmemoryUsage() { return startmemoryUsage; }

    /**
     * Method that returns the knapsack's CPU capacity.
     * @return
     */
    public double getCPUCapacity() {
        return cpuCapacity;
    }

    /**
     * Method that returns the knapsack's memory capacity.
     * @return
     */
    public double getMemoryCapacity() {
        return memoryCapacity;
    }

    /**
     * Sets the capacity to the memory usage provided to the method.
     * @param memoryCapacity
     */

    public void setMemoryCapacity(double memoryCapacity) {
        this.memoryCapacity = memoryCapacity;
    }


    /**
     * Method that returns the knapsack's resourceNode.
     * @return
     */
    public ResourceNode getresourceNode() {
        return resourceNode;
    }

    /**
     * Method that returns the partialSiddhiApps the knapsack is currently holding.
     * @return
     */
    public LinkedList<PartialSiddhiApp> getpartialSiddhiApps() {
        return partialSiddhiApps;
    }


}
