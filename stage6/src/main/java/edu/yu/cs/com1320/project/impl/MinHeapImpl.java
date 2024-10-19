package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.MinHeap;


public class MinHeapImpl<E extends Comparable<E>> extends MinHeap<E> {

    public MinHeapImpl() {
        // Initial size of the heap can be set based on expected usage
        this.elements = (E[]) new Comparable[10]; //size
    }

    @Override
    public void reHeapify(E element) {
        int index = getArrayIndex(element);
        if (index == -1) {
            return; // Element not found
        }
        upHeap(index);
        downHeap(index);
    }

    @Override
    protected int getArrayIndex(E element) {
        for (int i = 1; i <= this.count; i++) {
            if (this.elements[i].equals(element)) {
                return i;
            }
        }
        return -1; // Element not found
    }

    @Override
    protected void doubleArraySize() {
        E[] newArray = (E[]) new Comparable[this.elements.length * 2];
        System.arraycopy(this.elements, 0, newArray, 0, this.elements.length);
        this.elements = newArray;
    }

    public void remove(E element) {
        int index = getArrayIndex(element);
        if (index == -1) {
            return; // Element not found
        }

        // Swap the element with the last one and remove it
        swap(index, this.count);
        this.elements[this.count] = null;
        this.count--;

        // Reheapify the heap to maintain the min-heap property
        reHeapify(this.elements[index]);
    }

    public int size() {
        return this.count;
    }
}
