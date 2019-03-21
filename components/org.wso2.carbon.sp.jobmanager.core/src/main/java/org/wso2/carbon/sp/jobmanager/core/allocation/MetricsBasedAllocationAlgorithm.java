package org.wso2.carbon.sp.jobmanager.core.allocation;


import com.zaxxer.hikari.HikariDataSource;
import org.apache.log4j.Logger;
import org.wso2.carbon.datasource.core.api.DataSourceService;
import org.wso2.carbon.datasource.core.exception.DataSourceException;
import org.wso2.carbon.sp.jobmanager.core.appcreator.DistributedSiddhiQuery;
import org.wso2.carbon.sp.jobmanager.core.appcreator.SiddhiQuery;
import org.wso2.carbon.sp.jobmanager.core.bean.DeploymentConfig;
import org.wso2.carbon.sp.jobmanager.core.exception.ResourceManagerException;
import org.wso2.carbon.sp.jobmanager.core.internal.ServiceDataHolder;
import org.wso2.carbon.sp.jobmanager.core.model.ResourceNode;
import org.wso2.carbon.sp.jobmanager.core.model.SiddhiAppHolder;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * The Algorithm which evluate the allocation using the metrics of Partial SiddhiApps.
 */

public class MetricsBasedAllocationAlgorithm implements ResourceAllocationAlgorithm {

    private static final Logger logger = Logger.getLogger(MetricsBasedAllocationAlgorithm.class);
    private DeploymentConfig deploymentConfig = ServiceDataHolder.getDeploymentConfig();
    private Iterator resourceIterator;
    public Map<ResourceNode, List<PartialSiddhiApp>> output_map = new HashMap<>();
    public Map<String, Double> latency_map = new HashMap<>();
    public boolean check = false;
    Connection connection = null;
    Statement statement;
    int metricCounter;

    public Statement dbConnector() {
        try {
            String datasourceName = ServiceDataHolder.getDeploymentConfig().getDatasource();
            DataSourceService dataSourceService = ServiceDataHolder.getDataSourceService();
            DataSource datasource = (HikariDataSource) dataSourceService.getDataSource(datasourceName);
            connection = datasource.getConnection();
            Statement statement = connection.createStatement();
            return statement;
        } catch (SQLException e) {
            logger.error("SQL error : " + e.getMessage());
        } catch (DataSourceException e) {
            logger.error("Datasource error : " + e.getMessage());
        }
        return null;
    }

    private List<SiddhiAppHolder> getSiddhiAppHolders(DistributedSiddhiQuery distributedSiddhiQuery) {
        List<SiddhiAppHolder> siddhiAppHolders = new ArrayList<>();
        distributedSiddhiQuery.getQueryGroups().forEach(queryGroup -> {
            queryGroup.getSiddhiQueries().forEach(query -> {
                siddhiAppHolders.add(new SiddhiAppHolder(distributedSiddhiQuery.getAppName(),
                        queryGroup.getGroupName(), query.getAppName(), query.getApp(),
                        null, queryGroup.isReceiverQueryGroup(), queryGroup.getParallelism()));
            });
        });
        return siddhiAppHolders;
    }

