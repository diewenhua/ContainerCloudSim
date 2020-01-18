package org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.core.*;
import org.cloudbus.cloudsim.container.hostSelectionPolicies.HostSelectionPolicy;
import org.cloudbus.cloudsim.container.vmSelectionPolicies.PowerContainerVmSelectionPolicy;

import java.util.*;

/**
 * Created by sareh on 17/11/15.
 */
public class PowerContainerVmAllocationPolicyMigrationAbstractHostSelection extends PowerContainerVmAllocationPolicyMigrationAbstract {

    private HostSelectionPolicy hostSelectionPolicy;
    private double utilizationThreshold = 0.9;
    private double underUtilizationThreshold = 0.7;

    /**
     * Instantiates a new power vm allocation policy migration abstract.
     *
     * @param hostSelectionPolicy
     * @param hostList            the host list
     * @param vmSelectionPolicy   the vm selection policy
     */
    public PowerContainerVmAllocationPolicyMigrationAbstractHostSelection(List<? extends ContainerHost> hostList, PowerContainerVmSelectionPolicy vmSelectionPolicy, HostSelectionPolicy hostSelectionPolicy, double OlThreshold, double UlThreshold) {
        super(hostList, vmSelectionPolicy);
        setHostSelectionPolicy(hostSelectionPolicy);
        setUtilizationThreshold(OlThreshold);
        setUnderUtilizationThreshold(UlThreshold);
    }

    @Override
    /**
     * Find host for vm.
     *
     * @param vm            the vm
     * @param excludedHosts the excluded hosts
     * @return the power host
     */
    public PowerContainerHost findHostForVm(ContainerVm vm, Set<? extends ContainerHost> excludedHosts) {
        System.out.println("未使用-PowerContainerVmAllocationPolicyMigrationAbstractHostSelection.findHostForVm");
        PowerContainerHost allocatedHost = null;
        Boolean find = false;
        Set<ContainerHost> excludedHost1 = new HashSet<>();
        excludedHost1.addAll(excludedHosts);
        while (!find) {
            ArrayList<ContainerHost> hostList1=new ArrayList<ContainerHost>();
            hostList1.addAll(getContainerHostList());
            //Log.printLine("调用VMmigration-13-ContainerHostList.sortByAvailableMipsDescending");
            ContainerHostList.sortByAvailableMipsDescending(hostList1);
            Log.print("AvailableMips:");
            for (ContainerHost host1 : hostList1) {
                System.out.print(host1.getAvailableMips()+" ");
            }

            System.out.println("\n");
            ContainerHost host = getHostSelectionPolicy().getHost(hostList1, vm, excludedHost1);
            if (host == null) {
                return allocatedHost;
            }
            if (host.isSuitableForContainerVm(vm)) {
                find = true;
                allocatedHost = (PowerContainerHost) host;
            } else {
                excludedHost1.add(host);
                if (hostList1.size() == excludedHost1.size()) {
                    return null;
                }
            }

        }
        return allocatedHost;
    }


    public HostSelectionPolicy getHostSelectionPolicy() {
        return hostSelectionPolicy;
    }

    public void setHostSelectionPolicy(HostSelectionPolicy hostSelectionPolicy) {
        this.hostSelectionPolicy = hostSelectionPolicy;
    }


    /**
     * Checks if is host over utilized.
     *
     * @param host the _host
     * @return true, if is host over utilized
     */
    @Override
    protected boolean isHostOverUtilized(PowerContainerHost host) {
        addHistoryEntry(host, getUtilizationThreshold());
        double totalRequestedMips = 0;
        for (ContainerVm vm : host.getVmList()) {
            totalRequestedMips += vm.getCurrentRequestedTotalMips();
        }
        double utilization = totalRequestedMips / host.getTotalMips();
        return utilization > getUtilizationThreshold();
    }

    @Override
    protected boolean isHostUnderUtilized(PowerContainerHost host) {
        return false;
    }

    /**
     * Sets the utilization threshold.
     *
     * @param utilizationThreshold the new utilization threshold
     */
    protected void setUtilizationThreshold(double utilizationThreshold) {
        this.utilizationThreshold = utilizationThreshold;
    }

    /**
     * Gets the utilization threshold.
     *
     * @return the utilization threshold
     */
    protected double getUtilizationThreshold() {
        return utilizationThreshold;
    }

    public double getUnderUtilizationThreshold() {
        return underUtilizationThreshold;
    }

    public void setUnderUtilizationThreshold(double underUtilizationThreshold) {
        this.underUtilizationThreshold = underUtilizationThreshold;
    }


    @Override
    /**
     * Gets the under utilized host.
     *Checks if the utilization is under the threshold then counts it as underUtilized :)
     *返回低负荷主机的最大利用率的那个
     * @param excludedHosts the excluded hosts
     * @return the under utilized host
     */
    protected PowerContainerHost getUnderUtilizedHost(Set<? extends ContainerHost> excludedHosts) {

        System.out.println("VMmigration-7-2-4-PowerContainerVmAllocationPolicyMigrationAbstractHostSelection.getUnderUtilizedHost");
        //Log.printLine("调用同类getUnderUtilizedHostList");
        List<ContainerHost> underUtilizedHostList = getUnderUtilizedHostList(excludedHosts);
        if (underUtilizedHostList.size() == 0) {
            return null;
        }
        //System.out.println("VMmigration-7-2-6-调用VMmigration-12-sortByCpuUtilizationDescending");
        ContainerHostList.sortByCpuUtilizationDescending(underUtilizedHostList);
//        Log.print(String.format("The under Utilized Hosts are %d", underUtilizedHostList.size()));
        PowerContainerHost underUtilizedHost = (PowerContainerHost) underUtilizedHostList.get(0);

        return underUtilizedHost;
    }


    /**
     * Gets the under utilized host.
     *
     * @param excludedHosts the excluded hosts
     * @return the under utilized host
     */
    protected List<ContainerHost> getUnderUtilizedHostList(Set<? extends ContainerHost> excludedHosts) {
        System.out.println("VMmigration-7-2-5-底阈值-PowerContainerVmAllocationPolicyMigrationAbstractHostSelection.getUnderUtilizedHost");
        List<ContainerHost> underUtilizedHostList = new ArrayList<>();
        for (PowerContainerHost host : this.<PowerContainerHost>getContainerHostList()) {
            if (excludedHosts.contains(host)) {
                continue;
            }
            double utilization = host.getUtilizationOfCpu();
            if (!areAllVmsMigratingOutOrAnyVmMigratingIn(host) && utilization < getUnderUtilizationThreshold() && !areAllContainersMigratingOutOrAnyContainersMigratingIn(host)) {
                underUtilizedHostList.add(host);
            }
        }
        return underUtilizedHostList;
    }


    @Override
    public void setDatacenter(ContainerDatacenter datacenter) {
    }
    /**
     * 自己添加
     */

}
