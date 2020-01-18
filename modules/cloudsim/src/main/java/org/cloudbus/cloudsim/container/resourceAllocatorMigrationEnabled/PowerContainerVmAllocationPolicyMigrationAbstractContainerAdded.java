package org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerBwProvisionerSimple;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerPe;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerRamProvisionerSimple;
import org.cloudbus.cloudsim.container.containerProvisioners.CotainerPeProvisionerSimple;
import org.cloudbus.cloudsim.container.containerSelectionPolicies.PowerContainerSelectionPolicy;
import org.cloudbus.cloudsim.container.core.*;
import org.cloudbus.cloudsim.container.lists.*;
import org.cloudbus.cloudsim.container.resourceAllocators.PowerContainerAllocationPolicy;
import org.cloudbus.cloudsim.container.schedulers.ContainerSchedulerTimeSharedOverSubscription;
import org.cloudbus.cloudsim.container.utils.IDs;
import org.cloudbus.cloudsim.container.utils.RandomGen;
import org.cloudbus.cloudsim.container.vmSelectionPolicies.PowerContainerVmSelectionPolicy;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.util.ExecutionTimeMeasurer;


import org.cloudbus.cloudsim.container.core.ContainerHostList;

import java.util.*;


/**
 * Created by sareh on 30/07/15.
 */

public abstract class PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded extends PowerContainerVmAllocationPolicyMigrationAbstract {

    private ContainerDatacenter datacenter;
    /**
     * The container selection policy.
     */
    private PowerContainerSelectionPolicy containerSelectionPolicy;
    protected int numberOfVmTypes;
    protected int[] vmPes;
    protected float[] vmRam;
    protected long vmBw;
    protected long vmSize;
    protected double[] vmMips;

    private  List<Container> containerListA1=new ArrayList<Container>();
    private  List<Container> containerListA2=new ArrayList<Container>();
    private  List <List<Integer>> resultA1 = new ArrayList<List<Integer>>();
    private  List<Container> containerListB1=new ArrayList<Container>();
    private  List<Container> containerListB2=new ArrayList<Container>();
    List<List<Integer>> resultB1 = new ArrayList<List<Integer>>();


    public PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded(List<? extends ContainerHost> hostList,
                                                                           PowerContainerVmSelectionPolicy vmSelectionPolicy, PowerContainerSelectionPolicy containerSelectionPolicy,
                                                                           int numberOfVmTypes, int[] vmPes, float[] vmRam, long vmBw, long vmSize, double[] vmMips) {
        super(hostList, vmSelectionPolicy);
//        setDatacenter(datacenter);
        setContainerSelectionPolicy(containerSelectionPolicy);

        this.numberOfVmTypes = numberOfVmTypes;
        this.vmPes = vmPes;
        this.vmRam = vmRam;
        this.vmBw = vmBw;
        this.vmSize = vmSize;
        this.vmMips = vmMips;
    }

    @Override

    public List<Map<String, Object>> optimizeAllocation(List<? extends ContainerVm> vmList) {
        System.out.println("ContainerMigration-1-PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded.optimizeAllocation");
        ExecutionTimeMeasurer.start("optimizeAllocationTotal");

        ExecutionTimeMeasurer.start("optimizeAllocationHostSelection");
        //System.out.println("调用-高阈值VMmigration-7-1-getOverUtilizedHosts");
        /**
         * @see org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled.PowerContainerVmAllocationPolicyMigrationAbstract#getOverUtilizedHosts
         */
        List<PowerContainerHostUtilizationHistory> overUtilizedHosts = getOverUtilizedHosts();
        getExecutionTimeHistoryHostSelection().add(
                ExecutionTimeMeasurer.end("optimizeAllocationHostSelection"));

        printOverUtilizedHosts(overUtilizedHosts);

        saveAllocation();

        ExecutionTimeMeasurer.start("optimizeAllocationContainerSelection");
        //调用本类ContainerMigration-2-PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded.getContainersToMigrateFromHosts
        List<? extends Container> containersToMigrate = getContainersToMigrateFromHosts(overUtilizedHosts);
        getExecutionTimeHistoryVmSelection().add(ExecutionTimeMeasurer.end("optimizeAllocationContainerSelection"));

        Log.printLine("Reallocation of Containers from the over-utilized hosts:");
        ExecutionTimeMeasurer.start("optimizeAllocationVmReallocation");
        //调用本类ContainerMigration-3-PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded.getPlacementForLeftContainers
        List<Map<String, Object>> migrationMap = getPlacementForLeftContainers(containersToMigrate, new HashSet<ContainerHost>(overUtilizedHosts));


        getExecutionTimeHistoryVmReallocation().add(
                ExecutionTimeMeasurer.end("optimizeAllocationVmReallocation"));
        Log.printLine();
        /**
         * @see org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled.PowerContainerVmAllocationPolicyMigrationAbstractContainerHostSelection#getContainerMigrationMapFromUnderUtilizedHosts
         */
        migrationMap.addAll(getContainerMigrationMapFromUnderUtilizedHosts(overUtilizedHosts, migrationMap));

        restoreAllocation();

        getExecutionTimeHistoryTotal().add(ExecutionTimeMeasurer.end("optimizeAllocationTotal"));

        return migrationMap;
    }


