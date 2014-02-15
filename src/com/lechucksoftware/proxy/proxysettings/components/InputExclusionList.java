package com.lechucksoftware.proxy.proxysettings.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.lechucksoftware.proxy.proxysettings.R;
import com.lechucksoftware.proxy.proxysettings.constants.Measures;
import com.lechucksoftware.proxy.proxysettings.utils.UIUtils;
import com.shouldit.proxy.lib.log.LogWrapper;
import com.shouldit.proxy.lib.utils.ProxyUtils;

import java.util.*;

public class InputExclusionList extends LinearLayout
{
    private static final String TAG = InputExclusionList.class.getSimpleName();
    private LinearLayout fieldMainLayout;
    private TextView readonlyValueTextView;
    private LinearLayout bypassContainer;
    private TextView titleTextView;
    private String title;
    //    private boolean fullsize;
    private boolean readonly;
    private String exclusionListString = "";
    private Map<UUID, InputField> exclusionInputFieldsMap;
    private UIHandler uiHandler;
    private boolean singleLine;
    private float textSize;
    private float titleSize;
    private ArrayList<ValueChangedListener> mListeners;

    public InputExclusionList(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        exclusionInputFieldsMap = new HashMap<UUID, InputField>();

        uiHandler = new UIHandler();

        readStyleParameters(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = inflater.inflate(R.layout.input_exclusion, this);

        if (v != null)
        {
            fieldMainLayout = (LinearLayout) v.findViewById(R.id.field_main_layout);
            titleTextView = (TextView) v.findViewById(R.id.field_title);
            bypassContainer = (LinearLayout) v.findViewById(R.id.bypass_container);
            bypassContainer.removeAllViews();
            readonlyValueTextView = (TextView) v.findViewById(R.id.field_value_readonly);

            refreshUI();
        }
    }

    private InputField createExclusionInputField()
    {
        InputField inputField;
        inputField = new InputField(getContext());

        // TODO: Show inputfield readonly and enable the edit only on click

        //                    i.setOnClickListener(new OnClickListener()
        //                    {
        //                        @Override
        //                        public void onClick(View view)
        //                        {
        //
        //                        }
        //                    });

        inputField.setPadding(0, 0, 0, 0);
        inputField.setTag(inputField.getUUID());
        inputField.setFullsize(false);
        inputField.setReadonly(readonly);
        inputField.setVisibility(VISIBLE);
        inputField.setHint("Add bypass address");

        inputField.setFieldAction(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                UUID idToRemove = (UUID) view.getTag();
                InputField i = exclusionInputFieldsMap.remove(idToRemove);
                bypassContainer.removeView(i);
                uiHandler.callRefreshUI();
            }
        });

