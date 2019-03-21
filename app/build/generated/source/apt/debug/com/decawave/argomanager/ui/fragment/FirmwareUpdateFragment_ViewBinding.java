// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.fragment;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class FirmwareUpdateFragment_ViewBinding implements Unbinder {
  private FirmwareUpdateFragment target;

  private View view2131624112;

  @UiThread
  public FirmwareUpdateFragment_ViewBinding(final FirmwareUpdateFragment target, View source) {
    this.target = target;

    View view;
    view = Utils.findRequiredView(source, R.id.updateButton, "field 'btnUpdate' and method 'onUpdateButtonClicked'");
    target.btnUpdate = Utils.castView(view, R.id.updateButton, "field 'btnUpdate'", Button.class);
    view2131624112 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.onUpdateButtonClicked();
      }
    });
    target.nodeList = Utils.findRequiredViewAsType(source, R.id.nodeList, "field 'nodeList'", RecyclerView.class);
    target.refreshLayout = Utils.findRequiredViewAsType(source, R.id.swipeRefreshLayout, "field 'refreshLayout'", SwipeRefreshLayout.class);
    target.noNodesView = Utils.findRequiredView(source, R.id.tvNoNodes, "field 'noNodesView'");
  }

  @Override
  @CallSuper
  public void unbind() {
    FirmwareUpdateFragment target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.btnUpdate = null;
    target.nodeList = null;
    target.refreshLayout = null;
    target.noNodesView = null;

    view2131624112.setOnClickListener(null);
    view2131624112 = null;
  }
}
