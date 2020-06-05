package de.blau.android.photos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.ActionMenuView.OnMenuItemClickListener;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.contract.Ui;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;

/**
 * Very simple photo viewer
 * 
 * @author simon
 *
 */
public class PhotoViewerFragment extends ImmersiveDialogFragment implements OnMenuItemClickListener {
    private static final String DEBUG_TAG = PhotoViewerFragment.class.getName();

    public static final String TAG = "fragment_photo_viewer";

    static final String PHOTO_LIST_KEY = "photo_list";
    static final String START_POS_KEY  = "start_pos";

    private static final int MENUITEM_BACK    = 0;
    private static final int MENUITEM_SHARE   = 1;
    private static final int MENUITEM_GOTO    = 2;
    private static final int MENUITEM_DELETE  = 3;
    private static final int MENUITEM_FORWARD = 4;

    private List<String> photoList = null;

    SubsamplingScaleImageView photoView = null;

    private PhotoPagerAdapter photoPagerAdapter;

    private ViewPager viewPager;

    private MenuItem itemBackward = null;
    private MenuItem itemForward  = null;

    /**
     * Show an info dialog for the supplied OsmElement
     * 
     * @param activity the calling Activity
     * @param photoList list of Uris
     * @param startPos starting position in the list
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull ArrayList<String> photoList, int startPos) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            PhotoViewerFragment photoViewerFragment = newInstance(photoList, startPos);
            photoViewerFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * 
     * @param photoList list of Uris
     * @param startPos starting position in the list
     * @return a n new instance of PhotoViwerFragment
     */
    @NonNull
    public static PhotoViewerFragment newInstance(@NonNull ArrayList<String> photoList, int startPos) {
        PhotoViewerFragment f = new PhotoViewerFragment();

        Bundle args = new Bundle();
        args.putStringArrayList(PHOTO_LIST_KEY, photoList);
        args.putInt(START_POS_KEY, startPos);
        f.setArguments(args);

        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setPositiveButton(R.string.done, doNothingListener);
        builder.setView(createView(savedInstanceState));
        return builder.create();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (!getShowsDialog()) {
            return createView(savedInstanceState);
        }
        return null;
    }

    /**
     * Create the view we want to display
     * 
     * @return the View
     */
    @SuppressWarnings("deprecation")
    private View createView(@Nullable Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(activity);
        int startPos = 0;
        if (savedInstanceState == null) {
            photoList = getArguments().getStringArrayList(PHOTO_LIST_KEY);
            startPos = getArguments().getInt(START_POS_KEY);
        } else {
            photoList = savedInstanceState.getStringArrayList(PHOTO_LIST_KEY);
            startPos = savedInstanceState.getInt(START_POS_KEY);
        }

        View layout = themedInflater.inflate(R.layout.photo_viewer, null);
        photoPagerAdapter = new PhotoPagerAdapter(activity);

        viewPager = (ViewPager) layout.findViewById(R.id.pager);
        viewPager.setAdapter(photoPagerAdapter);
        viewPager.setOffscreenPageLimit(2);
        viewPager.setCurrentItem(startPos);
        ActionMenuView menuView = (ActionMenuView) layout.findViewById(R.id.photoMenuView);
        Menu menu = menuView.getMenu();
        itemBackward = menu.add(Menu.NONE, MENUITEM_BACK, Menu.NONE, R.string.back).setIcon(R.drawable.ic_arrow_back_white_36dp);
        itemBackward.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, MENUITEM_SHARE, Menu.NONE, R.string.share).setIcon(R.drawable.ic_share_white_36dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, MENUITEM_GOTO, Menu.NONE, R.string.photo_viewer_goto).setIcon(R.drawable.ic_map_white_36dp)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        if (Uri.parse(photoList.get(startPos)).getAuthority().equals(getString(R.string.content_provider))) {
            // we can only delete stuff that is provided by our provider
            menu.add(Menu.NONE, MENUITEM_DELETE, Menu.NONE, R.string.delete).setIcon(R.drawable.ic_delete_forever_white_36dp)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        itemForward = menu.add(Menu.NONE, MENUITEM_FORWARD, Menu.NONE, R.string.forward).setIcon(R.drawable.ic_arrow_forward_white_36dp);
        itemForward.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuView.setOnMenuItemClickListener(this);
        return layout;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean multiple = photoList.size() > 1;
        itemForward.setEnabled(multiple);
        itemBackward.setEnabled(multiple);
    }

    class PhotoPagerAdapter extends PagerAdapter {

        Context        mContext;
        LayoutInflater mLayoutInflater;

        /**
         * Construct a new adapter
         * 
         * @param context an Android Context
         */
        public PhotoPagerAdapter(@NonNull Context context) {
            mContext = context;
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return photoList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((LinearLayout) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View itemView = mLayoutInflater.inflate(R.layout.photo_viewer_item, container, false);

            SubsamplingScaleImageView view = itemView.findViewById(R.id.photoView);
            view.setOrientation(SubsamplingScaleImageView.ORIENTATION_90);
            view.setImage(ImageSource.uri(photoList.get(position)));
            container.addView(itemView);

            return itemView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout) object);
        }

