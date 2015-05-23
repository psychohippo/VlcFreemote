package com.nico.vlcfremote;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nico.vlcfremote.utils.VlcActionFragment;
import com.nico.vlcfremote.utils.VlcConnector;

import java.util.ArrayList;
import java.util.List;

public class DirListingFragment extends VlcActionFragment implements View.OnClickListener {

    // Is this Windows compatible? Who knows...
    private static final String VLC_DEFAULT_START_PATH = "~";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dir_listing, container, false);
    }

    String currentPath;
    String currentPath_display;
    DirListEntry_ViewAdapter dirViewAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        dirViewAdapter = new DirListEntry_ViewAdapter(this, getActivity());
        ((ListView) getActivity().findViewById(R.id.wDirListing_List)).setAdapter(dirViewAdapter);
        currentPath = VLC_DEFAULT_START_PATH;
        currentPath_display = getResources().getString(R.string.dir_listing_default_folder_label);
    }

    @Override
    public void onResume() {
        super.onResume();

        // If we're visible and belong to an activity (?) do an update
        if (getUserVisibleHint() && isAdded()) updateDirectoryList();
    }

    public void updateDirectoryList() {
        // If there's no activity we're not being displayed, so it's better not to update the UI
        if (!isAdded()) return;
        final FragmentActivity activity = getActivity();

        vlcConnection.getVlcConnector().getDirList(currentPath);
        ((TextView) activity.findViewById(R.id.wDirListing_CurrentPath)).setText(currentPath_display);

        dirViewAdapter.clear();
        activity.findViewById(R.id.wDirListing_List).setEnabled(false);
        activity.findViewById(R.id.wDirListing_LoadingIndicator).setVisibility(View.VISIBLE);
    }

    public void Vlc_OnDirListingFetched(List<VlcConnector.DirListEntry> contents) {
        // If there's no activity we're not being displayed, so it's better not to update the UI
        if (!isAdded()) return;
        final FragmentActivity activity = getActivity();

        activity.findViewById(R.id.wDirListing_List).setEnabled(true);
        activity.findViewById(R.id.wDirListing_LoadingIndicator).setVisibility(View.GONE);

        dirViewAdapter.addAll(contents);
    }

    public void Vlc_OnSelectDirIsInvalid() {
        currentPath = VLC_DEFAULT_START_PATH;
        currentPath_display = getResources().getString(R.string.dir_listing_default_folder_label);
        updateDirectoryList();
    }

    private void addPathToPlaylist(final String path) {
        vlcConnection.getVlcConnector().addToPlayList(path);
        if (!vlcConnection.getVlcConnector().getLastKnownStatus().isCurrentlyPlayingSomething()) {
            vlcConnection.getVlcConnector().togglePlay();
        }
        vlcConnection.getVlcConnector().updatePlaylist();
    }

    @Override
    public void onClick(View v) {
        VlcConnector.DirListEntry item = (VlcConnector.DirListEntry) v.getTag();

        switch (v.getId()) {
            case R.id.wDirListElement_Action:
                if (item == null) throw new RuntimeException(DirListingFragment.class.getName() + " received a menu item with no tag");
                addPathToPlaylist(item.path);
                break;

            case R.id.wDirListElement_Name:
                if (item == null) throw new RuntimeException(DirListingFragment.class.getName() + " received a menu item with no tag");
                if (item.isDirectory) {
                    currentPath = item.path;
                    currentPath_display = item.human_friendly_path;
                    updateDirectoryList();
                } else {
                    addPathToPlaylist(item.path);
                }

                break;

            default:
                throw new RuntimeException(DirListingFragment.class.getName() + " received a click event it can't handle.");
        }
    }

    private static class DirListEntry_ViewAdapter extends ArrayAdapter<VlcConnector.DirListEntry> {
        private static final int layoutResourceId = R.layout.fragment_dir_listing_list_element;

        final private LayoutInflater inflater;
        final private View.OnClickListener onClickCallback;

        public DirListEntry_ViewAdapter(View.OnClickListener onClickCallback, Context context) {
            super(context, layoutResourceId, new ArrayList<VlcConnector.DirListEntry>());
            this.inflater = ((Activity) context).getLayoutInflater();
            this.onClickCallback = onClickCallback;
        }

        public static class Row {
            VlcConnector.DirListEntry values;
            ImageView dirOrFile;
            TextView fName;
            ImageButton actionButton;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View row;
            if (convertView == null) {
                row = inflater.inflate(layoutResourceId, parent, false);
            } else {
                row = convertView;
            }

            Row holder = new Row();
            holder.values = this.getItem(position);

            holder.dirOrFile = (ImageView)row.findViewById(R.id.wDirListElement_DirOrFile);
            if (!holder.values.isDirectory) holder.dirOrFile.setVisibility(View.INVISIBLE);

            holder.fName = (TextView)row.findViewById(R.id.wDirListElement_Name);
            holder.fName.setText(holder.values.name);
            holder.fName.setTag(holder.values);
            holder.fName.setOnClickListener(onClickCallback);

            holder.actionButton = (ImageButton)row.findViewById(R.id.wDirListElement_Action);
            holder.actionButton.setTag(holder.values);
            holder.actionButton.setOnClickListener(onClickCallback);

            row.setTag(holder);

            return row;
        }
    }
}
