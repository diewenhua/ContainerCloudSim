package org.cloudbus.cloudsim.container.lists;

import org.cloudbus.cloudsim.container.core.ContainerVm;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by sareh on 28/07/15.
 */
public class PowerContainerVmList extends ContainerVmList {

    /**
     * Sort by cpu utilization.
     *降序排列
     * @param vmList the vm list
     */
    public static <T extends ContainerVm> void sortByCpuUtilization(List<T> vmList) {
        System.out.println("VMmigration-10-0-根据虚拟机CPU利用率降序排列-PowerContainerVmList.sortByCpuUtilization");
        Collections.sort(vmList, new Comparator<T>() {

            @Override
            public int compare(T a, T b) throws ClassCastException {
                System.out.println("VMMIPS: "+a.getAvailableMips()+" "+a.getHost().getTotalMips() / a.getHost().getNumberOfPes() * a.getNumberOfPes()
                        +" "+a.getTotalUtilizationOfCpuMips(CloudSim.clock())
                +" "+a.getCurrentRequestedTotalMips()+" "+a.getTotalMips());
                Double aUtilization = a.getTotalUtilizationOfCpuMips(CloudSim.clock());
                Double bUtilization = b.getTotalUtilizationOfCpuMips(CloudSim.clock());
                return bUtilization.compareTo(aUtilization);
            }
        });
    }

    public static <T extends ContainerVm> void sortByAvailableMipsDecreasing(List <T> vmList){
        System.out.println("VMmigration-10-1-虚拟机PowerContainerVmList.sortByAvailableMipsDecreasing");
        Collections.sort(vmList, new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                Double aAvailableMips=a.getAvailableMips();
                Double bAvailableMips=b.getAvailableMips();
                return bAvailableMips.compareTo(aAvailableMips);
            }
        });
    }
    public static <T extends ContainerVm> void sortByAvailableMipsAscending(List <T> vmList){
        System.out.println("VMmigration-10-2-虚拟机PowerContainerVmList.sortByAvailableMipsAscending");
        Collections.sort(vmList, new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                Double aAvailableMips=a.getAvailableMips();
                Double bAvailableMips=b.getAvailableMips();
                return aAvailableMips.compareTo(bAvailableMips);
            }
        });
    }

    public static <T extends ContainerVm> void sortByCurrentRequestedTotalMipsDecreasing(List <T> vmList){
        System.out.println("VMmigration-10-3-虚拟机PowerContainerVmList.sortByCurrentRequestedTotalMipsDecreasing");
        Collections.sort(vmList, new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                Double aCurrentRequestedTotalMips=a.getCurrentRequestedTotalMips();
                Double bCurrentRequestedTotalMips=b.getCurrentRequestedTotalMips();
                return aCurrentRequestedTotalMips.compareTo(bCurrentRequestedTotalMips);
            }
        });
    }
}
