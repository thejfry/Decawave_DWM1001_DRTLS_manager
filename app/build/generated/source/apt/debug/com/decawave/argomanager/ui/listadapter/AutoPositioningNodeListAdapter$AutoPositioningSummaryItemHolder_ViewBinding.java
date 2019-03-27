// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.listadapter;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class AutoPositioningNodeListAdapter$AutoPositioningSummaryItemHolder_ViewBinding implements Unbinder {
  private AutoPositioningNodeListAdapter.AutoPositioningSummaryItemHolder target;

  private View view2131624142;

  private View view2131624143;

  @UiThread
  public AutoPositioningNodeListAdapter$AutoPositioningSummaryItemHolder_ViewBinding(final AutoPositioningNodeListAdapter.AutoPositioningSummaryItemHolder target, View source) {
    this.target = target;

    View view;
    target.tvLegend = Utils.findRequiredViewAsType(source, R.id.tvLegend, "field 'tvLegend'", TextView.class);
    target.buttonContainer = Utils.findRequiredViewAsType(source, R.id.actionButtonContainer, "field 'buttonContainer'", LinearLayout.class);
    view = Utils.findRequiredView(source, R.id.btnPreview, "method 'onPreviewClicked'");
    view2131624142 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.onPreviewClicked();
      }
    });
    view = Utils.findRequiredView(source, R.id.btnSetupZaxis, "method 'onSetupZaxisClicked'");
    view2131624143 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.onSetupZaxisClicked();
      }
    });
  }

  @Override
  @CallSuper
  public void unbind() {
    AutoPositioningNodeListAdapter.AutoPositioningSummaryItemHolder target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.tvLegend = null;
    target.buttonContainer = null;

    view2131624142.setOnClickListener(null);
    view2131624142 = null;
    view2131624143.setOnClickListener(null);
    view2131624143 = null;
  }
}
