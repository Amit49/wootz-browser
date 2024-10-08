// Copyright 2011 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.ui.appmenu;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.os.Build;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.chromium.chrome.browser.extensions.ExtensionInfo;
import org.chromium.chrome.browser.extensions.Extensions;
import org.chromium.base.ContextUtils;
import org.chromium.components.embedder_support.view.ContentView;
import org.chromium.components.thinwebview.ThinWebView;
import org.chromium.components.thinwebview.ThinWebViewConstraints;
import org.chromium.components.thinwebview.ThinWebViewFactory;
import org.chromium.content_public.browser.LoadUrlParams;
import org.chromium.content_public.browser.WebContents;
import org.chromium.content_public.browser.WebContentsObserver;
import org.chromium.chrome.browser.content.WebContentsFactory;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.profiles.ProfileManager;
import org.chromium.base.version_info.VersionInfo;
import org.chromium.ui.base.IntentRequestTracker;
import org.chromium.ui.base.ViewAndroidDelegate;
import org.chromium.base.Callback;
import org.chromium.base.SysUtils;
import org.chromium.base.metrics.RecordHistogram;
import org.chromium.base.task.PostTask;
import org.chromium.base.task.TaskTraits;
import org.chromium.chrome.browser.ui.appmenu.internal.R;
import org.chromium.components.browser_ui.styles.ChromeColors;
import org.chromium.components.browser_ui.widget.chips.ChipView;
import org.chromium.components.browser_ui.widget.highlight.ViewHighlighter;
import org.chromium.components.browser_ui.widget.highlight.ViewHighlighter.HighlightParams;
import org.chromium.components.browser_ui.widget.highlight.ViewHighlighter.HighlightShape;
import org.chromium.ui.modelutil.MVCListAdapter.ModelList;
import org.chromium.ui.modelutil.ModelListAdapter;
import org.chromium.ui.modelutil.PropertyModel;
import org.chromium.ui.widget.Toast;
import android.widget.BaseAdapter;

import java.beans.Visibility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.fragment.app.FragmentManager;
import android.util.Log;
import androidx.core.graphics.drawable.DrawableCompat;
import android.database.DataSetObserver;

import androidx.core.widget.NestedScrollView;
import android.widget.LinearLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.Outline;
import android.view.ViewOutlineProvider;
import android.os.Build;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;

/**
 * Shows a popup of menuitems anchored to a host view. When a item is selected we call
 * AppMenuHandlerImpl.AppMenuDelegate.onOptionsItemSelected with the appropriate MenuItem.
 *   - Only visible MenuItems are shown.
 *   - Disabled items are grayed out.
 */
class AppMenu implements OnItemClickListener, OnKeyListener, AppMenuClickHandler {
    private static final float LAST_ITEM_SHOW_FRACTION = 0.5f;

    /** A means of reporting an exception/stack without crashing. */
    private static Callback<Throwable> sExceptionReporter;

    private final int mItemRowHeight;
    private final int mVerticalFadeDistance;
    private final int mNegativeSoftwareVerticalOffset;
    private final int mNegativeVerticalOffsetNotTopAnchored;
    private final int mChipHighlightExtension;
    private final int[] mTempLocation;

    private PopupWindow mPopup;
    private ListView mListView;
    private ModelListAdapter mAdapter;
    private AppMenuHandlerImpl mHandler;
    private View mFooterView;
    private int mCurrentScreenRotation = -1;
    private boolean mIsByPermanentButton;
    private AnimatorSet mMenuItemEnterAnimator;
    private long mMenuShownTimeMs;
    private boolean mSelectedItemBeforeDismiss;
    private ModelList mModelList;

    private int mHeaderResourceId;
    private int mFooterResourceId;

    private GridAdapter mGridAdapter;
    private NestedScrollView mScrollView;
    private BottomSheetBehavior<View> mBehavior;
    private ImageButton mFloatingBackButton;
    private WebContents mCurrentWebContents;
    /**
     * Creates and sets up the App Menu.
     * @param itemRowHeight Desired height for each app menu row.
     * @param handler AppMenuHandlerImpl receives callbacks from AppMenu.
     * @param res Resources object used to get dimensions and style attributes.
     */
    AppMenu(int itemRowHeight, AppMenuHandlerImpl handler, Resources res) {
        mItemRowHeight = itemRowHeight;
        assert mItemRowHeight > 0;

        mHandler = handler;

        mNegativeSoftwareVerticalOffset =
                res.getDimensionPixelSize(R.dimen.menu_negative_software_vertical_offset);
        mVerticalFadeDistance = res.getDimensionPixelSize(R.dimen.menu_vertical_fade_distance);
        mNegativeVerticalOffsetNotTopAnchored =
                res.getDimensionPixelSize(R.dimen.menu_negative_vertical_offset_not_top_anchored);
        mChipHighlightExtension =
                res.getDimensionPixelOffset(R.dimen.menu_chip_highlight_extension);

        mTempLocation = new int[2];
    }

    /**
     * Notifies the menu that the contents of the menu item specified by {@code menuRowId} have
     * changed.  This should be called if icons, titles, etc. are changing for a particular menu
     * item while the menu is open.
     * @param menuRowId The id of the menu item to change.  This must be a row id and not a child
     *                  id.
     */
    public void menuItemContentChanged(int menuRowId) {
        // Make sure we have all the valid state objects we need.
        if (mAdapter == null || mModelList == null || mPopup == null || mListView == null) {
            return;
        }

        // Calculate the item index.
        int index = -1;
        int menuSize = mModelList.size();
        for (int i = 0; i < menuSize; i++) {
            if (mModelList.get(i).model.get(AppMenuItemProperties.MENU_ITEM_ID) == menuRowId) {
                index = i;
                break;
            }
        }
        if (index == -1) return;

        // Check if the item is visible.
        int startIndex = mListView.getFirstVisiblePosition();
        int endIndex = mListView.getLastVisiblePosition();
        if (index < startIndex || index > endIndex) return;

        // Grab the correct View.
        View view = mListView.getChildAt(index - startIndex);
        if (view == null) return;

        // Cause the Adapter to re-populate the View.
        mAdapter.getView(index, view, mListView);
    }