    protected Collection<? extends Map<String, Object>> getContainerMigrationMapFromUnderUtilizedHosts(
            List<PowerContainerHostUtilizationHistory> overUtilizedHosts, List<Map<String, Object>> previouseMap) {

        System.out.println("未使用-getContainerMigrationMapFromUnderUtilizedHosts");
        List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
        List<PowerContainerHost> switchedOffHosts = getSwitchedOffHosts();

        // over-utilized hosts + hosts that are selected to migrate VMs to from over-utilized hosts
        Set<PowerContainerHost> excludedHostsForFindingUnderUtilizedHost = new HashSet<>();
        excludedHostsForFindingUnderUtilizedHost.addAll(overUtilizedHosts);
        excludedHostsForFindingUnderUtilizedHost.addAll(switchedOffHosts);
        excludedHostsForFindingUnderUtilizedHost.addAll(extractHostListFromMigrationMap(previouseMap));

        // over-utilized + under-utilized hosts
        Set<PowerContainerHost> excludedHostsForFindingNewContainerPlacement = new HashSet<PowerContainerHost>();
        excludedHostsForFindingNewContainerPlacement.addAll(overUtilizedHosts);
        excludedHostsForFindingNewContainerPlacement.addAll(switchedOffHosts);

        int numberOfHosts = getContainerHostList().size();

        while (true) {
            if (numberOfHosts == excludedHostsForFindingUnderUtilizedHost.size()) {
                break;
            }

            PowerContainerHost underUtilizedHost = getUnderUtilizedHost(excludedHostsForFindingUnderUtilizedHost);
            if (underUtilizedHost == null) {
                break;
            }

            Log.printConcatLine("Under-utilized host: host #", underUtilizedHost.getId(), "\n");

            excludedHostsForFindingUnderUtilizedHost.add(underUtilizedHost);
            excludedHostsForFindingNewContainerPlacement.add(underUtilizedHost);

            List<? extends ContainerVm> vmsToMigrateFromUnderUtilizedHost = getVmsToMigrateFromUnderUtilizedHost(underUtilizedHost);
            if (vmsToMigrateFromUnderUtilizedHost.isEmpty()) {
                continue;
            }

            Log.print("Reallocation of Containers from the under-utilized host: ");
            if (!Log.isDisabled()) {
                for (ContainerVm vm : vmsToMigrateFromUnderUtilizedHost) {
                    Log.print(vm.getId() + " ");
                }
            }
            Log.printLine();

            List<Map<String, Object>> newVmPlacement = getNewVmPlacementFromUnderUtilizedHost(
                    vmsToMigrateFromUnderUtilizedHost,
                    excludedHostsForFindingNewContainerPlacement);
            //Sareh
            if (newVmPlacement == null) {
//                Add the host to the placement founder option
                excludedHostsForFindingNewContainerPlacement.remove(underUtilizedHost);

            }

            excludedHostsForFindingUnderUtilizedHost.addAll(extractHostListFromMigrationMap(newVmPlacement));
            //The migration mapp does not have a value for container since the whole vm would be migrated.
            migrationMap.addAll(newVmPlacement);
            Log.printLine();
        }

        excludedHostsForFindingUnderUtilizedHost.clear();
        excludedHostsForFindingNewContainerPlacement.clear();
        return migrationMap;
    }

    private List<? extends Container> getContainersToMigrateFromHosts(List<PowerContainerHostUtilizationHistory> overUtilizedHosts) {
        //找出container之后，在Vm上destroy该container
        Log.printLine("ContainerMigration-2-PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded.getContainersToMigrateFromHosts");
        List<Container> containersToMigrate = new LinkedList<>();
        for (PowerContainerHostUtilizationHistory host : overUtilizedHosts) {
            while (true) {
                Container container = getContainerSelectionPolicy().getContainerToMigrate(host);
                if (container == null) {
                    break;
                }
                containersToMigrate.add(container);
                container.getVm().containerDestroy(container);
                if (!isHostOverUtilized(host)) {
                    break;
                }
            }
        }
        return containersToMigrate;
    }

    private  void DynamicContainerMigration(int n, List <Container> v, int V,ContainerVm vm) {
        //System.out.println("物体个数："+v.size());
        int [][] dpOne =new int [n+1][V+1];
        for (int i = 1; i <= n; i++) {
            int z=(int)(Math.ceil(v.get(i-1).getCurrentRequestedTotalMips()));
            //int z=(int)(v.get(i-1).getWorkloadTotalMips());
            for (int j = 1; j <= V; j++) {
                if (j >=z ) {
                    dpOne[i][j] = Math.max(dpOne[i - 1][j], dpOne[i - 1][j - z] + z);
                } else {
                    dpOne[i][j] = dpOne[i - 1][j];}
            }
        }
        List <Integer> x=new ArrayList<Integer>();
        int j=V;
        int sum=0;
        System.out.println("DynamicContainerMigrationVm:"+vm.getId()+" AvailableMips为:"+vm.getAvailableMips()+" 选中的容器是:");
        for (int i = n ; i > 0; i--){
            if (dpOne[i][j]>dpOne[i - 1][j]){
                x.add(i-1);
                sum+=(int)(Math.ceil(v.get(i-1).getCurrentRequestedTotalMips()));
                j = j - (int)(Math.ceil(v.get(i-1).getCurrentRequestedTotalMips()));
                System.out.print(containerListA1.get(i-1).getId()+","+containerListA1.get(i-1).getCurrentRequestedTotalMips()+"  ");
                containerListA1.remove(i-1);
            }
        }
        //System.out.println("移除后物体个数："+containerList1.size());
        System.out.println();
        System.out.println("容器RequestedTotalMips总和:"+sum);
        resultA1.add(x);
    }


