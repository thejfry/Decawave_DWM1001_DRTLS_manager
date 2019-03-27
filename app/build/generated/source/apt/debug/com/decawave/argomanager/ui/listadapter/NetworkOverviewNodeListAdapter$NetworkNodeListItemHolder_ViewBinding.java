// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.listadapter;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import com.decawave.argomanager.ui.view.NodeStateView;
import com.decawave.argomanager.ui.view.SignalStrengthView;
import java.lang.IllegalStateException;
import java.lang.Override;

public class NetworkOverviewNodeListAdapter$NetworkNodeListItemHolder_ViewBinding implements Unbinder {
  private NetworkOverviewNodeListAdapter.NetworkNodeListItemHolder target;

  @UiThread
  public NetworkOverviewNodeListAdapter$NetworkNodeListItemHolder_ViewBinding(NetworkOverviewNodeListAdapter.NetworkNodeListItemHolder target, View source) {
    this.target = target;

    target.nodeName = Utils.findRequiredViewAsType(source, R.id.nodeName, "field 'nodeName'", TextView.class);
    target.tvNodeBleAddress = Utils.findRequiredViewAsType(source, R.id.bleAddress, "field 'tvNodeBleAddress'", TextView.class);
    target.cardTop = Utils.findRequiredView(source, R.id.cardTop, "field 'cardTop'");
    target.cardTopSeparator = Utils.findRequiredView(source, R.id.cardTopSeparator, "field 'cardTopSeparator'");
    target.nodeSeparator = Utils.findRequiredView(source, R.id.bottomSeparator, "field 'nodeSeparator'");
    target.lastNodeSeparator = Utils.findRequiredView(source, R.id.lastNodeBottomSeparator, "field 'lastNodeSeparator'");
    target.nodeStateView = Utils.findRequiredViewAsType(source, R.id.nodeType, "field 'nodeStateView'", NodeStateView.class);
    target.signalStrengthView = Utils.findRequiredViewAsType(source, R.id.signalStrength, "field 'signalStrengthView'", SignalStrengthView.class);
    target.warningIcon = Utils.findRequiredViewAsType(source, R.id.warningIcon, "field 'warningIcon'", TextView.class);
    target.trackModeIcon = Utils.findRequiredViewAsType(source, R.id.trackModeIcon, "field 'trackModeIcon'", ImageView.class);
    target.locateIcon = Utils.findRequiredViewAsType(source, R.id.locateIcon, "field 'locateIcon'", ImageView.class);
    target.editIcon = Utils.findRequiredViewAsType(source, R.id.editIcon, "field 'editIcon'", ImageView.class);
    target.detailsTable = Utils.findRequiredViewAsType(source, R.id.detailsTable, "field 'detailsTable'", TableLayout.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    NetworkOverviewNodeListAdapter.NetworkNodeListItemHolder target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.nodeName = null;
    target.tvNodeBleAddress = null;
    target.cardTop = null;
    target.cardTopSeparator = null;
    target.nodeSeparator = null;
    target.lastNodeSeparator = null;
    target.nodeStateView = null;
    target.signalStrengthView = null;
    target.warningIcon = null;
    target.trackModeIcon = null;
    target.locateIcon = null;
    target.editIcon = null;
    target.detailsTable = null;
  }
}
