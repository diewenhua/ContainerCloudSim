package org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled;

import org.cloudbus.cloudsim.container.containerSelectionPolicies.PowerContainerSelectionPolicy;
import org.cloudbus.cloudsim.container.core.*;
import org.cloudbus.cloudsim.container.hostSelectionPolicies.HostSelectionPolicy;
import org.cloudbus.cloudsim.container.vmSelectionPolicies.PowerContainerVmSelectionPolicy;

import java.util.List;

/**
 * Created by sareh on 3/08/15.
 */
public class PowerContainerVmAllocationPolicyMigrationStaticThresholdMC extends PowerContainerVmAllocationPolicyMigrationAbstractContainerHostSelection {
//public class PowerContainerVmAllocationPolicyMigrationStaticThresholdMC extends PowerContainerVmAllocationPolicyMigrationAbstractContainerHostSelectionUnderUtilizedAdded {


    /**
     * The utilization threshold.
     */
    public static double overUtilizationThreshold = 0.9;
    private double underUtilizationThreshold = 0.9;

    /**
     * Instantiates a new power vm allocation policy migration mad.
     *
     * @param hostList             the host list
     * @param vmSelectionPolicy    the vm selection policy
     * @param overUtilizationThreshold the utilization threshold
     */
    public PowerContainerVmAllocationPolicyMigrationStaticThresholdMC(
            List<? extends ContainerHost> hostList,
            PowerContainerVmSelectionPolicy vmSelectionPolicy, PowerContainerSelectionPolicy containerSelectionPolicy,
            HostSelectionPolicy hostSelectionPolicy, double overUtilizationThreshold, double underUtilizationThreshold,
            int numberOfVmTypes, int[] vmPes, float[] vmRam, long vmBw, long vmSize, double[] vmMips) {
        super(hostList, vmSelectionPolicy, containerSelectionPolicy, hostSelectionPolicy,
        		 numberOfVmTypes, vmPes, vmRam, vmBw, vmSize, vmMips);
        setoverUtilizationThreshold(overUtilizationThreshold);
        setunderUtilizationThreshold(underUtilizationThreshold);
    }

    /**
     * Checks if is host over utilized.
     *
     * @param host the _host
     * @return true, if is host over utilized
     */
    @Override
//    原代码
//    protected boolean isHostOverUtilized(PowerContainerHost host) {
//        addHistoryEntry(host, getoverUtilizationThreshold());
//        double totalRequestedMips = 0;
//        for (ContainerVm vm : host.getVmList()) {
//            totalRequestedMips += vm.getCurrentRequestedTotalMips();
//        }
//        double utilization = totalRequestedMips / host.getTotalMips();
//        return utilization > getoverUtilizationThreshold();
//    }
    protected boolean isHostOverUtilized(PowerContainerHost host) {
        //System.out.println("VMmigration-7-1-1-PowerContainerVmAllocationPolicyMigrationStaticThresholdMC.isHostOverUtilized");
        addHistoryEntry(host, getoverUtilizationThreshold());
        double utilization1 = host.getUtilizationOfCpu();
        System.out.println("Host:"+host.getId()+"使用率为:"+utilization1);
        if(utilization1 > getoverUtilizationThreshold()){
            return true;
        }
        return false;
    }

    @Override
//    protected boolean isHostUnderUtilized(PowerContainerHost host) {
//        return false;
//    }
    protected boolean isHostUnderUtilized(PowerContainerHost host) {
        System.out.println("ContainerMigration-5-1-PowerContainerVmAllocationPolicyMigrationStaticThresholdMC.isHostUnderUtilized");
        addHistoryEntry(host, getunderUtilizationThreshold());
        double utilization = host.getUtilizationOfCpu();
        return utilization < getunderUtilizationThreshold();
    }

    /**
     * Sets the utilization threshold.
     *
     * @param utilizationThreshold the new utilization threshold
     */
    protected void setoverUtilizationThreshold(double utilizationThreshold) {
        this.overUtilizationThreshold = utilizationThreshold;
    }
    protected void setunderUtilizationThreshold(double utilizationThreshold) {
        this.underUtilizationThreshold = utilizationThreshold;
    }

    /**
     * Gets the utilization threshold.
     *
     * @return the utilization threshold
     */
    protected double getoverUtilizationThreshold() {
        return overUtilizationThreshold;
    }
    protected double getunderUtilizationThreshold() {
        return underUtilizationThreshold;
    }
    @Override
    public void setDatacenter(ContainerDatacenter datacenter) {
        super.setDatacenter(datacenter);
    }



}



