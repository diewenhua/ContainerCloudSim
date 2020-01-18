package org.cloudbus.cloudsim.container.schedulers;

import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmPe;
import org.cloudbus.cloudsim.container.lists.ContainerVmPeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sareh on 23/07/15.
 */
public class ContainerVmSchedulerTimeSharedOverSubscription extends  ContainerVmSchedulerTimeShared{

    public ContainerVmSchedulerTimeSharedOverSubscription(List<? extends ContainerVmPe> pelist) {
        super(pelist);
    }

    @Override
//    public boolean allocatePesForVm(String vmUid, List<Double> mipsShareRequested) {
//        //mipsShareRequested: containerVm.getCurrentRequestedMips()
//        System.out.println("HostPE-3-ContainerVmSchedulerTimeSharedOverSubscription.allocatePesForVm");
//        double totalRequestedMips = 0;
//
//        // if the requested mips is bigger than the capacity of a single PE, we cap[（可用或可借资金的）最高限额 ]
//        // the request to the PE's capacity
//        List<Double> mipsShareRequestedCapped = new ArrayList<Double>();
//        //getPeCapacity(Host): getPeList().get(0).getMips
//        double peMips = getPeCapacity();
//        //将mipsShareRequested根据getPeCapacity()转成mipsShareRequestedCapped
//        //totalRequestedMips: 转成mipsShareRequestedCapped元素总和
//        for (Double mips : mipsShareRequested) {
//            if (mips > peMips) {
//                System.out.println("HostPE-3-1");
//                mipsShareRequestedCapped.add(peMips);
//                totalRequestedMips += peMips;
//            } else {
//                mipsShareRequestedCapped.add(mips);
//                totalRequestedMips += mips;
//            }
//        }
//        getMipsMapRequested().put(vmUid, mipsShareRequested);
//        setPesInUse(getPesInUse() + mipsShareRequested.size());
//        if (getVmsMigratingIn().contains(vmUid)) {
//            // the destination host only experience 10% of the migrating VM's MIPS
//            System.out.println("HostPE-3-3");
//            totalRequestedMips *= 0.1;
//        }
//        if (getAvailableMips() >= totalRequestedMips) {
//            List<Double> mipsShareAllocated = new ArrayList<>();
//            for (Double mipsRequested : mipsShareRequestedCapped) {
//                if (getVmsMigratingOut().contains(vmUid)) {
//                    // performance degradation due to migration = 10% MIPS
//                    System.out.println("HostPE-3-6");
//                    mipsRequested *= 0.9;
//                } else if (getVmsMigratingIn().contains(vmUid)) {
//                    // the destination host only experience 10% of the migrating VM's MIPS
//                    System.out.println("HostPE-3-4");
//                    mipsRequested *= 0.1;
//                }
//                mipsShareAllocated.add(mipsRequested);
//            }
//            System.out.println("VmActuallyAllocatedMips:"+mipsShareAllocated);
//            getMipsMap().put(vmUid, mipsShareAllocated);
//            setAvailableMips(getAvailableMips() - totalRequestedMips);
//        } else {
//            redistributeMipsDueToOverSubscription();
//        }
//
//        mipsShareRequestedCapped.clear();
//        return true;
//    }
    //自己
    public boolean allocatePesForVm(String vmUid, List<Double> mipsShareRequested) {
        //mipsShareRequested: containerVm.getCurrentRequestedMips()
        System.out.println("HostPE-3-ContainerVmSchedulerTimeSharedOverSubscription.allocatePesForVm");
        double totalRequestedMips = 0;

        // if the requested mips is bigger than the capacity of a single PE, we cap[（可用或可借资金的）最高限额 ]
        // the request to the PE's capacity
        //List<Double> mipsShareRequestedCapped = new ArrayList<Double>();
        //getPeCapacity(Host): getPeList().get(0).getMips
        double peMips = getPeCapacity();
        //将mipsShareRequested根据getPeCapacity()转成mipsShareRequestedCapped
        //totalRequestedMips: 转成mipsShareRequestedCapped元素总和
        for (Double mips : mipsShareRequested) {
            //if (mips > peMips) {
//                System.out.println("HostPE-3-1");
//                mipsShareRequestedCapped.add(peMips);
//                totalRequestedMips += peMips;
//            } else {
//                mipsShareRequestedCapped.add(mips);
                totalRequestedMips += mips;
            }
        //}
        getMipsMapRequested().put(vmUid, mipsShareRequested);
        setPesInUse(getPesInUse() + mipsShareRequested.size());
        if (getVmsMigratingIn().contains(vmUid)) {
            // the destination host only experience 10% of the migrating VM's MIPS
            System.out.println("HostPE-3-3");
            totalRequestedMips *= 0.1;
        }else if (getVmsMigratingOut().contains(vmUid)){
            totalRequestedMips *= 0.9;
        }
        if (getAvailableMips() >= totalRequestedMips) {
            List<Double> mipsShareAllocated = new ArrayList<>();
            for (Double mipsRequested : mipsShareRequested) {
                if (getVmsMigratingOut().contains(vmUid)) {
                    // performance degradation due to migration = 10% MIPS
                    System.out.println("未使用-HostPE-3-6");
                    mipsRequested *= 0.9;
                } else if (getVmsMigratingIn().contains(vmUid)) {
                    // the destination host only experience 10% of the migrating VM's MIPS
                    System.out.println("未使用-HostPE-3-4");
                    mipsRequested *= 0.1;
                }
                mipsShareAllocated.add(mipsRequested);
            }
            //System.out.println("VmActuallyAllocatedMips:"+mipsShareAllocated);
            getMipsMap().put(vmUid, mipsShareAllocated);
            setAvailableMips(getAvailableMips() - totalRequestedMips);
        } else {
            redistributeMipsDueToOverSubscription();
        }

        //mipsShareRequestedCapped.clear();
        return true;
    }

