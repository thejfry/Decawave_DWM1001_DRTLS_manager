// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.listadapter;

import android.support.annotation.UiThread;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class DeviceErrorsListAdapter$DeviceErrorsHolder_ViewBinding extends DeviceErrorsListAdapter$ViewHolder_ViewBinding {
  private DeviceErrorsListAdapter.DeviceErrorsHolder target;

  @UiThread
  public DeviceErrorsListAdapter$DeviceErrorsHolder_ViewBinding(DeviceErrorsListAdapter.DeviceErrorsHolder target, View source) {
    super(target, source);

    this.target = target;

    target.tvBleAddress = Utils.findRequiredViewAsType(source, R.id.nodeBleAddress, "field 'tvBleAddress'", TextView.class);
    target.cardContent = Utils.findRequiredViewAsType(source, R.id.cardContent, "field 'cardContent'", LinearLayout.class);
  }

  @Override
  public void unbind() {
    DeviceErrorsListAdapter.DeviceErrorsHolder target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.tvBleAddress = null;
    target.cardContent = null;

    super.unbind();
  }
}
