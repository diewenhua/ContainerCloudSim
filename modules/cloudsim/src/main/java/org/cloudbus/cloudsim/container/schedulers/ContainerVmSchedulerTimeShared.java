package org.cloudbus.cloudsim.container.schedulers;

import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmPe;
import org.cloudbus.cloudsim.container.lists.ContainerVmPeList;
import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmPeProvisioner;
import org.cloudbus.cloudsim.container.core.ContainerVm;
import org.cloudbus.cloudsim.Log;

import java.util.*;

/**
 * Created by sareh on 14/07/15.
 */
public class ContainerVmSchedulerTimeShared extends ContainerVmScheduler {
    /** The mips map requested. */
    private Map<String, List<Double>> mipsMapRequested;

    /** The pes in use. */
    private int pesInUse;

    /**
     * Instantiates a new vm scheduler time shared.
     *
     * @param pelist the pelist
     */
    public ContainerVmSchedulerTimeShared(List<? extends ContainerVmPe> pelist) {
        super(pelist);
        setMipsMapRequested(new HashMap<String, List<Double>>());
    }

    @Override
    public boolean allocatePesForVm(ContainerVm containerVm, List<Double> mipsShare) {
        //Log.printLine("VmSchedulerTimeShared: allocatePesForVm with mips share size......" + mipsShare.size());
        System.out.println("HostPE-2-ContainerVmSchedulerTimeShared.allocatePesForVm");
        if (containerVm.isInMigration()) {
            System.out.println("HostPE-2-a-未使用");
            if (!getVmsMigratingIn().contains(containerVm.getUid()) && !getVmsMigratingOut().contains(containerVm.getUid())) {
                getVmsMigratingOut().add(containerVm.getUid());
            }
        } else {
            System.out.println("HostPE-2-b");
            if (getVmsMigratingOut().contains(containerVm.getUid())) {
                System.out.println("HostPE-2-c-未使用");
                getVmsMigratingOut().remove(containerVm.getUid());
            }
        }
        /**
         * @see org.cloudbus.cloudsim.container.schedulers.ContainerVmSchedulerTimeSharedOverSubscription#allocatePesForVm
         */
        System.out.println("VmCurrentRequestedMips:"+mipsShare);
        boolean result = allocatePesForVm(containerVm.getUid(), mipsShare);
        updatePeProvisioning();
        return result;
    }

    /**
     * Allocate pes for vm.
     *
     * @param vmUid the vm uid
     * @param mipsShareRequested the mips share requested
     * @return true, if successful
     */
    protected boolean allocatePesForVm(String vmUid, List<Double> mipsShareRequested) {
        //Log.printLine("VmSchedulerTimeShared: allocatePesForVm for Vmuid......"+vmUid);
        System.out.println("未使用-allocatePesForVm");
        double totalRequestedMips = 0;
        double peMips = getPeCapacity();
        for (Double mips : mipsShareRequested) {
            // each virtual PE of a VM must require not more than the capacity of a physical PE
            if (mips > peMips) {
                return false;
            }
            totalRequestedMips += mips;
        }

        // This scheduler does not allow over-subscription
        if (getAvailableMips() < totalRequestedMips) {
            return false;
        }

        getMipsMapRequested().put(vmUid, mipsShareRequested);
        setPesInUse(getPesInUse() + mipsShareRequested.size());

        if (getVmsMigratingIn().contains(vmUid)) {
            // the destination host only experience 10% of the migrating VM's MIPS
            totalRequestedMips *= 0.1;
        }

        List<Double> mipsShareAllocated = new ArrayList<Double>();
        for (Double mipsRequested : mipsShareRequested) {
            if (getVmsMigratingOut().contains(vmUid)) {
                // performance degradation due to migration = 10% MIPS
                mipsRequested *= 0.9;
            } else if (getVmsMigratingIn().contains(vmUid)) {
                // the destination host only experience 10% of the migrating VM's MIPS
                mipsRequested *= 0.1;
            }
            mipsShareAllocated.add(mipsRequested);
        }

        getMipsMap().put(vmUid, mipsShareAllocated);
        setAvailableMips(getAvailableMips() - totalRequestedMips);

        return true;
    }