    /**
     * Creates and shows the app menu anchored to the specified view.
     *
     * @param context               The context of the AppMenu (ensure the proper theme is set on
     *                              this context).
     * @param anchorView            The anchor {@link View} of the {@link PopupWindow}.
     * @param isByPermanentButton   Whether or not permanent hardware button triggered it. (oppose
     *                              to software button or keyboard).
     * @param screenRotation        Current device screen rotation.
     * @param visibleDisplayFrame   The display area rect in which AppMenu is supposed to fit in.
     * @param footerResourceId      The resource id for a view to add as a fixed view at the bottom
     *                              of the menu.  Can be 0 if no such view is required.  The footer
     *                              is always visible and overlays other app menu items if
     *                              necessary.
     * @param headerResourceId      The resource id for a view to add as the first item in menu
     *                              list. Can be null if no such view is required. See
     *                              {@link ListView#addHeaderView(View)}.
     * @param highlightedItemId     The resource id of the menu item that should be highlighted.
     *                              Can be {@code null} if no item should be highlighted.  Note that
     *                              {@code 0} is dedicated to custom menu items and can be declared
     *                              by external apps.
     * @param groupDividerResourceId     The resource id of divider menu items. This will be used to
     *         determine the number of dividers that appear in the menu.
     * @param customViewBinders     See {@link AppMenuPropertiesDelegate#getCustomViewBinders()}.
     * @param isMenuIconAtStart     Whether the menu is being shown from a menu icon positioned at
     *                              the start.
     */
    void show(
            Context context,
            final View anchorView,
            boolean isByPermanentButton,
            int screenRotation,
            Rect visibleDisplayFrame,
            @IdRes int footerResourceId,
            @IdRes int headerResourceId,
            @IdRes int groupDividerResourceId,
            Integer highlightedItemId,
            @Nullable List<CustomViewBinder> customViewBinders,
            boolean isMenuIconAtStart) {
        mPopup = new PopupWindow(context);
        mPopup.setFocusable(true);
        mPopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);

        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        View view = createContentView(true);
        dialog.setContentView(view);

        mPopup.setOnDismissListener(
                () -> {
                    recordTimeToTakeActionHistogram();
                    if (anchorView instanceof ImageButton) {
                        ((ImageButton) anchorView).setSelected(false);
                    }

                    if (mMenuItemEnterAnimator != null) mMenuItemEnterAnimator.cancel();

                    mHandler.appMenuDismissed();
                    mHandler.onMenuVisibilityChanged(false);

                    mPopup = null;
                    mAdapter = null;
                    mListView = null;
                    mFooterView = null;
                    mMenuItemEnterAnimator = null;
                });

        // Some OEMs don't actually let us change the background... but they still return the
        // padding of the new background, which breaks the menu height.  If we still have a
        // drawable here even though our style says @null we should use this padding instead...
        Drawable originalBgDrawable = mPopup.getBackground();

        // Setting this to a transparent ColorDrawable instead of null because setting it to null
        // prevents the menu from being dismissed by tapping outside or pressing the back button on
        // Android L.
        mPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // Make sure that the popup window will be closed when touch outside of it.
        mPopup.setOutsideTouchable(true);

    //         @Override
    //         public boolean onTouch(View v, MotionEvent event) {
    //             switch (event.getAction()) {
    //                 case MotionEvent.ACTION_DOWN:
    //                     startY = event.getY();
    //                     startX = event.getX();
    //                     break;
    //                 case MotionEvent.ACTION_UP:
    //                     float endY = event.getY();
    //                     float endX = event.getX();
    //                     if (!isAClick(startX, endX, startY, endY)) {
    //                         if (startY < endY && !mScrollView.canScrollVertically(-1)) {
    //                             // Swiping down and scroll view is at the top
    //                             mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    //                             return true;
    //                         } else if (startY > endY && !mScrollView.canScrollVertically(1)) {
    //                             // Swiping up and scroll view is at the bottom
    //                             dismiss();
    //                             return true;
    //                         }
    //                     }
    //                     break;
    //             }
    //             return false;
    //         }

    //         private boolean isAClick(float startX, float endX, float startY, float endY) {
    //             float differenceX = Math.abs(startX - endX);
    //             float differenceY = Math.abs(startY - endY);
    //             return !(differenceX > CLICK_ACTION_THRESHOLD || differenceY > CLICK_ACTION_THRESHOLD);
    //         }
    //     });
    // }

