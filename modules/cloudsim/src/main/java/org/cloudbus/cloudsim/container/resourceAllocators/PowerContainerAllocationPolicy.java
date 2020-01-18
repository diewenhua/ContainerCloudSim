package org.cloudbus.cloudsim.container.resourceAllocators;

import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerVm;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sareh on 16/07/15.
 */
public abstract class PowerContainerAllocationPolicy extends ContainerAllocationPolicy{

    /** The container table. */
    private final Map<String, ContainerVm> containerTable = new HashMap<>();

    /**
     * Instantiates a new power vm allocation policy abstract.
     *
     */
    public PowerContainerAllocationPolicy() {
        super();
    }

    /*
     * (non-Javadoc)
     * @see org.cloudbus.cloudsim.VmAllocationPolicy#allocateHostForVm(org.cloudbus.cloudsim.Vm)
     */
    @Override
    public boolean allocateVmForContainer(Container container, List<ContainerVm> containerVmList) {
        System.out.println("Containerallocation-1-1-PowerContainerAllocationPolicy.allocateVmForContainer");
        setContainerVmList(containerVmList);
        //调用同类的Containerallocation-1-2-PowerContainerAllocationPolicy.allocateVmForContainer
        //调用同类的Containerallocation-1-3-PowerContainerAllocationPolicy.findVmForContainer
        //return allocateVmForContainer(container, findVmForContainer(container));
        return allocateVmForContainer(container, findVmForContainer(container,containerVmList));
    }

    /*
     * (non-Javadoc)
     * @see org.cloudbus.cloudsim.VmAllocationPolicy#allocateHostForVm(org.cloudbus.cloudsim.Vm,
     * org.cloudbus.cloudsim.Host)
     */
    @Override
    public boolean allocateVmForContainer(Container container, ContainerVm containerVm) {
        System.out.println("Containerallocation-1-2-PowerContainerAllocationPolicy.allocateVmForContainer");
        if (containerVm == null) {
            Log.formatLine("%.2f: No suitable VM found for Container#" + container.getId() + "\n", CloudSim.clock());
            return false;
        }
        System.out.println("Vm分配之前的AvailableMips:"+ containerVm.getAvailableMips()+";Container的totalRequestedMips:"+container.getCurrentRequestedTotalMips());
        if (containerVm.containerCreate(container)) { // if vm has been succesfully created in the host
            getContainerTable().put(container.getUid(), containerVm);
//                container.setVm(containerVm);
            System.out.println("Vm:"+containerVm.getId()+"分配之后的AvailableMips:"+ containerVm.getAvailableMips());
            Log.formatLine(
                    "%.2f: Container #" + container.getId() + " has been allocated to the VM #" + containerVm.getId(),
                    CloudSim.clock());
            return true;
        }
        Log.formatLine(
                "%.2f: Creation of Container #" + container.getId() + " on the Vm #" + containerVm.getId() + " failed\n",
                CloudSim.clock());
        return false;
    }

    /**
     * Find host for vm.
     *开始Container阶段指定的container，找出第一个合适的VM
     * @param container the vm
     * @return the power host
     *
     */
    public ContainerVm findVmForContainer(Container container,List<ContainerVm> containerVmList) {
        System.out.println("Containerallocation-1-3-PowerContainerAllocationPolicy.findVmForContainer");
        for (ContainerVm containerVm : containerVmList) {
//                Log.printConcatLine("Trying vm #",containerVm.getId(),"For container #", container.getId());
            if (containerVm.isSuitableForContainer(container)) {
                return containerVm;
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.cloudbus.cloudsim.VmAllocationPolicy#deallocateHostForVm(org.cloudbus.cloudsim.Vm)
     */
    @Override
    public void deallocateVmForContainer(Container container) {
        ContainerVm containerVm = getContainerTable().remove(container.getUid());
        if (containerVm != null) {
            containerVm.containerDestroy(container);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.cloudbus.cloudsim.VmAllocationPolicy#getHost(org.cloudbus.cloudsim.Vm)
     */
    @Override
    public ContainerVm getContainerVm(Container container) {
        System.out.println("Containerallocation-2-1-PowerContainerAllocationPolicy.getContainerVm");
        return getContainerTable().get(container.getUid());
    }

    /*
     * (non-Javadoc)
     * @see org.cloudbus.cloudsim.VmAllocationPolicy#getHost(int, int)
     */
    @Override
    public ContainerVm getContainerVm(int containerId, int userId) {
        return getContainerTable().get(Container.getUid(userId, containerId));
    }

    /**
     * Gets the vm table.
     *
     * @return the vm table
     */
    public Map<String, ContainerVm> getContainerTable() {
        return containerTable;
    }

}
