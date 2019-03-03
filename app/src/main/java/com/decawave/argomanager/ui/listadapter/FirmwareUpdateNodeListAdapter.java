/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.listadapter;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.annimon.stream.function.Consumer;
import com.annimon.stream.function.Supplier;
import com.decawave.argo.api.struct.FirmwareMeta;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argo.api.struct.NetworkNodeProperty;
import com.decawave.argo.api.struct.OperatingFirmware;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.R;
import com.decawave.argomanager.components.BlePresenceApi;
import com.decawave.argomanager.components.struct.NetworkNodeEnhanced;
import com.decawave.argomanager.runner.FirmwareUpdateRunner;
import com.decawave.argomanager.ui.MainActivity;
import com.decawave.argomanager.ui.fragment.DeviceDebugConsoleFragment;
import com.decawave.argomanager.ui.fragment.FragmentType;
import com.decawave.argomanager.ui.view.NodeStateView;
import com.decawave.argomanager.ui.view.SimpleProgressView;
import com.decawave.argomanager.util.NetworkNodePropertyDecorator;
import com.decawave.argomanager.util.Util;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import eu.kryl.android.common.Pair;
import eu.kryl.android.common.log.ComponentLog;

import static com.decawave.argomanager.ArgoApp.daApp;

/**
 *
 */
public class FirmwareUpdateNodeListAdapter extends RecyclerView.Adapter<FirmwareUpdateNodeListAdapter.ViewHolder> {
    private static final ComponentLog log = new ComponentLog(FirmwareUpdateNodeListAdapter.class);

    private static final String BK_SELECTED_ITEMS = "BK_SELECTED_ITEMS";
    //
    private final MainActivity mainActivity;
    private final BlePresenceApi presenceApi;
    private final Supplier<FirmwareUpdateRunner> fwUpdateRunnerProvider;
    private final NetworkNodePropertyDecorator.DecoratedProperty fw1VersionPty;
    private final NetworkNodePropertyDecorator.DecoratedProperty fw2VersionPty;
    private final NetworkNodePropertyDecorator.DecoratedProperty fw1ChecksumPty;
    private final NetworkNodePropertyDecorator.DecoratedProperty fw2ChecksumPty;

    private Set<Long> checkedNodeIds;
    private FirmwareMeta firmware1Meta;
    private FirmwareMeta firmware2Meta;
    private Consumer<Set<Long>> checkedChangedListener;
    private List<NetworkNodeEnhanced> nodes = new ArrayList<>();

    private int cardBackgroundColor;        // white...
    private int cardBottomSeparatorColor;   // gray...
    private int errorTextColor;
    private int normalTextColor;
    // item types
    private static final int ITEM_TYPE_SUMMARY = 0;
    private static final int ITEM_TYPE_NODE = 1;

    public FirmwareUpdateNodeListAdapter(@NotNull Collection<NetworkNodeEnhanced> networkNodes,
                                         @NotNull FirmwareMeta firmware1Meta,
                                         @NotNull FirmwareMeta firmware2Meta,
                                         @NotNull NetworkNodePropertyDecorator propertyDecorator,
                                         @NotNull MainActivity mainActivity,
                                         @NotNull BlePresenceApi presenceApi,
                                         @NotNull Supplier<FirmwareUpdateRunner> fwUpdateRunnerProvider,
                                        @NotNull Consumer<Set<Long>> checkedChangedListener) {
        this.mainActivity = mainActivity;
        this.fw1ChecksumPty = propertyDecorator.decorate(NetworkNodeProperty.FW1_CHECKSUM);
        this.fw2ChecksumPty = propertyDecorator.decorate(NetworkNodeProperty.FW2_CHECKSUM);
        this.fw1VersionPty = propertyDecorator.decorate(NetworkNodeProperty.FW1_VERSION);
        this.fw2VersionPty = propertyDecorator.decorate(NetworkNodeProperty.FW2_VERSION);
        this.presenceApi = presenceApi;
        this.fwUpdateRunnerProvider = fwUpdateRunnerProvider;
        this.nodes = getSortedNetworkNodes(networkNodes);
        this.firmware1Meta = firmware1Meta;
        this.firmware2Meta = firmware2Meta;
        this.checkedChangedListener = checkedChangedListener;
        this.checkedNodeIds = new HashSet<>();
        //
        this.cardBackgroundColor = ContextCompat.getColor(daApp, R.color.cardview_light_background);
        this.cardBottomSeparatorColor = ContextCompat.getColor(daApp, R.color.color_list_item_separator_light);
        this.errorTextColor = ContextCompat.getColor(daApp, R.color.mtrl_primary);
        this.normalTextColor = ContextCompat.getColor(daApp, R.color.secondary_text_default_material_light);
    }

