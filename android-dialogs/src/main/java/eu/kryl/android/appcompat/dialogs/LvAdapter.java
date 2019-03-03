/*
 * Copyright 2017, Pavel Kryl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.kryl.android.appcompat.dialogs;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

import eu.kryl.appcompat.dialogs.R;

/**
 * 
 */
public class LvAdapter extends RecyclerView.Adapter<LvAdapter.ViewHolder> {
    /** */
    public static enum ChoiceMode {
        SINGLE_SANS_BUTTON(R.layout.list_single_choice_sans_button),    // a plain TextView
        SINGLE_WITH_BUTTON(R.layout.list_single_choice_with_button),    // a RadioBox
        MULTIPLE(R.layout.list_multiple_choice);                        // a CheckBox
        
        public int idLayoutResource;
        
        ChoiceMode(int idLayoutResource) {
            this.idLayoutResource = idLayoutResource;
        }
    }
    
    /** */
    public static interface ItemClickListener {
        public void onItemClicked(int position);
    }

    /** displayed text */
    private CharSequence[] strItems;

    /** indices of disabled items */
    private Set<Integer> idxDisabled = new HashSet<Integer>();
    
    /** indices of selected items, if {@link ChoiceMode#MULTIPLE} */
    private Set<Integer> idxSelected = new HashSet<Integer>();
    
    /** */
    private ChoiceMode choiceMode;
    
    /** */
    private ItemClickListener itemClickListener;
    
    /** */
    private Context ctx;
    
    /**
     *
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView textView;
        @Nullable
        public CompoundButton button;

        /** */
        public ViewHolder(View view) {
            super(view);
            this.textView = (TextView) view.findViewById(R.id.text);
            this.button = (CompoundButton) view.findViewById(R.id.button);
            view.setOnClickListener(this);
        }

        /** */
        @Override
        public void onClick(View v) {
            Integer pos = getPosition();
            switch (choiceMode) {
                case MULTIPLE:
                    if (idxSelected.contains(pos)) {
                        idxSelected.remove(pos);
                        button.setChecked(false);
                    } else {
                        idxSelected.add(getPosition());
                        button.setChecked(true);
                    }
                    // trickle down
                case SINGLE_SANS_BUTTON:
                case SINGLE_WITH_BUTTON:
                    if (itemClickListener != null) {
                        itemClickListener.onItemClicked(getPosition());
                    } else {
                        throw new RuntimeException("FixMe: register a clicklistener!");
                    }
                    break;
                default:
                    throw new RuntimeException("FixMe: choiceMode=" + choiceMode);
            }
        }
    }

    /**
     * NOTE: make sure @param context is a TINTING/APPCOMPAT context!
     */
    public LvAdapter(Context context, CharSequence[] items, ChoiceMode mode) {
        ctx = context;
        strItems = items;
        choiceMode = mode;
    }

    /**
     * nothing to recycle yet...
     */
    @Override
    public LvAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(choiceMode.idLayoutResource, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    /**
     * recycle...
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.textView.setText(strItems[position]);
        final boolean enabled = !idxDisabled.contains(position);
        holder.textView.setEnabled(enabled);
        if (holder.button != null) {
            holder.button.setEnabled(enabled);
            holder.button.setChecked(idxSelected.contains(position));
        }
    }

    /**
     * Return the size of our dataset (invoked by the layout manager)
     */
    @Override
    public int getItemCount() {
        return strItems.length;
    }
    
    /**
     * 
     */
    public void setItemClickListener(ItemClickListener listener) {
        itemClickListener = listener;
    }
    
    /**
     * 
     */
    public void setDisabledItems(int[] idxItems) {
        idxDisabled.clear();
        for (int i=0; i<idxItems.length; i++)
            idxDisabled.add(idxItems[i]);
    }
    
    /**
     * 
     */
    public void setSelectedItems(int[] idxItems) {
        idxSelected.clear();
        for (int i=0; i<idxItems.length; i++)
            idxSelected.add(idxItems[i]);
    }
}