    private List<Map<String, Object>> getNewContainerPlacement(List<? extends Container> containersToMigrate, Set<? extends ContainerHost> excludedHosts) {
        System.out.println("ContainerMigration-3-1-PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded.getNewContainerPlacement");
        List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();

        //powerContainerHost1.addAll(((PowerContainerHost) excludedHosts));
//        for (ContainerHost host1 : getContainerHostList()) {
//            if (excludedHosts.contains(host1)) {
//                continue;
//            }
//            host= host1;
//            break;
//        }
//        ContainerHostList.sortByAvailableMipsDescending(powerContainerHost1);
        //自己
        //动态规划+降序首次适应算法的前奏
        List<ContainerHost> hostListA1=new ArrayList<ContainerHost>();
        List<ContainerVm> containerVmListA1=new ArrayList<ContainerVm>();
        List<Container> containerListA3=new ArrayList<Container>();
        List<Container> containerLista=new ArrayList<Container>();
        List<ContainerVm> containerVmListA2=new ArrayList<ContainerVm>();

        containerListA1.addAll(containersToMigrate);
        containerListA2.addAll(containersToMigrate);
        containerLista.addAll(containersToMigrate);

        //PowerContainerList. sortByWorkloadTotalReDecreasing(containerLista);
        System.out.println("ContainerMigrationgetContainerHostList序列:");
        for (ContainerHost host : getContainerHostList()) {
            System.out.print(host.getId()+" ");
            if (excludedHosts.contains(host)) {
                continue;
            }
            hostListA1.add(host);
        }
        System.out.println();
       for (ContainerHost host:hostListA1){
           List<ContainerVm> ContainerVm=new ArrayList<ContainerVm>();
           ContainerVm.addAll(host.getVmList());
           for (ContainerVm vm:ContainerVm){
               if (!isVmOverUtilized(vm)){
                   continue;
               }
               containerVmListA1.add(vm);
           }
       }
        containerVmListA2.addAll(containerVmListA1);
        PowerContainerVmList.sortByAvailableMipsDecreasing(containerVmListA2);
        PowerContainerList.sortByCurrentRequestedTotalMipsDecreasing(containerLista);

        /**
         * 动态规划
         */
        for (int i = 0; containerListA1.size()>0; i++) {
//            System.out.println("containerListA1.size:"+containerListA1.size());
//            System.out.println("循环次数:"+(i+1));
            //ContainerDatacenterBroker.containerVmList3（动态规划的vmList）
            //System.out.println("ContainerDatacenterBroker.containerVmList3:"+ContainerDatacenterBroker.containerVmList3.size());
            System.out.println("containerListA1大小:"+containerListA1.size());
            if (i>=containerVmListA2.size()){
                break;
            }
            DynamicContainerMigration(containerListA1.size(),containerListA1,(int) containerVmListA2.get(i).getAvailableMips(),containerVmListA2.get(i));
        }
        for(int i=0;i<resultA1.size();i++){
            for (int j = 0; j <resultA1.get(i).size() ; j++) {
                containerListA3.add(containerListA2.get(resultA1.get(i).get(j)));
                containerListA2.remove((int)resultA1.get(i).get(j));
            }
        }
        if (containerListA1.size()!=0){
            containerListA3.addAll(containerListA1);
        }
        System.out.println("containerListA3序列:");
        for (Container container:containerListA3){
            System.out.print(container.getId()+",");
        }
        System.out.println();

        System.out.println("containerVmListA2序列:");
        for (ContainerVm vm:containerVmListA2){
            System.out.print(vm.getId()+" ");
        }
        System.out.println();
//        PowerContainerList.sortByCpuUtilization(containersToMigrate);
        //containerListA3:动态规划；containerLista:首次降序适应
        for (Container container : containerListA3) {//excludedHosts:排除在外的主机
            /**
             * @see org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled.PowerContainerVmAllocationPolicyMigrationAbstractContainerHostSelection#findHostForContainer
             */
            //Map<String, Object> allocationMap = findHostForContainer(container,hostListA1, excludedHosts, false);
            Map<String, Object> allocationMap = findVmForContainer1(container,containerVmListA2, excludedHosts, false);

            if (allocationMap.get("host") != null && allocationMap.get("vm") != null) {
                ContainerVm vm = (ContainerVm) allocationMap.get("vm");
                Log.printConcatLine("Container #", container.getId(), " allocated to host #", ((PowerContainerHost) allocationMap.get("host")).getId(), "The VM ID is #", vm.getId());
                Map<String, Object> migrate = new HashMap<String, Object>();
                migrate.put("container", container);
                migrate.put("vm", vm);
                migrate.put("host", (PowerContainerHost) allocationMap.get("host"));
                migrationMap.add(migrate);
            } else {
                //用到过
                Map<String, Object> migrate = new HashMap<String, Object>();
                migrate.put("NewVmRequired", container);
                migrationMap.add(migrate);
            }
        }
        containersToMigrate.clear();
        containerListA1.clear();
        containerListA2.clear();
        resultA1.clear();
        containerListA3.clear();
        return migrationMap;
    }

