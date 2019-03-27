package com.decawave.argomanager.ui.fragment;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.decawave.argomanager.R;
import com.decawave.argomanager.components.struct.Anchor;
import com.decawave.argomanager.ioc.ArgoComponent;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.ui.view.MyCanvasView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Allows user to select anchors based on their relative locations on the floorplan.
 */
public class SelectAnchorsFragment extends AbstractArgoFragment {

    public static final String TAG = "TN_debug";
    private Anchor receivedAnchor;

    //input
//    @BindView(R.id.vSelectAnchorView)
//    MyCanvasView vMapView;

    @BindView(R.id.btn_submit_anchor)
    Button btnSubmitAnchor;

    // dependencies
    @Inject
    AppPreferenceAccessor appPreferenceAccessor;

    public SelectAnchorsFragment(){
        super(FragmentType.SELECT_ANCHORS);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    protected void injectFrom(ArgoComponent injector) {
        injector.inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.select_anchor, container,false);
        ButterKnife.bind(this, view);
        Log.i(TAG, "Creating anchor selection fragment");

//        Button btnSubmitAnchor = (Button) view.findViewById(R.id.btn_submit_anchor);
//        MyCanvasView vMapView = new MyCanvasView(this.getContext());
////        View vMapView = v.findViewById(R.id.select_anchor_view);
//        MyCanvasView vMapView = (MyCanvasView) v.findViewById(R.id.select_anchor_view);

        btnSubmitAnchor.setOnClickListener((v) -> {
            Log.i(TAG,"Trying to submit anchor");
//            Log.i(TAG,"vMapView: " + vMapView);
////            Log.i(TAG,"findviewbyid: " + v.findViewById(R.id.select_anchor_view));
            Log.i(TAG,"btnSubmitAnchor: " + btnSubmitAnchor);
//            Log.i(TAG,"receivedAnchor: " + receivedAnchor);
//
//            receivedAnchor = vMapView.getTempAnchor();
            try {
                Log.i(TAG, "Received anchor 1:\n" + receivedAnchor.getName() + "\n" + receivedAnchor.getAnchorX() + "\n" + receivedAnchor.getAnchorY());
            } catch (Exception e){
                Log.i(TAG,"no anchor was received");
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