    private View createContentView(boolean test) {
        // Context context = getContext();
        // View contentView = LayoutInflater.from(context).inflate(R.layout.app_menu_bottom_sheet_layout, null);
        // mGridView = contentView.findViewById(R.id.app_menu_grid);
        // mGridView.setNumColumns(GRID_COLUMNS);
        // mGridView.setAdapter(mAdapter);
        // mGridView.setOnItemClickListener(this);

        // inflateHeader(mHeaderResourceId, contentView);
        // inflateFooter(mFooterResourceId, contentView);

        // return contentView;


        // Context context = getContext();
        // CoordinatorLayout coordinatorLayout = new CoordinatorLayout(context);
        // coordinatorLayout.setLayoutParams(new ViewGroup.LayoutParams(
        //         ViewGroup.LayoutParams.MATCH_PARENT,
        //         ViewGroup.LayoutParams.MATCH_PARENT));

        // mGridView = new GridView(context);
        // mGridView.setLayoutParams(new ViewGroup.LayoutParams(
        //         ViewGroup.LayoutParams.MATCH_PARENT,
        //         ViewGroup.LayoutParams.WRAP_CONTENT));
        // mGridView.setNumColumns(3); // Adjust as needed
        // mGridAdapter = new GridAdapter(context, mModelList);
        // mGridView.setAdapter(mGridAdapter);
        // mGridView.setOnItemClickListener(this);

        // coordinatorLayout.addView(mGridView);

        // coordinatorLayout.setOnTouchListener(new CustomTouchListener());

        // return coordinatorLayout;

        
        // NestedScrollView scrollView = new NestedScrollView(getContext());
        // scrollView.setLayoutParams(new ViewGroup.LayoutParams(
        //         ViewGroup.LayoutParams.MATCH_PARENT,
        //         ViewGroup.LayoutParams.WRAP_CONTENT));

        // mGridView = new GridView(getContext());
        // mGridView.setLayoutParams(new ViewGroup.LayoutParams(
        //         ViewGroup.LayoutParams.MATCH_PARENT,
        //         ViewGroup.LayoutParams.WRAP_CONTENT));
        // mGridView.setNumColumns(3); // Adjust as needed
        // mGridAdapter = new GridAdapter(getContext(), mModelList);
        // mGridView.setAdapter(mGridAdapter);
        // mGridView.setOnItemClickListener(this);

        // // Disable GridView scrolling
        // mGridView.setNestedScrollingEnabled(true);

        // scrollView.addView(mGridView);

        // return scrollView;


        NestedScrollView scrollView = new NestedScrollView(getContext());
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        
        // Create a FrameLayout to wrap the GridView
        FrameLayout gridViewWrapper = new FrameLayout(getContext());
        FrameLayout.LayoutParams wrapperParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        
        // Set margins for the wrapper (adjust these values as needed)
        int margin = dpToPx(32); // Convert 16dp to pixels
        wrapperParams.setMargins(
            0,
            -margin, 
            0, 
            margin
        );
        gridViewWrapper.setLayoutParams(wrapperParams);
        
        // if (!test) {
        //     mGridView = new GridView(getContext());
        //     mGridView.setLayoutParams(new ViewGroup.LayoutParams(
        //             ViewGroup.LayoutParams.MATCH_PARENT,
        //             ViewGroup.LayoutParams.WRAP_CONTENT));
        //     mGridView.setNumColumns(3); // Adjust as needed
        //     mGridAdapter = new GridAdapter(getContext(), mModelList);
        //     mGridView.setAdapter(mGridAdapter);
        //     mGridView.setOnItemClickListener(this);
            
        //     // Disable GridView scrolling
        //     mGridView.setNestedScrollingEnabled(true);
            
        //     // Add GridView to the wrapper
        //     gridViewWrapper.addView(mGridView);
            
        //     // Add the wrapper to the scrollView
        //     scrollView.addView(gridViewWrapper);
        // } else {
        //     Profile profile = ProfileManager.getLastUsedRegularProfile();
        //     WebContents webContents = WebContentsFactory.createWebContents(profile, true, true);
        //     ContentView contentView = ContentView.createContentView(getContext(), null, webContents);
    
        //     gridViewWrapper.addView(contentView);
    
        //     webContents.getNavigationController().loadUrl(
        //             new LoadUrlParams("chrome-extension://nooifbgheppjhcogpnlegfapppjlinno/src/pages/popup/index.html"));
        // }
        
        return scrollView;  

        // Context context = getContext();
        // View contentView = LayoutInflater.from(context).inflate(R.layout.app_menu_bottom_sheet_layout, null);
    
        // // Find the GridView in the inflated layout
        // mGridView = contentView.findViewById(R.id.app_menu_grid);
        
        // // Check if the GridView already has a parent
        // // if (mGridView.getParent() != null) {
        // //     ((ViewGroup) mGridView.getParent()).removeView(mGridView); // Remove it from its current parent
        // // }
    
        // // Set up the GridView adapter
        // mGridAdapter = new GridAdapter(context, mModelList);
        // mGridView.setAdapter(mGridAdapter);
        // mGridView.setOnItemClickListener(this);
    
        // // Create a NestedScrollView to wrap the GridView
        // // mScrollView = new NestedScrollView(context);
        // // mScrollView.setLayoutParams(new ViewGroup.LayoutParams(
        // //         ViewGroup.LayoutParams.MATCH_PARENT,
        // //         ViewGroup.LayoutParams.WRAP_CONTENT));
    
        // // // Add the GridView to the NestedScrollView
        // // mScrollView.addView(mGridView);
    
        // // Inflate header and footer if needed
        // inflateHeader(mHeaderResourceId, contentView);
        // inflateFooter(mFooterResourceId, contentView);
    
        // return mScrollView; // Return the NestedScrollViewew; // Return the NestedScrollView



    }

// In the code below we are setting the margin respective to parent i think