    private List<Map<String, Object>> getPlacementForLeftContainers(List<? extends Container> containersToMigrate, Set<? extends ContainerHost> excludedHostsList) {
        //excludedHostsList:overUtilizedHosts
        Log.printLine("ContainerMigration-3-PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded.getPlacementForLeftContainers");
        List<Map<String, Object>> newMigrationMap = new LinkedList<Map<String, Object>>();

        System.out.println("containersToMigrate.size():"+containersToMigrate.size());
        if (containersToMigrate.size() == 0) {
            return newMigrationMap;
        }
        HashSet<ContainerHost> excludedHostsforOverUtilized = new HashSet<>();
        //VMmigration-7-2-2-PowerContainerVmAllocationPolicyMigrationAbstract.getSwitchedOffHosts
        excludedHostsforOverUtilized.addAll(getSwitchedOffHosts());
        //excludedHostsList:overUtilizedHosts
        excludedHostsforOverUtilized.addAll(excludedHostsList);
        //Log.printLine("ContainerMigration-3-1-PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded.getPlacementForLeftContainers");
        List<Map<String, Object>> migrationMap = getNewContainerPlacement(containersToMigrate, excludedHostsforOverUtilized);
        if (migrationMap.size() == 0) {
            return migrationMap;
        }
//        List<Container> containerList = getExtraContainers(migrationMap);
        List<Container> containerList = new ArrayList<>();
        for (Map<String, Object> map : migrationMap) {
            if (map.containsKey("NewVmRequired")) {
                containerList.add((Container) map.get("NewVmRequired"));
            } else {
                newMigrationMap.add(map);
            }

        }
        if (containerList.size() == 0) {
            return newMigrationMap;
        }

        List<ContainerHost> underUtilizedHostList = getUnderUtilizedHostList(excludedHostsList);

        //getContainerMigrationMapFromUnderUtilizedHosts
        //自己将下面这几行注释了
//        List<Map<String, Object>> migrationMapUnderUtilized = findMapInUnderUtilizedHosts(underUtilizedHostList,containerList);
//        newMigrationMap.addAll(migrationMapUnderUtilized);
//        containerList.removeAll(getAssignedContainers(migrationMapUnderUtilized));
        if(containerList.size()!= 0){
            System.out.println("ContainerMigration-3-a");
            //List<Map<String, Object>> migrationMapSwitchedOff= findMapInSwitchedOffHosts(containerList);
            List<Map<String, Object>> migrationMapSwitchedOff= findMapInSwitchedOffHostsB(containerList);
            newMigrationMap.addAll(migrationMapSwitchedOff);
        }
// Now Check if there are any containers left without VMs.
        //firsthost chosen
        return newMigrationMap;
    }

    protected List<Map<String, Object>> findMapInUnderUtilizedHosts(List<ContainerHost> underUtilizedHostList, List<Container> containerList){
        List<Map<String, Object>> newMigrationMap = new ArrayList<>();
        //        Create new Vms on underUtilized hosts;
        List<Map<String, Object>> createdVmMap = new ArrayList<>();
        if (underUtilizedHostList.size() != 0) {
            for (ContainerHost host : underUtilizedHostList) {
//                   We try to create the largest Vm possible
                List<ContainerVm> VmList = createVms(host, true);
                if(VmList.size() != 0){
                    for(ContainerVm vm:VmList){
                        Map<String, Object> map = new HashMap<>();
                        map.put("host",host);
                        map.put("vm",vm);
                        createdVmMap.add(map);

                    }}
            }
            if(createdVmMap.size() ==0){

                return newMigrationMap;

            }

            // if there are any new Vms on the underUtilized Hosts we assign the containers to them first!
            // Sort the underUtilized host by the utilization, so that we first assign vms to the more utilized ones
            for (Container container : containerList) {
                Map<String, Object> allocationMap = findAvailableHostForContainer(container, createdVmMap);
                if (allocationMap.get("host") != null && allocationMap.get("vm") != null) {
                    ContainerVm vm = (ContainerVm) allocationMap.get("vm");
                    Log.printConcatLine("Container #", container.getId(), " allocated to host #", ((PowerContainerHost) allocationMap.get("host")).getId(), "The VM ID is #", vm.getId());
                    Map<String, Object> migrate = new HashMap<String, Object>();
//                    vm.setInWaiting(true);
                    migrate.put("NewEventRequired", container);
                    migrate.put("container", container);
                    migrate.put("vm", vm);
                    migrate.put("host", (PowerContainerHost) allocationMap.get("host"));
                    newMigrationMap.add(migrate);

                }
            }
        }

        return newMigrationMap;
    }

    protected List<Container> getAssignedContainers(List<Map<String, Object>> migrationMap){
        List<Container> assignedContainers = new ArrayList<>();
        for(Map<String, Object> map:migrationMap){
            if(map.containsKey("container")){
                assignedContainers.add((Container) map.get("container"));
            }
        }

        return assignedContainers;
    }
    protected ContainerVm createVMinHost(ContainerHost host, boolean vmStatus) {
        Log.printLine("ContainerMigration-3-2-1-PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded.createVMinHost");
        for (int i=0; i<numberOfVmTypes; i++) {
            ContainerVm vm = getNewVm(i);
            if (getUtilizationOfCpuMips((PowerContainerHost) host) != 0 && isHostOverUtilizedAfterAllocation((PowerContainerHost) host, vm)) {
                continue;
            }

            if(allocateHostForVm(vm, host)){
                Log.printLine("The vm ID #" + vm.getId() + "will be created ");
                vm.setInWaiting(vmStatus);
                return vm;
            }
        }

        return null;
    }

