/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ext;

/**
 * Argo project.
 */

public class Holder<T> {

    public T value;

    public Holder(T value) {
        this.value = value;
    }

    public Holder() {
    }
}