    public void retrieveData(DistributedSiddhiQuery distributedSiddhiQuery,
                             LinkedList<PartialSiddhiApp> partailSiddhiApps) {
        List<SiddhiAppHolder> appsToDeploy = getSiddhiAppHolders(distributedSiddhiQuery);
        statement = dbConnector();
        ResultSet resultSet;
        try {
            for (SiddhiAppHolder appHolder : appsToDeploy) {
                String[] SplitArray = appHolder.getAppName().split("-");

                int executionGroup = Integer.valueOf(SplitArray[SplitArray.length-2].substring(5));
                int parallelInstance = Integer.valueOf(SplitArray[SplitArray.length-1]);

                logger.info("Metric details of " + appHolder.getAppName() + "\n");
                logger.info("---------------------------------------------------");
                String query = "SELECT m3 ,m5, m7 ,m16 FROM metricstable where exec=" +
                        executionGroup + " and parallel=" + parallelInstance +
                        " order by iijtimestamp desc limit 1 ";
                resultSet = statement.executeQuery(query);

                if (resultSet.isBeforeFirst()) {     //Check the corresponding partial siddhi app is having the metrics
                    while (resultSet.next()) {

                        double throughput = resultSet.getDouble("m3");
                        logger.info("Throughput : " + throughput);

                        int eventCount = resultSet.getInt("m5");
                        logger.info("Event Count : " + eventCount);

                        double latency = resultSet.getLong("m7");
                        logger.info("latency : " + latency);
                        latency_map.put(appHolder.getAppName(), latency);

                        double processCPU = resultSet.getDouble("m16");
                        logger.info("process CPU : " + processCPU);

                        partailSiddhiApps.add(new PartialSiddhiApp(processCPU, (1 / latency), throughput, eventCount, appHolder.getAppName()));
                        logger.info(appHolder.getAppName() + " created with reciprocal of latency : " + (1 / latency) +
                                " and processCPU : " + processCPU + "\n");
                    }
                } else {
                    logger.warn("Metrics are not available for the siddhi app " + appHolder.getAppName()
                            + ". Hence using 0 as knapsack parameters");
                    metricCounter++;
                    double latency = 0.0;
                    logger.info("latency : " + latency);

                    double throughput = 0.0;
                    logger.info("Throughput = " + throughput);

                    double processCPU = 0.0;
                    logger.info("process CPU : " + processCPU);

                    int eventCount = 0;
                    logger.info("event count : " + eventCount);
                    partailSiddhiApps.add(new PartialSiddhiApp(processCPU, latency, throughput, eventCount, appHolder.getAppName()));
                    logger.info(appHolder.getAppName() + " created with reciprocal of latency : " + 0.0 +
                            ", Throughput :" + throughput +
                            " and processCPU : " + processCPU + "\n");
                }
            }
            if ((metricCounter + 2) > appsToDeploy.size()) {
                logger.error("Metrics are not available for required number of Partial siddhi apps");
            }

        } catch (SQLException e) {
            logger.error(e);
        }
    }

    public ResourceNode getNextResourceNode(Map<String, ResourceNode> resourceNodeMap,
                                            int minResourceCount,
                                            SiddhiQuery siddhiQuery) {

        long initialTimestamp = System.currentTimeMillis();
        logger.info("Trying to deploy " + siddhiQuery.getAppName());
        if (deploymentConfig != null && !resourceNodeMap.isEmpty()) {
            if (resourceNodeMap.size() >= minResourceCount) {
                check = true;
                ResourceNode resourceNode = null;
                try {
                    logger.info("outmapsize in getNextResourcNode :" + output_map.size());
                    for (ResourceNode key : output_map.keySet()) {
                        for (PartialSiddhiApp partialSiddhiApp : output_map.get(key)) {
                            if (partialSiddhiApp.getName() == siddhiQuery.getAppName()) {
                                resourceNode = key;
                                return resourceNode;
                            }
                        }
                    }
                    if (resourceNode == null) {
                        if (resourceIterator == null) {
                            resourceIterator = resourceNodeMap.values().iterator();
                        }
                        if (resourceIterator.hasNext()) {
                            logger.warn(siddhiQuery.getAppName() + " did not allocatd in MetricsBasedAlgorithm ." +
                                    "hence deploying in " + (ResourceNode) resourceIterator.next());
                            return (ResourceNode) resourceIterator.next();
                        } else {
                            resourceIterator = resourceNodeMap.values().iterator();
                            if (resourceIterator.hasNext()) {
                                logger.warn(siddhiQuery.getAppName() + " did not allocatd in MetricsBasedAlgorithm ." +
                                        "hence deploying in " + (ResourceNode) resourceIterator.next());
                                return (ResourceNode) resourceIterator.next();
                            }
                        }
                    }
                } catch (ResourceManagerException e) {
                    if ((System.currentTimeMillis() - initialTimestamp) >= (deploymentConfig.
                            getHeartbeatInterval() * 2))
                        throw e;
                }
            }
        } else {
            logger.error("There are no enough resources to deploy");
        }
        return null;
    }

