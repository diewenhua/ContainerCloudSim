package org.cloudbus.cloudsim.container.core;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerAllocationPolicy;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerVmAllocationPolicy;
import org.cloudbus.cloudsim.container.utils.CostumeCSVWriter;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.predicates.PredicateType;

import org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled.PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sareh on 3/08/15.
 */
public class PowerContainerDatacenterCM extends PowerContainerDatacenter {
    /**
     * The disable container migrations.
     */
    private boolean disableMigrations;
    public int containerMigrationCount;
    private CostumeCSVWriter newlyCreatedVmWriter;
    private int newlyCreatedVms;
    private List<Integer> newlyCreatedVmsList;
    private double vmStartupDelay;
    private double containerStartupDelay;


    public PowerContainerDatacenterCM(String name, ContainerDatacenterCharacteristics characteristics,
                                      ContainerVmAllocationPolicy vmAllocationPolicy,
                                      ContainerAllocationPolicy containerAllocationPolicy, List<Storage> storageList,
                                      double schedulingInterval, String experimentName, String logAddress,
                                      double vmStartupDelay, double containerStartupDelay) throws Exception {
        super(name, characteristics, vmAllocationPolicy, containerAllocationPolicy, storageList, schedulingInterval, experimentName, logAddress);
        String newlyCreatedVmsAddress;
        int index = getExperimentName().lastIndexOf("_");
        newlyCreatedVmsAddress = String.format("%s/NewlyCreatedVms/%s/%s.csv", getLogAddress(), getExperimentName().substring(0, index), getExperimentName());
        setNewlyCreatedVmWriter(new CostumeCSVWriter(newlyCreatedVmsAddress));
        setNewlyCreatedVms(0);
        setDisableMigrations(false);
        setNewlyCreatedVmsList(new ArrayList<Integer>());
        this.vmStartupDelay = vmStartupDelay;
        this.containerStartupDelay = containerStartupDelay;
    }