    private  void DynamicContainerMigrationB(int n, List <Container> v, int V,ContainerVm vm) {
        //System.out.println("物体个数："+v.size());
        int [][] dpOne =new int [n+1][V+1];
        for (int i = 1; i <= n; i++) {
            int z=(int)(Math.ceil(v.get(i-1).getCurrentRequestedTotalMips()));
            //int z=(int)(v.get(i-1).getWorkloadTotalMips());
            for (int j = 1; j <= V; j++) {
                if (j >=z ) {
                    dpOne[i][j] = Math.max(dpOne[i - 1][j], dpOne[i - 1][j - z] + z);
                } else {
                    dpOne[i][j] = dpOne[i - 1][j];}
            }
        }
        List <Integer> x=new ArrayList<Integer>();
        int j=V;
        int sum=0;
        System.out.println("DynamicContainerMigrationBVm:"+vm.getId()+" BvailableMips为:"+vm.getAvailableMips()+" 选中的容器是:");
        for (int i = n ; i > 0; i--){
            if (dpOne[i][j]>dpOne[i - 1][j]){
                x.add(i-1);
                sum+=(int)(Math.ceil(v.get(i-1).getCurrentRequestedTotalMips()));
                j = j - (int)(Math.ceil(v.get(i-1).getCurrentRequestedTotalMips()));
                System.out.print(containerListB1.get(i-1).getId()+","+containerListB1.get(i-1).getCurrentRequestedTotalMips()+"  ");
                containerListB1.remove(i-1);
            }
        }
        //System.out.println("移除后物体个数："+containerList1.size());
        System.out.println();
        System.out.println("容器RequestedTotalMips总和:"+sum);
        resultB1.add(x);
    }


    //自己
    protected List<Map<String, Object>> findMapInSwitchedOffHostsB(List<Container> containerList) {
        Log.printLine("ContainerMigration-3-2-PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded.findMapInSwitchedOffHostsB");
        List<PowerContainerHost> switchedOffHostsList = getSwitchedOffHosts();
        List<Map<String, Object>> newMigrationMap = new ArrayList<>();
        ContainerHostList.sortByAvailableMipsDescending(switchedOffHostsList);
        if (containerList.size() == 0) {
            return newMigrationMap;
        }

        ContainerHost previouseHost = null;
        ContainerVm previouseVm = null;
        while (containerList.size() != 0) {
            List<Container> assignedContainer = new ArrayList<>();
            //choose a random host
            if (previouseHost == null ) {
                if (switchedOffHostsList.size() == 0) {
                    Log.print("There is no hosts to create VMs");
                    return newMigrationMap;
                }
                //产生[0,switchedOffHostsList.size())的随机数
                //int hostIndex = new RandomGen().getNum(switchedOffHostsList.size());
                previouseHost = switchedOffHostsList.get(0);
                switchedOffHostsList.remove(0);
            }
            if (previouseHost != null && previouseVm == null) {
                previouseVm = createVMinHost(previouseHost, true);
                if (previouseVm == null){
                    previouseHost = null;
                    continue;
                }
                previouseHost.containerVmCreate(previouseVm);
            }



            //自己的
            List<ContainerVm> containerVmListB2=new ArrayList<ContainerVm>();
            List<Container> containerListB3=new ArrayList<Container>();
            containerListB1.addAll(containerList);
            containerListB2.addAll(containerList);
            containerVmListB2.add(previouseVm);
            for (int i = 0; containerListB1.size()>0; i++) {
//              System.out.println("containerListB1.size:"+containerListB1.size());
//              System.out.println("循环次数:"+(i+1));
                    //ContainerDatacenterBroker.containerVmList3（动态规划的vmList）
                    //System.out.println("ContainerDatacenterBroker.containerVmList3:"+ContainerDatacenterBroker.containerVmList3.size());
                System.out.println("containerListB1大小:"+containerListB1.size());
                if (i>=containerVmListB2.size()){
                        break;
                }
                DynamicContainerMigrationB(containerListB1.size(),containerListB1,(int) containerVmListB2.get(i).getAvailableMips(),containerVmListB2.get(i));
                }
            for(int i=0;i<resultB1.size();i++){
                for (int j = 0; j <resultB1.get(i).size() ; j++) {
                    containerListB3.add(containerListB2.get(resultB1.get(i).get(j)));
                    containerListB2.remove((int)resultB1.get(i).get(j));
                }
            }
            if (containerListB1.size()!=0){
                containerListB3.addAll(containerListB1);
            }
            System.out.println("containerListB3序列:");
            for (Container container:containerListB3){
                System.out.print(container.getId()+",");
            }
            System.out.println();


            for (Container container : containerListB3) {
                if (previouseVm.isSuitableForContainer(container)) {
                    previouseVm.containerCreate(container);
                    assignedContainer.add(container);
                    Map<String, Object> migrate = new HashMap<String, Object>();
                    System.out.println("DynamicContainerMigrationB_Container:"+container.getId()+" will be allocated to VM:"+previouseVm.getId());
//                    previouseVm.setInWaiting(true);
                    migrate.put("NewEventRequired", container);
                    migrate.put("container", container);
                    migrate.put("vm", previouseVm);
                    migrate.put("host", previouseHost);
                    newMigrationMap.add(migrate);
                }else {
                    previouseVm=null;
//                    assignedContainer.clear();
                    break;
                }
            }
            containerListB1.clear();
            containerListB2.clear();
            containerListB3.clear();
            containerVmListB2.clear();
            resultB1.clear();
            containerList.removeAll(assignedContainer);
        }
        return newMigrationMap;
    }



