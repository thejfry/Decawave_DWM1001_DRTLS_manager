// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.listadapter;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import com.decawave.argomanager.ui.view.NodeStateView;
import java.lang.IllegalStateException;
import java.lang.Override;

public class NetworkOverviewNodeListAdapter$NetworkSummaryItemHolder_ViewBinding implements Unbinder {
  private NetworkOverviewNodeListAdapter.NetworkSummaryItemHolder target;

  @UiThread
  public NetworkOverviewNodeListAdapter$NetworkSummaryItemHolder_ViewBinding(NetworkOverviewNodeListAdapter.NetworkSummaryItemHolder target, View source) {
    this.target = target;

    target.networkName = Utils.findRequiredViewAsType(source, R.id.networkName, "field 'networkName'", TextView.class);
    target.numberOfAnchors = Utils.findRequiredViewAsType(source, R.id.infoNumberOfAnchors, "field 'numberOfAnchors'", TextView.class);
    target.numberOfTags = Utils.findRequiredViewAsType(source, R.id.infoNumberOfTags, "field 'numberOfTags'", TextView.class);
    target.networkId = Utils.findRequiredViewAsType(source, R.id.infoNetworkId, "field 'networkId'", TextView.class);
    target.tagPictogram = Utils.findRequiredViewAsType(source, R.id.tagPictogram, "field 'tagPictogram'", NodeStateView.class);
    target.anchorPictogram = Utils.findRequiredViewAsType(source, R.id.anchorPictogram, "field 'anchorPictogram'", NodeStateView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    NetworkOverviewNodeListAdapter.NetworkSummaryItemHolder target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.networkName = null;
    target.numberOfAnchors = null;
    target.numberOfTags = null;
    target.networkId = null;
    target.tagPictogram = null;
    target.anchorPictogram = null;
  }
}