    private View createWebView(int i) {
        Log.d(TAG, "EXTS: " + Extensions.getExtensionsInfo().get(i).toString());

        NestedScrollView scrollView = new NestedScrollView(getContext());
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        
        // Create a FrameLayout to wrap the GridView
        FrameLayout viewWrapper = new FrameLayout(getContext());
        FrameLayout.LayoutParams wrapperParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        
        // Set margins for the wrapper (adjust these values as needed)
        int margin = dpToPx(32); // Convert 16dp to pixels
        wrapperParams.setMargins(
            0,
            -margin, 
            0, 
            margin
        );
        viewWrapper.setLayoutParams(wrapperParams);

        Profile profile = ProfileManager.getLastUsedRegularProfile();
        WebContents webContents = WebContentsFactory.createWebContents(profile, true, false);
        ContentView contentView = ContentView.createContentView(getContext(), null, webContents);
        webContents.setDelegates(
            VersionInfo.getProductVersion(),
            ViewAndroidDelegate.createBasicDelegate(contentView),
            contentView,
            mHandler.getWindowAndroid(),
            WebContents.createDefaultInternalsHolder());

        Log.d(TAG, "contentview " + contentView.toString());
        // viewWrapper.addView(contentView);

        IntentRequestTracker intentRequestTracker = mHandler.getWindowAndroid().getIntentRequestTracker();
        ThinWebView thinWebView = ThinWebViewFactory.create(
            getContext(), new ThinWebViewConstraints(), intentRequestTracker);
        thinWebView.attachWebContents(webContents, contentView, null);
        float borderRadius = dpToPx(24); // You can adjust this value as needed
        thinWebView.getView().setClipToOutline(true);
        thinWebView.getView().setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), borderRadius);
            }
        });


        String popupUrl = Extensions.getExtensionsInfo().get(i).getPopupUrl();
        webContents.getNavigationController().loadUrl(
                new LoadUrlParams(popupUrl));
        
        View view = thinWebView.getView();
        view.setTag(webContents);

        mCurrentWebContents = webContents;
        setupFloatingBackButton();

        return view;
    }

    private void returnToAppMenu() {
        View view = getView();
        if (view != null) {
            view.findViewById(R.id.app_menu_grid).setVisibility(View.VISIBLE);
            view.findViewById(R.id.app_menu_extensions).setVisibility(View.VISIBLE);
            view.findViewById(R.id.extensions_divider).setVisibility(View.VISIBLE);
            view.findViewById(R.id.web_view_container).setVisibility(View.GONE);
            if (mFloatingBackButton != null) {
                mFloatingBackButton.setVisibility(View.GONE);
            }
        }
    }

    private void setupFloatingBackButton() {
        View view = getView();
        if (view != null) {
            mFloatingBackButton = view.findViewById(R.id.floating_back_button);
            mFloatingBackButton.setVisibility(View.VISIBLE);
            updateFloatingBackButtonState();

            // Set up a runnable to periodically check and update the back button state
            final Handler handler = new Handler();
            final Runnable updateBackButton = new Runnable() {
                @Override
                public void run() {
                    updateFloatingBackButtonState();
                    handler.postDelayed(this, 500); // Check every 500ms
                }
            };
            handler.post(updateBackButton);
        }
    }

    private void updateFloatingBackButtonState() {
        if (mCurrentWebContents != null && mFloatingBackButton != null) {
            if (mCurrentWebContents.getNavigationController().canGoBack()) {
                mFloatingBackButton.setOnClickListener(v -> mCurrentWebContents.getNavigationController().goBack());
                mFloatingBackButton.setVisibility(View.VISIBLE);
            } else {
                mFloatingBackButton.setOnClickListener(v -> returnToAppMenu());
                mFloatingBackButton.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        View parent = (View) getView().getParent();
        parent.setBackgroundColor(Color.TRANSPARENT);

        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) parent.getLayoutParams();
        
        // Using hardcoded values: 16dp for left, right, and bottom margins
        int marginInPixels = dpToPx(16);
        
        layoutParams.setMargins(
            32,  // LEFT
            -32,               // TOP set the margin here
            32,  // RIGHT
            32   // BOTTOM /* for some reson this doesn't work so set negative margin on top */
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            float borderRadius = dpToPx(24); // You can adjust this value as needed
            parent.setClipToOutline(true);
            parent.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), borderRadius);
                }
            });
        }

        View gridView = parent.findViewById(R.id.app_menu_grid);
        gridView.setVisibility(View.VISIBLE);
        createExtensionsRow();
        FrameLayout webView = (FrameLayout) parent.findViewById(R.id.web_view);
        webView.setVisibility(View.GONE);

        // webView.addView(createWebView());


        // Set up bottom sheet callback to maintain bottom margin when expanded
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheet.setPadding(0, 0, 0, marginInPixels);
                } else {
                    bottomSheet.setPadding(0, 0, 0, 0);
                }
            }
            ViewHighlighter.turnOnHighlight(viewToHighlight, highlightParams);
        }

        // Set the adapter after the header is added to avoid crashes on JellyBean.
        // See crbug.com/761726.
        mListView.setAdapter(mAdapter);

        // super.onActivityCreated(savedInstanceState);
    
        // View parent = (View) getView().getParent();
        // parent.setBackgroundColor(Color.TRANSPARENT);
    
        // CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) parent.getLayoutParams();
        
        // // Set fixed margins
        // int marginInPixels = dpToPx(32);
        // layoutParams.setMargins(marginInPixels, -marginInPixels, marginInPixels, marginInPixels);
        // parent.setLayoutParams(layoutParams);
    
        // // Set up BottomSheetBehavior
        // mBehavior = BottomSheetBehavior.from(parent);
        // mBehavior.setSkipCollapsed(true);
        // mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        
        // // Disable dragging on the BottomSheetBehavior
        // mBehavior.setDraggable(false);
    
        // // Add a callback to maintain the bottom margin when expanded
        // mBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
        //     @Override
        //     public void onStateChanged(@NonNull View bottomSheet, int newState) {
        //         if (newState == BottomSheetBehavior.STATE_EXPANDED) {
        //             bottomSheet.setPadding(0, 0, 0, marginInPixels);
        //         } else {
        //             bottomSheet.setPadding(0, 0, 0, 0);
        //         }
        //     }
    
        //     @Override
        //     public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        //         // Not needed for this implementation
        //     }
        // });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private void createExtensionsRow() {
        Context context = getContext();
        View view = getView();
        if(view == null) return;

        View extensionsDivider = view.findViewById(R.id.extensions_divider);
        LinearLayout extensionsContainer = view.findViewById(R.id.app_menu_extensions_container);
        HorizontalScrollView scrollView = view.findViewById(R.id.extensions_scroll_view);
        LinearLayout parent = view.findViewById(R.id.app_menu_extensions);

        extensionsContainer.removeAllViews();

        int extensionsCount = 0;
        for(int i = 0; i < Extensions.getExtensionsInfo().size(); i++) {
            ExtensionInfo extension = Extensions.getExtensionsInfo().get(i);
            ImageButton extensionIcon = new ImageButton(context);

            if (extension.getName().equals("Web Store")) {
                continue;
            }

            if(extension.getIconBitmap() != null){
                extensionIcon.setImageBitmap(extension.getIconBitmap());
            } else {
                extensionIcon.setImageResource(R.drawable.test_extension_logo);
            }

            extensionIcon.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(48), dpToPx(48)));
            final int index = i;
            extensionIcon.setOnClickListener(v -> openExtensionWebView(index));
            extensionIcon.setOnLongClickListener(v -> {
                showDeleteExtensionDialog(index);
                return true;
            });
            extensionsContainer.addView(extensionIcon);
            extensionsCount ++;
        }

        if (extensionsCount == 0) {
            parent.setVisibility(View.GONE);
            extensionsDivider.setVisibility(View.GONE);
            scrollView.setVisibility(View.GONE);
        } else {
            parent.setVisibility(View.VISIBLE);
            extensionsDivider.setVisibility(View.VISIBLE);
            scrollView.setVisibility(View.VISIBLE);
        }
    }

    private void showDeleteExtensionDialog(int extensionIndex) {
        Context context = getContext();
        if (context == null) return;
    
        new androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Delete Extension")
            .setMessage("Do you want to delete this extension?")
            .setPositiveButton("Delete", (dialog, which) -> {
                deleteExtension(extensionIndex);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteExtension(int extensionIndex) {
        // Extensions.getExtensionsInfo().remove(extensionIndex);
        String extensionId = Extensions.getExtensionsInfo().get(extensionIndex).getId();
        Log.d(TAG,"Deleting extension " + extensionId);
        Extensions.uninstallExtension(extensionId);
        createExtensionsRow();
    }

    private void openExtensionWebView(int index) {
        View view = getView();
        if (view != null) {
            view.findViewById(R.id.app_menu_grid).setVisibility(View.GONE);
            view.findViewById(R.id.app_menu_extensions).setVisibility(View.GONE);
            view.findViewById(R.id.extensions_divider).setVisibility(View.GONE);
            FrameLayout webViewContainer = view.findViewById(R.id.web_view_container);
            webViewContainer.setVisibility(View.VISIBLE);
            FrameLayout webViewFrame = view.findViewById(R.id.web_view);
            webViewFrame.removeAllViews();
            webViewFrame.setVisibility(View.VISIBLE);
            View webView = createWebView(index);
            webViewFrame.addView(webView);
        }
    }

    public boolean onBackPressed() {
        View view = getView();
        if (view != null && view.findViewById(R.id.app_menu_grid).getVisibility() == View.GONE) {
            returnToAppMenu();
            return true;
        }
        return false;
    }

    @Override
    public void show(@NonNull FragmentManager manager, @Nullable String tag) {
        Log.d(TAG, Extensions.getExtensionsInfo().toString());

        Log.d(TAG, "show called with tag: " + tag);
        try {
            mPopup.showAtLocation(
                    anchorView.getRootView(),
                    Gravity.NO_GRAVITY,
                    popupPosition[0],
                    popupPosition[1]);
        } catch (WindowManager.BadTokenException e) {
            // Intentionally ignore BadTokenException. This can happen in a real edge case where
            // parent.getWindowToken is not valid. See http://crbug.com/826052 &
            // https://crbug.com/1105831.
            return;
        }

        mSelectedItemBeforeDismiss = false;
        mMenuShownTimeMs = SystemClock.elapsedRealtime();

        mListView.setOnItemClickListener(this);
        mListView.setItemsCanFocus(true);
        mListView.setOnKeyListener(this);

        mHandler.onMenuVisibilityChanged(true);

        if (mVerticalFadeDistance > 0) {
            mListView.setVerticalFadingEdgeEnabled(true);
            mListView.setFadingEdgeLength(mVerticalFadeDistance);
        }

        // Don't animate the menu items for low end devices.
        if (!SysUtils.isLowEndDevice()) {
            mListView.addOnLayoutChangeListener(
                    new View.OnLayoutChangeListener() {
                        @Override
                        public void onLayoutChange(
                                View v,
                                int left,
                                int top,
                                int right,
                                int bottom,
                                int oldLeft,
                                int oldTop,
                                int oldRight,
                                int oldBottom) {
                            mListView.removeOnLayoutChangeListener(this);
                            runMenuItemEnterAnimations();
                        }
                    });
        }
    }

    @VisibleForTesting
    static int[] getPopupPosition(
            int[] tempLocation,
            boolean isByPermanentButton,
            int negativeSoftwareVerticalOffset,
            int screenRotation,
            Rect appRect,
            Rect padding,
            View anchorView,
            int popupWidth,
            int viewLayoutDirection,
            int popupHeight) {
        anchorView.getLocationInWindow(tempLocation);
        int anchorViewX = tempLocation[0];
        int anchorViewY = tempLocation[1];
        if (ChromeFeatureList.sMoveTopToolbarToBottom.isEnabled()) {
            // moves the view offset up by the height of the popup
            anchorViewY -= popupHeight;
            // fix it if it goes offscreen
            if (anchorViewY <= negativeSoftwareVerticalOffset)
                anchorViewY = negativeSoftwareVerticalOffset;
        }
        int[] offsets = new int[2];
        // If we have a hardware menu button, locate the app menu closer to the estimated
        // hardware menu button location.
        if (isByPermanentButton) {
            int horizontalOffset = -anchorViewX;
            switch (screenRotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                    horizontalOffset += (appRect.width() - popupWidth) / 2;
                    break;
                case Surface.ROTATION_90:
                    horizontalOffset += appRect.width() - popupWidth;
                    break;
                case Surface.ROTATION_270:
                    break;
                default:
                    assert false;
                    break;
            }
            offsets[0] = horizontalOffset;
            // The menu is displayed above the anchored view, so shift the menu up by the bottom
            // padding of the background.
            offsets[1] = -padding.bottom;
        } else {
            offsets[1] = -negativeSoftwareVerticalOffset;
            if (viewLayoutDirection != View.LAYOUT_DIRECTION_RTL) {
                offsets[0] = anchorView.getWidth() - popupWidth;
            }
        }

        int xPos = anchorViewX + offsets[0];
        int yPos = anchorViewY + offsets[1];
        int[] position = {xPos, yPos};
        return position;
    }

    @Override
    public void onItemClick(PropertyModel model) {
        if (!model.get(AppMenuItemProperties.ENABLED)) return;

        int id = model.get(AppMenuItemProperties.MENU_ITEM_ID);
        mSelectedItemBeforeDismiss = true;
        dismiss();
        mHandler.onOptionsItemSelected(id);
    }

    @Override
    public boolean onItemLongClick(PropertyModel model, View view) {
        if (!model.get(AppMenuItemProperties.ENABLED)) return false;

        mSelectedItemBeforeDismiss = true;
        CharSequence titleCondensed = model.get(AppMenuItemProperties.TITLE_CONDENSED);
        CharSequence message =
                TextUtils.isEmpty(titleCondensed)
                        ? model.get(AppMenuItemProperties.TITLE)
                        : titleCondensed;
        return showToastForItem(message, view);
    }

    @VisibleForTesting
    boolean showToastForItem(CharSequence message, View view) {
        Context context = view.getContext();
        final @ColorInt int backgroundColor =
                ChromeColors.getSurfaceColor(context, R.dimen.toast_elevation);
        return new Toast.Builder(context)
                .withText(message)
                .withAnchoredView(view)
                .withBackgroundColor(backgroundColor)
                .withTextAppearance(R.style.TextAppearance_TextSmall_Primary)
                .buildAndShow();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        onItemClick(mModelList.get(position).model);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (mListView == null) return false;
        if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                event.startTracking();
                v.getKeyDispatcherState().startTracking(event, this);
                return true;
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                v.getKeyDispatcherState().handleUpEvent(event);
                if (event.isTracking() && !event.isCanceled()) {
                    dismiss();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Update the menu items.
     * @param newModelList The new menu item list will be displayed.
     * @param adapter The adapter for visible items in the Menu.
     */
    void updateMenu(ModelList newModelList, ModelListAdapter adapter) {
        mModelList = newModelList;
        mAdapter = adapter;
    }

    /** Dismisses the app menu and cancels the drag-to-scroll if it is taking place. */
    void dismiss() {
        if (isShowing()) {
            mPopup.dismiss();
        }
    }

    /**
     * @return Whether the app menu is currently showing.
     */
    boolean isShowing() {
        if (mPopup == null) {
            return false;
        }
        return mPopup.isShowing();
    }

    /**
     * @return {@link PopupWindow} that displays all the menu options and optional footer.
     */
    PopupWindow getPopup() {
        return mPopup;
    }

    /**
     * @return {@link ListView} that contains all of the menu options.
     */
    ListView getListView() {
        return mListView;
    }

    /**
     * @return The menu instance inside of this class.
     */
    ModelList getMenuModelList() {
        return mModelList;
    }

    /**
     * Find the {@link PropertyModel} associated with the given id. If the menu item is not found,
     * return null.
     * @param itemId The id of the menu item to find.
     * @return The {@link PropertyModel} has the given id. null if not found.
     */
    PropertyModel getMenuItemPropertyModel(int itemId) {
        for (int i = 0; i < mModelList.size(); i++) {
            PropertyModel model = mModelList.get(i).model;
            if (model.get(AppMenuItemProperties.MENU_ITEM_ID) == itemId) {
                return model;
            } else if (model.get(AppMenuItemProperties.SUBMENU) != null) {
                ModelList subList = model.get(AppMenuItemProperties.SUBMENU);
                for (int j = 0; j < subList.size(); j++) {
                    PropertyModel subModel = subList.get(j).model;
                    if (subModel.get(AppMenuItemProperties.MENU_ITEM_ID) == itemId) {
                        return subModel;
                    }
                }
            }
        }
        return null;
    }

    /** Invalidate the app menu data. See {@link AppMenuAdapter#notifyDataSetChanged}. */
    void invalidate() {
        if (mAdapter != null) mAdapter.notifyDataSetChanged();
    }

    private int setMenuHeight(
            List<Integer> menuItemIds,
            List<Integer> heightList,
            Rect appDimensions,
            Rect padding,
            int footerHeight,
            int headerHeight,
            View anchorView,
            @IdRes int groupDividerResourceId,
            int anchorViewOffset) {
        int anchorViewImpactHeight = mIsByPermanentButton ? anchorView.getHeight() : 0;

        int availableScreenSpace =
                appDimensions.height()
                        - anchorViewOffset
                        - padding.bottom
                        - footerHeight
                        - headerHeight
                        - anchorViewImpactHeight;
        if (ChromeFeatureList.sMoveTopToolbarToBottom.isEnabled()) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N) {
                // due to an Android Nougat bug the popup does not appear above the anchorview.
                // the display is not pleasant, so we reduce the space
                availableScreenSpace -= anchorView.getHeight();
            }
        }
        if (mIsByPermanentButton) availableScreenSpace -= padding.top;
        if (availableScreenSpace <= 0 && sExceptionReporter != null) {
            String logMessage =
                    "there is no screen space for app menn, mIsByPermanentButton = "
                            + mIsByPermanentButton
                            + ", anchorViewOffset = "
                            + anchorViewOffset
                            + ", appDimensions.height() = "
                            + appDimensions.height()
                            + ", anchorView.getHeight() = "
                            + anchorView.getHeight()
                            + " padding.top = "
                            + padding.top
                            + ", padding.bottom = "
                            + padding.bottom
                            + ", footerHeight = "
                            + footerHeight
                            + ", headerHeight = "
                            + headerHeight;
            PostTask.postTask(
                    TaskTraits.BEST_EFFORT_MAY_BLOCK,
                    () -> sExceptionReporter.onResult(new Throwable(logMessage)));
        }

        int menuHeight =
                calculateHeightForItems(
                        menuItemIds, heightList, groupDividerResourceId, availableScreenSpace);
        menuHeight += footerHeight + headerHeight + padding.top + padding.bottom;
        mPopup.setHeight(menuHeight);
        return menuHeight;

    }

    @VisibleForTesting
    int calculateHeightForItems(
            List<Integer> menuItemIds,
            List<Integer> heightList,
            @IdRes int groupDividerResourceId,
            int screenSpaceForItems) {
        int availableScreenSpace = screenSpaceForItems > 0 ? screenSpaceForItems : 0;
        int spaceForFullItems = 0;

        for (int i = 0; i < heightList.size(); i++) {
            spaceForFullItems += heightList.get(i);
        }

        int menuHeight;
        // Fade out the last item if we cannot fit all items.
        if (availableScreenSpace < spaceForFullItems) {
            int spaceForItems = 0;
            int lastItem = 0;
            // App menu should show 1 full item at least.
            do {
                spaceForItems += heightList.get(lastItem++);
                if (spaceForItems + heightList.get(lastItem) > availableScreenSpace) {
                    break;
                }
            } while (lastItem < heightList.size() - 1);

            int spaceForPartialItem = (int) (LAST_ITEM_SHOW_FRACTION * heightList.get(lastItem));
            // Determine which item needs hiding. We only show Partial of the last item, if there is
            // not enough screen space to partially show the last identified item, then partially
            // show the second to last item instead. We also do not show the partial divider line.
            assert menuItemIds.size() == heightList.size();
            while (lastItem > 1
                    && (spaceForItems + spaceForPartialItem > availableScreenSpace
                            || menuItemIds.get(lastItem) == groupDividerResourceId)) {
                // If we have space for < 2.5 items, size menu to available screen space.
                if (spaceForItems <= availableScreenSpace && lastItem < 3) {
                    spaceForPartialItem = availableScreenSpace - spaceForItems;
                    break;
                }
                spaceForItems -= heightList.get(lastItem - 1);
                spaceForPartialItem =
                        (int) (LAST_ITEM_SHOW_FRACTION * heightList.get(lastItem - 1));
                lastItem--;
            }

            menuHeight = spaceForItems + spaceForPartialItem;
        } else {
            menuHeight = spaceForFullItems;
        }
        return menuHeight;
    }

    private void runMenuItemEnterAnimations() {
        mMenuItemEnterAnimator = new AnimatorSet();
        AnimatorSet.Builder builder = null;

        ViewGroup list = mListView;
        for (int i = 0; i < list.getChildCount(); i++) {
            View view = list.getChildAt(i);
            Object animatorObject = view.getTag(R.id.menu_item_enter_anim_id);
            if (animatorObject != null) {
                if (builder == null) {
                    builder = mMenuItemEnterAnimator.play((Animator) animatorObject);
                } else {
                    builder.with((Animator) animatorObject);
                }
            }
        }

        mMenuItemEnterAnimator.start();
    }

    private int inflateFooter(int footerResourceId, View contentView, int menuWidth) {
        if (footerResourceId == 0) {
            mFooterView = null;
            return 0;
        }

        ViewStub footerStub = (ViewStub) contentView.findViewById(R.id.app_menu_footer_stub);
        footerStub.setLayoutResource(footerResourceId);
        mFooterView = footerStub.inflate();

        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(menuWidth, MeasureSpec.EXACTLY);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        mFooterView.measure(widthMeasureSpec, heightMeasureSpec);

        if (mHandler != null) mHandler.onFooterViewInflated(mFooterView);

        return mFooterView.getMeasuredHeight();
    }

    private int inflateHeader(int headerResourceId, View contentView, int menuWidth) {
        if (headerResourceId == 0) return 0;

        View headerView =
                LayoutInflater.from(contentView.getContext())
                        .inflate(headerResourceId, mListView, false);
        mListView.addHeaderView(headerView);

        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(menuWidth, MeasureSpec.EXACTLY);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        headerView.measure(widthMeasureSpec, heightMeasureSpec);

        if (mHandler != null) mHandler.onHeaderViewInflated(headerView);

        return headerView.getMeasuredHeight();
    }

    void finishAnimationsForTests() {
        if (mMenuItemEnterAnimator != null) mMenuItemEnterAnimator.end();
    }

    private void recordTimeToTakeActionHistogram() {
        final String histogramName =
                "Mobile.AppMenu.TimeToTakeAction."
                        + (mSelectedItemBeforeDismiss ? "SelectedItem" : "Abandoned");
        final long timeToTakeActionMs = SystemClock.elapsedRealtime() - mMenuShownTimeMs;
        RecordHistogram.recordMediumTimesHistogram(histogramName, timeToTakeActionMs);
    }

    private int getMenuItemHeight(
            int itemId, Context context, @Nullable List<CustomViewBinder> customViewBinders) {
        // Check if |item| is custom type
        if (customViewBinders != null) {
            for (int i = 0; i < customViewBinders.size(); i++) {
                CustomViewBinder binder = customViewBinders.get(i);
                if (binder.getItemViewType(itemId) != CustomViewBinder.NOT_HANDLED) {
                    return binder.getPixelHeight(context);
                }
            }
        }
        return mItemRowHeight;
    }

    /** @param reporter A means of reporting an exception without crashing. */
    static void setExceptionReporter(Callback<Throwable> reporter) {
        sExceptionReporter = reporter;
    }

    public void setHeaderResourceId(int headerResourceId) {
        mHeaderResourceId = headerResourceId;
    }

    public void setFooterResourceId(int footerResourceId) {
        mFooterResourceId = footerResourceId;
    }

    public boolean isShowing() {
        return getDialog() != null && getDialog().isShowing();
    }

    public GridView getGridView() {
        return mGridView;
    }

    private class GridAdapter extends BaseAdapter {
        private ModelList mModelList;
        private LayoutInflater mInflater;
        private Map<Integer, Integer> mDisplayToOriginalPosition;
        private List<Integer> mValidItemPositions;

        public GridAdapter(Context context, ModelList modelList) {
            mModelList = modelList;
            mInflater = LayoutInflater.from(context);
            updateValidItems();
        }

        public void updateValidItems() {
            if (mModelList == null) {
                return;
            }

            mDisplayToOriginalPosition = new HashMap<>();
            mValidItemPositions = new ArrayList<>();
            for (int i = 0; i < mModelList.size(); i++) {
                PropertyModel model = mModelList.get(i).model;
                if (isValidMenuItem(model)) {
                    mDisplayToOriginalPosition.put(mValidItemPositions.size(), i);
                    mValidItemPositions.add(i);
                }
            }
            notifyDataSetChanged();
        }

        private boolean isValidMenuItem(PropertyModel model) {
            return model != null &&
                   !TextUtils.isEmpty(model.get(AppMenuItemProperties.TITLE)) &&
                   model.get(AppMenuItemProperties.ICON) != null;
        }

        @Override
        public int getCount() {
            return mValidItemPositions.size();
        }

        @Override
        public Object getItem(int position) {
            int originalPosition = mValidItemPositions.get(position);
            return mModelList.get(originalPosition).model;
        }

        @Override
        public long getItemId(int position) {
            return mValidItemPositions.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.grid_item_layout, parent, false);
                convertView.setTag(new ViewHolder(convertView));
            }

            ViewHolder holder = (ViewHolder) convertView.getTag();
            PropertyModel model = (PropertyModel) getItem(position);
            holder.bindModel(model,convertView);

            return convertView;
        }

        private class ViewHolder {
            ImageView iconView;
            TextView titleView;

            ViewHolder(View view) {
                iconView = view.findViewById(R.id.item_icon);
                titleView = view.findViewById(R.id.item_title);
            }

            void bindModel(PropertyModel model,View view) {
                Drawable icon = model.get(AppMenuItemProperties.ICON);
                CharSequence title = model.get(AppMenuItemProperties.TITLE);

                if (icon != null) {
                    Drawable adaptiveIcon = DrawableCompat.wrap(icon.mutate());
                    DrawableCompat.setTint(adaptiveIcon, titleView.getCurrentTextColor());
                    iconView.setImageDrawable(adaptiveIcon);
                } else {
                    iconView.setImageDrawable(null);
                }
            
                titleView.setText(title);
            
                boolean isEnabled = model.get(AppMenuItemProperties.ENABLED);
                view.setEnabled(isEnabled);
                view.setAlpha(isEnabled ? 1.0f : 0.5f);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.app_menu_bottom_sheet_layout, container, false);
        
        LinearLayout contentLayout = view.findViewById(R.id.app_menu_content);
        
        // Add extensions row
        createExtensionsRow();
        // contentLayout.addView(createExtensionsRow(), 0);  // Add at the top

        mGridView = view.findViewById(R.id.app_menu_grid);
        mGridView.setNumColumns(GRID_COLUMNS);

        if (mModelList == null) {
            return view;
        }
        
        mGridAdapter = new GridAdapter(getContext(), mModelList);
        mGridView.setAdapter(mGridAdapter);
        
        mGridView.setOnItemClickListener(this);

        // ImageButton backButton = view.findViewById(R.id.back_to_menu_button);
        // backButton.setOnClickListener(v -> returnToAppMenu());

        return view;
    }
}