    protected List<Map<String, Object>> findMapInSwitchedOffHosts(List<Container> containerList) {
        Log.printLine("ContainerMigration-3-2-PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded.findMapInSwitchedOffHosts");
        Log.print(String.format(" %s :  Find Placement in the switched of hosts", CloudSim.clock()));
        List<PowerContainerHost> switchedOffHostsList = getSwitchedOffHosts();
        List<Map<String, Object>> newMigrationMap = new ArrayList<>();
        ContainerHostList.sortByAvailableMipsDescending(switchedOffHostsList);
        PowerContainerList.sortByCurrentRequestedTotalMipsDecreasing(containerList);
        int m=0;

        if (containerList.size() == 0) {
            return newMigrationMap;
        }

        ContainerHost previouseHost = null;
        ContainerVm previouseVm = null;
        while (containerList.size() != 0) {
            if (switchedOffHostsList.size() == 0) {
                Log.print("There is no hosts to create VMs");
                break;
            }
            List<Container> assignedContainer = new ArrayList<>();
            //choose a random host
            if (previouseHost == null && previouseVm == null) {
                if(switchedOffHostsList.size() ==0 ){
                    return newMigrationMap;
                }
                //产生[0,switchedOffHostsList.size())的随机数
                //int hostIndex = new RandomGen().getNum(switchedOffHostsList.size());
                previouseHost = switchedOffHostsList.get(m);
                m++;
                switchedOffHostsList.remove(previouseHost);
                previouseVm = createVMinHost(previouseHost, true);
                previouseHost.containerVmCreate(previouseVm);


                for (Container container : containerList) {
                    if (previouseVm.isSuitableForContainer(container)) {
                        //
                        previouseVm.containerCreate(container);
                        assignedContainer.add(container);
                        Map<String, Object> migrate = new HashMap<String, Object>();
//                        previouseVm.setInWaiting(true);
                        migrate.put("NewEventRequired", container);
                        migrate.put("container", container);
                        migrate.put("vm", previouseVm);
                        migrate.put("host", previouseHost);
                        newMigrationMap.add(migrate);
                    } else {
                        //同一个Host创建VM
                        previouseVm = createVMinHost(previouseHost, true);
                        if (previouseVm == null) {
                            switchedOffHostsList.remove(previouseHost);
                            previouseHost = null;
                            containerList.removeAll(assignedContainer);
                            break;
                        }
                        previouseVm.containerCreate(container);
                        assignedContainer.add(container);
                        Map<String, Object> migrate = new HashMap<String, Object>();
//                        previouseVm.setInWaiting(true);
                        migrate.put("NewEventRequired", container);
                        migrate.put("container", container);
                        migrate.put("vm", previouseVm);
                        migrate.put("host", previouseHost);
                        newMigrationMap.add(migrate);

                    }
                }
                containerList.removeAll(assignedContainer);
            } else {
                Log.printLine("未使用-ContainerMigration-3-2-a");
                for (Container container : containerList) {
                    if (previouseVm.isSuitableForContainer(container)) {
                        previouseVm.containerCreate(container);
                        assignedContainer.add(container);
                        Map<String, Object> migrate = new HashMap<String, Object>();
//                        previouseVm.setInWaiting(true);
                        migrate.put("NewEventRequired", container);
                        migrate.put("container", container);
                        migrate.put("vm", previouseVm);
                        migrate.put("host", previouseHost);
                        newMigrationMap.add(migrate);
                    } else {
                        previouseVm = createVMinHost(previouseHost, true);
                        if (previouseVm == null) {
                            switchedOffHostsList.remove(previouseHost);
                            previouseHost = null;
                            containerList.removeAll(assignedContainer);
                            break;
                        }
                        previouseVm.containerCreate(container);
                        assignedContainer.add(container);
                        Map<String, Object> migrate = new HashMap<String, Object>();
//                        previouseVm.setInWaiting(true);
                        migrate.put("NewEventRequired", container);
                        migrate.put("container", container);
                        migrate.put("vm", previouseVm);
                        migrate.put("host", previouseHost);
                        newMigrationMap.add(migrate);
                    }
                }

                containerList.removeAll(assignedContainer);
            }
        }
        return newMigrationMap;
    }

    //    This method should be re written!
    protected Map<String, Object> findAvailableHostForContainer(Container container, List<Map<String, Object>> createdVm){

        double minPower = Double.MAX_VALUE;
        PowerContainerHost allocatedHost = null;
        ContainerVm allocatedVm = null;
        List<ContainerHost> underUtilizedHostList = new ArrayList<>();
        List<ContainerVm> vmList = new ArrayList<>();
        for(Map<String, Object> map:createdVm){
            underUtilizedHostList.add((ContainerHost) map.get("host"));

        }
        ContainerHostList.sortByCpuUtilization(underUtilizedHostList);
        for (ContainerHost host1 : underUtilizedHostList) {

            PowerContainerHost host = (PowerContainerHost) host1;
            for(Map<String, Object> map:createdVm){
                if((ContainerHost) map.get("host")== host1){
                    vmList.add((ContainerVm) map.get("vm"));
                }

            }
            for (ContainerVm vm : vmList) {
//                if vm is not created no need for checking!

                if (vm.isSuitableForContainer(container)) {
                    // if vm is overutilized or host would be overutilized after the allocation, this host is not chosen!
                    if (!isVmOverUtilized(vm)) {
                        continue;
                    }
                    if (getUtilizationOfCpuMips(host) != 0 && isHostOverUtilizedAfterContainerAllocation(host, vm, container)) {
                        continue;
                    }

                    try {
                        double powerAfterAllocation = getPowerAfterContainerAllocation(host, container, vm);
                        if (powerAfterAllocation != -1) {
                            double powerDiff = powerAfterAllocation - host.getPower();
                            if (powerDiff < minPower) {
                                minPower = powerDiff;
                                allocatedHost = host;
                                allocatedVm = vm;
                            }
                        }
                    } catch (Exception e) {
                        Log.print("Error: Exception in powerDiff algorithm containerAdded");
                    }
                }
            }
        }


        Map<String, Object> map = new HashMap<>();
        map.put("vm", allocatedVm);
        map.put("host", allocatedHost);

        return map;


    }

