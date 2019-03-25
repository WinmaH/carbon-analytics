package org.wso2.carbon.sp.jobmanager.core.allocation;

import org.apache.log4j.Logger;
import org.wso2.carbon.sp.jobmanager.core.model.ResourceNode;

import java.util.*;

public class MultipleKnapsack {

    private static final Logger log = Logger.getLogger(MetricsBasedAllocationAlgorithm.class);
    private LinkedList<Knapsack> knapsacks;
    private LinkedList<PartialSiddhiApp> partialSiddhiApps;
    private LinkedList<PartialSiddhiApp> updatedPartialSiddhiApps;
    private double latency;

    double upperBound;
    double throughputValue;
    double initialUpperBound = Double.MAX_VALUE;
    int noOfMaxLevels;
    int level = 0;

    List<BranchAndBoundNode> branchAndBoundTreeNodes = Collections.synchronizedList(new ArrayList<>());
    List<BranchAndBoundNode> tempTreeNodes = Collections.synchronizedList(new ArrayList<>());
    List<BranchAndBoundNode> feasibleTreeNodes = Collections.synchronizedList(new ArrayList<>());
    public Map<ResourceNode, List<PartialSiddhiApp>> output_map = new HashMap<>();

    public LinkedList<PartialSiddhiApp> executeBranchAndBoundKnapsack(LinkedList<PartialSiddhiApp> partialSiddhiApps) {
        noOfMaxLevels=partialSiddhiApps.size();
        log.info("No of levels==="+noOfMaxLevels);
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

            if(partialSiddhiApps.size()==0){
                break;
            }

            for (int i = 0; i < partialSiddhiApps.size(); i++) {
                if (!this.partialSiddhiApps.contains(partialSiddhiApps.get(i))) {
                    this.partialSiddhiApps.add(partialSiddhiApps.get(i));
                }
            }
            log.info("Executing graph traversal with " + partialSiddhiApps.size() + " partial siddhi apps..........");

            initialUpperBound=Double.MAX_VALUE;
            updatedPartialSiddhiApps = partialSiddhiApps;
            level = 0;
            feasibleTreeNodes = graphTraversal(updatedPartialSiddhiApps, knapsack);

            log.info("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWw");
            log.info("Size of final feasible tree nodes inside execute branch and bound knapsack= " + feasibleTreeNodes.size());
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

            log.info("Length of feasible tree nodes (should be 1....)= " + feasibleTreeNodes.size());

            BranchAndBoundNode finalTreeNode = feasibleTreeNodes.get(0);

            for (PartialSiddhiApp app : finalTreeNode.getPartialSiddhiAppsOfNode()) {
                knapsack.addPartialSiddhiApps(app);
                partialSiddhiApps.remove(app);
                log.info("******************** Knapsack :- "+knapsack.getresourceNode());
                log.info("******************** Partial Siddhi App:- "+app);
            }

            branchAndBoundTreeNodes.clear();
            feasibleTreeNodes.clear();
        }
        return partialSiddhiApps;
    }

    public List<BranchAndBoundNode> graphTraversal(LinkedList<PartialSiddhiApp> partialSiddhiApps, Knapsack knapsack) {
        log.info("Starting graph traversal.....");
        log.info("Size of branch and bound===="+branchAndBoundTreeNodes.size()+".........");

        if (branchAndBoundTreeNodes.size() != 0 ) {       //ignore unfeasible tree nodes
            for (BranchAndBoundNode branchAndBoundNode : branchAndBoundTreeNodes) {
                if (branchAndBoundNode.getThroughputValue() > initialUpperBound) {
                    branchAndBoundTreeNodes.remove(branchAndBoundNode);
                }
            }
        }

        if (level < noOfMaxLevels) {
            log.info("level @ beginning ==="+level+"???????????????????");
            log.info("Initial upper bound = " + initialUpperBound);
            upperBound = calculateUpperBound(partialSiddhiApps, knapsack) * -1;
            log.info("Upper bound = " + upperBound);
            throughputValue = calculateThroughputValue(partialSiddhiApps, knapsack) * -1;
            log.info("Throughput value = " + throughputValue);

            if (initialUpperBound > upperBound) {
                initialUpperBound = upperBound;
                log.info("Initial upper bound====" + initialUpperBound);
            }
            if (throughputValue < initialUpperBound) {
                log.info("throughputValue < initialUpperBound1.........................................");
                branchAndBoundTreeNodes.add(new BranchAndBoundNode(upperBound, throughputValue, partialSiddhiApps));
                tempTreeNodes.add(new BranchAndBoundNode(upperBound, throughputValue, partialSiddhiApps));
                log.info("Size of branch and bound tree nodes=====" + branchAndBoundTreeNodes.size());
                log.info("Size of tempTreeNodes===== " + tempTreeNodes.size());
            }
            log.info("Size of partial sidddhi apps==="+partialSiddhiApps.size()+"....................");
            log.info("size original=="+getpartialSiddhiApps().size());
            log.info("get partial siddhi app=="+getpartialSiddhiApps().get(level).getName());

            partialSiddhiApps.remove(getpartialSiddhiApps().get(level));
            level++;
            upperBound = calculateUpperBound(partialSiddhiApps, knapsack) * -1;
            throughputValue = calculateThroughputValue(partialSiddhiApps, knapsack) * -1;

            if (initialUpperBound > upperBound) {
                initialUpperBound = upperBound;
            }

            if (throughputValue < initialUpperBound) {
                log.info("throughputValue < initialUpperBound2.........................................");
                branchAndBoundTreeNodes.add(new BranchAndBoundNode(upperBound, throughputValue, partialSiddhiApps));
                tempTreeNodes.add(new BranchAndBoundNode(upperBound, throughputValue, partialSiddhiApps));
                log.info("Size of branch and bound tree nodes=====" + branchAndBoundTreeNodes.size());
                log.info("Size of tempTreeNodes===== " + tempTreeNodes.size());
            }

            while (tempTreeNodes.size() != 0) {
                log.info("tempTreeNodes is not empty...................................");
                log.info("Size of tempTreeNodes inside remainingTreeNodes=======" + tempTreeNodes.size());
                log.info(",,,,,,,,,,,,,,,," + tempTreeNodes.get(0).getPartialSiddhiAppsOfNode().size());

                if (tempTreeNodes.get(0).getPartialSiddhiAppsOfNode().size() != 0) {
                    graphTraversal(tempTreeNodes.get(0).getPartialSiddhiAppsOfNode(), knapsack);
                }
                if(tempTreeNodes.size()!=0){
                    tempTreeNodes.remove(tempTreeNodes.get(0));
                }else {
                    break;
                }
                log.info("Size of tempTreeNode" + tempTreeNodes.size() + "..................................");
            }
            log.info("tempTreeNodes is empty....................");
            log.info("Size of final feasible tree nodes after graph traversing1= " + branchAndBoundTreeNodes.size() + "........................");
            log.info(".....level===="+level);
            log.info(".....partial siddhiapps size==="+partialSiddhiApps.size());
        }
        log.info("Size of final feasible tree nodes after graph traversing2= " + branchAndBoundTreeNodes.size() + "........................");
        log.info("ppppppppppppppppppppppppppppppppppppppppppppppppppppppp");
        log.info("end of the graph traversal for the knapsack "+ knapsack.getresourceNode());
        return branchAndBoundTreeNodes;

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

    public double calculateThroughputValue(LinkedList<PartialSiddhiApp> partialSiddhiApps, Knapsack knapsack) {
        double maxCPUUsage = 0.0;
        double remainingCPUUsage = 0.0;
        double remainingThroughputValues = 0.0;
        double dif;
        double lastCpuValue = 0.0;
        double throughputValue = 0.0;
        double upperBound = 0.0;

        for (PartialSiddhiApp app : partialSiddhiApps) {
            if (knapsack.getcapacity() > maxCPUUsage && knapsack.getcapacity() > app.getcpuUsage()) {
                maxCPUUsage += app.getcpuUsage();
                throughputValue += app.getThroughput();
                lastCpuValue = app.getcpuUsage();
            } else {
                if (knapsack.getcapacity() < maxCPUUsage) {
                    upperBound = maxCPUUsage - lastCpuValue;
                    lastCpuValue = 0.0;
                }
                dif = knapsack.getcapacity() - upperBound;
                remainingCPUUsage += app.getcpuUsage();
                remainingThroughputValues += app.getThroughput();

                log.info("Remaining cpu usages ====" + remainingCPUUsage);
                if (remainingCPUUsage != 0.0) {
                    throughputValue = throughputValue + (remainingThroughputValues / remainingCPUUsage) * dif;
                }
            }
        }
        return throughputValue;
    }

    public double calculateUpperBound(LinkedList<PartialSiddhiApp> updatedPartialSiddhiApps, Knapsack knapsack) {
        double maxCPUUsage = 0.0;
        double upperBound = 0.0;
        double lastThroughputValue = 0.0;

        for (PartialSiddhiApp app : updatedPartialSiddhiApps) {
            log.info("inside cal upper bound,,, app cpu usage = " + app.getcpuUsage() + "knapsack capacity =" + knapsack.getcapacity());
            log.info("totalcpu usage knapsack can hold===" + maxCPUUsage);
            if (knapsack.getcapacity() > maxCPUUsage && knapsack.getcapacity() > app.getcpuUsage()) {

                maxCPUUsage += app.getcpuUsage();
                upperBound += app.getThroughput();

                log.info("Cpu usages of Partial siddhi apps of Knapsack :" + maxCPUUsage + "..........");
                lastThroughputValue = app.getThroughput();
            } else {
                if (knapsack.getcapacity() < maxCPUUsage) {
                    upperBound -= lastThroughputValue;
                    lastThroughputValue = 0.0;
                }
                break;
            }
        }
        return upperBound;
    }


}