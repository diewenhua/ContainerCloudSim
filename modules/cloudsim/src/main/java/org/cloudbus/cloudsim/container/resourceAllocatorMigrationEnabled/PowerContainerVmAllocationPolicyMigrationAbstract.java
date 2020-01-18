package org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled;

import org.cloudbus.cloudsim.container.resourceAllocators.PowerContainerVmAllocationAbstract;
import org.cloudbus.cloudsim.container.vmSelectionPolicies.PowerContainerVmSelectionPolicy;
import org.cloudbus.cloudsim.container.core.*;
import org.cloudbus.cloudsim.container.core.ContainerDatacenter;
import org.cloudbus.cloudsim.container.lists.PowerContainerVmList;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.util.ExecutionTimeMeasurer;

//自己
import org.cloudbus.cloudsim.container.core.ContainerDatacenterBroker;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerBwProvisionerSimple;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerPe;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerRamProvisionerSimple;
import org.cloudbus.cloudsim.container.containerProvisioners.CotainerPeProvisionerSimple;
import org.cloudbus.cloudsim.container.containerSelectionPolicies.PowerContainerSelectionPolicy;
import org.cloudbus.cloudsim.container.lists.*;
import org.cloudbus.cloudsim.container.resourceAllocators.PowerContainerAllocationPolicy;
import org.cloudbus.cloudsim.container.schedulers.ContainerSchedulerTimeSharedOverSubscription;
import org.cloudbus.cloudsim.container.utils.IDs;
import org.cloudbus.cloudsim.container.utils.RandomGen;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled.PowerContainerVmAllocationPolicyMigrationStaticThresholdMC;

import java.util.*;

/**
 * Created by sareh on 28/07/15.
 */
public abstract class PowerContainerVmAllocationPolicyMigrationAbstract extends PowerContainerVmAllocationAbstract {

    protected List<? extends ContainerVm> vmList;
    /**
     * The vm selection policy.
     */
    private PowerContainerVmSelectionPolicy vmSelectionPolicy;

    /**
     * The saved allocation.
     */
    private final List<Map<String, Object>> savedAllocation = new ArrayList<Map<String, Object>>();

    /**
     * The utilization history.
     */
    private final Map<Integer, List<Double>> utilizationHistory = new HashMap<Integer, List<Double>>();

    /**
     * The metric history.
     */
    private final Map<Integer, List<Double>> metricHistory = new HashMap<Integer, List<Double>>();

    /**
     * The time history.
     */
    private final Map<Integer, List<Double>> timeHistory = new HashMap<Integer, List<Double>>();

    /**
     * The execution time history vm selection.
     */
    private final List<Double> executionTimeHistoryVmSelection = new LinkedList<Double>();

    /**
     * The execution time history host selection.
     */
    private final List<Double> executionTimeHistoryHostSelection = new LinkedList<Double>();

    /**
     * The execution time history vm reallocation.
     */
    private final List<Double> executionTimeHistoryVmReallocation = new LinkedList<Double>();

    /**
     * The execution time history total.
     */
    private final List<Double> executionTimeHistoryTotal = new LinkedList<Double>();

    //自己
    private ContainerDatacenter datacenter;

    /**
     * Instantiates a new power vm allocation policy migration abstract.
     *
     * @param hostList          the host list
     * @param vmSelectionPolicy the vm selection policy
     */
    public PowerContainerVmAllocationPolicyMigrationAbstract(
            List<? extends ContainerHost> hostList,
            PowerContainerVmSelectionPolicy vmSelectionPolicy) {
        super(hostList);
        setVmSelectionPolicy(vmSelectionPolicy);

    }


