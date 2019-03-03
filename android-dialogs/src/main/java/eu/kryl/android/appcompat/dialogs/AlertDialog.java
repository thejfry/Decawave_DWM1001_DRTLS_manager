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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

import eu.kryl.android.common.android.LayoutDirectionUtil;
import eu.kryl.appcompat.dialogs.R;

public class AlertDialog extends Dialog {
    private ImageView mIcon = null;
    private View mHeader = null;
    private TextView mTitleTextView = null;
    private ScrollView mMsgScrollView = null;
    private TextView mMsgTextView = null;
    private LinearLayout mFooter = null;
    private RecyclerView mItemsList = null;
    
    /** custom dialog instance state handler */
    private InstanceStateHandler mInstanceStateHandler;
    
    /**
     * 
     */
    public static interface InstanceStateHandler {
        public void onRestoreInstanceState(Bundle savedInstanceState);
        public Bundle onSaveInstanceState();
    }
    
    /**
     * 
     */
    public AlertDialog(Context context, int theme) {
        super(context, theme);
    }

    public AlertDialog(Context context) {
        super(context);
    }
    
    /**
     * Changing the text on the fly, enabling reuse of the dialog
     */
    public void setMessage(CharSequence msg) {
        if (mMsgTextView != null) {
            mMsgTextView.setText(msg);
            mMsgTextView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }
    
    /**
     * Changing the title on the fly, enabling reuse of the dialog
     */
    @Override
    public void setTitle(CharSequence title) {
        if (title != null) {
            if (mTitleTextView != null) {
                mTitleTextView.setText(title);
            }
            mHeader.setVisibility(View.VISIBLE);
        } else {
            mHeader.setVisibility(View.GONE);
        }
    }
    
    /**
     * @return the content listview that was built if any of the set*Items(..) methods was
     * called on the builder.
     */
    public RecyclerView getRecyclerView() {
        return mItemsList;
    }
    
    /**
     * 
     */
    public LinearLayout getFooter() {
        return mFooter;
    }

    /**
     * @return positive button if set, null otherwise
     */
    public Button getPositiveButton() {
        return getButton(R.id.positiveButton);
    }
    
    /**
     * @return negative button if set, null otherwise
     */
    public Button getNegativeButton() {
        return getButton(R.id.negativeButton);
    }

    private Button getButton(int buttonId) {
        final View button = findViewById(buttonId);
        return (Button) (button.getVisibility() == View.GONE ? null : button);
    }

    /**
     * 
     */
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (mInstanceStateHandler != null) {
            mInstanceStateHandler.onRestoreInstanceState(savedInstanceState);
        }
    }
    
