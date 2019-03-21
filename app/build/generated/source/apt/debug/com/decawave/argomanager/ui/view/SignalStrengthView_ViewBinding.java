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

public class SignalStrengthView_ViewBinding implements Unbinder {
  @UiThread
  public SignalStrengthView_ViewBinding(SignalStrengthView target) {
    this(target, target.getContext());
  }

  /**
   * @deprecated Use {@link #SignalStrengthView_ViewBinding(SignalStrengthView, Context)} for direct creation.
   *     Only present for runtime invocation through {@code ButterKnife.bind()}.
   */
  @Deprecated
  @UiThread
  public SignalStrengthView_ViewBinding(SignalStrengthView target, View source) {
    this(target, source.getContext());
  }

  @UiThread
  public SignalStrengthView_ViewBinding(SignalStrengthView target, Context context) {
    target.colorActive = ContextCompat.getColor(context, R.color.signal_bar_active);
    target.colorActiveObsolete = ContextCompat.getColor(context, R.color.signal_bar_active_obsolete);
    target.colorInactive = ContextCompat.getColor(context, R.color.signal_bar_inactive);
  }

  @Override
  @CallSuper
  public void unbind() {
  }
}