    private ContainerVm getNewVm(int vmType) {

        ArrayList<ContainerPe> peList = new ArrayList<ContainerPe>();
//        int vmType = new RandomGen().getNum(ConstantsEx.VM_TYPES);
//            int vmType = i / (int) Math.ceil((double) containerVmsNumber / 4.0D);
//            int vmType = 1;
//        Log.print(vmType);
        for (int j = 0; j < vmPes[vmType]; ++j) {
            peList.add(new ContainerPe(j, new CotainerPeProvisionerSimple((double) vmMips[vmType])));
        }
        int brokerId = 2;
        PowerContainerVm vm = new PowerContainerVm(IDs.pollId(ContainerVm.class), brokerId, (double) vmMips[vmType],
                vmRam[vmType],
                vmBw, vmSize, "Xen",
                new ContainerSchedulerTimeSharedOverSubscription(peList),
                new ContainerRamProvisionerSimple(vmRam[vmType]),
                new ContainerBwProvisionerSimple(vmBw), peList, 300);
        return vm;
    }


    /**
     * Gets the under utilized host.
     *
     * @param excludedHosts the excluded hosts
     * @return the under utilized host
     */
    protected List<ContainerHost>   getUnderUtilizedHostList(Set<? extends ContainerHost> excludedHosts) {
        //excludedHosts:overUtilizedHosts
        System.out.println("未使用-getUnderUtilizedHostList-1");
        List<ContainerHost> underUtilizedHostList = new ArrayList<>();
        double minUtilization = 1;
        for (PowerContainerHost host : this.<PowerContainerHost>getContainerHostList()) {
            if (excludedHosts.contains(host)) {
                continue;
            }
            double utilization = host.getUtilizationOfCpu();
            if (utilization > 0 && utilization < minUtilization
                    && !areAllVmsMigratingOutOrAnyVmMigratingIn(host) && !areAllContainersMigratingOutOrAnyContainersMigratingIn(host)) {
                minUtilization = utilization;
                underUtilizedHostList.add(host);
            }
        }
        return underUtilizedHostList;
    }

