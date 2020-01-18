package org.cloudbus.cloudsim.container.resourceAllocators;


import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.core.ContainerHostList;
import org.cloudbus.cloudsim.container.core.ContainerVm;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Created by sareh on 14/07/15.
 */
public abstract  class PowerContainerVmAllocationAbstract extends ContainerVmAllocationPolicy{

    /** The vm table. */
    private final Map<String, ContainerHost> vmTable = new HashMap<String, ContainerHost>();

    /**
     * Instantiates a new power vm allocation policy abstract.
     *
     * @param list the list
     */
    public PowerContainerVmAllocationAbstract(List<? extends ContainerHost> list) {
        super(list);
    }

    /**
     * (non-Javadoc)
     * @see org.cloudbus.cloudsim.VmAllocationPolicy#allocateHostForVm(org.cloudbus.cloudsim.Vm)
     */
    @Override
    public boolean allocateHostForVm(ContainerVm containerVm) {
        System.out.println("VMallocation-2-PowerContainerVmAllocationAbstract.allocateHostForVm");
        /**
         * @see org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled.PowerContainerVmAllocationPolicyMigrationAbstractHostSelection#findHostForVm(ContainerVm)
         */
        return allocateHostForVm(containerVm, findHostForVm(containerVm));
    }

    /**
     * (non-Javadoc)
     * @see org.cloudbus.cloudsim.VmAllocationPolicy#allocateHostForVm(org.cloudbus.cloudsim.Vm,
     * org.cloudbus.cloudsim.Host)
     */
    @Override
    public boolean allocateHostForVm(ContainerVm containerVm, ContainerHost host) {
        System.out.println("VMallocation-3-PowerContainerVmAllocationAbstract.allocateHostForVm");
        if (host == null) {
            System.out.println("VMallocation-3-1");
            Log.formatLine("%.2f: No suitable host found for VM #" + containerVm.getId() + "\n", CloudSim.clock());
            return false;
        }
        if (host.containerVmCreate(containerVm)) { // if vm has been succesfully created in the host
            System.out.println("VMallocation-3-2");
            getVmTable().put(containerVm.getUid(), host);
            Log.formatLine(
                    "%.2f:VM #" + containerVm.getId() + " has been allocated to the host #" + host.getId(),
                    CloudSim.clock());
            return true;
        }
        System.out.println("VMallocation-3-3");
        Log.formatLine(
                "%.2f: Creation of VM #" + containerVm.getId() + " on the host #" + host.getId() + " failed\n",
                CloudSim.clock());
        return false;
    }

    /**
     * Find host for vm.
     *
     * @param containerVm the vm
     * @return the power host
     */
    public ContainerHost findHostForVm(ContainerVm containerVm) {

        List<ContainerHost> containerHostList1=new ArrayList<ContainerHost>();
        containerHostList1.addAll(this.<ContainerHost> getContainerHostList());
        ContainerHostList.sortByAvailableMipsDescending(containerHostList1);
        //for (ContainerHost host : this.<ContainerHost> getContainerHostList()) {
        for (ContainerHost host : containerHostList1) {
            System.out.println("未使用findHostForVm");
            if (host.isSuitableForContainerVm(containerVm)) {
                return host;
            }
        }
        return null;
    }

    /**
     * (non-Javadoc)
     * @see org.cloudbus.cloudsim.VmAllocationPolicy#deallocateHostForVm(org.cloudbus.cloudsim.Vm)
     */
    @Override
    public void deallocateHostForVm(ContainerVm containerVm) {
        System.out.println("VMmigration-6-PowerContainerVmAllocationAbstract.deallocateHostForVm");
        ContainerHost host = getVmTable().remove(containerVm.getUid());
        if (host != null) {
            System.out.println("VM# "+containerVm.getUid() +"is removed from Host#"+host.getId());
            host.containerVmDestroy(containerVm);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.cloudbus.cloudsim.VmAllocationPolicy#getHost(org.cloudbus.cloudsim.Vm)
     */
    @Override
    public ContainerHost getHost(ContainerVm vm) {
        return getVmTable().get(vm.getUid());
    }

    /*
     * (non-Javadoc)
     * @see org.cloudbus.cloudsim.VmAllocationPolicy#getHost(int, int)
     */
    @Override
    public ContainerHost getHost(int vmId, int userId) {
        return getVmTable().get(ContainerVm.getUid(userId, vmId));
    }

    /**
     * Gets the vm table.
     *
     * @return the vm table
     */
    public Map<String, ContainerHost> getVmTable() {
        return vmTable;
    }

    public List<ContainerVm> getOverUtilizedVms() {
        List<ContainerVm> vmList = new ArrayList<ContainerVm>();
        for (ContainerHost host : getContainerHostList()) {
            for (ContainerVm vm : host.getVmList()) {
                if (vm.getTotalUtilizationOfCpuMips(CloudSim.clock()) > vm.getTotalMips()) {
                    vmList.add(vm);

                }
            }
        }
        return vmList;
    }

    /**
     * 自己添加
     */
    @Override
    public List<Map<String, Object>> optimizeAllocation_ContainerMigration(List<? extends ContainerVm> vmList) {
        // TODO Auto-generated method stub
        return null;
    }
}