package org.cloudbus.cloudsim.container.lists;

import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by sareh on 31/07/15.
 */
public class PowerContainerList {


    /**
     * Sort by cpu utilization.
     *
     * @param containerList the vm list
     */
    public static <T extends Container> void sortByCpuUtilization(List<T> containerList) {
        System.out.println("Containerallocation-2-0-PowerContainerList.sortByCpuUtilization");
        Collections.sort(containerList, new Comparator<T>() {

            @Override
            public int compare(T a, T b) throws ClassCastException {
                /**
                 * @see org.cloudbus.cloudsim.container.core.Container#getTotalUtilizationOfCpuMips
                 */
                //a.getTotalUtilizationOfCpuMips(CloudSim.clock())=a.getCurrentRequestedTotalMips()
                System.out.println("TotalUtilizationOfCpuMips "+a.getTotalUtilizationOfCpuMips(CloudSim.clock())+" "+a.getCurrentRequestedTotalMips());
                Double aUtilization = a.getTotalUtilizationOfCpuMips(CloudSim.clock());
                Double bUtilization = b.getTotalUtilizationOfCpuMips(CloudSim.clock());
                return bUtilization.compareTo(aUtilization);
            }
        });
    }

    public static <T extends Container> void sortByCurrentRequestedTotalMipsDecreasing(List<T> containerList){
        System.out.println("Containerallocation-2-1-容器PowerContainerList.sortByCurrentRequestedTotalMips");
        Collections.sort(containerList, new Comparator<T>() {
            @Override
            public int compare(T a, T b) throws ClassCastException {
                Double aCurrentRequestedTotalMips=a.getCurrentRequestedTotalMips();
                Double bCurrentRequestedTotalMips=b.getCurrentRequestedTotalMips();
                return bCurrentRequestedTotalMips.compareTo(aCurrentRequestedTotalMips);
            }
        });
    }

    public static <T extends Container> void sortByWorkloadTotalMipsDecreasing(List<T> containerList){
        System.out.println("Containerallocation-2-1-容器PowerContainerList.sortByCurrentRequestedTotalMips");
        Collections.sort(containerList, new Comparator<T>() {
            @Override
            public int compare(T a, T b) throws ClassCastException {
                Double aWorkloadTotalMips=a.getWorkloadTotalMips();
                Double bWorkloadTotalMips=b.getWorkloadTotalMips();
                return bWorkloadTotalMips.compareTo(aWorkloadTotalMips);
            }
        });
    }

}