    public Map<String, Object> findHostForContainer(Container container,List<ContainerHost> containerHostList, Set<? extends ContainerHost> excludedHosts, boolean checkForVM) {
        System.out.println("ContainerMigration-3-2-未使用-PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded.findHostForContainer");
        double minPower = Double.MAX_VALUE;
        PowerContainerHost allocatedHost = null;
        ContainerVm allocatedVm = null;

        for (PowerContainerHost host : this.<PowerContainerHost>getContainerHostList()) {
            if (excludedHosts.contains(host)) {
                continue;
            }
            for (ContainerVm vm : host.getVmList()) {
                if (checkForVM) {
                    if (vm.isInWaiting()) {
                        continue;
                    }
                }
                if (vm.isSuitableForContainer(container)) {
                    // if vm is overutilized or host would be overutilized after the allocation, this host is not chosen!
                    if (!isVmOverUtilized(vm)) {
                        continue;
                    }
                    if (getUtilizationOfCpuMips(host) != 0 && isHostOverUtilizedAfterContainerAllocation(host, vm, container)) {
                        continue;
                    }

                    try {
                        double powerAfterAllocation = getPowerAfterContainerAllocation(host, container, vm);
                        if (powerAfterAllocation != -1) {
                            double powerDiff = powerAfterAllocation - host.getPower();
                            if (powerDiff < minPower) {
                                minPower = powerDiff;
                                allocatedHost = host;
                                allocatedVm = vm;
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
        Map<String, Object> map = new HashMap<>();
        map.put("vm", allocatedVm);
        map.put("host", allocatedHost);

        return map;
    }

    protected boolean isVmOverUtilized(ContainerVm vm) {
        //System.out.println("ContainerMigration-8-1-PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded.isVmOverUtilized");
        boolean isOverUtilized = true;
        double util = 0;
//        Log.printConcatLine("Checking if the vm is over utilized or not!");
        for (Container container : vm.getContainerList()) {
            util += container.getTotalUtilizationOfCpuMips(CloudSim.clock());
        }
        //vm.getHost().getTotalMips() / vm.getHost().getNumberOfPes() * vm.getNumberOfPes():VM获得的MIPS
        if (util > vm.getHost().getTotalMips() / vm.getHost().getNumberOfPes() * vm.getNumberOfPes()) {
            System.out.println("未使用-isVmOverUtilized");
            return false;
        }
        //System.out.println("isVmOverUtilized:"+isOverUtilized);
        return isOverUtilized;
    }

    /**
     * Gets the power after allocation.
     *
     * @param host      the host
     * @param container the vm
     * @return the power after allocation
     */
    protected double getPowerAfterContainerAllocation(PowerContainerHost host, Container container, ContainerVm vm) {
        double power = 0;
        try {
            power = host.getPowerModel().getPower(getMaxUtilizationAfterContainerAllocation(host, container, vm));
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
     * @param host      the host
     * @param container the vm
     * @return the power after allocation
     */
    protected double getMaxUtilizationAfterContainerAllocation(PowerContainerHost host, Container container, ContainerVm containerVm) {
        double requestedTotalMips = container.getCurrentRequestedTotalMips();
        if (requestedTotalMips > containerVm.getMips()) {
            requestedTotalMips = containerVm.getMips();
        }
        double hostUtilizationMips = getUtilizationOfCpuMips(host);
        double hostPotentialUtilizationMips = hostUtilizationMips + requestedTotalMips;
        double pePotentialUtilization = hostPotentialUtilizationMips / host.getTotalMips();
        return pePotentialUtilization;
    }

    /**
     * Gets the utilization of the CPU in MIPS for the current potentially allocated VMs.
     *
     * @param containerVm the host
     * @return the utilization of the CPU in MIPS
     */
    protected double getUtilizationOfCpuMipsofVm(ContainerVm containerVm) {
        double vmUtilizationMips = 0;
        for (Container container : containerVm.getContainerList()) {

            vmUtilizationMips += containerVm.getTotalAllocatedMipsForContainer(container);
        }
        return vmUtilizationMips;
    }

    /**
     * Checks if is host over utilized after allocation.
     *
     * @param host      the host
     * @param container the vm
     * @return true, if is host over utilized after allocation
     */
    protected boolean isHostOverUtilizedAfterContainerAllocation(PowerContainerHost host, ContainerVm vm, Container container) {
        //System.out.println("ContainerMigration-8-c-1-PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded.isHostOverUtilizedAfterContainerAllocation");
        boolean isHostOverUtilizedAfterAllocation = true;
        if (vm.containerCreate(container)) {
            isHostOverUtilizedAfterAllocation = isHostOverUtilized(host);
            vm.containerDestroy(container);
        }
        return isHostOverUtilizedAfterAllocation;
    }


    /**
     * Save allocation.
     */
    @Override
    protected void saveAllocation() {
        getSavedAllocation().clear();
        for (ContainerHost host : getContainerHostList()) {
            for (ContainerVm vm : host.getVmList()) {
                if (host.getVmsMigratingIn().contains(vm)) {
                    continue;
                }
                for (Container container : vm.getContainerList()) {
                    if (vm.getContainersMigratingIn().contains(container)) {
                        continue;
                    }
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("host", host);
                    map.put("vm", vm);
                    map.put("container", container);
                    getSavedAllocation().add(map);
                }
            }
        }
        Log.printLine(String.format("The length of the saved map is ....%d", getSavedAllocation().size()));

    }


    /**
     * Restore allocation.
     */
    protected void restoreAllocation() {
        for (ContainerHost host : getContainerHostList()) {
            for (ContainerVm vm : host.getVmList()) {
                vm.containerDestroyAll();
                vm.reallocateMigratingInContainers();
            }

            host.containerVmDestroyAll();
            host.reallocateMigratingInContainerVms();
        }
        for (Map<String, Object> map : getSavedAllocation()) {
            PowerContainerVm vm = (PowerContainerVm) map.get("vm");

            PowerContainerHost host = (PowerContainerHost) map.get("host");
            if (!host.getVmList().contains(vm)) {
                if (!host.containerVmCreate(vm)) {
                    Log.printConcatLine("Couldn't restore VM #", vm.getId(), " on host #", host.getId());
                    System.exit(0);
                }

                getVmTable().put(vm.getUid(), host);
            }
//            vm.containerDestroyAll();
//            vm.reallocateMigratingInContainers();
        }
//        List<ContainerVm > restoredVms = new ArrayList<>();
        for (Map<String, Object> map : getSavedAllocation()) {
            PowerContainerVm vm = (PowerContainerVm) map.get("vm");
            if (map.get("container") != null && map.containsKey("container")) {
                Container container = (Container) map.get("container");
//                Log.print(container);

                if (!vm.getContainerList().contains(container)) {
                    if (!vm.containerCreate(container)) {
                        Log.printConcatLine("Couldn't restore Container #", container.getId(), " on vm #", vm.getId());
                        System.exit(0);
                    }
                } else {

                    Log.print("The Container is in the VM already");
                }

                if (container.getVm() == null) {
                    Log.print("The Vm is null");

                }
                ((PowerContainerAllocationPolicy) getDatacenter().getContainerAllocationPolicy()).
                        getContainerTable().put(container.getUid(), vm);
//            container.setVm(vm);

            }
        }


    }

    protected List<ContainerVm> createVms(ContainerHost host, boolean vmStatus) {
        List<ContainerVm> vmList = new ArrayList<>();
        while (true) {
            ContainerVm vm = createVMinHost(host, vmStatus);
            if (vm == null) {
                break;
            }
            vmList.add(vm);
        }
        return vmList;


    }


        public ContainerDatacenter getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(ContainerDatacenter datacenter) {
        this.datacenter = datacenter;
    }

    public PowerContainerSelectionPolicy getContainerSelectionPolicy() {
        return containerSelectionPolicy;
    }

    public void setContainerSelectionPolicy(PowerContainerSelectionPolicy containerSelectionPolicy) {
        this.containerSelectionPolicy = containerSelectionPolicy;
    }

    public abstract Map<String, Object> findVmForContainer1(Container container,List<ContainerVm> vmList, Set<? extends ContainerHost> excludedHosts, boolean checkForVM);
}