        inputField.addTextChangedListener(new BypassTextWatcher(inputField));

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 0);

        bypassContainer.addView(inputField, layoutParams);

        return inputField;
    }

    protected void readStyleParameters(Context context, AttributeSet attributeSet)
    {
        TypedArray a = context.obtainStyledAttributes(attributeSet, R.styleable.InputFieldTags);

        try
        {
            title = a.getString(R.styleable.InputField_title);
            singleLine = a.getBoolean(R.styleable.InputField_singleLine, false);
//            fullsize = a.getBoolean(R.styleable.InputField_fullsize, false);
            readonly = a.getBoolean(R.styleable.InputField_readonly, false);
            titleSize = a.getDimension(R.styleable.InputField_titleSize, Measures.DefaultTitleSize);
            textSize = a.getDimension(R.styleable.InputField_textSize, Measures.DefaultTextFontSize);
        }
        finally
        {
            a.recycle();
        }
    }

    public void setExclusionString(String exclusionString)
    {
        if (!exclusionListString.equals(exclusionString))
        {
            uiHandler.callSetExclusionList(exclusionString);
        }
        else
        {
            // DO Nothing: No need to update UI
        }
    }

    public String getExclusionListString()
    {
        List<String> values = new ArrayList<String>();
        for (InputField i : exclusionInputFieldsMap.values())
        {
            values.add(i.getValue());
        }

        String result = TextUtils.join(",", values);
        return result;
    }

    private void refreshUI()
    {
        LogWrapper.startTrace(TAG, "refreshUI", Log.ASSERT, true);
        // Layout
        if (singleLine)
        {
            fieldMainLayout.setOrientation(HORIZONTAL);
            titleTextView.setWidth((int) UIUtils.convertDpToPixel(80, getContext()));
        }
        else
        {
            fieldMainLayout.setOrientation(VERTICAL);
            titleTextView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        // Title
        if (!TextUtils.isEmpty(title))
        {
            titleTextView.setText(title.toUpperCase());
        }

        refreshExclusionList();

        titleTextView.setTextSize(titleSize);
        readonlyValueTextView.setTextSize(textSize);

        LogWrapper.stopTrace(TAG, "refreshUI", Log.ASSERT);
    }

    private void refreshExclusionList()
    {
        LogWrapper.startTrace(TAG, "refreshExclusionList", Log.ASSERT, true);

        List<UUID> toRemove = new ArrayList<UUID>();

        for (UUID uuid : exclusionInputFieldsMap.keySet())
        {
            if (exclusionInputFieldsMap.containsKey(uuid))
            {
                InputField inputField = exclusionInputFieldsMap.get(uuid);
                String value = inputField.getValue();
                if (TextUtils.isEmpty(value))
                {
                    toRemove.add(inputField.getUUID());
                }
            }
        }

        for (UUID uuid : toRemove)
        {
            InputField inputField = exclusionInputFieldsMap.remove(uuid);
            bypassContainer.removeView(inputField);
        }

        String updatedExclusionString = getExclusionListString();
        if (!exclusionListString.equals(updatedExclusionString))
        {
            exclusionListString = updatedExclusionString;
            sendOnValueChanged(exclusionListString);
        }

        if (readonly)
        {
            if (exclusionInputFieldsMap != null && exclusionInputFieldsMap.size() > 0)
            {
                readonlyValueTextView.setVisibility(GONE);

                bypassContainer.setVisibility(VISIBLE);
            }
            else
            {
                readonlyValueTextView.setVisibility(VISIBLE);
                readonlyValueTextView.setText(R.string.not_set);

                bypassContainer.setVisibility(GONE);
            }
        }
        else
        {
            readonlyValueTextView.setVisibility(GONE);

            bypassContainer.setVisibility(VISIBLE);
            addEmptyItem();
        }

        LogWrapper.stopTrace(TAG, "refreshExclusionList", Log.ASSERT);
    }

    private void addEmptyItem()
    {
        LogWrapper.startTrace(TAG, "addEmptyItem", Log.ASSERT, true);

        InputField i = createExclusionInputField();
        i.setValue("");
        exclusionInputFieldsMap.put(i.getUUID(), i);
//        uiHandler.callRefreshExclusionList();

        LogWrapper.stopTrace(TAG, "addEmptyItem", Log.ASSERT);
    }

    private class UIHandler extends Handler
    {
        private static final String REFRESH_UI_ACTION = "REFRESH_UI_ACTION";
        private static final String ADD_EMPTY_ITEM_ACTION = "ADD_EMPTY_ITEM_ACTION";
        private static final String REFRESH_EXCLUSION_LIST_ACTION = "REFRESH_EXCLUSION_LIST_ACTION";
        private static final String SET_EXCLUSION_STRING_ACTION = "SET_EXCLUSION_STRING_ACTION";

        @Override
        public void handleMessage(Message message)
        {
            Bundle b = message.getData();

            LogWrapper.w(TAG, "handleMessage: " + b.toString());

            if (b.containsKey(REFRESH_UI_ACTION))
                refreshUI();
            else if (b.containsKey(ADD_EMPTY_ITEM_ACTION))
                addEmptyItem();
            else if (b.containsKey(REFRESH_EXCLUSION_LIST_ACTION))
                refreshExclusionList();
            else if (b.containsKey(SET_EXCLUSION_STRING_ACTION))
                setExclusionString(b.getString(SET_EXCLUSION_STRING_ACTION));
        }

        public void callRefreshUI()
        {
            Message message = this.obtainMessage();
            Bundle b = new Bundle();
            b.putString(REFRESH_UI_ACTION, "");
            message.setData(b);
            sendMessageDelayed(message, 0);
        }

        public void callRefreshExclusionList()
        {
            Message message = this.obtainMessage();
            Bundle b = new Bundle();
            b.putString(REFRESH_EXCLUSION_LIST_ACTION, "");
            message.setData(b);
            sendMessageDelayed(message, 0);
        }

        public void callAddEmptyItem()
        {
            Message message = this.obtainMessage();
            Bundle b = new Bundle();
            b.putString(ADD_EMPTY_ITEM_ACTION, "");
            message.setData(b);
            sendMessageDelayed(message, 0);
        }

        public void callSetExclusionList(String exclusionString)
        {
            Message message = this.obtainMessage();
            Bundle b = new Bundle();
            b.putString(SET_EXCLUSION_STRING_ACTION, exclusionString);
            message.setData(b);
            sendMessageDelayed(message, 0);
        }

        private void setExclusionString(String exclusionString)
        {
            LogWrapper.startTrace(TAG, "setExclusionString", Log.ASSERT, true);
            String[] exclusion = null;
            exclusionListString = exclusionString;

            if (TextUtils.isEmpty(exclusionListString))
            {
                exclusion = new String[]{""};
            }
            else
            {
                exclusion = ProxyUtils.parseExclusionList(exclusionListString);
            }

            bypassContainer.removeAllViews();
            exclusionInputFieldsMap.clear();

            for (String bypass : exclusion)
            {
                InputField inputField = createExclusionInputField();
                inputField.setValue(bypass);
                exclusionInputFieldsMap.put(inputField.getUUID(), inputField);
            }

            refreshExclusionList();

            LogWrapper.stopTrace(TAG, "setExclusionString", Log.ASSERT);
        }
    }

    public class BypassTextWatcher implements TextWatcher
    {
        private final InputField inputField;

        public BypassTextWatcher(InputField field)
        {
            inputField = field;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int before, int count)
        {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count)
        {
            if (!readonly && inputField.enableTextListener)
            {
                if (start == 0 && before == 0 && count >= 1)
                {
                    uiHandler.callAddEmptyItem();
//                    uiHandler.refreshExclusionList();
                }
            }
        }

        @Override
        public void afterTextChanged(Editable editable)
        {
        }
    }

    public void addValueChangedListener(ValueChangedListener watcher)
    {

        if (mListeners == null)
        {
            mListeners = new ArrayList<ValueChangedListener>();
        }

        mListeners.add(watcher);
    }

    public interface ValueChangedListener
    {
        public void onExclusionListChanged(String result);
    }

    void sendOnValueChanged(String value)
    {
        if (mListeners != null)
        {
            final ArrayList<ValueChangedListener> list = mListeners;
            final int count = list.size();
            for (int i = 0; i < count; i++)
            {
                list.get(i).onExclusionListChanged(value);
            }
        }
    }
}
