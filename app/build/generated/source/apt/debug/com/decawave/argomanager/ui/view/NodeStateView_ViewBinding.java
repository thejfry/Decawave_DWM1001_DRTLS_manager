// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.view;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.support.v4.content.ContextCompat;
import android.view.View;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import com.decawave.argomanager.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class NodeStateView_ViewBinding implements Unbinder {
  private NodeStateView target;

  private View viewSource;

  @UiThread
  public NodeStateView_ViewBinding(NodeStateView target) {
    this(target, target);
  }

  @UiThread
  public NodeStateView_ViewBinding(final NodeStateView target, View source) {
    this.target = target;

    viewSource = source;
    source.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.onClick();
      }
    });

    Context context = source.getContext();
    target.anchorTriangleColor = ContextCompat.getColor(context, R.color.anchor_color);
    target.exclamationMarkColor = ContextCompat.getColor(context, R.color.exclamation_mark_color);
    target.fixedColor = ContextCompat.getColor(context, R.color.anchor_color);
    target.passiveColor = ContextCompat.getColor(context, R.color.color_passive_node);
  }

  @Override
  @CallSuper
  public void unbind() {
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    target = null;


    viewSource.setOnClickListener(null);
    viewSource = null;
  }
}
