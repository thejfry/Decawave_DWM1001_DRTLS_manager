/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.listadapter;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.decawave.argo.api.struct.NetworkNode;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.R;
import com.decawave.argomanager.components.AutoPositioningManager;
import com.decawave.argomanager.components.AutoPositioningState;
import com.decawave.argomanager.components.impl.AutoPositioningAlgorithm;
import com.decawave.argomanager.components.struct.ComputedPosition;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.prefs.LengthUnit;
import com.decawave.argomanager.ui.MainActivity;
import com.decawave.argomanager.ui.dialog.ZaxisValueDialogFragment;
import com.decawave.argomanager.ui.fragment.AutoPositioningFragment;
import com.decawave.argomanager.ui.fragment.FragmentType;
import com.decawave.argomanager.ui.view.SimpleProgressView;
import com.decawave.argomanager.util.Util;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import eu.kryl.android.common.Pair;
import eu.kryl.android.common.log.ComponentLog;

import static com.decawave.argomanager.ArgoApp.daApp;

/**
 *
 */
public class AutoPositioningNodeListAdapter extends RecyclerView.Adapter<AutoPositioningNodeListAdapter.ViewHolder> {
    private static final ComponentLog log = new ComponentLog(AutoPositioningNodeListAdapter.class);

    private static final String BK_ORDERED_ITEMS = "BK_ORDERED_ITEMS";
    //
    private final MainActivity mainActivity;
    private final AutoPositioningManager autoPositioningManager;
    private final AppPreferenceAccessor appPreferenceAccessor;

    private List<NetworkNode> nodes = new ArrayList<>();

    private int cardBackgroundColor;        // white...
    private int cardBottomSeparatorColor;   // gray...
    // item types
    private static final int ITEM_TYPE_SUMMARY = 0;
    private static final int ITEM_TYPE_NODE = 1;