    /**
     * 
     */
    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        if (mInstanceStateHandler != null) {
            Bundle extraState = mInstanceStateHandler.onSaveInstanceState();
            state.putAll(extraState);
        }
        return state;
    }
    
    /**
     * 
     */
    public static class Builder {
        private String mTitle;

        private CharSequence mMessage;
        
        private Integer mMsgTextSizeDips;
        
        private CharSequence[] mItems;
        
        private int[] mDisabledItems; // can be null
        
        private int[] mSelectedItems; // for multiple choice lists; can be null
        
        private LvAdapter.ChoiceMode mListChoiceMode;

        private String mBtnTextPositive;

        private String mBtnTextNegative;

        private View mContentView;

        private Context mContext;

        private Integer mIconResourceId;

        private DialogInterface.OnClickListener mBtnClickListenerPositive;

        private DialogInterface.OnClickListener mBtnClickListenerNegative;

        private DialogInterface.OnClickListener mListViewClickListener;

        private int mBackgroundColor = -1;

        private ArrayList<View> mCustomBtns = new ArrayList<View>();

        int mCntSet;

        boolean mIsCancelable = true;

        boolean mRemoveTopPadding = false;

        DialogInterface.OnCancelListener mOnCancelListener = null;

        InstanceStateHandler mInstanceStateHandler = null;

        /**
         * Wrapping the context and setting the current Dialog Theme on this new
         * wrapped context to have all our dialog theme related attributes defined.
         * Without this wrapping we would get resource not found exceptions.
         *
         * @param context
         * @param theme
         *
         * (!) NOTE 1: use an appcompat 21 context to inflate material widgets:
         * final Context ctx = getSupportActionBar().getThemedContext();
         */
        public Builder(Context context, int theme) {
            mContext = new ContextThemeWrapper(context, theme);
            // mContext = context;
            mCntSet = 0;
        }

        public Builder(Context context) {
            this(context, R.style.MtrlDialogTheme_Light);
        }

            /**
             * @param icon is a drawable resource ID
             */
        public Builder setIcon(int icon) {
            mIconResourceId = icon;
            return this;
        }

        /**
         * @param title is a string resource ID
         * @return
         */
        public Builder setTitle(int title) {
            mTitle = (String) mContext.getText(title);
            return this;
        }

        /**
         * @param title
         * @return
         */
        public Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        /**
         * @param message is a string resource ID
         * @return
         */
        public Builder setMessage(int message) {
            mMessage = mContext.getText(message);
            mCntSet++;
            return this;
        }

        /**
         * @param message is a string resource ID
         * @return
         */
        public Builder setHtmlMessage(int message) {
            mMessage = Html.fromHtml((String) mContext.getText(message));
            mCntSet++;
            return this;
        }

        /**
         * @param message
         * @return
         */
        public Builder setMessage(CharSequence message) {
            mMessage = message;
            mCntSet++;
            return this;
        }

        /**
         * @param size - font size in DIPs
         */
        public Builder setMessageTextSize(int size) {
            mMsgTextSizeDips = size;
            return this;
        }

        /**
         *
         */
        public Builder setRemoveTopPadding(boolean flag) {
            mRemoveTopPadding = flag;
            return this;
        }

        /**
         *
         */
        public Builder setCancelable(boolean flag) {
            mIsCancelable = flag;
            return this;
        }

        /**
         *
         */
        public Builder setOnCancelListener(DialogInterface.OnCancelListener listener) {
            mOnCancelListener = listener;
            return this;
        }

        /**
         *
         */
        public Builder setItems(CharSequence[] items, DialogInterface.OnClickListener clickListener) {
            mItems = items;
            mListViewClickListener = clickListener;
            mCntSet++;
            mListChoiceMode = LvAdapter.ChoiceMode.SINGLE_SANS_BUTTON;
            return this;
        }

        /**
         *
         */
        public Builder setItems(CharSequence[] items, DialogInterface.OnClickListener clickListener,
                                int[] disabledItems) {
            mItems = items;
            mDisabledItems = disabledItems;
            mListViewClickListener = clickListener;
            mListChoiceMode = LvAdapter.ChoiceMode.SINGLE_SANS_BUTTON;
            mCntSet++;
            return this;
        }

        /**
         *
         */
        public Builder setSingleChoiceItems(CharSequence[] items, int selected, DialogInterface.OnClickListener clickListener) {
            mItems = items;
            mListViewClickListener = clickListener;
            mListChoiceMode = LvAdapter.ChoiceMode.SINGLE_WITH_BUTTON;
            mSelectedItems = (selected < 0) ? new int[0] : new int[] {selected};
            mCntSet++;
            return this;
        }

        /**
         *
         */
        public Builder setSingleChoiceItems(CharSequence[] items, int selected, DialogInterface.OnClickListener clickListener,
                                            int[] disabledItems) {
            mItems = items;
            mDisabledItems = disabledItems;
            mListViewClickListener = clickListener;
            mListChoiceMode = LvAdapter.ChoiceMode.SINGLE_WITH_BUTTON;
            mSelectedItems = (selected < 0) ? new int[0] : new int[] {selected};
            mCntSet++;
            return this;
        }

        /**
         *
         */
        public Builder setMultipleChoiceItems(CharSequence[] items, int[] selected, DialogInterface.OnClickListener clickListener) {
            mItems = items;
            mListViewClickListener = clickListener;
            mListChoiceMode = LvAdapter.ChoiceMode.MULTIPLE;
            mSelectedItems = selected;
            mCntSet++;
            return this;
        }

        /**
         *
         */
        public Builder setMultipleChoiceItems(CharSequence[] items, int[] selected, int[] disabled, DialogInterface.OnClickListener clickListener) {
            mItems = items;
            mListViewClickListener = clickListener;
            mListChoiceMode = LvAdapter.ChoiceMode.MULTIPLE;
            mSelectedItems = selected;
            mDisabledItems = disabled;
            mCntSet++;
            return this;
        }

        /**
         * @param v is an arbitrary view (most often a ListView)
         * @return
         */
        public Builder setView(View v) {
            mContentView = v;
            mCntSet++;
            return this;
        }

        public Builder setPositiveButton(int positiveButtonText,
                DialogInterface.OnClickListener listener) {
            mBtnTextPositive = (String) mContext.getText(positiveButtonText);
            mBtnClickListenerPositive = listener;
            return this;
        }

        public Builder setPositiveButton(String positiveButtonText,
                DialogInterface.OnClickListener listener) {
            mBtnTextPositive = positiveButtonText;
            mBtnClickListenerPositive = listener;
            return this;
        }

        public Builder setNegativeButton(int negativeButtonText,
                DialogInterface.OnClickListener listener) {
            mBtnTextNegative = (String) mContext.getText(negativeButtonText);
            mBtnClickListenerNegative = listener;
            return this;
        }

        public Builder setNegativeButton(String negativeButtonText,
                DialogInterface.OnClickListener listener) {
            mBtnTextNegative = negativeButtonText;
            mBtnClickListenerNegative = listener;
            return this;
        }

        public Builder addCustomButton(Button btn) {
            mCustomBtns.add(btn);
            return this;
        }

        public void setBackgroundColor(int color) {
            mBackgroundColor = color;
        }

        public void setInstanceStateHandler(InstanceStateHandler ish) {
            mInstanceStateHandler = ish;
        }

        public Context getContext() {
            return mContext;
        }

        @SuppressWarnings("deprecation")
        public AlertDialog create() {
            FrameLayout flContainer;
            Button btnPositive;
            Button btnNegative;

            // we are going to count the number of buttons placed in the footer
            // one by one, so that we can properly add spacers when more than two get added,
            // or remove the whole footer if none get added.
            int cntBtn = 0;

            // sanity check
            if (mCntSet != 1) {
                throw new IllegalStateException("You can only set exactly one of {message|content|items} on this alertdialog!");
            }

            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            // create the dialog with our current theme..
            final AlertDialog dialog = new AlertDialog(mContext);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_full_holo_light);

            // set layout direction
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    dialog.getWindow().getDecorView().setLayoutDirection(LayoutDirectionUtil.isRtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }


            View layout = inflater.inflate(R.layout.alert_dialog, null);

            if (mRemoveTopPadding) {
                layout.setPadding(0, 0, 0, 0);
            }

            // obtain a reference to our payload container & footer
            flContainer = (FrameLayout) layout.findViewById(R.id.content);

            if(mBackgroundColor != -1) {
                flContainer.setBackgroundColor(mBackgroundColor);
            }

            dialog.mHeader = layout.findViewById(R.id.header);
            dialog.mFooter = (LinearLayout) layout.findViewById(R.id.footer);

            // set icon
            if (mIconResourceId != null) {
                dialog.mIcon = (ImageView) layout.findViewById(R.id.icon);
                dialog.mIcon.setImageResource(mIconResourceId);
            }
            else {
                dialog.mIcon = (ImageView) layout.findViewById(R.id.icon);
                dialog.mIcon.setVisibility(View.GONE);
            }

            // set title
            dialog.mTitleTextView = (TextView) layout.findViewById(R.id.title);
            dialog.setTitle(mTitle);

            // set positive button
            btnPositive = (Button) layout.findViewById(R.id.positiveButton);
            if (mBtnTextPositive != null) {
                cntBtn++;
                btnPositive.setText(mBtnTextPositive);
                if (mBtnClickListenerPositive != null) {
                    btnPositive.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mBtnClickListenerPositive.onClick(dialog,
                                    DialogInterface.BUTTON_POSITIVE);
                            dialog.dismiss();
                        }
                    });
                }
                else {
                    btnPositive.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });
                }
            } else {
                // hide the button, if not requested
                btnPositive.setVisibility(View.GONE);
            }

            // set negative button
            btnNegative = (Button) layout.findViewById(R.id.negativeButton);
            if (mBtnTextNegative != null) {
                cntBtn++;
                btnNegative.setText(mBtnTextNegative);
                if (mBtnClickListenerNegative != null) {
                    btnNegative.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mBtnClickListenerNegative.onClick(dialog,
                                    DialogInterface.BUTTON_NEGATIVE);
                            dialog.dismiss();
                        }
                    });
                }
                else {
                    btnNegative.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.cancel();
                        }
                    });
                }
            } else {
                // hide the button, if not requested
                btnNegative.setVisibility(View.GONE);
            }

            // set any custom buttons
            if (mCustomBtns.size() > 0) {
                LinearLayout.LayoutParams lllp_btn = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                for (int i=0; i<mCustomBtns.size(); i++) {
                    dialog.mFooter.addView(mCustomBtns.get(i), lllp_btn);
                    cntBtn++;
                }
            }

            // display the buttonbar separator, if we have at least one button, and we are displaying a ListView...
            if (cntBtn > 0 && mItems != null) {
                View separator = (View) layout.findViewById(R.id.footer_separator);
                separator.setVisibility(View.VISIBLE);
            }

            // set the content message
            dialog.mMsgTextView = (TextView) layout.findViewById(R.id.message);
            if (mMessage != null) {
                dialog.setMessage(mMessage);
                if (mMsgTextSizeDips != null) {
                    dialog.mMsgTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mMsgTextSizeDips);
                }
            } else {
                // remove the textview and its scroller
                dialog.mMsgScrollView = (ScrollView) layout.findViewById(R.id.messageScroller);
                dialog.mMsgScrollView.removeView(dialog.mMsgTextView);
                flContainer.removeView(dialog.mMsgScrollView);
            }

            // set the content view
            if (mContentView != null) {
                ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                flContainer.addView(mContentView, lp);
            }

            // create & set a listview as content
            if (mItems != null) {
                ViewGroup.LayoutParams vglp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.mItemsList = buildListView(dialog, mItems, mListChoiceMode, mListViewClickListener, mSelectedItems, mDisabledItems);
                flContainer.addView(dialog.mItemsList, vglp);
            }

            // remove the footer if no buttons are showing (e.g. listview content only)..
            if (cntBtn == 0) {
                dialog.mFooter.setVisibility(View.GONE);
            }

            dialog.setContentView(layout);

            // set up the onCancelListener callback
            dialog.setCancelable(mIsCancelable);
            if (mOnCancelListener != null) {
                dialog.setOnCancelListener(mOnCancelListener);
            }

            // set up the InstanceStateHandler callback
            if (mInstanceStateHandler != null) {
                dialog.mInstanceStateHandler = this.mInstanceStateHandler;
            }

            // set the dialog width on pre-ICS devices, where the android:windowMinWidthMajor
            // and android:windowMinWidthMinor attributes are unknown...
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                Display display = ((android.view.WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                // the percent below depends on screen size, and major/minor axes (read from different XML resources)...
                int percent = mContext.getResources().getInteger(R.integer.dlg_width_percent);
                dialog.getWindow().setLayout((int)(display.getWidth() * (percent/100.0f)), ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            return dialog;
        }

        /**
         *
         */
        private RecyclerView buildListView(final Dialog dlg, CharSequence[] items, LvAdapter.ChoiceMode choiceMode,
                                           final DialogInterface.OnClickListener clickListener, int[] selectedItems, int[] disabledItems)
        {
            RecyclerView listView = (RecyclerView) LayoutInflater.from(dlg.getContext()).inflate(R.layout.dlg_recyclerview, null);
            listView.setId(choiceMode.idLayoutResource);
            listView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            listView.setHasFixedSize(true);
            
            LvAdapter adapter = new LvAdapter(mContext, mItems, choiceMode);
            if (disabledItems != null)
                adapter.setDisabledItems(disabledItems);
            if (selectedItems != null)
                adapter.setSelectedItems(selectedItems);
            if (clickListener != null) {
                adapter.setItemClickListener(new LvAdapter.ItemClickListener() {
                    @Override
                    public void onItemClicked(int position) {
                        clickListener.onClick(dlg, position);
                    }
                });
            }
            
            listView.setAdapter(adapter);
            listView.setLayoutManager(new LinearLayoutManager(mContext));


            return listView;
        }
    }
}