    @NonNull
    private List<NetworkNodeEnhanced> getSortedNetworkNodes(Collection<NetworkNodeEnhanced> nodes) {
        LinkedList<NetworkNodeEnhanced> lst = new LinkedList<>(nodes);
        // sort nodes by id
        Collections.sort(lst, (n1, n2) -> {
            boolean n1i = Util.isRealInitiator(n1.asPlainNode());
            boolean n2i = Util.isRealInitiator(n2.asPlainNode());
            if (n1i == n2i) {
                return Constants.NETWORK_NODE_COMPARATOR.compare(n1, n2);
            }
            return n1i ? -1 : 1;
        });
        return lst;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case ITEM_TYPE_SUMMARY:
                View view = inflater.inflate(R.layout.li_firmware_summary, parent, false);
                // wrap with view holder
                return new FirmwareSummaryItemHolder(view);
            case ITEM_TYPE_NODE:
                view = inflater.inflate(R.layout.li_firmware_node_item, parent, false);
                // wrap with view holder
                return new FwNodeListItemHolder(view);
            default:
                throw new IllegalStateException("unsupported item view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (holder instanceof FirmwareSummaryItemHolder) {
            ((FirmwareSummaryItemHolder) holder).bind(firmware1Meta, firmware2Meta);
        } else if (holder instanceof FwNodeListItemHolder) {
            int p = position - 1;
            FwNodeListItemHolder h = (FwNodeListItemHolder) holder;
            h.bind(nodes.get(p).asPlainNode(), p == 0, p == nodes.size() - 1);
        } // else: no need to bind
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return ITEM_TYPE_SUMMARY;
        } else {
            return ITEM_TYPE_NODE;
        }
    }

    @Override
    public int getItemCount() {
        return nodes.size() + 1;
    }

    public void removeNode(long id) {
        // find position where the node has been removed
        Pair<NetworkNodeEnhanced, ListIterator<NetworkNodeEnhanced>> nodeIt = findNodeById(id);
        if (nodeIt != null) {
            ListIterator<NetworkNodeEnhanced> it = nodeIt.second;
            int idx = getViewIndexForNodeIterator(it);
            int firstNetworkNodeIndex = 1;
            int lastNetworkNodeIndex = getLastNetworkNodeIndex();
            it.remove();
            notifyItemRemoved(idx);
            if (idx == firstNetworkNodeIndex) {
                notifyItemChanged(idx);
            } else if (idx == lastNetworkNodeIndex) {
                notifyItemChanged(idx - 1);
            }
        }
    }

    public void addNode(NetworkNodeEnhanced node) {
        // find position where to add the node
        ListIterator<NetworkNodeEnhanced> it = nodes.listIterator();
        int i = 0;
        while (it.hasNext()) {
            NetworkNodeEnhanced n = it.next();
            if (Constants.NETWORK_NODE_COMPARATOR.compare(n, node) > 0) {
                // stop here, move back
                it.previous();
                break;
            }
            i++;
        }
        //
        int idx = getViewIndexForNodeIterator(it);
        int firstNetworkNodeIndex = 1;
        int lastNetworkNodeIndex = getLastNetworkNodeIndex();
        // insert
        it.add(node);
        // notify the adapter
        notifyItemInserted(i + firstNetworkNodeIndex);
        //
        if (i == 0 && it.hasNext()) {
            // the item was inserted before the first (now second) element
            // such element needs to be redrawn as well
            notifyItemChanged(firstNetworkNodeIndex + 1);
        } else if (idx == lastNetworkNodeIndex && nodes.size() > 1) {
            // the item was inserted after the last element
            // such element needs to be redrawn as well
            notifyItemChanged(firstNetworkNodeIndex + i - 1);
        }
    }

    public void onFwUpdateOverallStatusChanged(FirmwareUpdateRunner.OverallStatus overallStatus) {
        switch (overallStatus) {
            case NOT_STARTED:
                throw new IllegalStateException("how comes?");
            case UPDATING:
                // update the selected nodes and remove the unselected or missing nodes
                int idx = 1;    // we are starting from idx 1 (idx 0 is summary)
                Iterator<NetworkNodeEnhanced> it = nodes.iterator();
                while (it.hasNext()) {
                    NetworkNodeEnhanced node = it.next();
                    if (fwUpdateRunnerProvider.get().getNodeUpdateStatus(node.getId()) != null) {
                        // update is being performed upon
                        notifyItemChanged(idx);
                        idx++;
                    } else {
                        it.remove();
                        notifyItemRemoved(idx);
                        // keep the idx
                    }
                }
                break;
            case FINISHED:
            case TERMINATED:
                // nothing to change
                break;
        }
    }

    public void updateNode(NetworkNodeEnhanced node) {
        // find the position of the affected node
        Pair<NetworkNodeEnhanced, ListIterator<NetworkNodeEnhanced>> nodeIt = findNodeById(node.getId());
        if (nodeIt != null) {
            nodeIt.second.set(node);
            int idx = getViewIndexForNodeIterator(nodeIt.second);
            notifyItemChanged(idx);
        }
    }

    private int getViewIndexForNodeIterator(@NotNull ListIterator<NetworkNodeEnhanced> listIterator) {
        return 1 + listIterator.previousIndex();
    }

    private int getLastNetworkNodeIndex() {
        return nodes.size();
    }

    /**
     * Unfortunately we are not able to return iterator which keep the next() returned value.
     * Actually we are, but this wouldn't be very nice.
     *
     * @param id identifies node
     */
    private Pair<NetworkNodeEnhanced, ListIterator<NetworkNodeEnhanced>> findNodeById(long id) {
        ListIterator<NetworkNodeEnhanced> it = nodes.listIterator();
        //noinspection WhileLoopReplaceableByForEach
        while (it.hasNext()) {
            NetworkNodeEnhanced n = it.next();
            if (n.getId() == id) {
                // stop here
                return new Pair<>(n, it);
            }
        }
        return null;
    }

    private Pair<NetworkNodeEnhanced, ListIterator<NetworkNodeEnhanced>> findNodeByBleAddress(String bleAddress) {
        ListIterator<NetworkNodeEnhanced> it = nodes.listIterator();
        //noinspection WhileLoopReplaceableByForEach
        while (it.hasNext()) {
            NetworkNodeEnhanced n = it.next();
            if (n.getBleAddress().equals(bleAddress)) {
                // stop here
                return new Pair<>(n, it);
            }
        }
        return null;
    }

    private Integer getNodePosition(String nodeBleAddress) {
        Pair<NetworkNodeEnhanced, ListIterator<NetworkNodeEnhanced>> nodeIt = findNodeByBleAddress(nodeBleAddress);
        if (nodeIt != null) {
            return getViewIndexForNodeIterator(nodeIt.second);
        }
        return null;
    }

    public void onNodePresenceChanged(String bleAddress, boolean present) {
        // let us simply redraw the whole item
        notifyItemChanged(bleAddress);
    }

    private void notifyItemChanged(String bleAddress) {
        Integer pos = getNodePosition(bleAddress);
        if (pos != null) {
            notifyItemChanged(pos);
        }
    }

    private void onNodeChecked(Long nodeId, boolean checked) {
        if (checked) {
            // the item could not be removed, we will add it
            checkedNodeIds.add(nodeId);
        } else {
            checkedNodeIds.remove(nodeId);
        }
        if (checkedChangedListener != null) {
            checkedChangedListener.accept(checkedNodeIds);
        }
    }

    public void onFwUpdateNodeStatusChanged(String bleAddress) {
        if (Constants.DEBUG)
            log.d("onFwUpdateNodeStatusChanged() called with: " + "bleAddress = [" + bleAddress + "]");
        notifyItemChanged(bleAddress);
    }

    public void onUploadProgressChanged(String bleAddress, int bytes) {
        // do it simply...
        notifyItemChanged(bleAddress);
    }

    abstract class ViewHolder extends RecyclerView.ViewHolder {

        ViewHolder(View itemView) {
            super(itemView);
        }
    }

    class FwNodeListItemHolder extends ViewHolder {
        // references to views
        @BindView(R.id.nodeCheckbox)
        CheckBox nodeCheckbox;
        @BindView(R.id.nodeType)
        NodeStateView nodeTypeView;
        @BindView(R.id.nodeName)
        TextView nodeName;
        @BindView(R.id.bleAddress)
        TextView tvNodeBleAddress;
        @BindView(R.id.tvFirmware1VersionAndChecksum)
        TextView tvFirmware1VersionChecksum;
        @BindView(R.id.tvFirmware2VersionAndChecksum)
        TextView tvFirmware2VersionChecksum;
        @BindView(R.id.uploadProgress)
        View uploadProgressContainer;
        @BindView(R.id.uploadFwType)
        TextView tvUploadFwType;
        @BindView(R.id.uploadPercentage)
        TextView tvUploadPercentage;
        @BindView(R.id.cardContent)
        View cardContent;
        @BindView(R.id.cardTop)
        View cardTop;
        @BindView(R.id.progressView)
        SimpleProgressView progressViewSeparator;
        @BindView(R.id.lastNodeBottomSeparator)
        View lastNodeSeparator;

        // identification of the network node
        long nodeId;
        String nodeBle;

        FwNodeListItemHolder(View itemView) {
            super(itemView);
            // extract references
            ButterKnife.bind(this,itemView);
            // set onclick listener
            itemView.findViewById(R.id.cardContent).setOnClickListener(view -> {
                FirmwareUpdateRunner.NodeUpdateStatus status = fwUpdateRunnerProvider.get().getNodeUpdateStatus(nodeId);
                if (status == FirmwareUpdateRunner.NodeUpdateStatus.FAIL) {
                    // we will show BLE log
                    mainActivity.showFragment(FragmentType.DEVICE_DEBUG_CONSOLE, DeviceDebugConsoleFragment.getArgsForDevice(nodeBle));
                } else {
                    // toggle
                    nodeCheckbox.setChecked(!nodeCheckbox.isChecked());
                }
            });
            nodeCheckbox.setOnCheckedChangeListener((button, isChecked) -> onNodeChecked(nodeId, isChecked));
        }

        void bind(NetworkNode networkNode, boolean firstNode, boolean lastNode) {
            // bind the network node first
            // fill simple variables first
            nodeId = networkNode.getId();
            nodeBle = networkNode.getBleAddress();
            //
            itemView.setTag(nodeBle);
            // fill UI elements next
            nodeName.setText(networkNode.getLabel());
            tvNodeBleAddress.setText(networkNode.getBleAddress());
            // set up firmware1 information
            tvFirmware1VersionChecksum.setText(daApp.getString(R.string.fw_update_fw1version_checksum_short,
                    fw1VersionPty.formatValue(networkNode.getFw1Version()), fw1ChecksumPty.formatValue(networkNode.getFw1Checksum())));
            tvFirmware2VersionChecksum.setText(daApp.getString(R.string.fw_update_fw2version_checksum_short,
                    fw2VersionPty.formatValue(networkNode.getFw2Version()), fw2ChecksumPty.formatValue(networkNode.getFw2Checksum())));
            // make the texts bold if they do not match with builtin FW
            boolean boldFw1 = firmware1Meta.firmwareVersion != networkNode.getFw1Version() || firmware1Meta.firmwareChecksum != networkNode.getFw1Checksum();
            boolean boldFw2 = firmware2Meta.firmwareVersion != networkNode.getFw2Version() || firmware2Meta.firmwareChecksum != networkNode.getFw2Checksum();
            makeBoldTextView(tvFirmware1VersionChecksum, boldFw1);
            makeBoldTextView(tvFirmware2VersionChecksum, boldFw2);
            // and top/bottom margins
            cardTop.setVisibility(firstNode ? View.VISIBLE : View.GONE);
            if (lastNode) {
                lastNodeSeparator.setVisibility(View.VISIBLE);
                // do not hide progress separator, just set a different background color
                progressViewSeparator.setBackgroundColor(cardBackgroundColor);
            } else {
                lastNodeSeparator.setVisibility(View.GONE);
                progressViewSeparator.setBackgroundColor(cardBottomSeparatorColor);
            }
            // adjust enabled/disabled/clickable/non-clickable state
            boolean fwUpdateNotStarted = fwUpdateRunnerProvider.get().getOverallStatus() == FirmwareUpdateRunner.OverallStatus.NOT_STARTED;
            // is the node checked?
            nodeCheckbox.setChecked(checkedNodeIds.contains(nodeId));
            if (presenceApi.isNodePresent(networkNode.getBleAddress())) {
                // the node is present
                cardContent.setEnabled(true);
                FirmwareUpdateRunner.NodeUpdateStatus status = fwUpdateRunnerProvider.get().getNodeUpdateStatus(nodeId);
                cardContent.setClickable(fwUpdateNotStarted || status == FirmwareUpdateRunner.NodeUpdateStatus.FAIL);
            } else {
                // enabled is about visual representation (colors)
                cardContent.setEnabled(false);
                // clickable is about actions
                cardContent.setClickable(false);
                // set checkbox status
                nodeCheckbox.setClickable(false);
            }
            // adjust visibility based on overall status
            nodeTypeView.setNetworkNode(networkNode);
            nodeTypeView.setState(NodeStateView.State.SHOW_NODE_TYPE, false);
            nodeTypeView.setVisibility(fwUpdateNotStarted ? View.GONE : View.VISIBLE);
            nodeCheckbox.setVisibility(fwUpdateNotStarted ? View.VISIBLE : View.GONE);
            tvFirmware1VersionChecksum.setVisibility(fwUpdateNotStarted ? View.VISIBLE : View.GONE);
            tvFirmware2VersionChecksum.setVisibility(fwUpdateNotStarted ? View.VISIBLE : View.GONE);
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // upload progress view separator
            FirmwareUpdateRunner.NodeUpdateStatus status = fwUpdateRunnerProvider.get().getNodeUpdateStatus(nodeId);
            log.d("drawing UI for node update status: " + status);
            // this is the default
            tvUploadFwType.setVisibility(View.GONE);
            if (status == null) {
                progressViewSeparator.makeInactive();
                uploadProgressContainer.setVisibility(View.GONE);
            } else {
                switch (status) {
                    case INITIATING:
                        progressViewSeparator.makeIndeterminate();
                        uploadProgressContainer.setVisibility(View.GONE);
                        break;
                    case UPLOADING_FW1:
                        setViewUploadingFw(firmware1Meta, OperatingFirmware.FW1);
                        break;
                    case UPLOADING_FW2:
                        setViewUploadingFw(firmware2Meta, OperatingFirmware.FW2);
                        break;
                    case RESTORING_INITIAL_STATE:
                        makeBoldTextView(tvUploadPercentage, true);
                        uploadProgressContainer.setVisibility(View.VISIBLE);
                        tvUploadPercentage.setText(daApp.getString(R.string.fw_upload_restoring_initial_state));
                        tvUploadPercentage.setTextColor(normalTextColor);
                        progressViewSeparator.makeIndeterminate();
                        break;
                    case SUCCESS:
                        makeBoldTextView(tvUploadPercentage, true);
                        uploadProgressContainer.setVisibility(View.VISIBLE);
                        tvUploadPercentage.setText(daApp.getString(R.string.fw_upload_success));
                        tvUploadPercentage.setTextColor(normalTextColor);
                        progressViewSeparator.makeInactive();
                        break;
                    case FAIL:
                        uploadProgressContainer.setVisibility(View.VISIBLE);
                        makeBoldTextView(tvUploadPercentage, true);
                        tvUploadPercentage.setText(daApp.getString(R.string.fw_upload_fail));
                        tvUploadPercentage.setTextColor(errorTextColor);
                        progressViewSeparator.makeInactive();
                        break;
                    case SKIPPED_UP_TO_DATE:
                        makeBoldTextView(tvUploadPercentage, true);
                        uploadProgressContainer.setVisibility(View.VISIBLE);
                        tvUploadPercentage.setText(daApp.getString(R.string.fw_upload_skipped));
                        tvUploadPercentage.setTextColor(normalTextColor);
                        progressViewSeparator.makeInactive();
                        break;
                    case CANCELLED:
                        uploadProgressContainer.setVisibility(View.VISIBLE);
                        makeBoldTextView(tvUploadPercentage, true);
                        tvUploadPercentage.setText(daApp.getString(R.string.fw_upload_cancelled));
                        tvUploadPercentage.setTextColor(normalTextColor);
                        progressViewSeparator.makeInactive();
                        break;
                    case PENDING:
                        uploadProgressContainer.setVisibility(View.GONE);
                        progressViewSeparator.makeInactive();
                        break;
                }
            }
        }

        private void setViewUploadingFw(FirmwareMeta firmwareMeta, OperatingFirmware firmwareType) {
            Integer bytesSent = fwUpdateRunnerProvider.get().getUploadByteCounter(nodeId);
            progressViewSeparator.setMaxValue(firmwareMeta.size);
            progressViewSeparator.setCurrValue(bytesSent);
            uploadProgressContainer.setVisibility(View.VISIBLE);
            tvUploadFwType.setVisibility(View.VISIBLE);
            tvUploadFwType.setText(firmwareType == OperatingFirmware.FW1 ? R.string.fw1 : R.string.fw2);
            makeBoldTextView(tvUploadPercentage, false);
            tvUploadPercentage.setTextColor(normalTextColor);
            tvUploadPercentage.setText(daApp.getString(R.string.fw_upload_percentage, 100 * bytesSent / firmwareMeta.size));
        }

        private void makeBoldTextView(TextView textView, boolean bold) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                textView.setTextAppearance(bold ? R.style.NodeTitleInNodeList : R.style.UnreachableNodeTitleInNodeList);
            } else {
                //noinspection deprecation
                textView.setTextAppearance(mainActivity,
                        bold ? R.style.NodeTitleInNodeList : R.style.UnreachableNodeTitleInNodeList);
            }
        }

    }

    class FirmwareSummaryItemHolder extends ViewHolder {
        // references to views
        @BindView(R.id.tvFirmware1VersionAndChecksum)
        TextView tvFirmware1VersionChecksum;
        @BindView(R.id.tvFirmware2VersionAndChecksum)
        TextView tvFirmware2VersionChecksum;
        //
        FirmwareMeta firmware1;
        FirmwareMeta firmware2;


        FirmwareSummaryItemHolder(View itemView) {
            super(itemView);
            // extract references
            ButterKnife.bind(this, itemView);
        }

        void bind(FirmwareMeta firmware1Meta, FirmwareMeta firmware2Meta) {
            this.firmware1 = firmware1Meta;
            this.firmware2 = firmware2Meta;
            // fill UI elements
            this.tvFirmware1VersionChecksum.setText(daApp.getString(R.string.fw_update_fw1version_checksum,
                    fw1VersionPty.formatValue(firmware1Meta.firmwareVersion), fw1ChecksumPty.formatValue(firmware1Meta.firmwareChecksum)));

            this.tvFirmware2VersionChecksum.setText(daApp.getString(R.string.fw_update_fw2version_checksum,
                    fw1VersionPty.formatValue(firmware2Meta.firmwareVersion), fw2ChecksumPty.formatValue(firmware2Meta.firmwareChecksum)));
        }

    }

    public void setCheckedNodeIds(Set<Long> checkedNodeIds) {
        this.checkedNodeIds = checkedNodeIds;
    }

    public static Bundle getState(Set<Long> checkedNodeIds) {
        Bundle b = new Bundle();
        Long[] arr = checkedNodeIds.toArray(new Long[checkedNodeIds.size()]);
        b.putLongArray(BK_SELECTED_ITEMS, ArrayUtils.toPrimitive(arr));
        return b;
    }

    public Bundle saveState() {
        return getState(checkedNodeIds);
    }

    public void restoreState(Bundle bundle) {
        long[] longs = bundle.getLongArray(BK_SELECTED_ITEMS);
        if (longs != null) {
            for (long aLong : longs) {
                checkedNodeIds.add(aLong);
            }
        }
    }

    public Set<Long> getCheckedNodeIds() {
        return checkedNodeIds;
    }

    public List<NetworkNode> getCheckedNodesInOrder() {
        List<NetworkNode> nodes = new LinkedList<>();
        for (NetworkNodeEnhanced node : this.nodes) {
            if (checkedNodeIds.contains(node.getId())) {
                nodes.add(node.asPlainNode());
            }
        }
        return nodes;
    }

}
