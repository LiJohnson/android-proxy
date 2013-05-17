package com.lechucksoftware.proxy.proxysettings.fragments;

import android.content.Intent;
import com.lechucksoftware.proxy.proxysettings.R;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import com.lechucksoftware.proxy.proxysettings.ApplicationGlobals;
import com.lechucksoftware.proxy.proxysettings.activities.DetailsActivity;
import com.lechucksoftware.proxy.proxysettings.activities.MainActivity;
import com.lechucksoftware.proxy.proxysettings.utils.ProxySelectorListAdapter;
import com.shouldit.proxy.lib.ProxyConfiguration;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by marco on 17/05/13.
 */
public class APSelectorFragment extends ListFragment
{
    boolean mDualPane;
    int mCurCheckPosition = 0;

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        final ArrayList<ProxyConfiguration> confsList = (ArrayList<ProxyConfiguration>) ApplicationGlobals.getConfigurationsList();
        Collections.sort(confsList);
        setListAdapter(new ProxySelectorListAdapter(getActivity(), android.R.id.list, confsList));

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View detailsFrame = getActivity().findViewById(R.id.details);
        mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

        if (savedInstanceState != null) {
            // Restore last state for checked position.
            mCurCheckPosition = savedInstanceState.getInt("curChoice", 0);
        }

        if (mDualPane) {
            // In dual-pane mode, the list view highlights the selected item.
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            // Make sure our UI is in the correct state.
            showDetails(mCurCheckPosition);
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("curChoice", mCurCheckPosition);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        showDetails(position);
    }

    /**
     * Helper function to show the details of a selected item, either by
     * displaying a fragment in-place in the current UI, or starting a
     * whole new activity in which it is displayed.
     */
    void showDetails(int index)
    {
        mCurCheckPosition = index;

//        if (mDualPane)
//        {
            // We can display everything in-place with fragments, so update
            // the list to highlight the selected item and show the data.
            getListView().setItemChecked(index, true);

            // Check what fragment is currently shown, replace if needed.
            MainAPPrefsFragment details = (MainAPPrefsFragment) getFragmentManager().findFragmentById(R.id.details);
            if (details == null || details.getShownIndex() != index)
            {
                // Make new fragment to show this selection.
                details = MainAPPrefsFragment.newInstance(index);

                // Execute a transaction, replacing any existing fragment
                // with this one inside the frame.
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                if (index == 0)
                {
                    ft.replace(R.id.details, details);
                }
                else
                {
                    // TODO Check here
                    ft.replace(R.id.details, details);
                }
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }

//        }
//        else
//        {
//            // Otherwise we need to launch a new activity to display
//            // the dialog fragment with selected text.
//            Intent intent = new Intent();
//            intent.setClass(getActivity(), DetailsActivity.class);
//            intent.putExtra("index", index);
//            startActivity(intent);
//        }
    }
}
