package org.wso2.carbon.sp.jobmanager.core.allocation;

import org.apache.log4j.Logger;
import org.wso2.carbon.sp.jobmanager.core.model.ResourceNode;

import java.util.*;

public class MultipleKnapsack {

    private static final Logger log = Logger.getLogger(MetricsBasedAllocationAlgorithm.class);
    private LinkedList<Knapsack> knapsacks;
    private LinkedList<PartialSiddhiApp> partialSiddhiApps;

    private double latency;

    double upperBound;
    double throughputValue;
    double initialUpperBound = Double.MAX_VALUE;
    int level = 0;

    List<BranchAndBoundNode> branchAndBoundTreeNodes = Collections.synchronizedList(new ArrayList<>());
    List<BranchAndBoundNode> tempTreeNodes = Collections.synchronizedList(new ArrayList<>());
    public Map<ResourceNode, List<PartialSiddhiApp>> output_map = new HashMap<>();

    public LinkedList<PartialSiddhiApp> executeBranchAndBoundKnapsack(LinkedList<PartialSiddhiApp> partialSiddhiApps) {
        Collections.sort(partialSiddhiApps, new Comparator<PartialSiddhiApp>() {
            @Override
            public int compare(PartialSiddhiApp i1, PartialSiddhiApp i2) {
                if (i1.getcpuUsage() > i2.getcpuUsage()) {
                    return -1;
                } else if (i2.getcpuUsage() > i1.getcpuUsage()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        for (Knapsack knapsack : knapsacks) {
            //ArrayList<BranchAndBoundNode> feasibleTreeNodes;
            for (int i = 0; i < partialSiddhiApps.size(); i++) {
                if (!this.partialSiddhiApps.contains(partialSiddhiApps.get(i))) {
                    this.partialSiddhiApps.add(partialSiddhiApps.get(i));
                }
            }
            log.info("Executing graph traversal with " + partialSiddhiApps.size() + " partial siddhi apps..........");

            List<BranchAndBoundNode> feasibleTreeNodes = graphTraversal(partialSiddhiApps);
            log.info("All feasible nodes : " + feasibleTreeNodes.size());
            double maxThroughput = 0.0;
            for (BranchAndBoundNode feasibleNode : feasibleTreeNodes) {
                int weight = 0;
                double currentThroughput = 0.0;
                for (PartialSiddhiApp app : feasibleNode.getPartialSiddhiAppsOfNode()) {
                    weight += app.getcpuUsage();
                    currentThroughput += app.getThroughput();
                }
                if (weight > knapsack.getcapacity()) {
                    feasibleTreeNodes.remove(feasibleNode);

                } else {
                    if (maxThroughput > currentThroughput) {
                        feasibleTreeNodes.remove(feasibleNode);
                    } else {
                        maxThroughput = currentThroughput;
                    }
                }
            }

            log.info("Length of tree nodes = " + feasibleTreeNodes.size());
            BranchAndBoundNode finalNode = feasibleTreeNodes.get(0);
            for (PartialSiddhiApp app : finalNode.getPartialSiddhiAppsOfNode()) {
                knapsack.addPartialSiddhiApps(app);
                partialSiddhiApps.remove(app);
            }
        }
        return partialSiddhiApps;
    }

    public List<BranchAndBoundNode> graphTraversal(LinkedList<PartialSiddhiApp> partialSiddhiApps) {
        List<BranchAndBoundNode> feasibleTreeNodes = null;
        if (!branchAndBoundTreeNodes.isEmpty()) {
            for (BranchAndBoundNode branchAndBoundNode : branchAndBoundTreeNodes) {
                if (branchAndBoundNode.getThroughputCost() > initialUpperBound) {
                    branchAndBoundTreeNodes.remove(branchAndBoundNode);
                }
            }
        }
        if (level < partialSiddhiApps.size()) {
            log.info("Initial upper bound = " + initialUpperBound);
            upperBound = calculateUpperBound(partialSiddhiApps) * -1;
            log.info("Upper bound = " + upperBound);
            throughputValue = calculateThroughputValue(partialSiddhiApps) * -1;
            log.info("Throughput value = " + throughputValue);

            if (initialUpperBound > upperBound) {
                initialUpperBound = upperBound;
                log.info("Initial upper bound===="+initialUpperBound);

            }
            if (throughputValue < initialUpperBound) {
                log.info("throughputValue < initialUpperBound1.........................................");
                branchAndBoundTreeNodes.add(new BranchAndBoundNode(upperBound, throughputValue, partialSiddhiApps));
                tempTreeNodes.add(new BranchAndBoundNode(upperBound, throughputValue, partialSiddhiApps));
                log.info("Size of branch and bound tree nodes====="+branchAndBoundTreeNodes.size());
            }
            partialSiddhiApps.remove(partialSiddhiApps.get(level));
            level++;
            upperBound = calculateUpperBound(partialSiddhiApps) * -1;
            throughputValue = calculateThroughputValue(partialSiddhiApps) * -1;

            if (initialUpperBound > upperBound) {
                initialUpperBound = upperBound;

            }
            if (throughputValue < initialUpperBound) {
                log.info("throughputValue < initialUpperBound2.........................................");
                branchAndBoundTreeNodes.add(new BranchAndBoundNode(upperBound, throughputValue, partialSiddhiApps));
                tempTreeNodes.add(new BranchAndBoundNode(upperBound, throughputValue, partialSiddhiApps));
            }
            log.info("Size of branch and bound tree nodes =" + branchAndBoundTreeNodes.size());
            log.info("Size of temp tree nodes = " + tempTreeNodes.size());
            feasibleTreeNodes = executeRemainingTreeNode(tempTreeNodes);
        }

/*
        while (!tempTreeNodes.isEmpty()) {
            feasibleTreeNodes = graphTraversal(tempTreeNodes.get(0).getPartialSiddhiAppsOfNode());
            tempTreeNodes.remove();
        }*/
   /*     level = 0;
        branchAndBoundTreeNodes.clear();
        initialUpperBound = Integer.MAX_VALUE;*/


        return feasibleTreeNodes;

    }




    public List<BranchAndBoundNode> executeRemainingTreeNode(List<BranchAndBoundNode> tempTreeNodes) {
        List<BranchAndBoundNode> feasibleTreeNodes = Collections.synchronizedList(new ArrayList<>());
        while (tempTreeNodes.size()!=0) {
            log.info("tempTreeNodes is not empty...................................");
            log.info("Size of tempTreeNodes======="+ tempTreeNodes.size());
            log.info(",,,,,,,,,,,,,,,,"+tempTreeNodes.get(0).getPartialSiddhiAppsOfNode().size());

            if(tempTreeNodes.get(0).getPartialSiddhiAppsOfNode().size() != 0){
                feasibleTreeNodes = graphTraversal(tempTreeNodes.get(0).getPartialSiddhiAppsOfNode());
            }

            tempTreeNodes.remove(tempTreeNodes.get(0));
            log.info("Size of tempTreeNode" + tempTreeNodes.size() + "..................................");
        }
        level = 0;
        branchAndBoundTreeNodes.clear();
        initialUpperBound = Integer.MAX_VALUE;
        log.info("Size of feasible tree nodes = " + feasibleTreeNodes.size() + "........................");
        return feasibleTreeNodes;
    }


    /**
     * Method that calculates a MultipleKnapsack's total latency.
     */
    public void calculatelatency() {
        double latency = 0;

        for (Knapsack knapsack : knapsacks) {
            for (PartialSiddhiApp item : knapsack.getpartialSiddhiApps()) {
                latency += item.getlatency();
            }
        }

        this.latency = latency;
    }

    /**
     * Method that prints out the result of a MultipleKnapsack.
     */
    public LinkedList<PartialSiddhiApp> printResult(boolean flag) {
        for (Knapsack knapsack : knapsacks) {
            ;
            log.info("\nResourceNode\n" + "resourceNode: " + knapsack.getresourceNode()
                    + "\nTotal Usable CPU : " + knapsack.getStartcpuUsage() +
                    "\nUsed CPU in this iteration: " + (knapsack.getStartcpuUsage() - knapsack.getcapacity()) +
                    "\nRemaining CPU : " + knapsack.getcapacity() + "\n");

            knapsack.setStartcpuUsage(knapsack.getcapacity());

            log.info("Initial CPU Usage of " + knapsack.getresourceNode() + " is set to " + knapsack.getStartcpuUsage() + "\n");
            try {
                for (PartialSiddhiApp item : knapsack.getpartialSiddhiApps()) {
                    log.info("\n\nPartial siddhi app\n" + "Name: " + item.getName()
                            + "\nLatency : " + item.getlatency() + "\nCPU Usage : " + item.getcpuUsage());
                    partialSiddhiApps.remove(item);
                    log.info("removing " + item.getName() + " from partialsiddhiapps list");
                    log.info("\n");
                }
            } catch (ConcurrentModificationException e) {
                e.printStackTrace();
            }
            log.info("---------------------------\n");
        }

        log.info("Total latency: " + latency);
        return partialSiddhiApps;
    }


    public Map<ResourceNode, List<PartialSiddhiApp>> updatemap(Map<ResourceNode, List<PartialSiddhiApp>> map) {
        for (Knapsack knapsack : knapsacks) {
            List<PartialSiddhiApp> temp = new LinkedList<>();
            for (PartialSiddhiApp item : knapsack.getpartialSiddhiApps()) {
                temp.add(item);
                //log.info("Adding " + item.getName() + " to " + knapsack.getresourceNode());
            }
            if (map.containsKey(knapsack.getresourceNode())) {
                for (PartialSiddhiApp partialSiddhiApps : temp) {
                    map.get(knapsack.getresourceNode()).add(partialSiddhiApps);
                    log.info("Updating " + knapsack.getresourceNode().getId() + " with " +
                            partialSiddhiApps.getName());
                }
            } else {
                map.put(knapsack.getresourceNode(), temp);
                log.info("Adding to" + knapsack.getresourceNode().getId());
            }
        }
        return map;
    }

    public ResourceNode getResourceNode(String partialSiddhiAppName) {
        for (Knapsack knapsack : knapsacks) {
            log.info("checking" + knapsack.getresourceNode().toString());
            for (PartialSiddhiApp item : knapsack.getpartialSiddhiApps()) {
                if (item.getName() == partialSiddhiAppName) {
                    log.info("deploying node is " + knapsack.getresourceNode().toString());
                    return knapsack.getresourceNode();

                }
            }
        }
        return null;
    }

    /**
     * Method that sets the partialSiddhiApps that are not in a knapsack already.
     *
     * @param partialSiddhiApps
     */
    public void setpartialSiddhiApps(LinkedList<PartialSiddhiApp> partialSiddhiApps) {
        this.partialSiddhiApps = partialSiddhiApps;
    }

    /**
     * Method that sets all of the knapsacks.
     *
     * @param knapsacks
     */
    public void setKnapsacks(LinkedList<Knapsack> knapsacks) {
        this.knapsacks = knapsacks;
    }

    /**
     * Method that gets the total latency of a MultipleKnapsack.
     *
     * @return
     */
    public double getlatency() {
        return latency;
    }

    /**
     * Constructor that instantiates necessary objects.
     */
    public MultipleKnapsack() {
        knapsacks = new LinkedList<>();
        partialSiddhiApps = new LinkedList<>();
    }

    /**
     * Method that gets all of the knapsacks in the MultipleKnapsack.
     *
     * @return
     */
    public LinkedList<Knapsack> getKnapsacks() {
        return knapsacks;
    }

    /**
     * Method that gets all of the partialSiddhiApps that are not in a knapsack already.
     *
     * @return
     */
    public LinkedList<PartialSiddhiApp> getpartialSiddhiApps() {
        return partialSiddhiApps;
    }

    /**
     * Method that adds a knapsack into the MultipleKnapsack.
     *
     * @param knapsack
     */
    public void addKnapsack(Knapsack knapsack) {
        knapsacks.add(knapsack);
    }

    public void modifyKnapsack(int index, double capacityacity, String resourceNode) {
        //knapsacks.set(index,new Knapsack(capacityacity, resourceNode));
    }

    public Map<ResourceNode, List<PartialSiddhiApp>> getMap() {
        return output_map;
    }

    public double calculateThroughputValue(LinkedList<PartialSiddhiApp> updatedPartialSiddhiApps) {
        double maxCPUUsage = 0.0;
        int i = 0;
        double remainingCPUUsage = 0.0;
        double remainingThroughputValues = 0.0;
        double dif = 0.0;
        double lastCpuValue = 0.0;
        double throughputValue = 0.0;
        double upperLimit = 0.0;


        while (i < knapsacks.size()) {
            for (PartialSiddhiApp app : updatedPartialSiddhiApps) {
                if (knapsacks.get(i).getcapacity() > maxCPUUsage) {

                    maxCPUUsage += app.getcpuUsage();
                    throughputValue += app.getThroughput();
                    lastCpuValue = app.getcpuUsage();
                } else {
                    if (knapsacks.get(i).getcapacity() < maxCPUUsage) {
                        upperLimit = maxCPUUsage - lastCpuValue;
                        lastCpuValue = 0.0;
                    }

                    dif = knapsacks.get(i).getcapacity() - upperLimit;
                    remainingCPUUsage += app.getcpuUsage();
                    remainingThroughputValues += app.getThroughput();

                    log.info("Remaining cpu usages ===="+remainingCPUUsage);
                    if(remainingCPUUsage != 0.0){
                        throughputValue = throughputValue + (remainingThroughputValues / remainingCPUUsage) * dif;
                    }


                }
            }

            if (throughputValue != 0.0) {
                //knapsacks.get(i).setcapacity(0.0);
                break;
            }
            i++;

        }

        return throughputValue;
    }

    public double calculateUpperBound(LinkedList<PartialSiddhiApp> updatedPartialSiddhiApps) {
        int i = 0;
        double maxCPUUsage = 0.0;
        double throughputValue = 0.0;

        log.info("Inside Calculate upper bound ...................");
        while (i < knapsacks.size()) {
            log.info("Calculate upper bound knapsack " + i + "...........");
            maxCPUUsage = 0.0;
            throughputValue = 0.0;

            double lastThroughputValue = 0.0;
            for (PartialSiddhiApp app : updatedPartialSiddhiApps) {
                log.info("inside cal upper bound,,, app cpu usage = " + app.getcpuUsage() + "knapsack capacity =" + knapsacks.get(i).getcapacity());
                log.info("totalcpu usage knapsack can withstand===" + maxCPUUsage);
                if (knapsacks.get(i).getcapacity() > maxCPUUsage) {

                    maxCPUUsage += app.getcpuUsage();
                    throughputValue += app.getThroughput();

                    log.info("Cpu usages of Partial siddhi apps of Knapsack :" + maxCPUUsage + "..........");
                    lastThroughputValue = app.getThroughput();
                } else {
                    if (knapsacks.get(i).getcapacity() < maxCPUUsage) {
                        throughputValue -= lastThroughputValue;
                        lastThroughputValue = 0.0;
                    }
                    break;
                }
            }
            if (throughputValue != 0.0) {
                log.info("Calculated upper bound : " + throughputValue);
                log.info("knapsack===="+i);
                break;
            }
            i++;
        }
        return throughputValue;
    }


}