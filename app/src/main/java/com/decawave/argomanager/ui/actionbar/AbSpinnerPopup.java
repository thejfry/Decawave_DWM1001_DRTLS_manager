/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.actionbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.decawave.argomanager.R;
import com.decawave.argomanager.ui.DisplayMetrics;

/**
 * PopupWindow of actionbar's spinner
 */
public class AbSpinnerPopup extends PopupWindow {

    private AbSpinnerAdapter mAbSpinnerAdapter;

    public interface OnAbSpinnerPopupItemSelectListener {

        void onSpinnerItemSelected(SpinnerItem spinnerItem);

    }

    @SuppressLint("PrivateResource")
    public AbSpinnerPopup(Context context, final AbSpinnerAdapter<?> abSpinnerAdapter, final OnAbSpinnerPopupItemSelectListener listener) {
        super(context);

        mAbSpinnerAdapter = abSpinnerAdapter;

        setFocusable(true);
        setOutsideTouchable(true);
        setClippingEnabled(false);

        // listview content
        @SuppressLint("InflateParams")
        ListView listView = (ListView) LayoutInflater.from(context).inflate(R.layout.simple_popup_window, null);
        listView.setDivider(null);
        listView.setCacheColorHint(0x00000000); // prevent black BG issues..
        listView.setAdapter(abSpinnerAdapter);
        listView.setOnItemClickListener((parent, v, position, id) -> {
            listener.onSpinnerItemSelected(abSpinnerAdapter.getItem(position));
            AbSpinnerPopup.this.dismiss();
        });


        setContentView(listView);
        //setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);

        //noinspection deprecation
        setBackgroundDrawable(context.getResources().getDrawable(R.drawable.abc_popup_background_mtrl_mult));
    }

    public void show(View anchorView) {
        final int backgroundDrawablePadding = (int) (16 * DisplayMetrics.LCD_DIP_SCALING_FACTOR + 0.5);
        final int listViewWidth = mAbSpinnerAdapter.measureContentWidth();
        setWidth(listViewWidth + backgroundDrawablePadding);
        final int xOffset = -(int) ((8 + 8) * DisplayMetrics.LCD_DIP_SCALING_FACTOR + 0.5);
        final int yOffset = -(int) (8 * DisplayMetrics.LCD_DIP_SCALING_FACTOR + 0.5) - anchorView.getHeight();
        showAsDropDown(anchorView, xOffset, yOffset);
    }
}
