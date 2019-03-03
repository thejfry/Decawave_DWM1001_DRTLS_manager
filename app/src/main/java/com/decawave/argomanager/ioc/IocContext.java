/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ioc;

/**
 * Argo project.
 */

public class IocContext {

    public static ArgoComponent daCtx;


    public static void init() {
        // build the component
        daCtx = DaggerArgoComponent.create();
    }

}