        @Override
        public int getItemPosition(Object item) {
            return POSITION_NONE; // hack so that everything gets updated on notifyDataSetChanged
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int size = photoList.size();
        FragmentActivity caller = getActivity();
        Map map = (caller instanceof Main) ? ((Main) caller).getMap() : null;
        final de.blau.android.layer.photos.MapOverlay overlay = map != null ? map.getPhotoLayer() : null;

        int pos = viewPager.getCurrentItem();
        if (photoList != null && !photoList.isEmpty() && pos < size) {
            switch (item.getItemId()) {
            case MENUITEM_BACK:
                pos = pos - 1;
                if (pos == -1) {
                    pos = size - 1;
                }
                viewPager.setCurrentItem(pos);
                break;
            case MENUITEM_FORWARD:
                pos = (pos + 1) % size;
                viewPager.setCurrentItem(pos);
                break;
            case MENUITEM_GOTO:
                try {
                    Photo p = new Photo(caller, Uri.parse(photoList.get(pos)));
                    if (getShowsDialog()) {
                        if (map != null && overlay != null) {
                            App.getLogic().setZoom(map, Ui.ZOOM_FOR_ZOOMTO);
                            map.getViewBox().moveTo(map, p.getLon(), p.getLat());
                            overlay.setSelected(p); // this isn't the same instance as in the layer but should work
                            map.invalidate();
                        }
                        getDialog().dismiss();
                    } else {
                        Intent intent = new Intent(getContext(), Main.class);
                        intent.setData(p.getRefUri(getContext()));
                        getContext().startActivity(intent);
                    }
                } catch (NumberFormatException | IOException e) {
                    Log.e(DEBUG_TAG, "Invalid photo for " + photoList.get(pos));
                }
                break;
            case MENUITEM_SHARE:
                de.blau.android.layer.photos.Util.startExternalPhotoViewer(getContext(), Uri.parse(photoList.get(pos)));
                if (getShowsDialog() && size == 1) {
                    getDialog().dismiss();
                }
                break;
            case MENUITEM_DELETE:
                new AlertDialog.Builder(getContext()).setTitle(R.string.photo_viewer_delete_title)
                        .setPositiveButton(R.string.photo_viewer_delete_button, (dialog, which) -> {
                            int position = viewPager.getCurrentItem();
                            if (position >= 0) { // avoid crashes from bouncing
                                Uri photoUri = Uri.parse(photoList.get(position));
                                try {
                                    // delete from in memory and on device index
                                    try (PhotoIndex index = new PhotoIndex(getContext())) {
                                        index.deletePhoto(getContext(), photoUri);
                                    }
                                    if (getShowsDialog() && overlay != null) {
                                        // as the Photo was selected before calling this it will still have a
                                        // reference in the layer
                                        overlay.setSelected(null);
                                        overlay.invalidate();
                                    } else {
                                        Intent intent = new Intent(getContext(), Main.class);
                                        intent.setAction(Main.ACTION_DELETE_PHOTO);
                                        intent.setData(photoUri);
                                        getContext().startActivity(intent);
                                    }
                                    // actually delete
                                    if (getContext().getContentResolver().delete(photoUri, null, null) >= 1) {
                                        photoList.remove(position);
                                        position = Math.min(position, size - 1); // this will set pos to -1 if empty,
                                        // but we will exit in that case in any case
                                        if (getShowsDialog() && photoList.isEmpty()) { // in fragment mode we want
                                                                                       // to do something else
                                            getDialog().dismiss();
                                        } else {
                                            photoPagerAdapter.notifyDataSetChanged();
                                            viewPager.setCurrentItem(position);
                                            if (photoList.size() == 1 && itemBackward != null && itemForward != null) {
                                                itemBackward.setEnabled(false);
                                                itemForward.setEnabled(false);
                                            }
                                        }
                                    }
                                } catch (java.lang.SecurityException sex) {
                                    Snack.toastTopError(getContext(), getString(R.string.toast_permission_denied, sex.getMessage()));
                                }
                            }
                        }).setNeutralButton(R.string.cancel, null).show();
                break;
            default:
                // do nothing
            }
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        outState.putStringArrayList(PHOTO_LIST_KEY, (ArrayList<String>) photoList);
        outState.putInt(START_POS_KEY, viewPager.getCurrentItem());
    }

    /**
     * Get the current position of the ViewPager
     * 
     * @return the current position
     */
    int getCurrentPosition() {
        return viewPager.getCurrentItem();
    }

    /**
     * Set the ViewPager to the this position
     * 
     * @param pos the new position
     */
    void setCurrentPosition(int pos) {
        viewPager.setCurrentItem(pos);
    }

    /**
     * Add a photo at the end of the list and switch to it
     * 
     * @param photo the photo Uri as a String
     */
    void addPhoto(@NonNull String photo) {
        photoList.add(photo);
        photoPagerAdapter.notifyDataSetChanged();
        setCurrentPosition(photoList.size() - 1);
    }

    /**
     * Replace the existing list of photos
     * 
     * @param list the new list
     * @param pos the new position in list
     */
    void replacePhotos(@NonNull ArrayList<String> list, int pos) {
        photoList.clear();
        photoList.addAll(list);
        photoPagerAdapter.notifyDataSetChanged();
        setCurrentPosition(pos);
    }
}