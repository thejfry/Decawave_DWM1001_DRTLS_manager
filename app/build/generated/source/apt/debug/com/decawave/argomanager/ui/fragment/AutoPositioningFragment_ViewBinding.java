// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.fragment;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class AutoPositioningFragment_ViewBinding implements Unbinder {
  private AutoPositioningFragment target;

  private View view2131624078;

  private View view2131624079;

  @UiThread
  public AutoPositioningFragment_ViewBinding(final AutoPositioningFragment target, View source) {
    this.target = target;

    View view;
    target.footerButtonBar = Utils.findRequiredView(source, R.id.footerButtonBar, "field 'footerButtonBar'");
    view = Utils.findRequiredView(source, R.id.measureButton, "field 'measureBtn' and method 'onMeasureButtonClicked'");
    target.measureBtn = Utils.castView(view, R.id.measureButton, "field 'measureBtn'", Button.class);
    view2131624078 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.onMeasureButtonClicked();
      }
    });
    view = Utils.findRequiredView(source, R.id.saveButton, "field 'saveBtn' and method 'onSaveButtonClicked'");
    target.saveBtn = Utils.castView(view, R.id.saveButton, "field 'saveBtn'", Button.class);
    view2131624079 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.onSaveButtonClicked();
      }
    });
    target.nodeList = Utils.findRequiredViewAsType(source, R.id.nodeList, "field 'nodeList'", RecyclerView.class);
    target.contentView = Utils.findRequiredView(source, R.id.contentView, "field 'contentView'");
    target.noNodesView = Utils.findRequiredView(source, R.id.tvNoNodes, "field 'noNodesView'");
  }

  @Override
  @CallSuper
  public void unbind() {
    AutoPositioningFragment target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.footerButtonBar = null;
    target.measureBtn = null;
    target.saveBtn = null;
    target.nodeList = null;
    target.contentView = null;
    target.noNodesView = null;

    view2131624078.setOnClickListener(null);
    view2131624078 = null;
    view2131624079.setOnClickListener(null);
    view2131624079 = null;
  }
}