    /**
     * Optimize allocation of the VMs according to current utilization.
     *
     * @param vmList the vm list
     * @return the array list< hash map< string, object>>
     */
    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends ContainerVm> vmList) {
        System.out.println("VMmigration-3B-PowerContainerVmAllocationPolicyMigrationAbstract.optimizeAllocation");
        ExecutionTimeMeasurer.start("optimizeAllocationTotal");

        ExecutionTimeMeasurer.start("optimizeAllocationHostSelection");
        //System.out.println("VMmigration-3B-1-调用同类VMmigration-7-1-PowerContainerVmAllocationPolicyMigrationAbstract.getOverUtilizedHosts");
        /**
         * @see PowerContainerVmAllocationPolicyMigrationAbstract#getOverUtilizedHosts()
         */
        List<PowerContainerHostUtilizationHistory> overUtilizedHosts = getOverUtilizedHosts();
        getExecutionTimeHistoryHostSelection().add(
                ExecutionTimeMeasurer.end("optimizeAllocationHostSelection"));

        printOverUtilizedHosts(overUtilizedHosts);

        saveAllocation();

        ExecutionTimeMeasurer.start("optimizeAllocationVmSelection");
        //System.out.println("VMmigration-3B-2-调用同类VMmigration-7-1-3-PowerVmAllocationPolicyMigrationAbstract.getVmsToMigrateFromHosts");
        List<? extends ContainerVm> vmsToMigrate = getVmsToMigrateFromHosts(overUtilizedHosts);
        getExecutionTimeHistoryVmSelection().add(ExecutionTimeMeasurer.end("optimizeAllocationVmSelection"));

        Log.printLine("Reallocation of VMs from the over-utilized hosts:");
        ExecutionTimeMeasurer.start("optimizeAllocationVmReallocation");
        //System.out.println("VMmigration-3B-3-调用同类VMmigration-9-PowerContainerVmAllocationPolicyMigrationAbstract.getNewVmPlacement");
        /**
         * @see PowerContainerVmAllocationPolicyMigrationAbstract#getNewVmPlacement(List, Set)
         */
        List<Map<String, Object>> migrationMap = getNewVmPlacement(vmsToMigrate, new HashSet<ContainerHost>(
                overUtilizedHosts));
        getExecutionTimeHistoryVmReallocation().add(
                ExecutionTimeMeasurer.end("optimizeAllocationVmReallocation"));
        Log.printLine();

        System.out.println("VMmigration-3B-4-调用-VMmigration-7-2-PowerVmAllocationPolicyMigrationAbstract.getMigrationMapFromUnderUtilizedHosts");
        migrationMap.addAll(getMigrationMapFromUnderUtilizedHosts(overUtilizedHosts, migrationMap));

        restoreAllocation();

        getExecutionTimeHistoryTotal().add(ExecutionTimeMeasurer.end("optimizeAllocationTotal"));

        return migrationMap;
    }


    /**
     * Gets the over utilized hosts.
     *
     * @return the over utilized hosts
     */
    protected List<PowerContainerHostUtilizationHistory> getOverUtilizedHosts() {
        System.out.println("VMmigration-7-高阈值-PowerContainerVmAllocationPolicyMigrationAbstract.getOverUtilizedHosts");
        List<PowerContainerHostUtilizationHistory> overUtilizedHosts = new LinkedList<PowerContainerHostUtilizationHistory>();
        for (PowerContainerHostUtilizationHistory host : this.<PowerContainerHostUtilizationHistory>getContainerHostList()) {
            //System.out.println("VMmigration-7-1-1-判断是不是负载过重主机");
            if (isHostOverUtilized(host)) {
                overUtilizedHosts.add(host);
            }
        }
        return overUtilizedHosts;
    }

    /**
     * Prints the over utilized hosts.
     *
     * @param overUtilizedHosts the over utilized hosts
     */
    protected void printOverUtilizedHosts(List<PowerContainerHostUtilizationHistory> overUtilizedHosts) {
        if (!Log.isDisabled()) {
            System.out.println("VMmigration-7-1-2-PowerVmAllocationPolicyMigrationAbstract.printOverUtilizedHosts");
            Log.printLine("Over-utilized hosts:");
            for (PowerContainerHostUtilizationHistory host : overUtilizedHosts) {
                Log.printConcatLine("Host #", host.getId());
            }
            Log.printLine();
        }
    }

    /**
     * Gets the vms to migrate from hosts.
     *
     * @param overUtilizedHosts the over utilized hosts
     * @return the vms to migrate from hosts
     */
    protected List<? extends ContainerVm> getVmsToMigrateFromHosts(List<PowerContainerHostUtilizationHistory> overUtilizedHosts) {
        System.out.println("VMmigration-7-1-3-PowerVmAllocationPolicyMigrationAbstract.getVmsToMigrateFromHosts");
        List<ContainerVm> vmsToMigrate = new LinkedList<ContainerVm>();
        for (PowerContainerHostUtilizationHistory host : overUtilizedHosts) {
            while (true) {

                ContainerVm vm = getVmSelectionPolicy().getVmToMigrate(host);
                if (vm == null) {
                    break;
                }
                vmsToMigrate.add(vm);
                host.containerVmDestroy(vm);
                if (!isHostOverUtilized(host)) {
                    break;
                }
            }
        }
        for (int i = 0; i < vmsToMigrate.size(); i++) {
            if (i!=0&&i%5==0) {
                System.out.println("VM#"+vmsToMigrate.get(i).getId()+"  ");
            }else {
                System.out.print("VM#"+vmsToMigrate.get(i).getId()+"  ");
            }
        }
        return vmsToMigrate;
    }

    /**
     * Gets the migration map from under utilized hosts.
     *
     * @param overUtilizedHosts the over utilized hosts
     * @return the migration map from under utilized hosts
     */
    protected List<Map<String, Object>> getMigrationMapFromUnderUtilizedHosts(
            List<PowerContainerHostUtilizationHistory> overUtilizedHosts, List<Map<String, Object>> previouseMap) {
        System.out.println("VMmigration-7-2-PowerVmAllocationPolicyMigrationAbstract.getMigrationMapFromUnderUtilizedHosts");
        List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
        //System.out.println("VMmigration-7-2-1-调用同类VMmigration-7-2-2-PowerContainerVmAllocationPolicyMigrationAbstract.getSwitchedOffHosts");
        List<PowerContainerHost> switchedOffHosts = getSwitchedOffHosts();

        // over-utilized hosts + hosts that are selected to migrate VMs to from over-utilized hosts
        Set<PowerContainerHost> excludedHostsForFindingUnderUtilizedHost = new HashSet<>();
        excludedHostsForFindingUnderUtilizedHost.addAll(overUtilizedHosts);
        excludedHostsForFindingUnderUtilizedHost.addAll(switchedOffHosts);
        excludedHostsForFindingUnderUtilizedHost.addAll(extractHostListFromMigrationMap(previouseMap));

        // over-utilized + under-utilized hosts
        Set<PowerContainerHost> excludedHostsForFindingNewVmPlacement = new HashSet<>();
        excludedHostsForFindingNewVmPlacement.addAll(overUtilizedHosts);
        excludedHostsForFindingNewVmPlacement.addAll(switchedOffHosts);

        int numberOfHosts = getContainerHostList().size();

        while (true) {
            if (numberOfHosts == excludedHostsForFindingUnderUtilizedHost.size()) {
                break;
            }

            /**
             * @see org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled.PowerContainerVmAllocationPolicyMigrationAbstractHostSelection#getUnderUtilizedHost(Set)
             */
            //System.out.println("VMmigration-7-2-3-调用VMmigration-7-2-4-PowerContainerVmAllocationPolicyMigrationAbstractHostSelection.getUnderUtilizedHost");
            PowerContainerHost underUtilizedHost = getUnderUtilizedHost(excludedHostsForFindingUnderUtilizedHost);
            if (underUtilizedHost == null) {
                break;
            }

            Log.printConcatLine("Under-utilized host: host #", underUtilizedHost.getId(), "\n");

            excludedHostsForFindingUnderUtilizedHost.add(underUtilizedHost);
            excludedHostsForFindingNewVmPlacement.add(underUtilizedHost);

            //System.out.println("VMmigration-7-2-7-调用本类VMmigration-7-2-8-PowerContainerVmAllocationPolicyMigrationAbstract.getVmsToMigrateFromUnderUtilizedHost");
            List<? extends ContainerVm> vmsToMigrateFromUnderUtilizedHost = getVmsToMigrateFromUnderUtilizedHost(underUtilizedHost);
            if (vmsToMigrateFromUnderUtilizedHost.isEmpty()) {
                continue;
            }

            Log.print("Reallocation of VMs from the under-utilized host: ");
            if (!Log.isDisabled()) {
                for (ContainerVm vm : vmsToMigrateFromUnderUtilizedHost) {
                    Log.print(vm.getId() + " ");
                }
            }
            Log.printLine();

            System.out.println("VMmigration-7-2-9-调用本类VMmigration-7-2-10-getNewVmPlacementFromUnderUtilizedHost");
            List<Map<String, Object>> newVmPlacement = getNewVmPlacementFromUnderUtilizedHost(
                    vmsToMigrateFromUnderUtilizedHost,
                    excludedHostsForFindingNewVmPlacement);

            excludedHostsForFindingUnderUtilizedHost.addAll(extractHostListFromMigrationMap(newVmPlacement));

            migrationMap.addAll(newVmPlacement);
            Log.printLine();
        }

        excludedHostsForFindingUnderUtilizedHost.clear();
        excludedHostsForFindingNewVmPlacement.clear();
        return migrationMap;
    }

    /**
     * Gets the under utilized host.
     *
     * @param excludedHosts the excluded hosts
     * @return the under utilized host
     */
    protected PowerContainerHost getUnderUtilizedHost(Set<? extends ContainerHost> excludedHosts) {

        System.out.println("ContainerMigration-5-PowerContainerVmAllocationPolicyMigrationAbstract.getUnderUtilizedHost");
        //double minUtilization = 1;
        PowerContainerHost underUtilizedHost = null;
        List<PowerContainerHost> containerHostList1=new ArrayList<PowerContainerHost>();
        containerHostList1.addAll(this.<PowerContainerHost>getContainerHostList());
        ContainerHostList.sortByUtilizationMipsDescending(containerHostList1);

        for (PowerContainerHost host : this.<PowerContainerHost>getContainerHostList()) {
            //System.out.println("<PowerContainerHost>getContainerHostList:lenth"+this.<PowerContainerHost>getContainerHostList().size());
            if (excludedHosts.contains(host)) {
                continue;
            }

            double utilization = host.getUtilizationOfCpu();
//            if (utilization > 0 && utilization <minUtilization
//                    && !areAllVmsMigratingOutOrAnyVmMigratingIn(host)&& !areAllContainersMigratingOutOrAnyContainersMigratingIn(host)) {
//                minUtilization = utilization;
//                underUtilizedHost = host;
//            }
            if (utilization > 0 && isHostUnderUtilized(host)
                    && !areAllVmsMigratingOutOrAnyVmMigratingIn(host)&& !areAllContainersMigratingOutOrAnyContainersMigratingIn(host)) {
                underUtilizedHost = host;
            }
        }
        return underUtilizedHost;
    }

    protected List<PowerContainerHost> getUnderUtilizedHostsC(Set<? extends ContainerHost> excludedHosts) {

        System.out.println("ContainerMigration-5-PowerContainerVmAllocationPolicyMigrationAbstract.getUnderUtilizedHost");
        //double minUtilization = 1;
        List<PowerContainerHost> underUtilizedHosts = new ArrayList<PowerContainerHost>();
        for (PowerContainerHost host : this.<PowerContainerHost>getContainerHostList()) {
            //System.out.println("<PowerContainerHost>getContainerHostList:lenth"+this.<PowerContainerHost>getContainerHostList().size());
            if (excludedHosts.contains(host)) {
                continue;
            }

            double utilization = host.getUtilizationOfCpu();
//            if (utilization > 0 && utilization <minUtilization
//                    && !areAllVmsMigratingOutOrAnyVmMigratingIn(host)&& !areAllContainersMigratingOutOrAnyContainersMigratingIn(host)) {
//                minUtilization = utilization;
//                underUtilizedHost = host;
//            }
            if (utilization > 0 && isHostUnderUtilized(host)
                    && !areAllVmsMigratingOutOrAnyVmMigratingIn(host)&& !areAllContainersMigratingOutOrAnyContainersMigratingIn(host)) {
                underUtilizedHosts.add(host);
            }
        }
        return underUtilizedHosts;
    }

    /**
     * Find host for vm.
     *
     * @param vm            the vm
     * @param excludedHosts the excluded hosts
     * @return the power host
     */
    public PowerContainerHost findHostForVm(ContainerVm vm, Set<? extends ContainerHost> excludedHosts) {
        System.out.println("VMallocation-5-PowerContainerVmAllocationPolicyMigrationAbstract.findHostForVm");
        double minPower = Double.MAX_VALUE;
        PowerContainerHost allocatedHost = null;
        //System.out.println("this.<PowerContainerHost>getContainerHostList:"+this.<PowerContainerHost>getContainerHostList().size());
        System.out.println("VMallocationExcludedHosts:"+excludedHosts.size());
        //List<PowerContainerHost> powerContainerHost1=new ArrayList<PowerContainerHost>();
        //powerContainerHost1.addAll(this.<PowerContainerHost> getContainerHostList());
        List<ContainerHost> containerHost1=new ArrayList<ContainerHost>();
        //containerHost1.addAll(ContainerDatacenterBroker.containerHostList111);
        int cishu=0;
        //for (PowerContainerHost host : this.<PowerContainerHost>getContainerHostList()) {
        //for (PowerContainerHost host :  containerHost1) {
        for (ContainerHost host1 :  ContainerDatacenterBroker.containerHostList111) {
            System.out.println("host循环次数"+(++cishu)+"host ID"+host1.getId());
            PowerContainerHost host=(PowerContainerHost)host1;
            if (excludedHosts.contains(host)) {
                continue;
            }
            /**
             * VMallocation-5-1-ContainerHost.isSuitableForContainerVm
             * VMallocation-5A-PowerContainerVmAllocationPolicyMigrationAbstract.getUtilizationOfCpuMips
             * VMallocation-5B-PowerContainerVmAllocationPolicyMigrationAbstract.isHostOverUtilizedAfterAllocation
             * @see org.cloudbus.cloudsim.container.core.ContainerHost#isSuitableForContainerVm
             */
            if (host.isSuitableForContainerVm(vm)) {
                //isSuitableForContainerVm内容
//                  return (getContainerVmScheduler().getPeCapacity() >= currentRequestedMaxMips1
//                  && getContainerVmScheduler().getAvailableMips() >= currentRequestedTotalMips1
//                  && getContainerVmRamProvisioner().isSuitableForContainerVm(vm, vm.getCurrentRequestedRam()) && getContainerVmBwProvisioner()
//                  .isSuitableForContainerVm(vm, vm.getCurrentRequestedBw()));
                System.out.println("调用VMallocation-5A-VMallocation-5B");
                //if (getUtilizationOfCpuMips(host) != 0 && isHostOverUtilizedAfterAllocation(host, vm)) {
                if (isHostOverUtilizedAfterAllocation(host, vm)) {
                    System.out.println("host:"+host.getId()+"isHostOverUtilizedAfterAllocation:"+vm.getId());
                    continue;
                }

                try {
                    double powerAfterAllocation = getPowerAfterAllocation(host, vm);
                    if (powerAfterAllocation != -1) {
                        double powerDiff = powerAfterAllocation - host.getPower();
                        if (powerDiff < minPower) {
                            minPower = powerDiff;
                            allocatedHost = host;
                        }
                    }
                } catch (Exception e) {
                }
            }else{
                System.out.println("host:"+host.getId()+"isNotSuitableForContainerVm:"+vm.getId());
            }
            if (allocatedHost!=null){
                break;
            }
        }
        //ContainerDatacenterBroker.containerHostList111.clear();
        return allocatedHost;
    }

    /**
     * Checks if is host over utilized after allocatio
     * @param host the host
     * @param vm   the vm
     * @return true, if is host over utilized after allocation
     */
    protected boolean isHostOverUtilizedAfterAllocation(PowerContainerHost host, ContainerVm vm) {
        System.out.println("VMallocation-5B-PowerContainerVmAllocationPolicyMigrationAbstract.isHostOverUtilizedAfterAllocation");
        boolean isHostOverUtilizedAfterAllocation = true;
        System.out.println("host的availableMIPS:"+host.getAvailableMips()+"vm的requestedMIPS:"+vm.getCurrentRequestedTotalMips());
        if (host.containerVmCreate(vm)) {

            /**
             * @see PowerContainerVmAllocationPolicyMigrationStaticThresholdMC#isHostOverUtilized
             */
            System.out.println("host分配之后的availableMIPS:"+host.getAvailableMips());
            isHostOverUtilizedAfterAllocation = isHostOverUtilized(host);
            host.containerVmDestroy(vm);
        }
        return isHostOverUtilizedAfterAllocation;
    }

    /**
     * Find host for vm.
     *
     * @param vm the vm
     * @return the power host
     */
    @Override
    public PowerContainerHost findHostForVm(ContainerVm vm) {
        System.out.println("VMallocation-4-PowerContainerVmAllocationPolicyMigrationAbstractHostSelection.findHostForVm");
        Set<ContainerHost> excludedHosts = new HashSet<>();
        if (vm.getHost() != null) {
            excludedHosts.add(vm.getHost());
        }
        //调用同类
        PowerContainerHost hostForVm = findHostForVm(vm, excludedHosts);
        excludedHosts.clear();

        return hostForVm;
    }

    /**
     * Extract host list from migration map.
     *
     * @param migrationMap the migration map
     * @return the list
     */
    protected List<PowerContainerHost> extractHostListFromMigrationMap(List<Map<String, Object>> migrationMap) {
        List<PowerContainerHost> hosts = new LinkedList<PowerContainerHost>();
        for (Map<String, Object> map : migrationMap) {
            hosts.add((PowerContainerHost) map.get("host"));
        }

        return hosts;
    }

    /**
     * Gets the new vm placement.
     *
     * @param vmsToMigrate  the vms to migrate
     * @param excludedHosts the excluded hosts
     * @return the new vm placement
     */
    protected List<Map<String, Object>> getNewVmPlacement(
            List<? extends ContainerVm> vmsToMigrate,
            Set<? extends ContainerHost> excludedHosts) {
        System.out.println("VMmigration-9-PowerContainerVmAllocationPolicyMigrationAbstract.getNewVmPlacement");
        List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
        //System.out.println("VMmigration-9-1-调用VMmigration-10-根据虚拟机CPU利用率降序排列-PowerContainerVmList.sortByCpuUtilization");
        PowerContainerVmList.sortByCpuUtilization(vmsToMigrate);
        for (ContainerVm vm : vmsToMigrate) {
            //System.out.println("VMmigration-9-2-未使用-调用未知findHostForVm");
            PowerContainerHost allocatedHost = findHostForVm(vm, excludedHosts);
            if (allocatedHost != null) {
                allocatedHost.containerVmCreate(vm);
                Log.printConcatLine("VM #", vm.getId(), " allocated to host #", allocatedHost.getId());

                Map<String, Object> migrate = new HashMap<String, Object>();
                migrate.put("vm", vm);
                migrate.put("host", allocatedHost);
                migrationMap.add(migrate);
            }
        }
        return migrationMap;
    }

    /**
     * Gets the new vm placement from under utilized host.
     *
     * @param vmsToMigrate  the vms to migrate
     * @param excludedHosts the excluded hosts
     * @return the new vm placement from under utilized host
     */
    protected List<Map<String, Object>> getNewVmPlacementFromUnderUtilizedHost(
            List<? extends ContainerVm> vmsToMigrate,
            Set<? extends ContainerHost> excludedHosts) {
        System.out.println("VMmigration-7-2-10-PowerContainerVmAllocationPolicyMigrationAbstract.getNewVmPlacementFromUnderUtilizedHost");
        List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
        //System.out.println("VMmigration-7-2-11-调用VMmigration-10-根据虚拟机CPU利用率降序排列-sortByCpuUtilization");
        PowerContainerVmList.sortByCpuUtilization(vmsToMigrate);
        for (ContainerVm vm : vmsToMigrate) {
            //System.out.println("VMmigration-7-2-12-调用VMallocation-5-PowerContainerVmAllocationPolicyMigrationAbstractHostSelection.findHostForVm");
            /**
             * @see org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled.PowerContainerVmAllocationPolicyMigrationAbstractHostSelection#findHostForVm(ContainerVm, Set)
             */
            PowerContainerHost allocatedHost = findHostForVm(vm, excludedHosts);
            if (allocatedHost != null) {
                allocatedHost.containerVmCreate(vm);
                Log.printConcatLine("VM #", vm.getId(), " allocated to host #", allocatedHost.getId());

                Map<String, Object> migrate = new HashMap<String, Object>();
                migrate.put("vm", vm);
                migrate.put("host", allocatedHost);
                migrationMap.add(migrate);
            } else {
                Log.printLine("Not all VMs can be reallocated from the host, reallocation cancelled");
                for (Map<String, Object> map : migrationMap) {
                    ((ContainerHost) map.get("host")).containerVmDestroy((ContainerVm) map.get("vm"));
                }
                migrationMap.clear();
                break;
            }
        }
        return migrationMap;
    }


    /**
     * Gets the vms to migrate from under utilized host.
     *vm读取顺序为顺序读取,
     * @param host the host
     * @return the vms to migrate from under utilized host
     */
    protected List<? extends ContainerVm> getVmsToMigrateFromUnderUtilizedHost(PowerContainerHost host) {
        System.out.println("VMmigration-7-2-8-从底负荷主机找可以迁移的虚拟机-PowerContainerVmAllocationPolicyMigrationAbstract.getVmsToMigrateFromUnderUtilizedHost");
        List<ContainerVm> vmsToMigrate = new LinkedList<ContainerVm>();
        for (ContainerVm vm : host.getVmList()) {
            if (!vm.isInMigration()) {
                //迁移出低负荷主机里全部的非正在迁移的虚拟机
                vmsToMigrate.add(vm);
            }
        }
        return vmsToMigrate;
    }

    /**
     * Gets the switched off host.
     *
     * @return the switched off host
     */
    protected List<PowerContainerHost> getSwitchedOffHosts() {
        System.out.println("VMmigration-7-2-2-PowerContainerVmAllocationPolicyMigrationAbstract.getSwitchedOffHosts");
        List<PowerContainerHost> switchedOffHosts = new LinkedList<PowerContainerHost>();
        for (PowerContainerHost host : this.<PowerContainerHost>getContainerHostList()) {
            if (host.getUtilizationOfCpu() == 0) {
                switchedOffHosts.add(host);
            }
        }
        return switchedOffHosts;
    }






    /**
     * Checks whether all vms are in migration.
     *
     * @param host the host
     * @return true, if successful
     */
    protected boolean areAllVmsMigratingOutOrAnyVmMigratingIn(PowerContainerHost host) {
        for (PowerContainerVm vm : host.<PowerContainerVm>getVmList()) {
            if (!vm.isInMigration()) {
                return false;
            }
            if (host.getVmsMigratingIn().contains(vm)) {
                return true;
            }
        }
        return true;
    }

    /**
     * Checks whether all vms are in migration.
     *
     * @param host the host
     * @return true, if successful
     */
    protected boolean areAllContainersMigratingOutOrAnyContainersMigratingIn(PowerContainerHost host) {
        for (PowerContainerVm vm : host.<PowerContainerVm>getVmList()) {
            if(vm.getContainersMigratingIn().size() != 0){
                return true;
            }
            for(Container container:vm.getContainerList()){
                if(!container.isInMigration()){
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if is host over utilized.
     *
     * @param host the host
     * @return true, if is host over utilized
     */
    protected abstract boolean isHostOverUtilized(PowerContainerHost host);

    /**
     * Checks if is host over utilized.
     *
     * @param host the host
     * @return true, if is host over utilized
     */
    protected abstract boolean isHostUnderUtilized(PowerContainerHost host);


    /**
     * Adds the history value.
     *
     * @param host   the host
     * @param metric the metric
     */
    protected void addHistoryEntry(ContainerHostDynamicWorkload host, double metric) {
        int hostId = host.getId();
        if (!getTimeHistory().containsKey(hostId)) {
            getTimeHistory().put(hostId, new LinkedList<Double>());
        }
        if (!getUtilizationHistory().containsKey(hostId)) {
            getUtilizationHistory().put(hostId, new LinkedList<Double>());
        }
        if (!getMetricHistory().containsKey(hostId)) {
            getMetricHistory().put(hostId, new LinkedList<Double>());
        }
        if (!getTimeHistory().get(hostId).contains(CloudSim.clock())) {
            getTimeHistory().get(hostId).add(CloudSim.clock());
            getUtilizationHistory().get(hostId).add(host.getUtilizationOfCpu());
            getMetricHistory().get(hostId).add(metric);
        }
    }

    /**
     * Save allocation.
     * 原来
     */
    protected void saveAllocation() {
        getSavedAllocation().clear();
        for (ContainerHost host : getContainerHostList()) {
            for (ContainerVm vm : host.getVmList()) {
                if (host.getVmsMigratingIn().contains(vm)) {
                    continue;
                }
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("host", host);
                map.put("vm", vm);
                getSavedAllocation().add(map);
            }
        }
    }

    /**
     * Restore allocation.
     * 原来
     */
    protected void restoreAllocation() {
        for (ContainerHost host : getContainerHostList()) {
            host.containerVmDestroyAll();
            host.reallocateMigratingInContainerVms();
        }
        for (Map<String, Object> map : getSavedAllocation()) {
            ContainerVm vm = (ContainerVm) map.get("vm");
            PowerContainerHost host = (PowerContainerHost) map.get("host");
            if (!host.containerVmCreate(vm)) {
                Log.printConcatLine("Couldn't restore VM #", vm.getId(), " on host #", host.getId());
                System.exit(0);
            }
            getVmTable().put(vm.getUid(), host);
        }
    }

    /**
     * Gets the power after allocation.
     *
     * @param host the host
     * @param vm   the vm
     * @return the power after allocation
     */
    protected double getPowerAfterAllocation(PowerContainerHost host, ContainerVm vm) {
        double power = 0;
        try {
            power = host.getPowerModel().getPower(getMaxUtilizationAfterAllocation(host, vm));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return power;
    }

    /**
     * Gets the power after allocation. We assume that load is balanced between PEs. The only
     * restriction is: VM's max MIPS < PE's MIPS
     *
     * @param host the host
     * @param vm   the vm
     * @return the power after allocation
     */
    protected double getMaxUtilizationAfterAllocation(PowerContainerHost host, ContainerVm vm) {
        double requestedTotalMips = vm.getCurrentRequestedTotalMips();
        double hostUtilizationMips = getUtilizationOfCpuMips(host);
        double hostPotentialUtilizationMips = hostUtilizationMips + requestedTotalMips;
        double pePotentialUtilization = hostPotentialUtilizationMips / host.getTotalMips();
        return pePotentialUtilization;
    }

    /**
     * Gets the utilization of the CPU in MIPS for the current potentially allocated VMs.
     *
     * @param host the host
     * @return the utilization of the CPU in MIPS
     */
    protected double getUtilizationOfCpuMips(PowerContainerHost host) {
        //System.out.println("VMallocation-5A-PowerContainerVmAllocationPolicyMigrationAbstract.getUtilizationOfCpuMips");
        double hostUtilizationMips = 0;
        for (ContainerVm vm2 : host.getVmList()) {
            if (host.getVmsMigratingIn().contains(vm2)) {
                System.out.println("未使用-host.getTotalAllocatedMipsForContainerVm(vm2) * 0.9 / 0.1");
                // calculate additional potential CPU usage of a migrating in VM
                hostUtilizationMips += host.getTotalAllocatedMipsForContainerVm(vm2) * 0.9 / 0.1;
            }
            hostUtilizationMips += host.getTotalAllocatedMipsForContainerVm(vm2);
        }
        //System.out.println("hostUtilizationOfCpuMips:"+hostUtilizationMips);
        return hostUtilizationMips;
    }

    /**
     * Gets the saved allocation.
     *
     * @return the saved allocation
     */
    protected List<Map<String, Object>> getSavedAllocation() {
        return savedAllocation;
    }

    /**
     * Sets the vm selection policy.
     *
     * @param vmSelectionPolicy the new vm selection policy
     */
    protected void setVmSelectionPolicy(PowerContainerVmSelectionPolicy vmSelectionPolicy) {
        this.vmSelectionPolicy = vmSelectionPolicy;
    }

    /**
     * Gets the vm selection policy.
     *
     * @return the vm selection policy
     */
    protected PowerContainerVmSelectionPolicy getVmSelectionPolicy() {
        return vmSelectionPolicy;
    }

    /**
     * Gets the utilization history.
     *
     * @return the utilization history
     */
    public Map<Integer, List<Double>> getUtilizationHistory() {
        return utilizationHistory;
    }

    /**
     * Gets the metric history.
     *
     * @return the metric history
     */
    public Map<Integer, List<Double>> getMetricHistory() {
        return metricHistory;
    }

    /**
     * Gets the time history.
     *
     * @return the time history
     */
    public Map<Integer, List<Double>> getTimeHistory() {
        return timeHistory;
    }

    /**
     * Gets the execution time history vm selection.
     *
     * @return the execution time history vm selection
     */
    public List<Double> getExecutionTimeHistoryVmSelection() {
        return executionTimeHistoryVmSelection;
    }

    /**
     * Gets the execution time history host selection.
     *
     * @return the execution time history host selection
     */
    public List<Double> getExecutionTimeHistoryHostSelection() {
        return executionTimeHistoryHostSelection;
    }

    /**
     * Gets the execution time history vm reallocation.
     *
     * @return the execution time history vm reallocation
     */
    public List<Double> getExecutionTimeHistoryVmReallocation() {
        return executionTimeHistoryVmReallocation;
    }

    /**
     * Gets the execution time history total.
     *
     * @return the execution time history total
     */
    public List<Double> getExecutionTimeHistoryTotal() {
        return executionTimeHistoryTotal;
    }







//    public abstract List<? extends Container> getContainersToMigrateFromHosts(List<PowerContainerHostUtilizationHistory> overUtilizedHosts);
//protected Collection<? extends Map<String, Object>> getContainerMigrationMapFromUnderUtilizedHosts(
//        List<PowerContainerHostUtilizationHistory> overUtilizedHosts, List<Map<String, Object>> previouseMap) {
//
//    List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
//    List<PowerContainerHost> switchedOffHosts = getSwitchedOffHosts();
//
//    // over-utilized hosts + hosts that are selected to migrate VMs to from over-utilized hosts
//    Set<PowerContainerHost> excludedHostsForFindingUnderUtilizedHost = new HashSet<>();
//    excludedHostsForFindingUnderUtilizedHost.addAll(overUtilizedHosts);
//    excludedHostsForFindingUnderUtilizedHost.addAll(switchedOffHosts);
//    excludedHostsForFindingUnderUtilizedHost.addAll(extractHostListFromMigrationMap(previouseMap));
//
//    // over-utilized + under-utilized hosts
//    Set<PowerContainerHost> excludedHostsForFindingNewContainerPlacement = new HashSet<PowerContainerHost>();
//    excludedHostsForFindingNewContainerPlacement.addAll(overUtilizedHosts);
//    excludedHostsForFindingNewContainerPlacement.addAll(switchedOffHosts);
//
//    int numberOfHosts = getContainerHostList().size();
//
//    while (true) {
//        if (numberOfHosts == excludedHostsForFindingUnderUtilizedHost.size()) {
//            break;
//        }
//
//        PowerContainerHost underUtilizedHost = getUnderUtilizedHost(excludedHostsForFindingUnderUtilizedHost);
//        if (underUtilizedHost == null) {
//            break;
//        }
//
//        Log.printConcatLine("Under-utilized host: host #", underUtilizedHost.getId(), "\n");
//
//        excludedHostsForFindingUnderUtilizedHost.add(underUtilizedHost);
//        excludedHostsForFindingNewContainerPlacement.add(underUtilizedHost);
//
//        List<? extends ContainerVm> vmsToMigrateFromUnderUtilizedHost = getVmsToMigrateFromUnderUtilizedHost(underUtilizedHost);
//        if (vmsToMigrateFromUnderUtilizedHost.isEmpty()) {
//            continue;
//        }
//
//        Log.print("Reallocation of Containers from the under-utilized host: ");
//        if (!Log.isDisabled()) {
//            for (ContainerVm vm : vmsToMigrateFromUnderUtilizedHost) {
//                Log.print(vm.getId() + " ");
//            }
//        }
//        Log.printLine();
//
//        List<Map<String, Object>> newVmPlacement = getNewVmPlacementFromUnderUtilizedHost(
//                vmsToMigrateFromUnderUtilizedHost,
//                excludedHostsForFindingNewContainerPlacement);
//        //Sareh
//        if (newVmPlacement == null) {
////                Add the host to the placement founder option
//            excludedHostsForFindingNewContainerPlacement.remove(underUtilizedHost);
//
//        }
//
//        excludedHostsForFindingUnderUtilizedHost.addAll(extractHostListFromMigrationMap(newVmPlacement));
//        //The migration mapp does not have a value for container since the whole vm would be migrated.
//        migrationMap.addAll(newVmPlacement);
//        Log.printLine();
//    }
//
//    excludedHostsForFindingUnderUtilizedHost.clear();
//    excludedHostsForFindingNewContainerPlacement.clear();
//    return migrationMap;
//}
//
//    private List<? extends Container> getContainersToMigrateFromHosts(List<PowerContainerHostUtilizationHistory> overUtilizedHosts) {
//        List<Container> containersToMigrate = new LinkedList<>();
//        for (PowerContainerHostUtilizationHistory host : overUtilizedHosts) {
//            while (true) {
//                Container container = getContainerSelectionPolicy().getContainerToMigrate(host);
//                if (container == null) {
//                    break;
//                }
//                containersToMigrate.add(container);
//                container.getVm().containerDestroy(container);
//                if (!isHostOverUtilized(host)) {
//                    break;
//                }
//            }
//        }
//        return containersToMigrate;
//    }
//
//
//    private List<Map<String, Object>> getNewContainerPlacement(List<? extends Container> containersToMigrate, Set<? extends ContainerHost> excludedHosts) {
//        System.out.println("ContainerMigration-x-PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded.getNewContainerPlacement");
//        List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
//
//        PowerContainerList.sortByCpuUtilization(containersToMigrate);
//        for (Container container : containersToMigrate) {
//            Map<String, Object> allocationMap = findHostForContainer(container, excludedHosts, false);
//
//            if (allocationMap.get("host") != null && allocationMap.get("vm") != null) {
//                ContainerVm vm = (ContainerVm) allocationMap.get("vm");
//                Log.printConcatLine("Container #", container.getId(), " allocated to host #", ((PowerContainerHost) allocationMap.get("host")).getId(), "The VM ID is #", vm.getId());
//                Map<String, Object> migrate = new HashMap<String, Object>();
//                migrate.put("container", container);
//                migrate.put("vm", vm);
//                migrate.put("host", (PowerContainerHost) allocationMap.get("host"));
//                migrationMap.add(migrate);
//            } else {
//                Map<String, Object> migrate = new HashMap<String, Object>();
//                migrate.put("NewVmRequired", container);
//                migrationMap.add(migrate);
//
//            }
//
//        }
//        containersToMigrate.clear();
//        return migrationMap;
//    }
//
//    private List<Map<String, Object>> getPlacementForLeftContainers(List<? extends Container> containersToMigrate, Set<? extends ContainerHost> excludedHostsList) {
//        List<Map<String, Object>> newMigrationMap = new LinkedList<Map<String, Object>>();
//
//        if (containersToMigrate.size() == 0) {
//            return newMigrationMap;
//        }
//        HashSet<ContainerHost> excludedHostsforOverUtilized = new HashSet<>();
//        excludedHostsforOverUtilized.addAll(getSwitchedOffHosts());
//        excludedHostsforOverUtilized.addAll(excludedHostsList);
//        List<Map<String, Object>> migrationMap = getNewContainerPlacement(containersToMigrate, excludedHostsforOverUtilized);
//        if (migrationMap.size() == 0) {
//            return migrationMap;
//        }
////        List<Container> containerList = getExtraContainers(migrationMap);
//        List<Container> containerList = new ArrayList<>();
//        for (Map<String, Object> map : migrationMap) {
//            if (map.containsKey("NewVmRequired")) {
//                containerList.add((Container) map.get("NewVmRequired"));
//
//            } else {
//                newMigrationMap.add(map);
//            }
//
//        }
//        if (containerList.size() == 0) {
//            return newMigrationMap;
//        }
//
//        List<ContainerHost> underUtilizedHostList = getUnderUtilizedHostList(excludedHostsList);
//
//        List<Map<String, Object>> migrationMapUnderUtilized = findMapInUnderUtilizedHosts(underUtilizedHostList,containerList);
//        newMigrationMap.addAll(migrationMapUnderUtilized);
//        containerList.removeAll(getAssignedContainers(migrationMapUnderUtilized));
//        if(containerList.size()!= 0){
//            List<Map<String, Object>> migrationMapSwitchedOff= findMapInSwitchedOffHosts(containerList);
//            newMigrationMap.addAll(migrationMapSwitchedOff);
//
//        }
//// Now Check if there are any containers left without VMs.
//        //firsthost chosen
//        return newMigrationMap;
//    }
//
//    protected List<Map<String, Object>> findMapInUnderUtilizedHosts(List<ContainerHost> underUtilizedHostList, List<Container> containerList){
//        List<Map<String, Object>> newMigrationMap = new ArrayList<>();
//        //        Create new Vms on underUtilized hosts;
//        List<Map<String, Object>> createdVmMap = new ArrayList<>();
//        if (underUtilizedHostList.size() != 0) {
//            for (ContainerHost host : underUtilizedHostList) {
////                   We try to create the largest Vm possible
//                List<ContainerVm> VmList = createVms(host, true);
//                if(VmList.size() != 0){
//                    for(ContainerVm vm:VmList){
//                        Map<String, Object> map = new HashMap<>();
//                        map.put("host",host);
//                        map.put("vm",vm);
//                        createdVmMap.add(map);
//
//                    }}
//            }
//            if(createdVmMap.size() ==0){
//
//                return newMigrationMap;
//
//            }
//
//            // if there are any new Vms on the underUtilized Hosts we assign the containers to them first!
//            // Sort the underUtilized host by the utilization, so that we first assign vms to the more utilized ones
//            for (Container container : containerList) {
//                Map<String, Object> allocationMap = findAvailableHostForContainer(container, createdVmMap);
//                if (allocationMap.get("host") != null && allocationMap.get("vm") != null) {
//                    ContainerVm vm = (ContainerVm) allocationMap.get("vm");
//                    Log.printConcatLine("Container #", container.getId(), " allocated to host #", ((PowerContainerHost) allocationMap.get("host")).getId(), "The VM ID is #", vm.getId());
//                    Map<String, Object> migrate = new HashMap<String, Object>();
////                    vm.setInWaiting(true);
//                    migrate.put("NewEventRequired", container);
//                    migrate.put("container", container);
//                    migrate.put("vm", vm);
//                    migrate.put("host", (PowerContainerHost) allocationMap.get("host"));
//                    newMigrationMap.add(migrate);
//
//                }
//            }
//        }
//
//        return newMigrationMap;
//    }
//
//    protected List<Container> getAssignedContainers(List<Map<String, Object>> migrationMap){
//        List<Container> assignedContainers = new ArrayList<>();
//        for(Map<String, Object> map:migrationMap){
//            if(map.containsKey("container")){
//                assignedContainers.add((Container) map.get("container"));
//            }
//        }
//
//        return assignedContainers;
//    }
//    protected ContainerVm createVMinHost(ContainerHost host, boolean vmStatus) {
//
//        for (int i=0; i<numberOfVmTypes; i++) {
//            ContainerVm vm = getNewVm(i);
//            if (getUtilizationOfCpuMips((PowerContainerHost) host) != 0 && isHostOverUtilizedAfterAllocation((PowerContainerHost) host, vm)) {
//                continue;
//            }
//
//            if(allocateHostForVm(vm, host)){
//                Log.printLine("The vm ID #" + vm.getId() + "will be created ");
//                vm.setInWaiting(vmStatus);
//                return vm;
//            }
//        }
//
//        return null;
//    }
//
//    protected List<Map<String, Object>> findMapInSwitchedOffHosts(List<Container> containerList) {
//        Log.print(String.format(" %s :  Find Placement in the switched of hosts", CloudSim.clock()));
//        List<PowerContainerHost> switchedOffHostsList = getSwitchedOffHosts();
//        List<Map<String, Object>> newMigrationMap = new ArrayList<>();
//
//        if (containerList.size() == 0) {
//
//            return newMigrationMap;
//        }
//
//        ContainerHost previouseHost = null;
//        ContainerVm previouseVm = null;
//        while (containerList.size() != 0) {
//            if (switchedOffHostsList.size() == 0) {
//
//                Log.print("There is no hosts to create VMs");
//                break;
//            }
//            List<Container> assignedContainer = new ArrayList<>();
//            //choose a random host
//            if (previouseHost == null && previouseVm == null) {
//                if(switchedOffHostsList.size() ==0 ){
//                    return newMigrationMap;
//                }
//                int hostIndex = new RandomGen().getNum(switchedOffHostsList.size());
//                previouseHost = switchedOffHostsList.get(hostIndex);
//                switchedOffHostsList.remove(previouseHost);
//                previouseVm = createVMinHost(previouseHost, true);
//                previouseHost.containerVmCreate(previouseVm);
//
//                for (Container container : containerList) {
//                    if (previouseVm.isSuitableForContainer(container)) {
//                        previouseVm.containerCreate(container);
//                        assignedContainer.add(container);
//                        Map<String, Object> migrate = new HashMap<String, Object>();
////                        previouseVm.setInWaiting(true);
//                        migrate.put("NewEventRequired", container);
//                        migrate.put("container", container);
//                        migrate.put("vm", previouseVm);
//                        migrate.put("host", previouseHost);
//                        newMigrationMap.add(migrate);
//                    } else {
//
//                        previouseVm = createVMinHost(previouseHost, true);
//                        if (previouseVm == null) {
//                            switchedOffHostsList.remove(previouseHost);
//                            previouseHost = null;
//                            containerList.removeAll(assignedContainer);
//                            break;
//                        }
//                        previouseVm.containerCreate(container);
//                        assignedContainer.add(container);
//                        Map<String, Object> migrate = new HashMap<String, Object>();
////                        previouseVm.setInWaiting(true);
//                        migrate.put("NewEventRequired", container);
//                        migrate.put("container", container);
//                        migrate.put("vm", previouseVm);
//                        migrate.put("host", previouseHost);
//                        newMigrationMap.add(migrate);
//
//                    }
//                }
//
//                containerList.removeAll(assignedContainer);
//            } else {
//
//                for (Container container : containerList) {
//                    if (previouseVm.isSuitableForContainer(container)) {
//                        previouseVm.containerCreate(container);
//                        assignedContainer.add(container);
//                        Map<String, Object> migrate = new HashMap<String, Object>();
////                        previouseVm.setInWaiting(true);
//                        migrate.put("NewEventRequired", container);
//                        migrate.put("container", container);
//                        migrate.put("vm", previouseVm);
//                        migrate.put("host", previouseHost);
//                        newMigrationMap.add(migrate);
//                    } else {
//
//                        previouseVm = createVMinHost(previouseHost, true);
//                        if (previouseVm == null) {
//                            switchedOffHostsList.remove(previouseHost);
//                            previouseHost = null;
//                            containerList.removeAll(assignedContainer);
//                            break;
//                        }
//                        previouseVm.containerCreate(container);
//                        assignedContainer.add(container);
//                        Map<String, Object> migrate = new HashMap<String, Object>();
////                        previouseVm.setInWaiting(true);
//                        migrate.put("NewEventRequired", container);
//                        migrate.put("container", container);
//                        migrate.put("vm", previouseVm);
//                        migrate.put("host", previouseHost);
//                        newMigrationMap.add(migrate);
//
//                    }
//                }
//
//                containerList.removeAll(assignedContainer);
//
//
//            }
//        }
//        return newMigrationMap;
//
//
//    }
//
//    //    This method should be re written!
//    protected Map<String, Object> findAvailableHostForContainer(Container container, List<Map<String, Object>> createdVm){
//
//        double minPower = Double.MAX_VALUE;
//        PowerContainerHost allocatedHost = null;
//        ContainerVm allocatedVm = null;
//        List<ContainerHost> underUtilizedHostList = new ArrayList<>();
//        List<ContainerVm> vmList = new ArrayList<>();
//        for(Map<String, Object> map:createdVm){
//            underUtilizedHostList.add((ContainerHost) map.get("host"));
//
//        }
//        ContainerHostList.sortByCpuUtilization(underUtilizedHostList);
//        for (ContainerHost host1 : underUtilizedHostList) {
//
//            PowerContainerHost host = (PowerContainerHost) host1;
//            for(Map<String, Object> map:createdVm){
//                if((ContainerHost) map.get("host")== host1){
//                    vmList.add((ContainerVm) map.get("vm"));
//                }
//
//            }
//            for (ContainerVm vm : vmList) {
////                if vm is not created no need for checking!
//
//                if (vm.isSuitableForContainer(container)) {
//                    // if vm is overutilized or host would be overutilized after the allocation, this host is not chosen!
//                    if (!isVmOverUtilized(vm)) {
//                        continue;
//                    }
//                    if (getUtilizationOfCpuMips(host) != 0 && isHostOverUtilizedAfterContainerAllocation(host, vm, container)) {
//                        continue;
//                    }
//
//                    try {
//                        double powerAfterAllocation = getPowerAfterContainerAllocation(host, container, vm);
//                        if (powerAfterAllocation != -1) {
//                            double powerDiff = powerAfterAllocation - host.getPower();
//                            if (powerDiff < minPower) {
//                                minPower = powerDiff;
//                                allocatedHost = host;
//                                allocatedVm = vm;
//                            }
//                        }
//                    } catch (Exception e) {
//                        Log.print("Error: Exception in powerDiff algorithm containerAdded");
//                    }
//                }
//            }
//        }
//
//
//        Map<String, Object> map = new HashMap<>();
//        map.put("vm", allocatedVm);
//        map.put("host", allocatedHost);
//
//        return map;
//
//
//    }
//
//    private ContainerVm getNewVm(int vmType) {
//
//        ArrayList<ContainerPe> peList = new ArrayList<ContainerPe>();
////        int vmType = new RandomGen().getNum(ConstantsEx.VM_TYPES);
////            int vmType = i / (int) Math.ceil((double) containerVmsNumber / 4.0D);
////            int vmType = 1;
////        Log.print(vmType);
//        for (int j = 0; j < vmPes[vmType]; ++j) {
//            peList.add(new ContainerPe(j, new CotainerPeProvisionerSimple((double) vmMips[vmType])));
//        }
//        int brokerId = 2;
//        PowerContainerVm vm = new PowerContainerVm(IDs.pollId(ContainerVm.class), brokerId, (double) vmMips[vmType],
//                vmRam[vmType],
//                vmBw, vmSize, "Xen",
//                new ContainerSchedulerTimeSharedOverSubscription(peList),
//                new ContainerRamProvisionerSimple(vmRam[vmType]),
//                new ContainerBwProvisionerSimple(vmBw), peList, 300);
//        return vm;
//
//    }
//
//
//    /**
//     * Gets the under utilized host.
//     *
//     * @param excludedHosts the excluded hosts
//     * @return the under utilized host
//     */
//    protected List<ContainerHost> getUnderUtilizedHostList(Set<? extends ContainerHost> excludedHosts) {
//        System.out.println("未使用-getUnderUtilizedHostList-1");
//        List<ContainerHost> underUtilizedHostList = new ArrayList<>();
//        double minUtilization = 1;
//        for (PowerContainerHost host : this.<PowerContainerHost>getContainerHostList()) {
//            if (excludedHosts.contains(host)) {
//                continue;
//            }
//            double utilization = host.getUtilizationOfCpu();
//            if (utilization > 0 && utilization < minUtilization
//                    && !areAllVmsMigratingOutOrAnyVmMigratingIn(host) && !areAllContainersMigratingOutOrAnyContainersMigratingIn(host)) {
//                minUtilization = utilization;
//                underUtilizedHostList.add(host);
//            }
//        }
//        return underUtilizedHostList;
//    }
//
//    public Map<String, Object> findHostForContainer(Container container, Set<? extends ContainerHost> excludedHosts, boolean checkForVM) {
//        System.out.println("容器在虚拟机之间迁移ContainerMigration-x-PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded.findHostForContainer");
//        double minPower = Double.MAX_VALUE;
//        PowerContainerHost allocatedHost = null;
//        ContainerVm allocatedVm = null;
//
//        for (PowerContainerHost host : this.<PowerContainerHost>getContainerHostList()) {
//            if (excludedHosts.contains(host)) {
//                continue;
//            }
//            for (ContainerVm vm : host.getVmList()) {
//                if (checkForVM) {
//                    if (vm.isInWaiting()) {
//                        continue;
//                    }
//                }
//                if (vm.isSuitableForContainer(container)) {
//                    // if vm is overutilized or host would be overutilized after the allocation, this host is not chosen!
//                    if (!isVmOverUtilized(vm)) {
//                        continue;
//                    }
//                    if (getUtilizationOfCpuMips(host) != 0 && isHostOverUtilizedAfterContainerAllocation(host, vm, container)) {
//                        continue;
//                    }
//
//                    try {
//                        double powerAfterAllocation = getPowerAfterContainerAllocation(host, container, vm);
//                        if (powerAfterAllocation != -1) {
//                            double powerDiff = powerAfterAllocation - host.getPower();
//                            if (powerDiff < minPower) {
//                                minPower = powerDiff;
//                                allocatedHost = host;
//                                allocatedVm = vm;
//                            }
//                        }
//                    } catch (Exception e) {
//                    }
//                }
//            }
//        }
//        Map<String, Object> map = new HashMap<>();
//        map.put("vm", allocatedVm);
//        map.put("host", allocatedHost);
//
//        return map;
//    }
//
//    protected boolean isVmOverUtilized(ContainerVm vm) {
//        boolean isOverUtilized = true;
//        double util = 0;
////        Log.printConcatLine("Checking if the vm is over utilized or not!");
//        for (Container container : vm.getContainerList()) {
//            util += container.getTotalUtilizationOfCpuMips(CloudSim.clock());
//        }
//        if (util > vm.getHost().getTotalMips() / vm.getHost().getNumberOfPes() * vm.getNumberOfPes()) {
//            return false;
//        }
//
//
//        return isOverUtilized;
//    }
//
//    /**
//     * Gets the power after allocation.
//     *
//     * @param host      the host
//     * @param container the vm
//     * @return the power after allocation
//     */
//    protected double getPowerAfterContainerAllocation(PowerContainerHost host, Container container, ContainerVm vm) {
//        double power = 0;
//        try {
//            power = host.getPowerModel().getPower(getMaxUtilizationAfterContainerAllocation(host, container, vm));
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.exit(0);
//        }
//        return power;
//    }
//
//    /**
//     * Gets the power after allocation. We assume that load is balanced between PEs. The only
//     * restriction is: VM's max MIPS < PE's MIPS
//     *
//     * @param host      the host
//     * @param container the vm
//     * @return the power after allocation
//     */
//    protected double getMaxUtilizationAfterContainerAllocation(PowerContainerHost host, Container container, ContainerVm containerVm) {
//        double requestedTotalMips = container.getCurrentRequestedTotalMips();
//        if (requestedTotalMips > containerVm.getMips()) {
//            requestedTotalMips = containerVm.getMips();
//        }
//        double hostUtilizationMips = getUtilizationOfCpuMips(host);
//        double hostPotentialUtilizationMips = hostUtilizationMips + requestedTotalMips;
//        double pePotentialUtilization = hostPotentialUtilizationMips / host.getTotalMips();
//        return pePotentialUtilization;
//    }
//
//    /**
//     * Gets the utilization of the CPU in MIPS for the current potentially allocated VMs.
//     *
//     * @param containerVm the host
//     * @return the utilization of the CPU in MIPS
//     */
//    protected double getUtilizationOfCpuMipsofVm(ContainerVm containerVm) {
//        double vmUtilizationMips = 0;
//        for (Container container : containerVm.getContainerList()) {
//
//            vmUtilizationMips += containerVm.getTotalAllocatedMipsForContainer(container);
//        }
//        return vmUtilizationMips;
//    }
//
//    /**
//     * Checks if is host over utilized after allocation.
//     *
//     * @param host      the host
//     * @param container the vm
//     * @return true, if is host over utilized after allocation
//     */
//    protected boolean isHostOverUtilizedAfterContainerAllocation(PowerContainerHost host, ContainerVm vm, Container container) {
//        boolean isHostOverUtilizedAfterAllocation = true;
//        if (vm.containerCreate(container)) {
//            isHostOverUtilizedAfterAllocation = isHostOverUtilized(host);
//            vm.containerDestroy(container);
//        }
//        return isHostOverUtilizedAfterAllocation;
//    }
//
//
//    /**
//     * Save allocation.
//     */
//
//    protected void saveAllocation() {
//        getSavedAllocation().clear();
//        for (ContainerHost host : getContainerHostList()) {
//            for (ContainerVm vm : host.getVmList()) {
//                if (host.getVmsMigratingIn().contains(vm)) {
//                    continue;
//                }
//                for (Container container : vm.getContainerList()) {
//                    if (vm.getContainersMigratingIn().contains(container)) {
//                        continue;
//                    }
//                    Map<String, Object> map = new HashMap<String, Object>();
//                    map.put("host", host);
//                    map.put("vm", vm);
//                    map.put("container", container);
//                    getSavedAllocation().add(map);
//                }
//            }
//        }
//        Log.printLine(String.format("The length of the saved map is ....%d", getSavedAllocation().size()));
//
//    }
//
//
//    /**
//     * Restore allocation.
//     */
//    protected void restoreAllocation() {
//        for (ContainerHost host : getContainerHostList()) {
//            for (ContainerVm vm : host.getVmList()) {
//                vm.containerDestroyAll();
//                vm.reallocateMigratingInContainers();
//            }
//
//            host.containerVmDestroyAll();
//            host.reallocateMigratingInContainerVms();
//        }
//        for (Map<String, Object> map : getSavedAllocation()) {
//            PowerContainerVm vm = (PowerContainerVm) map.get("vm");
//
//            PowerContainerHost host = (PowerContainerHost) map.get("host");
//            if (!host.getVmList().contains(vm)) {
//                if (!host.containerVmCreate(vm)) {
//                    Log.printConcatLine("Couldn't restore VM #", vm.getId(), " on host #", host.getId());
//                    System.exit(0);
//                }
//
//                getVmTable().put(vm.getUid(), host);
//            }
////            vm.containerDestroyAll();
////            vm.reallocateMigratingInContainers();
//        }
////        List<ContainerVm > restoredVms = new ArrayList<>();
//        for (Map<String, Object> map : getSavedAllocation()) {
//            PowerContainerVm vm = (PowerContainerVm) map.get("vm");
//            if (map.get("container") != null && map.containsKey("container")) {
//                Container container = (Container) map.get("container");
////                Log.print(container);
//
//                if (!vm.getContainerList().contains(container)) {
//                    if (!vm.containerCreate(container)) {
//                        Log.printConcatLine("Couldn't restore Container #", container.getId(), " on vm #", vm.getId());
//                        System.exit(0);
//                    }
//                } else {
//
//                    Log.print("The Container is in the VM already");
//                }
//
//                if (container.getVm() == null) {
//                    Log.print("The Vm is null");
//
//                }
//                ((PowerContainerAllocationPolicy) getDatacenter().getContainerAllocationPolicy()).
//                        getContainerTable().put(container.getUid(), vm);
////            container.setVm(vm);
//
//            }
//        }
//
//
//    }
//
//    protected List<ContainerVm> createVms(ContainerHost host, boolean vmStatus) {
//        List<ContainerVm> vmList = new ArrayList<>();
//        while (true) {
//            ContainerVm vm = createVMinHost(host, vmStatus);
//            if (vm == null) {
//                break;
//            }
//            vmList.add(vm);
//        }
//        return vmList;
//
//
//    }
//
//    public ContainerDatacenter getDatacenter() {
//        return  datacenter;
//    }
//
//    public void setDatacenter(ContainerDatacenter datacenter) {
//        this.datacenter = datacenter;
//    }
////
////    public PowerContainerSelectionPolicy getContainerSelectionPolicy() {
////        return containerSelectionPolicy;
////    }
////
////    public void setContainerSelectionPolicy(PowerContainerSelectionPolicy containerSelectionPolicy) {
////        this.containerSelectionPolicy = containerSelectionPolicy;
////    }

}