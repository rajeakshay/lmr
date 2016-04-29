package com.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to extend capabilities of Java Collections.
 * @author Akshay Raje
 */
public class CollectionUtils {
    /**
     * Returns consecutive sublists of a list, each of the same size (the last list may be smaller). The source list
     * is unmodified. Changes to source list DO NOT reflect in sublists. Sublists should be regenerated with a call to
     * CollectionUtils.partition method.
     * Examples:
     * Partitioning a list containing [a, b, c, d, e] with a partition size of 3 yields [[a, b, c], [d, e]].
     * Partitioning a list containing [a, b, c, d, e] with a partition size of 0 yields [a, b, c, d, e].
     * Partitioning a list containing [a, b, c, d, e] with a partition size of 5 yields [[a, b, c, d, e]].
     * @param list Source list to return consecutive lists from
     * @param size Desired size of each sublist (the last may be smaller)
     * @return a list of consecutive sublists
     * @throws IllegalArgumentException if size is negative
     */
    public static <T> List<List<T>> partition(List<T> list, int size){
        // Null list
        if(list == null) return null;
        // Empty list
        if(list.isEmpty()) return new ArrayList<List<T>>();
        if(size < 0) throw new IllegalArgumentException();
        // If partition size is equal to 0 OR list size is equal to partition size,
        // return the list containing itself.
        if((size == 0) || (list.size() == size)) {
            List<List<T>> foo = new ArrayList<List<T>>();
            foo.add(list);
            return foo;
        }
        List<List<T>> partitionedList = new ArrayList<List<T>>();
        int counter = 1;
        List<T> freshList = new ArrayList<T>();
        for(T elem : list){
            if(counter < size){
                freshList.add(elem);
                counter++;
            }
            else if(counter == size){
                freshList.add(elem);
                partitionedList.add(freshList);
                freshList = new ArrayList<T>();
                counter = 1;
            }
        }
        if(!freshList.isEmpty()) {
            partitionedList.add(freshList);
        }
        return partitionedList;
    }
}