    /**
     * This method recalculates distribution of MIPs among VMs considering eventual shortage of MIPS
     * compared to the amount requested by VMs.
     */
    protected void redistributeMipsDueToOverSubscription() {
        // First, we calculate the scaling factor - the MIPS allocation for all VMs will be scaled
        // proportionally
        System.out.println("HostPE-5-ContainerVmSchedulerTimeSharedOverSubscription.redistributeMipsDueToOverSubscription");
        double totalRequiredMipsByAllVms = 0;

        Map<String, List<Double>> mipsMapCapped = new HashMap<String, List<Double>>();
        for (Map.Entry<String, List<Double>> entry : getMipsMapRequested().entrySet()) {

            double requiredMipsByThisVm = 0.0;
            String vmId = entry.getKey();
            List<Double> mipsShareRequested = entry.getValue();
            List<Double> mipsShareRequestedCapped = new ArrayList<>();
            double peMips = getPeCapacity();
            for (Double mips : mipsShareRequested) {
                if (mips > peMips) {
                    mipsShareRequestedCapped.add(peMips);
                    requiredMipsByThisVm += peMips;
                } else {
                    mipsShareRequestedCapped.add(mips);
                    requiredMipsByThisVm += mips;
                }
            }

            mipsMapCapped.put(vmId, mipsShareRequestedCapped);

            if (getVmsMigratingIn().contains(entry.getKey())) {
                // the destination host only experience 10% of the migrating VM's MIPS
                requiredMipsByThisVm *= 0.1;
            }
            totalRequiredMipsByAllVms += requiredMipsByThisVm;
        }

        double totalAvailableMips = ContainerVmPeList.getTotalMips(getPeList());
        double scalingFactor = totalAvailableMips / totalRequiredMipsByAllVms;

        // Clear the old MIPS allocation
        getMipsMap().clear();

        // Update the actual MIPS allocated to the VMs
        for (Map.Entry<String, List<Double>> entry : mipsMapCapped.entrySet()) {
            String vmUid = entry.getKey();
            List<Double> requestedMips = entry.getValue();

            List<Double> updatedMipsAllocation = new ArrayList<>();
            for (Double mips : requestedMips) {
                if (getVmsMigratingOut().contains(vmUid)) {
                    // the original amount is scaled
                    mips *= scalingFactor;
                    // performance degradation due to migration = 10% MIPS
                    mips *= 0.9;
                } else if (getVmsMigratingIn().contains(vmUid)) {
                    // the destination host only experiences 10% of the migrating VM's MIPS
                    mips *= 0.1;
                    // the final 10% of the requested MIPS are scaled
                    mips *= scalingFactor;
                } else {
                    mips *= scalingFactor;
                }

                updatedMipsAllocation.add(Math.floor(mips));
            }

            // add in the new map
            getMipsMap().put(vmUid, updatedMipsAllocation);

        }

        mipsMapCapped.clear();

        // As the host is oversubscribed, there no more available MIPS
        setAvailableMips(0);
    }
}