    public AutoPositioningNodeListAdapter(@NotNull MainActivity mainActivity,
                                          @NotNull AutoPositioningManager autoPositioningManager,
                                          @NotNull AppPreferenceAccessor appPreferenceAccessor) {
        this.mainActivity = mainActivity;
        this.nodes = new LinkedList<>(autoPositioningManager.getNodes());
        this.autoPositioningManager = autoPositioningManager;
        this.appPreferenceAccessor = appPreferenceAccessor;
        //
        this.cardBackgroundColor = ContextCompat.getColor(daApp, R.color.cardview_light_background);
        this.cardBottomSeparatorColor = ContextCompat.getColor(daApp, R.color.color_list_item_separator_light);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case ITEM_TYPE_SUMMARY:
                View view = inflater.inflate(R.layout.li_autopositioning_summary, parent, false);
                // wrap with view holder
                return new AutoPositioningSummaryItemHolder(view);
            case ITEM_TYPE_NODE:
                view = inflater.inflate(R.layout.li_autopositioning_node_item, parent, false);
                // wrap with view holder
                return new AutoPosNodeListItemHolder(view);
            default:
                throw new IllegalStateException("unsupported item view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (holder instanceof AutoPosNodeListItemHolder) {
            int p = position - 1;
            AutoPosNodeListItemHolder h = (AutoPosNodeListItemHolder) holder;
            h.bind(nodes.get(p), p == nodes.size() - 1);
        } else if (holder instanceof AutoPositioningSummaryItemHolder) {
            ((AutoPositioningSummaryItemHolder) holder).bind();
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

    public void refreshSummary() {
        notifyItemChanged(0);
    }


    private int getViewIndexForNodeIterator(@NotNull ListIterator<NetworkNode> listIterator) {
        return 1 + listIterator.previousIndex();
    }

    /**
     * Unfortunately we are not able to return iterator which keep the next() returned value.
     * Actually we are, but this wouldn't be very nice.
     *
     * @param bleAddress identifies node
     */
    private Pair<NetworkNode, ListIterator<NetworkNode>> findNodeByBleAddress(String bleAddress) {
        ListIterator<NetworkNode> it = nodes.listIterator();
        //noinspection WhileLoopReplaceableByForEach
        while (it.hasNext()) {
            NetworkNode n = it.next();
            if (n.getBleAddress().equals(bleAddress)) {
                // stop here
                return new Pair<>(n, it);
            }
        }
        return null;
    }

    private @Nullable Integer getNodePosition(String nodeBleAddress) {
        Pair<NetworkNode, ListIterator<NetworkNode>> nodeIt = findNodeByBleAddress(nodeBleAddress);
        if (nodeIt != null) {
            return getViewIndexForNodeIterator(nodeIt.second);
        }
        return null;
    }

    public void notifyItemChanged(String bleAddress) {
        Integer pos = getNodePosition(bleAddress);
        if (pos != null) {
            notifyItemChanged(pos);
        }
    }

    public void moveNetworkNode(int from, int to) {
        NetworkNode node = nodes.remove(from);
        to = Math.max(0, Math.min(to, nodes.size()));
        nodes.add(to, node);
        notifyDataSetChanged();
    }

    public List<NetworkNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    abstract class ViewHolder extends RecyclerView.ViewHolder {

        ViewHolder(View itemView) {
            super(itemView);
        }
    }

    class AutoPosNodeListItemHolder extends ViewHolder {
        // references to views
        @BindView(R.id.btnDragHandle)
        View btnDragHandle;
        @BindView(R.id.nodeName)
        TextView nodeName;
        @BindView(R.id.bleAddress)
        TextView tvNodeBleAddress;
        @BindView(R.id.tvNodeState)
        TextView tvNodeState;
        @BindView(R.id.cardContent)
        View cardContent;
        @BindView(R.id.progressView)
        SimpleProgressView progressViewSeparator;
        @BindView(R.id.tvPosition)
        TextView tvPosition;

        // identification of the network node
        long nodeId;
        String nodeBle;

        AutoPosNodeListItemHolder(View itemView) {
            super(itemView);
            // extract references
            ButterKnife.bind(this,itemView);
        }

        void bind(NetworkNode networkNode, boolean lastNode) {
            // bind the network node first
            // fill simple variables first
            nodeId = networkNode.getId();
            nodeBle = networkNode.getBleAddress();
            //
            itemView.setTag(nodeBle);
            // fill UI elements
            nodeName.setText(networkNode.getLabel());
            tvNodeBleAddress.setText(networkNode.getBleAddress());
            // retrieve the computed position
            cfgNodePosition(networkNode);
            // check if we need to draw the progress
            AutoPositioningState.NodeState nodeState = autoPositioningManager.getNodeRunningState(nodeId);
            cfgProgressViewSeparator(nodeState, lastNode);
            cfgNodeStatus(nodeState, Util.isRealInitiator(networkNode));
        }

        private void cfgProgressViewSeparator(AutoPositioningState.NodeState nodeState, boolean lastNode) {
            if (nodeState == AutoPositioningState.NodeState.IDLE) {
                progressViewSeparator.makeInactive();
            } else {
                progressViewSeparator.makeIndeterminate();
            }
            // and top/bottom margins
            if (lastNode) {
                // do not hide progress separator, just set a different background color
                progressViewSeparator.setBackgroundColor(cardBackgroundColor);
            } else {
                progressViewSeparator.setBackgroundColor(cardBottomSeparatorColor);
            }
        }

        private void cfgNodePosition(NetworkNode networkNode) {
            ComputedPosition cPosition = autoPositioningManager.getComputedPosition(nodeId);
            if (cPosition != null && cPosition.success) {
                String positionStr = formatPositionString(networkNode, cPosition);
                tvPosition.setText(positionStr);
            } else if (cPosition != null) {
                // this is position compute fail
                tvPosition.setText(R.string.ap_position_compute_failed);
            } else {
                tvPosition.setText(daApp.getString(R.string.ap_position_not_computed_yet));
            }
        }

        private void cfgNodeStatus(AutoPositioningState.NodeState nodeState, boolean initiator) {
            // precedence have failed states
            AutoPositioningState.TaskState nodeInitiatorCheckStatus = autoPositioningManager.getNodeInitiatorCheckStatus(nodeId);
            AutoPositioningState.TaskState nodePositionSaveStatus = autoPositioningManager.getNodePositionSaveStatus(nodeId);
            AutoPositioningState.TaskState nodeDistanceCollectionStatus = autoPositioningManager.getNodeDistanceCollectionStatus(nodeId);
            if (Constants.DEBUG) {
                log.d("cfgNodeStatus [" + nodeBle + "]: node state = " + nodeState +
                        ", initiatorCheck = " + nodeInitiatorCheckStatus +
                        ", position = " + nodePositionSaveStatus +
                        ", distance = " + nodeDistanceCollectionStatus);
            }
            boolean visible = true;
            boolean error = false;
            if (nodeInitiatorCheckStatus == AutoPositioningState.TaskState.FAILED) {
                tvNodeState.setText(R.string.ap_initiator_check_fail);
                error = true;
            } else if (nodeInitiatorCheckStatus == AutoPositioningState.TaskState.TERMINATED) {
                tvNodeState.setText(R.string.ap_initiator_check_terminated);
            } else if (nodePositionSaveStatus == AutoPositioningState.TaskState.FAILED) {
                tvNodeState.setText(R.string.ap_position_save_fail);
                error = true;
            } else if (nodePositionSaveStatus == AutoPositioningState.TaskState.TERMINATED) {
                tvNodeState.setText(R.string.ap_position_save_terminated);
            } else if (nodeDistanceCollectionStatus == AutoPositioningState.TaskState.FAILED) {
                tvNodeState.setText(R.string.ap_distance_retrieval_fail);
                error = true;
            } else if (nodeDistanceCollectionStatus == AutoPositioningState.TaskState.TERMINATED) {
                tvNodeState.setText(R.string.ap_legend_distance_retrieval_terminated);
            } else if (nodeState != null) {
                switch (nodeState) {
                    case CHECKING_INITIATOR:
                        tvNodeState.setText(R.string.ap_node_status_initiator_check);
                        break;
                    case COLLECTING_DISTANCES:
                        if (initiator) {
                            tvNodeState.setText(R.string.ap_node_status_measuring_distances);
                        } else {
                            tvNodeState.setText(R.string.ap_node_status_retrieving_distances);
                        }
                        break;
                    case SAVING_POSITION:
                        tvNodeState.setText(R.string.ap_node_status_saving_position);
                        break;
                    case IDLE:
                        // we are not doing anything, now have we ended up in an error state
                        visible = false;
                        break;
                    default:
                        throw new IllegalStateException("unexpected node status");
                }
            } else {
                // the node state is null
                visible = false;
            }
            if (visible) {
                tvNodeState.setVisibility(View.VISIBLE);
                makeError(tvNodeState, error);
            } else {
                // set visibility
                tvNodeState.setVisibility(View.GONE);
            }
        }

        private void makeError(TextView textView, boolean error) {
            textView.setTextColor(error ?
                    ContextCompat.getColor(mainActivity, R.color.mtrl_primary)
                    : ContextCompat.getColor(mainActivity, R.color.secondary_text_w_light_selection_light));
        }


    }

    @NonNull
    private String formatPositionString(NetworkNode networkNode, ComputedPosition cPosition) {
        LengthUnit lengthUnit = appPreferenceAccessor.getLengthUnit();
        String x = Util.formatLength(cPosition.position.x, lengthUnit);
        String y = Util.formatLength(cPosition.position.y, lengthUnit);
        String z = Util.formatLength(cPosition.position.z, lengthUnit);
        String positionStr;
        if (cPosition.position.equalsInCoordinates(networkNode.extractPositionDirect())) {
            // no need to save the position
            positionStr = daApp.getString(R.string.ap_position_template_no_save, x, y, z);
        } else {
            // we need to save the position
            positionStr = daApp.getString(R.string.ap_position_template_needs_save, x, y, z);
        }
        return positionStr;
    }

    class AutoPositioningSummaryItemHolder extends ViewHolder {
        @BindView(R.id.tvLegend)
        TextView tvLegend;

        @BindView(R.id.actionButtonContainer)
        LinearLayout buttonContainer;

        //
        AutoPositioningState.ApplicationState lastApplicationState;

        AutoPositioningSummaryItemHolder(View itemView) {
            super(itemView);
            // extract references
            ButterKnife.bind(this, itemView);
        }

        @OnClick(R.id.btnPreview)
        void onPreviewClicked() {
            // show a new fragment
            mainActivity.showFragment(FragmentType.AP_PREVIEW);
        }

        @OnClick(R.id.btnSetupZaxis)
        void onSetupZaxisClicked() {
            ZaxisValueDialogFragment.showDialog(mainActivity.getSupportFragmentManager(),
                    Util.formatLength(autoPositioningManager.getZaxis(), appPreferenceAccessor.getLengthUnit()));
            // wait for the result, receive the result in the fragment (IhCallback)
        }

        public void bind() {
            // retrieve the auto positioning manager state and set a proper message
            AutoPositioningState.ApplicationState applicationState = autoPositioningManager.getApplicationState();
            if (lastApplicationState != applicationState) {
                if (applicationState == AutoPositioningState.ApplicationState.DISTANCE_COLLECTION_SUCCESS_POSITION_COMPUTE_SUCCESS) {
                    buttonContainer.setVisibility(View.VISIBLE);
                } else {
                    buttonContainer.setVisibility(View.GONE);
                }
                switch (applicationState) {
                    case NOT_STARTED:
                        setClickableLegendContent(daApp.getString(R.string.ap_legend_start_measure_distances), daApp.getString(R.string.ap_legend_instructions));
                        break;
                    case INITIATOR_CHECK_FAILED:
                    case INITIATOR_CHECK_MISSING:
                        tvLegend.setText(R.string.ap_legend_initiator_failed);
                        break;
                    case INITIATOR_CHECK_TERMINATED:
                        tvLegend.setText(R.string.ap_legend_initiator_check_terminated);
                        break;
                    case CHECKING_INITIATOR:
                        tvLegend.setText(R.string.ap_legend_checking_initiator);
                        break;
                    case COLLECTING_DISTANCES:
                        tvLegend.setText(R.string.ap_legend_collecting_distances);
                        break;
                    case DISTANCE_COLLECTION_FAILED:
                        tvLegend.setText(R.string.ap_legend_distance_collection_failed);
                        break;
                    case DISTANCE_COLLECTION_TERMINATED:
                        tvLegend.setText(R.string.ap_legend_distance_retrieval_terminated);
                        break;
                    case DISTANCE_COLLECTION_SUCCESS_POSITION_COMPUTE_FAIL:
                        AutoPositioningAlgorithm.ResultCode resultCode = autoPositioningManager.getPositionComputeResultCode();
                        switch (resultCode) {
                            case MISSING_DISTANCE_0_TO_1:
                                tvLegend.setText(daApp.getString(R.string.ap_legend_missing_node_distance, nodes.get(0).getLabel(), nodes.get(1).getLabel()));
                                break;
                            case MISSING_DISTANCE_1_TO_2:
                                tvLegend.setText(daApp.getString(R.string.ap_legend_missing_node_distance, nodes.get(1).getLabel(), nodes.get(2).getLabel()));
                                break;
                            case MISSING_DISTANCE_0_TO_2:
                                tvLegend.setText(daApp.getString(R.string.ap_legend_missing_node_distance, nodes.get(0).getLabel(), nodes.get(2).getLabel()));
                                break;
                            case DRIVING_NODES_NOT_ORTHOGONAL_ENOUGH:
                                setClickableLegendContent(daApp.getString(R.string.ap_legend_driving_nodes_not_orthogonal_enough), daApp.getString(R.string.ap_legend_instructions));
                                break;
                            case SUCCESS:
                            default:
                                throw new IllegalStateException("how comes?: " + resultCode);
                        }
                        break;
                    case DISTANCE_COLLECTION_SUCCESS_POSITION_COMPUTE_SUCCESS:
                        tvLegend.setText(R.string.ap_legend_distance_success_compute_success);
                        break;
                    case SAVING_POSITIONS:
                        tvLegend.setText(R.string.ap_legend_saving_positions);
                        break;
                    case POSITIONS_SAVE_FAILED:
                        tvLegend.setText(R.string.ap_legend_position_save_failed);
                        break;
                    case POSITIONS_SAVE_TERMINATED:
                        tvLegend.setText(R.string.ap_legend_position_save_terminated);
                        break;
                    case POSITIONS_SAVE_SUCCESS:
                        tvLegend.setText(R.string.ap_legend_position_save_success);
                        break;
                }
                lastApplicationState = applicationState;
            }
        }

        private void setClickableLegendContent(String legendText, String linkText) {
            SpannableString ss = new SpannableString(legendText);
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    AutoPositioningFragment.showApInstructions(mainActivity.getSupportFragmentManager());
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                }
            };
            int idx = legendText.indexOf(linkText);
            if (idx != -1) {
                ss.setSpan(clickableSpan, idx, idx + linkText.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                tvLegend.setClickable(true);
            }
            tvLegend.setText(ss);
            tvLegend.setMovementMethod(LinkMovementMethod.getInstance());
        }

    }

    public static Bundle getState(List<NetworkNode> nodesInOrder) {
        Bundle b = new Bundle();
        List<Long> nodeIdsInOrder = Stream.of(nodesInOrder).map(NetworkNode::getId).collect(Collectors.toList());
        Long[] arr = nodeIdsInOrder.toArray(new Long[nodeIdsInOrder.size()]);
        b.putLongArray(BK_ORDERED_ITEMS, ArrayUtils.toPrimitive(arr));
        return b;
    }

}
