package app.nicegram;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appvillis.feature_nicegram_billing.NicegramBillingHelper;
import com.appvillis.nicegram.NicegramConsts;
import com.appvillis.nicegram.NicegramPrefs;
import com.appvillis.nicegram.network.NicegramNetwork;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.PasscodeActivity;

import java.util.ArrayList;

public class NicegramSettingsActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter adapter;

    private int nicegramSectionRow;
    private int unblockGuideRow;

    private int maxAccountsRow;
    private int skipReadHistoryRow;
    private int showProfileIdRow;
    private int showRegDateRow;
    private int openLinksRow;
    private int startWithRearCameraRow;
    private int downloadVideosToGallery;
    private int hidePhoneNumberRow;
    private int doubleBottomRow;
    private int restoreRow;
    private int quickRepliesRow;
    private int hideReactionsRow;
    private int shareChannelsInfoRow;
    private int rowCount = 0;

    @Override
    public boolean onFragmentCreate() {
        nicegramSectionRow = -1;
        unblockGuideRow = rowCount++;
        maxAccountsRow = rowCount++;
        startWithRearCameraRow = rowCount++;
        downloadVideosToGallery = rowCount++;
        hidePhoneNumberRow = rowCount++;
        quickRepliesRow = rowCount++;
        if (!NicegramDoubleBottom.INSTANCE.getLoggedToDbot()) doubleBottomRow = rowCount++;
        else doubleBottomRow = -1;
        restoreRow = rowCount++;
        showProfileIdRow = rowCount++;
        showRegDateRow = rowCount++;
        hideReactionsRow = rowCount++;
        skipReadHistoryRow = rowCount++;
        openLinksRow = rowCount++;
        shareChannelsInfoRow = rowCount++;

        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("NicegramSettings"));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setItemAnimator(null);
        listView.setLayoutAnimation(null);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        });
        listView.setVerticalScrollBarEnabled(false);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(adapter = new ListAdapter(context));
        listView.setOnItemClickListener((view, position, x, y) -> {
            boolean enabled = false;
            if (getParentActivity() == null) {
                return;
            }
            if (position == hideReactionsRow) {
                SharedPreferences preferences = MessagesController.getNicegramSettings(currentAccount);
                SharedPreferences.Editor editor = preferences.edit();
                enabled = preferences.getBoolean(NicegramPrefs.PREF_HIDE_REACTIONS, NicegramPrefs.PREF_HIDE_REACTIONS_DEFAULT);
                editor.putBoolean(NicegramPrefs.PREF_HIDE_REACTIONS, !enabled);
                editor.apply();
            } else if (position == startWithRearCameraRow) {
                SharedPreferences preferences = MessagesController.getNicegramSettings(currentAccount);
                SharedPreferences.Editor editor = preferences.edit();
                enabled = preferences.getBoolean(NicegramPrefs.PREF_START_WITH_REAR_CAMERA, NicegramPrefs.PREF_START_WITH_REAR_CAMERA_DEFAULT);
                editor.putBoolean(NicegramPrefs.PREF_START_WITH_REAR_CAMERA, !enabled);
                editor.apply();
            } else if (position == downloadVideosToGallery) {
                SharedPreferences preferences = MessagesController.getNicegramSettings(currentAccount);
                SharedPreferences.Editor editor = preferences.edit();
                enabled = preferences.getBoolean(NicegramPrefs.PREF_DOWNLOAD_VIDEOS_TO_GALLERY, NicegramPrefs.PREF_DOWNLOAD_VIDEOS_TO_GALLERY_DEFAULT);
                editor.putBoolean(NicegramPrefs.PREF_DOWNLOAD_VIDEOS_TO_GALLERY, !enabled);
                editor.apply();
            } else if (position == hidePhoneNumberRow) {
                SharedPreferences preferences = MessagesController.getNicegramSettings(currentAccount);
                SharedPreferences.Editor editor = preferences.edit();
                enabled = preferences.getBoolean(NicegramPrefs.PREF_HIDE_PHONE_NUMBER, NicegramPrefs.PREF_HIDE_PHONE_NUMBER_DEFAULT);
                editor.putBoolean(NicegramPrefs.PREF_HIDE_PHONE_NUMBER, !enabled);
                editor.apply();
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
            } else if (position == skipReadHistoryRow) {
                SharedPreferences preferences = MessagesController.getNicegramSettings(currentAccount);
                SharedPreferences.Editor editor = preferences.edit();
                enabled = preferences.getBoolean(NicegramPrefs.PREF_SKIP_READ_HISTORY, NicegramPrefs.PREF_SKIP_READ_HISTORY_DEFAULT);
                editor.putBoolean(NicegramPrefs.PREF_SKIP_READ_HISTORY, !enabled);
                editor.apply();
            } else if (position == showProfileIdRow) {
                SharedPreferences preferences = MessagesController.getNicegramSettings(currentAccount);
                SharedPreferences.Editor editor = preferences.edit();
                enabled = preferences.getBoolean(NicegramPrefs.PREF_SHOW_PROFILE_ID, NicegramPrefs.PREF_SHOW_PROFILE_ID_DEFAULT);
                editor.putBoolean(NicegramPrefs.PREF_SHOW_PROFILE_ID, !enabled);
                editor.apply();
            } else if (position == showRegDateRow) {
                SharedPreferences preferences = MessagesController.getNicegramSettings(currentAccount);
                SharedPreferences.Editor editor = preferences.edit();
                enabled = preferences.getBoolean(NicegramPrefs.PREF_SHOW_REG_DATE, NicegramPrefs.PREF_SHOW_REG_DATE_DEFAULT);
                editor.putBoolean(NicegramPrefs.PREF_SHOW_REG_DATE, !enabled);
                editor.apply();
            } else if (position == shareChannelsInfoRow) {
                SharedPreferences preferences = MessagesController.getNicegramSettings(currentAccount);
                SharedPreferences.Editor editor = preferences.edit();
                enabled = preferences.getBoolean(NicegramPrefs.PREF_SHARE_CHANNEL_INFO, NicegramPrefs.PREF_SHARE_CHANNEL_INFO_DEFAULT);
                editor.putBoolean(NicegramPrefs.PREF_SHARE_CHANNEL_INFO, !enabled);
                editor.apply();
            } else if (position == openLinksRow) {
                SharedPreferences preferences = MessagesController.getNicegramSettings(currentAccount);
                SharedPreferences.Editor editor = preferences.edit();
                enabled = preferences.getBoolean(NicegramPrefs.PREF_OPEN_LINKS_IN_BROWSER, NicegramPrefs.PREF_OPEN_LINKS_IN_BROWSER_DEFAULT);
                editor.putBoolean(NicegramPrefs.PREF_OPEN_LINKS_IN_BROWSER, !enabled);
                editor.apply();
            } else if (position == unblockGuideRow) {
                Browser.openUrl(getParentActivity(), NicegramConsts.UNBLOCK_URL);
            } else if (position == doubleBottomRow) {
                if (NicegramDoubleBottom.INSTANCE.hasDbot()) {
                    NicegramDoubleBottom.INSTANCE.disableDbot(getParentActivity());
                    ((TextCheckCell) view).setChecked(false);
                } else {
                    if (SharedConfig.passcodeHash.length() <= 0 || UserConfig.getActivatedAccountsCount() < 2) {
                        Toast.makeText(getParentActivity(), LocaleController.getString("NicegramDoubleBottomDesc"), Toast.LENGTH_LONG).show();
                    } else {
                        presentFragment(new PasscodeActivity(PasscodeActivity.TYPE_SETUP_CODE, true), true);
                    }
                }
            } else if (position == quickRepliesRow) {
                presentFragment(new QuickRepliesNgFragment());
            } else if (position == restoreRow) {
                NicegramNetwork.INSTANCE.restorePremium(getAccountInstance().getUserConfig().clientUserId, success -> {
                    if (success) {
                        NicegramBillingHelper.INSTANCE.setGiftedPremium(true);
                    } else {
                        if (getParentActivity() == null) return null;
                        Toast.makeText(getParentActivity(), R.string.NicegramRequestIsPending, Toast.LENGTH_LONG).show();
                    }
                    return null;
                });
            } else if (position == maxAccountsRow) {
                enabled = PrefsHelper.INSTANCE.getMaxAccountCount(context) == NicegramPrefs.PREF_MAX_ACCOUNTS_MAX;
                if (!enabled) {
                    AlertsCreator.createSimpleAlert(
                            getParentActivity(),
                            "", LocaleController.getString("NicegramMaxAccount_IncreaseWarn"),
                            LocaleController.getString("NicegramMaxAccount_Increase"),
                            () -> {
                                PrefsHelper.INSTANCE.setMaxAccountCount(context, NicegramPrefs.PREF_MAX_ACCOUNTS_MAX);
                                view.postDelayed(() -> {RebirthHelper.INSTANCE.triggerRebirth(context);}, 500);
                            },
                            getResourceProvider()
                    ).create().show();
                } else {
                    AlertsCreator.createSimpleAlert(
                            getParentActivity(),
                            "", LocaleController.getString("NicegramMaxAccount_ReduceWarn"),
                            LocaleController.getString("NicegramMaxAccount_Reduce"),
                            () -> {
                                PrefsHelper.INSTANCE.setMaxAccountCount(context, NicegramPrefs.PREF_MAX_ACCOUNTS_DEFAULT);
                                view.postDelayed(() -> {RebirthHelper.INSTANCE.triggerRebirth(context);}, 500);
                            },
                            getResourceProvider()
                    ).create().show();
                }
            }
            if (view instanceof TextCheckCell && position != doubleBottomRow && position != maxAccountsRow) {
                ((TextCheckCell) view).setChecked(!enabled);
            }
        });

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            return !(position == nicegramSectionRow);
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 2:
                    view = new TextCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 1:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 0:
                default:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == nicegramSectionRow) {
                        headerCell.setText("Nicegram");
                    }
                    break;
                }
                case 1: {
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    SharedPreferences preferences = MessagesController.getNicegramSettings(currentAccount);
                    if (position == skipReadHistoryRow) {
                        checkCell.setTextAndCheck(LocaleController.getString("NicegramSkipReadHistory"), preferences.getBoolean(NicegramPrefs.PREF_SKIP_READ_HISTORY, NicegramPrefs.PREF_SKIP_READ_HISTORY_DEFAULT), false);
                    } else if (position == showProfileIdRow) {
                        checkCell.setTextAndCheck(LocaleController.getString("NicegramShowProfileID"), preferences.getBoolean(NicegramPrefs.PREF_SHOW_PROFILE_ID, NicegramPrefs.PREF_SHOW_PROFILE_ID_DEFAULT), false);
                    } else if (position == showRegDateRow) {
                        checkCell.setTextAndCheck(LocaleController.getString("NicegramShowRegistrationDate"), preferences.getBoolean(NicegramPrefs.PREF_SHOW_REG_DATE, NicegramPrefs.PREF_SHOW_REG_DATE_DEFAULT), false);
                    } else if (position == openLinksRow) {
                        checkCell.setTextAndCheck(LocaleController.getString("NicegramOpenLinksInBrowser"), preferences.getBoolean(NicegramPrefs.PREF_OPEN_LINKS_IN_BROWSER, NicegramPrefs.PREF_OPEN_LINKS_IN_BROWSER_DEFAULT), false);
                    } else if (position == doubleBottomRow) {
                        checkCell.setTextAndValueAndCheck(LocaleController.getString("NicegramDoubleBottom"), LocaleController.getString("NicegramDoubleBottomDesc"), NicegramDoubleBottom.INSTANCE.hasDbot(), true, false);
                        checkCell.setEnabled(false);
                    } else if (position == startWithRearCameraRow) {
                        checkCell.setTextAndCheck(LocaleController.getString("NicegramStartWithRearCamera"), preferences.getBoolean(NicegramPrefs.PREF_START_WITH_REAR_CAMERA, NicegramPrefs.PREF_START_WITH_REAR_CAMERA_DEFAULT), false);
                    } else if (position == downloadVideosToGallery) {
                        checkCell.setTextAndCheck(LocaleController.getString("NicegramDownloadVideosToGallery"), preferences.getBoolean(NicegramPrefs.PREF_DOWNLOAD_VIDEOS_TO_GALLERY, NicegramPrefs.PREF_DOWNLOAD_VIDEOS_TO_GALLERY_DEFAULT), false);
                    } else if (position == hidePhoneNumberRow) {
                        checkCell.setTextAndValueAndCheck(LocaleController.getString("NicegramHidePhoneNumber"), LocaleController.getString("NicegramHidePhoneNumberDesc"), preferences.getBoolean(NicegramPrefs.PREF_HIDE_PHONE_NUMBER, NicegramPrefs.PREF_HIDE_PHONE_NUMBER_DEFAULT), true, false);
                    } else if (position == hideReactionsRow) {
                        checkCell.setTextAndCheck(LocaleController.getString("NicegramHideReactions"), preferences.getBoolean(NicegramPrefs.PREF_HIDE_REACTIONS, NicegramPrefs.PREF_HIDE_REACTIONS_DEFAULT), false);
                    } else if (position == shareChannelsInfoRow) {
                        checkCell.setTextAndValueAndCheck(LocaleController.getString("NicegramShareChannelInfo"), LocaleController.getString("NicegramShareChannelInfoDesc"), preferences.getBoolean(NicegramPrefs.PREF_SHARE_CHANNEL_INFO, NicegramPrefs.PREF_SHARE_CHANNEL_INFO_DEFAULT), true, false);
                    } else if (position == maxAccountsRow) {
                        checkCell.setTextAndValueAndCheck(LocaleController.getString("NicegramMaxAccount_IncreaseTo"), LocaleController.getString("NicegramMaxAccount_Default"), PrefsHelper.INSTANCE.getMaxAccountCount(getContext()) == NicegramPrefs.PREF_MAX_ACCOUNTS_MAX, true, false);
                        checkCell.setEnabled(false);
                    }
                    break;
                }
                case 2: {
                    TextCell textCell = (TextCell) holder.itemView;
                    if (position == unblockGuideRow) {
                        textCell.setText(LocaleController.getString("NicegramUnblockGuide"), false);
                    } else if (position == quickRepliesRow) {
                        textCell.setText(LocaleController.getString("QuickReplies"), false);
                    } else if (position == restoreRow) {
                        textCell.setText(LocaleController.getString("NicegramRestorePremium"), false);
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == nicegramSectionRow) {
                return 0;
            } else if (position == skipReadHistoryRow || position == openLinksRow ||
                    position == showRegDateRow || position == showProfileIdRow ||
                    position == startWithRearCameraRow || position == downloadVideosToGallery ||
                    position == hidePhoneNumberRow || position == hideReactionsRow || position == doubleBottomRow || position == maxAccountsRow || position == shareChannelsInfoRow) {
                return 1;
            } else if (position == unblockGuideRow || position == quickRepliesRow || position == restoreRow) {
                return 2;
            } else {
                return 0;
            }
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{HeaderCell.class, TextCheckCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        return themeDescriptions;
    }
}
