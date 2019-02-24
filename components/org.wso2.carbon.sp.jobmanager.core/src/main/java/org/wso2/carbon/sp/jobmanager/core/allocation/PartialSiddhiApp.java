package org.wso2.carbon.sp.jobmanager.core.allocation;

public class PartialSiddhiApp {

    private double cpuUsage;
    private double memoryUsage;
    private double latency;
    private double latencyBycpuUsage;
    private double latencyBymemoryUsage;
    private String name;

    /**
     * Constructor that instantiates cpuUsage, latency and name for an item.
     * @param cpuUsage
     * @param latency
     * @param name
     */
    public PartialSiddhiApp (double cpuUsage, double memoryUsage, double latency, String name) {
        this.cpuUsage = cpuUsage;
        this.latency = latency;
        this.setMemoryUsage(memoryUsage);
        latencyBycpuUsage = (double) latency / (double) cpuUsage;
        latencyBymemoryUsage = latency / memoryUsage;
        this.name = name;
    }

    /**
     * Method that gets the latency / cpuUsage latency from an item.
     * @return
     */
    public double getlatencyBycpuUsage() {
        return latencyBycpuUsage;
    }

    /**
     * Method that returns the cpuUsage an item has.
     * @return
     */
    public double getcpuUsage() {
        return cpuUsage;
    }

    /**
     * Method that gets the latency an item has.
     * @return
     */
    public double getlatency() {
        return latency;
    }

    /**
     * Method that sets the name of an item.
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Method that returns the memoryUsage of an item.
     * @return
     */
    public double getMemoryUsage() {
        return memoryUsage;
    }

    /**
     * Method that sets the memoryUsage of an item.
     * @return
     */
    public void setMemoryUsage(double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    /**
     * Method that returns the latency / memoryUsage an item has.
     * @return
     */
    public double getLatencyBymemoryUsage() {
        return latencyBymemoryUsage;
    }

    /**
     * Method that sets the latency / memoryUsage of an item.
     * @return
     */
    public void setLatencyBymemoryUsage(double latencyBymemoryUsage) {
        this.latencyBymemoryUsage = latencyBymemoryUsage;
    }
}