    public void executeKnapsack(Map<String, ResourceNode> resourceNodeMap,
                                int minResourceCount,
                                DistributedSiddhiQuery distributedSiddhiQuery) {
        logger.info("Inside New Knapsack ..............................................");

        if (deploymentConfig != null && !resourceNodeMap.isEmpty()) {
            if (resourceNodeMap.size() >= minResourceCount) {
                resourceIterator = resourceNodeMap.values().iterator();
                MultipleKnapsack multipleKnapsack = new MultipleKnapsack();

                LinkedList<PartialSiddhiApp> partialSiddhiApps = new LinkedList<>();
                retrieveData(distributedSiddhiQuery, partialSiddhiApps);

                logger.info("size of partial siddhi apps list at beginning : " + partialSiddhiApps.size());
                double TotalCPUUsagePartialSiddhi = 0.0;
                // int TotalEventCount = 0;

                for (int j = 0; j < partialSiddhiApps.size(); j++) {
                    TotalCPUUsagePartialSiddhi = TotalCPUUsagePartialSiddhi + partialSiddhiApps.get(j).getcpuUsage();
                    //TotalEventCount = TotalEventCount +partialSiddhiApps.get(j).getEventCount();
                }
                logger.info("TotalCPUUsagePartialSiddhi : " + TotalCPUUsagePartialSiddhi + "\n");
                //logger.info("Total Event Count : " + TotalEventCount+"....................");

                //  int averageEventCount = (int)Math.round((double)TotalEventCount/partialSiddhiApps.size());
                //logger.info("Average Event Count = " + averageEventCount+".........................");

                LinkedList<PartialSiddhiApp> busyPartialSiddhiApps = new LinkedList<>();
                LinkedList<PartialSiddhiApp> normalPartialSiddhiApps = new LinkedList<>();
                double totalCPUUsagesOfNormalPartialSiddhiApps = 0.0;
                double totalCPUUsagesOfBusyPartialSiddhiApps = 0.0;

              /*  for(int j = 0; j < partialSiddhiApps.size(); j++){
                    if(partialSiddhiApps.get(j).getEventCount() >= averageEventCount){
                        busyPartialSiddhiApps.add(partialSiddhiApps.get(j));
                        totalCPUUsagesOfBusyPartialSiddhiApps += partialSiddhiApps.get(j).getcpuUsage();
                    }else{
                        normalPartialSiddhiApps.add(partialSiddhiApps.get(j));
                        totalCPUUsagesOfNormalPartialSiddhiApps += partialSiddhiApps.get(j).getcpuUsage();
                    }
                }*/

                /*logger.info("Size of busy partial siddhi apps : " + busyPartialSiddhiApps.size());
                logger.info("Size of normal partial siddhi apps : " + normalPartialSiddhiApps.size());*/
                for (int p = 0; p < resourceNodeMap.size(); p++) {
                    ResourceNode resourceNode = (ResourceNode) resourceIterator.next();
                    multipleKnapsack.addKnapsack(new Knapsack((TotalCPUUsagePartialSiddhi / resourceNodeMap.size()),
                            resourceNode));
                    logger.info("created a knapsack of " + resourceNode);

                }
                logger.info("Starting branch and bound for normal partial siddhi apps....................");
                for(PartialSiddhiApp app : partialSiddhiApps){
                    logger.info("CPU Usage of app===="+app.getcpuUsage()+".........................");
                }
                partialSiddhiApps = multipleKnapsack.executeBranchAndBoundKnapsack(partialSiddhiApps);

              /*  for (int p = 0; p < multipleKnapsack.getKnapsacks().size(); p++) {
                    multipleKnapsack.getKnapsacks().get(p)
                            .setcapacity(totalCPUUsagesOfBusyPartialSiddhiApps/multipleKnapsack.getKnapsacks().size()
                                    + multipleKnapsack.getKnapsacks().get(p).getcapacity());
                }*/

                // logger.info("Starting branch and bound for busy partial siddhi apps.................................");
                // busyPartialSiddhiApps = multipleKnapsack.executeBranchAndBoundKnapsack(busyPartialSiddhiApps);
                //multipleKnapsack.calculatelatency();
                //multipleKnapsack.updatemap(output_map);

               /* partialSiddhiApps.clear();
                partialSiddhiApps.addAll(normalPartialSiddhiApps);
                partialSiddhiApps.addAll(busyPartialSiddhiApps);
*/
                if (partialSiddhiApps.size() > 0) {
                    /*Collections.sort(partialSiddhiApps, new Comparator<PartialSiddhiApp>() {
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
                    });*/
                    multipleKnapsack.executeBranchAndBoundKnapsack(partialSiddhiApps);
                    multipleKnapsack.calculatelatency();
                    multipleKnapsack.updatemap(output_map);

                    logger.info("Remaining partial siddhi apps after complete iteration " + partialSiddhiApps.size());
                }

            } else {
                logger.error("Minimum resource requirement did not match, hence not deploying the partial siddhi app ");
            }
        }

    }
}