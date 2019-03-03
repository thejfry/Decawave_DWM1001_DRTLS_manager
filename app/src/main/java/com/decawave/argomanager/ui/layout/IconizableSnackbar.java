/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.layout;

import android.support.design.widget.BaseTransientBottomBar;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.decawave.argomanager.R;
import com.decawave.argomanager.ui.uiutil.ISnackbar;

/**
 * Argo project.
 */

public class IconizableSnackbar extends BaseTransientBottomBar<IconizableSnackbar> implements ISnackbar {

    /**
     * Constructor for the transient bottom bar.
     *
     * @param parent              The parent for this transient bottom bar.
     * @param content             The content view for this transient bottom bar.
     * @param contentViewCallback The content view callback for this transient bottom bar.
     */
    private IconizableSnackbar(ViewGroup parent, View content,
                               ContentViewCallback contentViewCallback) {
        super(parent, content, contentViewCallback);
    }

    private static class ContentViewCallback implements
            BaseTransientBottomBar.ContentViewCallback {

        // view inflated from custom layout
        private View content;

        ContentViewCallback(View content) {
            this.content = content;
        }

        @Override
        public void animateContentIn(int delay, int duration) {
            // add custom *in animations for your views
            // e.g. original snackbar uses alpha animation, from 0 to 1
            ViewCompat.setScaleY(content, 0f);
            ViewCompat.animate(content)
                    .scaleY(1f).setDuration(duration)
                    .setStartDelay(delay);
        }

        @Override
        public void animateContentOut(int delay, int duration) {
            // add custom *out animations for your views
            // e.g. original snackbar uses alpha animation, from 1 to 0
            ViewCompat.setScaleY(content, 1f);
            ViewCompat.animate(content)
                    .scaleY(0f)
                    .setDuration(duration)
                    .setStartDelay(delay);
        }
    }

    public static IconizableSnackbar make(ViewGroup parent, @Duration int duration) {
        // inflate custom layout
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View content = inflater.inflate(R.layout.autopos_snackbar, parent, false);

        // create snackbar with custom view
        ContentViewCallback callback= new ContentViewCallback(content);
        IconizableSnackbar customSnackbar = new IconizableSnackbar(parent, content, callback);

        // set snackbar duration
        customSnackbar.setDuration(duration);
        return customSnackbar;
    }

    // set text in custom layout
    public IconizableSnackbar setText(CharSequence text) {
        TextView textView = (TextView) getView().findViewById(R.id.snackbar_text);
        textView.setText(text);
        return this;
    }


    @Override
    public ISnackbar setAction(int resId, View.OnClickListener listener) {
        Button actionView = (Button) getView().findViewById(R.id.snackbar_action);
        actionView.setText(resId);
        actionView.setVisibility(View.VISIBLE);
        actionView.setOnClickListener(view -> {
            listener.onClick(view);
            // Now dismiss the Snackbar
            dismiss();
        });
        return this;
    }
}