    /**
     * Update allocation of VMs on PEs.
     */
    protected void updatePeProvisioning() {
        //Log.printLine("VmSchedulerTimeShared: update the pe provisioning......");
        //System.out.println("HostPE-6-ContainerVmSchedulerTimeShared.updatePeProvisioning");
        getPeMap().clear();
//        Log.printConcatLine("The Pe Map is being cleared ");
        for (ContainerVmPe pe : getPeList()) {
            pe.getContainerVmPeProvisioner().deallocateMipsForAllContainerVms();
        }

        Iterator<ContainerVmPe> peIterator = getPeList().iterator();
        ContainerVmPe pe = peIterator.next();
        ContainerVmPeProvisioner containerVmPeProvisioner = pe.getContainerVmPeProvisioner();
        //getAvailableMips:针对Host的单个PE
        double availableMips = containerVmPeProvisioner.getAvailableMips();
        for (Map.Entry<String, List<Double>> entry : getMipsMap().entrySet()) {
            String vmUid = entry.getKey();
            getPeMap().put(vmUid, new LinkedList<ContainerVmPe>());

            for (double mips : entry.getValue()) {
//                Log.printConcatLine("The mips value is: ",mips);
                while (mips >= 0.1) {
                    //主机的PE挨个为虚拟机的经过加工过的requestedMips安排空间
                    if (availableMips >= mips) {
                        //System.out.println("HostPE-6-a");
                        /**
                         * @see org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmPeProvisionerSimple#allocateMipsForContainerVm
                         */
                        //mips:VM的mipsShareAllocated（处理过的currentRequestedMips）的单个元素
                        containerVmPeProvisioner.allocateMipsForContainerVm(vmUid, mips);
                        getPeMap().get(vmUid).add(pe);
//                        Log.formatLine("The allocated Mips is % f to Pe Id % d", mips, pe.getId());
                        availableMips -= mips;
//                        Log.print(getPeMap().get(vmUid));
                        break;
                    } else {
                        //System.out.println("HostPE-6-b");
                        containerVmPeProvisioner.allocateMipsForContainerVm(vmUid, availableMips);
                        if(availableMips != 0){
                        getPeMap().get(vmUid).add(pe);}
                        mips -= availableMips;
//                        Log.print(getPeMap().get(vmUid));
                        if (mips <= 0.1) {
                            break;
                        }
                        if (!peIterator.hasNext()) {
                            Log.printConcatLine("There is no enough MIPS (", mips, ") to accommodate VM ", vmUid);
                            // System.exit(0);
                        }
                        pe = peIterator.next();
                        containerVmPeProvisioner = pe.getContainerVmPeProvisioner();
                        availableMips = containerVmPeProvisioner.getAvailableMips();
                    }
                }
            }
        }
//        Log.printConcatLine("These are the values",getPeMap().keySet());


    }




    @Override
    public void deallocatePesForVm(ContainerVm containerVm) {
        //Log.printLine("VmSchedulerTimeShared: deallocatePesForVm.....");
        getMipsMapRequested().remove(containerVm.getUid());
        setPesInUse(0);
        getMipsMap().clear();
        setAvailableMips(ContainerVmPeList.getTotalMips(getPeList()));

        for (ContainerVmPe pe : getPeList()) {
            pe.getContainerVmPeProvisioner().deallocateMipsForContainerVm(containerVm);
        }
        //Log.printLine("VmSchedulerTimeShared: deallocatePesForVm. allocates again!!!!!!!....");
        for (Map.Entry<String, List<Double>> entry : getMipsMapRequested().entrySet()) {
            allocatePesForVm(entry.getKey(), entry.getValue());
        }

        updatePeProvisioning();

    }

    /**
     * Releases PEs allocated to all the VMs.
     *
     * @pre $none
     * @post $none
     */
    @Override
    public void deallocatePesForAllContainerVms() {
        super.deallocatePesForAllContainerVms();
        getMipsMapRequested().clear();
        setPesInUse(0);
    }
    /**
     * Returns maximum available MIPS among all the PEs. For the time shared policy it is just all
     * the avaiable MIPS.
     *
     * @return max mips
     */
    @Override
    public double getMaxAvailableMips() {
        return getAvailableMips();
    }


    public Map<String, List<Double>> getMipsMapRequested() {
        return mipsMapRequested;
    }

    public void setMipsMapRequested(Map<String, List<Double>> mipsMapRequested) {
        this.mipsMapRequested = mipsMapRequested;
    }

    public int getPesInUse() {
        return pesInUse;
    }

    public void setPesInUse(int pesInUse) {
        this.pesInUse = pesInUse;
    }
}
