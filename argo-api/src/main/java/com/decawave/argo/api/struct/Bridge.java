/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argo.api.struct;

import com.decawave.argo.api.interaction.Fail;

import java.util.List;

import rx.functions.Action1;

/**
 * Bridge functionality encapsulated.
 * Currently not used.
 */
public interface Bridge {

    List<AnchorNode> getAnchorProxies();

    void updateAnchorProxy(AnchorNode anchorNode, Action1<NetworkNode> onSuccess, Action1<Fail> onFail);

}
