// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.view;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.support.v4.content.ContextCompat;
import android.view.View;
import butterknife.Unbinder;
import com.decawave.argomanager.R;
import java.lang.Deprecated;
import java.lang.Override;

public class SimpleProgressView_ViewBinding implements Unbinder {
  @UiThread
  public SimpleProgressView_ViewBinding(SimpleProgressView target) {
    this(target, target.getContext());
  }

  /**
   * @deprecated Use {@link #SimpleProgressView_ViewBinding(SimpleProgressView, Context)} for direct creation.
   *     Only present for runtime invocation through {@code ButterKnife.bind()}.
   */
  @Deprecated
  @UiThread
  public SimpleProgressView_ViewBinding(SimpleProgressView target, View source) {
    this(target, source.getContext());
  }

  @UiThread
  public SimpleProgressView_ViewBinding(SimpleProgressView target, Context context) {
    target.barColor = ContextCompat.getColor(context, R.color.mtrl_primary);
  }

  @Override
  @CallSuper
  public void unbind() {
  }
}
