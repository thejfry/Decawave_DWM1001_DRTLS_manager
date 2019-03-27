// Generated code from Butter Knife. Do not modify!
package com.decawave.argomanager.ui.listadapter;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.decawave.argomanager.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class FirmwareUpdateNodeListAdapter$FirmwareSummaryItemHolder_ViewBinding implements Unbinder {
  private FirmwareUpdateNodeListAdapter.FirmwareSummaryItemHolder target;

  @UiThread
  public FirmwareUpdateNodeListAdapter$FirmwareSummaryItemHolder_ViewBinding(FirmwareUpdateNodeListAdapter.FirmwareSummaryItemHolder target, View source) {
    this.target = target;

    target.tvFirmware1VersionChecksum = Utils.findRequiredViewAsType(source, R.id.tvFirmware1VersionAndChecksum, "field 'tvFirmware1VersionChecksum'", TextView.class);
    target.tvFirmware2VersionChecksum = Utils.findRequiredViewAsType(source, R.id.tvFirmware2VersionAndChecksum, "field 'tvFirmware2VersionChecksum'", TextView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    FirmwareUpdateNodeListAdapter.FirmwareSummaryItemHolder target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.tvFirmware1VersionChecksum = null;
    target.tvFirmware2VersionChecksum = null;
  }
}
