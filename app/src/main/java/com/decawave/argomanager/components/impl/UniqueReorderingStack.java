/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.impl;

import com.decawave.argomanager.Constants;
import com.google.common.base.Preconditions;

import java.util.LinkedList;

import javax.inject.Inject;

/**
 * Reordering stack holding unique elements.
 * Whenever an already existing is pushed at the top, the old instance in the stack is removed.
 */
public class UniqueReorderingStack<T> {
    private LinkedList<T> list;

    @Inject
    public UniqueReorderingStack() {
        list = new LinkedList<>();
    }

    /**
     * Adds a new element.
     * If the element is already present, it is moved at the top.
     * @param element to be added
     */
    public void pushOrMove(T element) {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(element);
        }
        // optimization - check if this is the last element
        if (!list.isEmpty() && list.getLast().equals(element)) {
            // nothing to do
            return;
        }
        // make sure that the element does not exist
        list.remove(element);
        // add at the very end
        list.add(element);
    }

    /**
     *
     * @return removes the element at the top
     */
    public T pop() {
        if (list.isEmpty()) return null;
        return list.removeLast();
    }

    public T peek() {
        if (list.isEmpty()) return null;
        return list.getLast();
    }

    public boolean remove(T element) {
        return list.remove(element);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }
}
