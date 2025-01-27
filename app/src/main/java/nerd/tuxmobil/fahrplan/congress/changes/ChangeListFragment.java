package nerd.tuxmobil.fahrplan.congress.changes;

import android.content.Context;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.CallSuper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import nerd.tuxmobil.fahrplan.congress.MyApp;
import nerd.tuxmobil.fahrplan.congress.R;
import nerd.tuxmobil.fahrplan.congress.base.AbstractListFragment;
import nerd.tuxmobil.fahrplan.congress.contract.BundleKeys;
import nerd.tuxmobil.fahrplan.congress.models.Meta;
import nerd.tuxmobil.fahrplan.congress.models.Session;
import nerd.tuxmobil.fahrplan.congress.notifications.NotificationHelper;

import static nerd.tuxmobil.fahrplan.congress.extensions.ViewExtensions.requireViewByIdCompat;


/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnSessionListClick}
 * interface.
 */
public class ChangeListFragment extends AbstractListFragment {

    private static final String LOG_TAG = "ChangeListFragment";
    public static final String FRAGMENT_TAG = "changes";
    private OnSessionListClick mListener;
    private List<Session> changesList;
    private boolean sidePane = false;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ChangeListAdapter mAdapter;

    public static ChangeListFragment newInstance(boolean sidePane) {
        ChangeListFragment fragment = new ChangeListFragment();
        Bundle args = new Bundle();
        args.putBoolean(BundleKeys.SIDEPANE, sidePane);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ChangeListFragment() {
    }

    @MainThread
    @CallSuper
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = requireArguments();
        sidePane = args.getBoolean(BundleKeys.SIDEPANE);
        changesList = appRepository.loadChangedSessions();
        Meta meta = appRepository.readMeta();
        boolean useDeviceTimeZone = appRepository.readUseDeviceTimeZoneEnabled();
        Context context = requireContext();
        mAdapter = new ChangeListAdapter(context, changesList, meta.getNumDays(), useDeviceTimeZone);
        MyApp.LogDebug(LOG_TAG, "onCreate, " + changesList.size() + " changes");
    }

    @Override
    @Nullable
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        Context contextThemeWrapper = new ContextThemeWrapper(requireContext(), R.style.Theme_AppCompat_Light);
        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
        int fragmentLayout = sidePane ? R.layout.fragment_session_list_narrow : R.layout.fragment_session_list;
        int headerLayout = sidePane ? R.layout.changes_header : R.layout.header_empty;
        View fragmentView = localInflater.inflate(fragmentLayout, container, false);
        View headerView = localInflater.inflate(headerLayout, null, false);
        ListView listView = requireViewByIdCompat(fragmentView, android.R.id.list);
        listView.addHeaderView(headerView, null, false);
        listView.setHeaderDividersEnabled(false);
        listView.setAdapter(mAdapter);
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        new NotificationHelper(requireContext()).cancelScheduleUpdateNotification();
        appRepository.updateScheduleChangesSeen(true);
    }

    @MainThread
    @CallSuper
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (OnSessionListClick) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnSessionListClick");
        }
    }

    @MainThread
    @CallSuper
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void onRefresh() {
        List<Session> updatedChanges = appRepository.loadChangedSessions();
        if (changesList != null) {
            changesList.clear();
            changesList.addAll(updatedChanges);
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        MyApp.LogDebug(LOG_TAG, "onItemClick");
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            position--;
            Session clicked = changesList.get(mAdapter.getItemIndex(position));
            if (clicked.changedIsCanceled) return;
            mListener.onSessionListClick(clicked);
        }
    }
}