    /**
     * 自己测试，已无用
     */
    @Override
    protected void updateCloudletProcessing_ContainerMigration() {
        System.out.println("ContainerMigration-2-未使用-PowerContainerDatacenterCM.updateCloudletProcessing_ContainerMigration");
        //        Log.printLine("Power data center is Updating the cloudlet processing");
        if (getCloudletSubmitted() == -1 || getCloudletSubmitted() == CloudSim.clock()) {
            CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
            schedule(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);
            System.out.println("ContainerMigration-1-1");
            return;
        }
        double currentTime = CloudSim.clock();

        // if some time passed since last processing
        if (currentTime > getLastProcessTime()) {
            System.out.println("ContainerMigration-2-2");
            System.out.print(currentTime + " ");
            double minTime = updateCloudetProcessingWithoutSchedulingFutureEventsForce();

            if (!isDisableMigrations()) {
//                    PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded ContainerMigration_1=new PowerContainerVmAllocationPolicyMigrationAbstractContainerAdded
//                    		(List<? extends ContainerHost> hostList,
//                    	    		PowerContainerVmSelectionPolicy vmSelectionPolicy, PowerContainerSelectionPolicy containerSelectionPolicy,
//                    	    		int numberOfVmTypes, int[] vmPes, float[] vmRam, long vmBw, long vmSize, double[] vmMips) {
//
//    					@Override
//    					protected boolean isHostUnderUtilized(PowerContainerHost host) {
//    						// TODO Auto-generated method stub
//    						return false;
//    					}
//
//    					@Override
//    					protected boolean isHostOverUtilized(PowerContainerHost host) {
//    						// TODO Auto-generated method stub
//    						return false;
//    					}
//    				};

                System.out.println("ContainerMigration-2-3");
//                Class cls=Class.forName("com.ywq.Son");
//                Son son=(Son) cls.newInstance();
//                son.fun2();
                List<Map<String, Object>> migrationMap = getVmAllocationPolicy().optimizeAllocation_ContainerMigration(getContainerVmList());
                int previousContainerMigrationCount = getContainerMigrationCount();
                int previousVmMigrationCount = getVmMigrationCount();
                System.out.println("previousContainerMigrationCount:"+previousContainerMigrationCount+"  previousVmMigrationCount:"+previousVmMigrationCount);
                if (migrationMap != null) {
                    //incrementContainerMigrationCount();
                    //System.out.println("VMmigration-2-4");
                    List<ContainerVm> vmList = new ArrayList<ContainerVm>();
                    for (Map<String, Object> migrate : migrationMap) {
                        if (migrate.containsKey("container")) {
                            //System.out.println("VMmigration-2-5");
                            Container container = (Container) migrate.get("container");
                            ContainerVm targetVm = (ContainerVm) migrate.get("vm");
                            ContainerVm oldVm = container.getVm();
                            if (oldVm == null) {
                                Log.formatLine(
                                        "%.2f: Migration of Container #%d to Vm #%d is started",
                                        currentTime,
                                        container.getId(),
                                        targetVm.getId());
                            } else {
                                Log.formatLine(
                                        "%.2f: Migration of Container #%d from Vm #%d to VM #%d is started",
                                        currentTime,
                                        container.getId(),
                                        oldVm.getId(),
                                        targetVm.getId());
                            }
                            System.out.println("未使用-incrementContainerMigrationCount()");
                            incrementContainerMigrationCount();
                            targetVm.addMigratingInContainer(container);

                            if (migrate.containsKey("NewEventRequired")) {
                                //System.out.println("VMmigration-2-6");
                                if (!vmList.contains(targetVm)) {
                                    // A new VM is created  send a vm create request with delay :)
//                                    Send a request to create Vm after 100 second
//                                                create a new event for this. or overright the vm create
                                    Log.formatLine(
                                            "%.2f: Migration of Container #%d to newly created Vm #%d is started",
                                            currentTime,
                                            container.getId(),
                                            targetVm.getId());
                                    targetVm.containerDestroyAll();
                                    send(
                                            getId(),
                                            vmStartupDelay,
                                            CloudSimTags.VM_CREATE,
                                            migrate);
                                    vmList.add(targetVm);

                                    send(
                                            getId(),
                                            containerStartupDelay + vmStartupDelay
                                            , containerCloudSimTags.CONTAINER_MIGRATE,
                                            migrate);

                                } else {
                                    Log.formatLine(
                                            "%.2f: Migration of Container #%d to newly created Vm #%d is started",
                                            currentTime,
                                            container.getId(),
                                            targetVm.getId());
//                                        send a request for container migration after the vm is created
//                                        it would be 100.4
                                    send(
                                            getId(),
                                            containerStartupDelay + vmStartupDelay
                                            , containerCloudSimTags.CONTAINER_MIGRATE,
                                            migrate);


                                }


                            } else {
                                //System.out.println("VMmigration-2-7");
                                send(
                                        getId(),
                                        containerStartupDelay,
                                        containerCloudSimTags.CONTAINER_MIGRATE,
                                        migrate);


                            }
                        } else {
                            //System.out.println("VMmigration-2-8");
                            ContainerVm vm = (ContainerVm) migrate.get("vm");
                            PowerContainerHost targetHost = (PowerContainerHost) migrate.get("host");
                            PowerContainerHost oldHost = (PowerContainerHost) vm.getHost();

                            if (oldHost == null) {
                                Log.formatLine(
                                        "%.2f: Migration of VM #%d to Host #%d is started",
                                        currentTime,
                                        vm.getId(),
                                        targetHost.getId());
                            } else {
                                Log.formatLine(
                                        "%.2f: Migration of VM #%d from Host #%d to Host #%d is started",
                                        currentTime,
                                        vm.getId(),
                                        oldHost.getId(),
                                        targetHost.getId());
                            }

                            targetHost.addMigratingInContainerVm(vm);
                            incrementMigrationCount();

                            /** VM migration delay = RAM / bandwidth **/
                            // we use BW / 2 to model BW available for migration purposes, the other
                            // half of BW is for VM communication
                            // around 16 seconds for 1024 MB using 1 Gbit/s network
                            send(
                                    getId(),
                                    vm.getRam() / ((double) targetHost.getBw() / (2 * 8000)),
                                    CloudSimTags.VM_MIGRATE,
                                    migrate);
                        }


                    }

                    migrationMap.clear();
                    vmList.clear();
                }
                getContainerMigrationList().add((double) (getContainerMigrationCount() - previousContainerMigrationCount));

                Log.printConcatLine(CloudSim.clock(), ": The Number Container of Migrations is:  ", getContainerMigrationCount() - previousContainerMigrationCount);
                Log.printConcatLine(CloudSim.clock(), ": The Number of VM Migrations is:  ", getVmMigrationCount() - previousVmMigrationCount);
                String[] vmMig = {Double.toString(CloudSim.clock()), Integer.toString(getVmMigrationCount() - previousVmMigrationCount)};                   // <--declared statement
                String[] msg = {Double.toString(CloudSim.clock()), Integer.toString(getContainerMigrationCount() - previousContainerMigrationCount)};                   // <--declared statement
                try {
                    getContainerMigrationWriter().writeTofile(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    getVmMigrationWriter().writeTofile(vmMig);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                int numberOfNewVms = getNewlyCreatedVms();
                getNewlyCreatedVmsList().add(numberOfNewVms);
                String[] msg1 = {Double.toString(CloudSim.clock()), Integer.toString(numberOfNewVms)};                   // <--declared statement
                try {
                    getNewlyCreatedVmWriter().writeTofile(msg1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            // schedules an event to the next time
            if (minTime != Double.MAX_VALUE) {
                System.out.println("ContainerMigration-1-x");
                CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
                send(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);
            }

            setLastProcessTime(currentTime);

        }

    }



    @Override
    protected void updateCloudletProcessing() {
        System.out.println("VMmigration-2-PowerContainerDatacenterCM.updateCloudletProcessing");
        //        Log.printLine("Power data center is Updating the cloudlet processing");
        if (getCloudletSubmitted() == -1 || getCloudletSubmitted() == CloudSim.clock()) {
            CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
            schedule(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);
            System.out.println("VMmigration-2-1");
            return;
        }
        double currentTime = CloudSim.clock();

        // if some time passed since last processing
        if (currentTime > getLastProcessTime()) {
            //System.out.println("VMmigration-2-2");
            System.out.print(currentTime + " ");
            double minTime = updateCloudetProcessingWithoutSchedulingFutureEventsForce();

            if (!isDisableMigrations()) {
                /**
                 * @see org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled.PowerContainerVmAllocationPolicyMigrationAbstract#optimizeAllocation(List)
                 */
                System.out.println("VMmigration-2-3");
                /**
                 * @see org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled.PowerContainerVmAllocationPolicyMigrationAbstract.optimizeAllocation
                 */
                List<Map<String, Object>> migrationMap = getVmAllocationPolicy().optimizeAllocation(
                        getContainerVmList());
                int previousContainerMigrationCount = getContainerMigrationCount();
                int previousVmMigrationCount = getVmMigrationCount();
                System.out.println("previousContainerMigrationCount:"+previousContainerMigrationCount+"  previousVmMigrationCount:"+previousVmMigrationCount);
                if (migrationMap != null) {
                    //incrementContainerMigrationCount();
                    System.out.println("VMmigration-2-4");
                    List<ContainerVm> vmList = new ArrayList<ContainerVm>();
                    for (Map<String, Object> migrate : migrationMap) {
                        if (migrate.containsKey("container")) {
                            System.out.println("VMmigration-2-5");
                            Container container = (Container) migrate.get("container");
                            ContainerVm targetVm = (ContainerVm) migrate.get("vm");
                            ContainerVm oldVm = container.getVm();
                            if (oldVm == null) {
                                Log.formatLine(
                                        "%.2f: Migration of Container #%d to Vm #%d is started",
                                        currentTime,
                                        container.getId(),
                                        targetVm.getId());
                            } else {
                                Log.formatLine(
                                        "%.2f: Migration of Container #%d from Vm #%d to VM #%d is started",
                                        currentTime,
                                        container.getId(),
                                        oldVm.getId(),
                                        targetVm.getId());
                            }
                            System.out.println("未使用-incrementContainerMigrationCount()");
                            incrementContainerMigrationCount();
                            targetVm.addMigratingInContainer(container);

                            if (migrate.containsKey("NewEventRequired")) {
                                System.out.println("VMmigration-2-6");
                                if (!vmList.contains(targetVm)) {
                                    // A new VM is created  send a vm create request with delay :)
//                                Send a request to create Vm after 100 second
//                                            create a new event for this. or overright the vm create
                                    Log.formatLine(
                                            "%.2f: Migration of Container #%d to newly created Vm #%d is started",
                                            currentTime,
                                            container.getId(),
                                            targetVm.getId());
                                    targetVm.containerDestroyAll();
                                    send(
                                            getId(),
                                            vmStartupDelay,
                                            CloudSimTags.VM_CREATE,
                                            migrate);
                                    vmList.add(targetVm);

                                    send(
                                            getId(),
                                            containerStartupDelay + vmStartupDelay
                                            , containerCloudSimTags.CONTAINER_MIGRATE,
                                            migrate);

                                } else {
                                    Log.formatLine(
                                            "%.2f: Migration of Container #%d to newly created Vm #%d is started",
                                            currentTime,
                                            container.getId(),
                                            targetVm.getId());
//                                    send a request for container migration after the vm is created
//                                    it would be 100.4
                                    send(
                                            getId(),
                                            containerStartupDelay + vmStartupDelay
                                            , containerCloudSimTags.CONTAINER_MIGRATE,
                                            migrate);


                                }


                            } else {
                                System.out.println("VMmigration-2-7");
                                send(
                                        getId(),
                                        containerStartupDelay,
                                        containerCloudSimTags.CONTAINER_MIGRATE,
                                        migrate);


                            }
                        } else {
                            System.out.println("VMmigration-2-8");
                            ContainerVm vm = (ContainerVm) migrate.get("vm");
                            PowerContainerHost targetHost = (PowerContainerHost) migrate.get("host");
                            PowerContainerHost oldHost = (PowerContainerHost) vm.getHost();

                            if (oldHost == null) {
                                Log.formatLine(
                                        "%.2f: Migration of VM #%d to Host #%d is started",
                                        currentTime,
                                        vm.getId(),
                                        targetHost.getId());
                            } else {
                                Log.formatLine(
                                        "%.2f: Migration of VM #%d from Host #%d to Host #%d is started",
                                        currentTime,
                                        vm.getId(),
                                        oldHost.getId(),
                                        targetHost.getId());
                            }

                            targetHost.addMigratingInContainerVm(vm);
                            incrementMigrationCount();

                            /** VM migration delay = RAM / bandwidth **/
                            // we use BW / 2 to model BW available for migration purposes, the other
                            // half of BW is for VM communication
                            // around 16 seconds for 1024 MB using 1 Gbit/s network
                            send(
                                    getId(),
                                    vm.getRam() / ((double) targetHost.getBw() / (2 * 8000)),
                                    CloudSimTags.VM_MIGRATE,
                                    migrate);
                        }


                    }

                    migrationMap.clear();
                    vmList.clear();
                }
                getContainerMigrationList().add((double) (getContainerMigrationCount() - previousContainerMigrationCount));

                Log.printConcatLine(CloudSim.clock(), ": The Number Container of Migrations is:  ", getContainerMigrationCount() - previousContainerMigrationCount);
                Log.printConcatLine(CloudSim.clock(), ": The Number of VM Migrations is:  ", getVmMigrationCount() - previousVmMigrationCount);
                String[] vmMig = {Double.toString(CloudSim.clock()), Integer.toString(getVmMigrationCount() - previousVmMigrationCount)};                   // <--declared statement
                String[] msg = {Double.toString(CloudSim.clock()), Integer.toString(getContainerMigrationCount() - previousContainerMigrationCount)};                   // <--declared statement
                try {
                    getContainerMigrationWriter().writeTofile(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    getVmMigrationWriter().writeTofile(vmMig);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                int numberOfNewVms = getNewlyCreatedVms();
                getNewlyCreatedVmsList().add(numberOfNewVms);
                String[] msg1 = {Double.toString(CloudSim.clock()), Integer.toString(numberOfNewVms)};                   // <--declared statement
                try {
                    getNewlyCreatedVmWriter().writeTofile(msg1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            // schedules an event to the next time
            if (minTime != Double.MAX_VALUE) {
                System.out.println("VMmigration-2-9");
                CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
                send(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);
            }

            setLastProcessTime(currentTime);

        }

    }

    @Override
    protected void processVmCreate(SimEvent ev, boolean ack) {
        System.out.println("VMallocation-0-0-PowerContainerDatacenterCM.processVmCreate");
//    here we override the method
        //if未使用
        if (ev.getData() instanceof Map) {
            System.out.println("if未使用");
            Map<String, Object> map = (Map<String, Object>) ev.getData();
            ContainerVm containerVm = (ContainerVm) map.get("vm");
            ContainerHost host = (ContainerHost) map.get("host");
            boolean result = getVmAllocationPolicy().allocateHostForVm(containerVm, host);
//                set the containerVm in waiting state
            containerVm.setInWaiting(true);
//                containerVm.addMigratingInContainer((Container) map.get("container"));
            ack = true;
            if (ack) {
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("vm", containerVm);
                data.put("result", containerVm);
                data.put("datacenterID", getId());

                if (result) {
                    data.put("result", CloudSimTags.TRUE);
                } else {
                    data.put("result", CloudSimTags.FALSE);
                }
                send(2, CloudSim.getMinTimeBetweenEvents(), containerCloudSimTags.VM_NEW_CREATE, data);
            }

            if (result) {
                Log.printLine(String.format("%s VM ID #%d is created on Host #%d", CloudSim.clock(), containerVm.getId(), host.getId()));
                incrementNewlyCreatedVmsCount();
                getContainerVmList().add(containerVm);


                if (containerVm.isBeingInstantiated()) {
                    containerVm.setBeingInstantiated(false);
                }

                containerVm.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(containerVm).getContainerVmScheduler()
                        .getAllocatedMipsForContainerVm(containerVm));
            }

        } else {
            //调用"VMallocation-0-1-PowerContainerDatacenterCM.processVmCreate
            /**
             * @see org.cloudbus.cloudsim.container.core.ContainerDatacenter#processVmCreate
             */
            super.processVmCreate(ev, ack);
        }

    }

    /**
     * Increment migration count.
     */
    protected void incrementContainerMigrationCount() {
        System.out.println("未使用-incrementContainerMigrationCount");
        setContainerMigrationCount(getContainerMigrationCount() + 1);
    }

    /**
     * Increment migration count.
     */
    protected void incrementNewlyCreatedVmsCount() {
        setNewlyCreatedVms(getNewlyCreatedVms() + 1);
    }

    /**
     * Checks if is disable migrations.
     *
     * @return true, if is disable migrations
     */
    public boolean isDisableMigrations() {
        return disableMigrations;
    }

    /**
     * Sets the disable migrations.
     *
     * @param disableMigrations the new disable migrations
     */
    public void setDisableMigrations(boolean disableMigrations) {
        this.disableMigrations = disableMigrations;
    }


    public int getContainerMigrationCount() {
        return containerMigrationCount;
    }

    public void setContainerMigrationCount(int containerMigrationCount) {
        this.containerMigrationCount = containerMigrationCount;
    }

    public CostumeCSVWriter getNewlyCreatedVmWriter() {
        return newlyCreatedVmWriter;
    }

    public void setNewlyCreatedVmWriter(CostumeCSVWriter newlyCreatedVmWriter) {
        this.newlyCreatedVmWriter = newlyCreatedVmWriter;
    }

    public int getNewlyCreatedVms() {
        return newlyCreatedVms;
    }

    public void setNewlyCreatedVms(int newlyCreatedVms) {
        this.newlyCreatedVms = newlyCreatedVms;
    }

    public List<Integer> getNewlyCreatedVmsList() {
        return newlyCreatedVmsList;
    }

    public void setNewlyCreatedVmsList(List<Integer> newlyCreatedVmsList) {
        this.newlyCreatedVmsList = newlyCreatedVmsList;
    }


}