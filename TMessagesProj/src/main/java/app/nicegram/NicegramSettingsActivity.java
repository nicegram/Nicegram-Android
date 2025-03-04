package app.nicegram;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.collection.MutableIntObjectMap;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appvillis.feature_ai_shortcuts.AiShortcutsEntryPoint;
import com.appvillis.feature_nicegram_client.HiddenChatsHelper;
import com.appvillis.feature_nicegram_client.NicegramClientHelper;
import com.appvillis.feature_nicegram_client.NicegramConsts;
import com.appvillis.nicegram.NicegramPinChatsPlacementHelper;
import com.appvillis.nicegram.NicegramPrefs;
import com.appvillis.rep_placements.domain.entity.PinnedChatsPlacement;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
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
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.PasscodeActivity;

import java.util.ArrayList;

import dagger.hilt.EntryPoints;

public class NicegramSettingsActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter adapter;


    private int pinSectionHeaderRow;
    private final MutableIntObjectMap<PinRow> pinSectionRowsMap = new MutableIntObjectMap<>();
    private int unblockGuideRow;

    private int nicegramSectionRow;

    private int maxAccountsRow;
    private int showProfileIdRow;
    private int showRegDateRow;
    private int startWithRearCameraRow;
    private int downloadVideosToGallery;
    private int hidePhoneNumberRow;
    private int doubleBottomRow;
    private int quickRepliesRow;
    private int hideReactionsRow;
    private int hideStoriesRow;
    private int hideReactionsNotificationRow;
    private int hideUnreadCounterRow;
    private int hideMentionNotificationRow;
    private int animationsInChatList;
    private int fullscreenGrayscale;
    private int grayscaleInChatList;
    private int grayscaleInChat;
    private int shareChannelsInfoRow;
    private int shareBotsInfoRow;
    private int shareStickersInfoRow;
    private int showHiddenChatsRow;
    private int showNgBtnInChatRow;
    private int showAiShortcutsRow;
    private int rowCount = 0;

    abstract static class PinRow {
        abstract String getName();

        static class ChatPlacementPin extends PinRow {
            PinnedChatsPlacement placement;

            public ChatPlacementPin(PinnedChatsPlacement placement) {
                this.placement = placement;
            }
            @Override
            String getName() {
                return placement.getName();
            }
        }

        static class PumpPin extends PinRow {
            @Override
            String getName() {
                return "Pump Advertising";
            }
        }
    }

    @Override
    public boolean onFragmentCreate() {
        if (!NicegramPinChatsPlacementHelper.INSTANCE.getPossiblePinChatsPlacements(ApplicationLoader.applicationContext).isEmpty() || NicegramPinChatsPlacementHelper.INSTANCE.pumpFeatureEnabled(ApplicationLoader.applicationContext))
            pinSectionHeaderRow = rowCount++;
        for (PinnedChatsPlacement placement : NicegramPinChatsPlacementHelper.INSTANCE.getPossiblePinChatsPlacements(ApplicationLoader.applicationContext)) {
            pinSectionRowsMap.put(rowCount++, new PinRow.ChatPlacementPin(placement));
        }
        if (NicegramPinChatsPlacementHelper.INSTANCE.pumpFeatureEnabled(ApplicationLoader.applicationContext)) {
            pinSectionRowsMap.put(rowCount++, new PinRow.PumpPin());
        }

        nicegramSectionRow = rowCount++;
        unblockGuideRow = rowCount++;
        maxAccountsRow = rowCount++;
        startWithRearCameraRow = rowCount++;
        downloadVideosToGallery = rowCount++;
        hidePhoneNumberRow = rowCount++;
        quickRepliesRow = rowCount++;
        if (!NicegramDoubleBottom.INSTANCE.getLoggedToDbot()) doubleBottomRow = rowCount++;
        else doubleBottomRow = -1;
        showNgBtnInChatRow = rowCount++;
        showAiShortcutsRow = rowCount++;
        showProfileIdRow = rowCount++;
        showRegDateRow = rowCount++;
        hideReactionsRow = rowCount++;
        hideStoriesRow = rowCount++;
        hideReactionsNotificationRow = rowCount++;
        hideUnreadCounterRow = rowCount++;
        hideMentionNotificationRow = rowCount++;
        animationsInChatList = rowCount++;
        fullscreenGrayscale = rowCount++;
        grayscaleInChatList = rowCount++;
        grayscaleInChat = rowCount++;
        shareChannelsInfoRow = rowCount++;
        //shareBotsInfoRow = rowCount++;
        //shareStickersInfoRow = rowCount++;
        showHiddenChatsRow = rowCount++;

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
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_CHAT);
            } else if (position == hideStoriesRow) {
                enabled = NicegramClientHelper.INSTANCE.getPreferences().getHideStories();
                NicegramClientHelper.INSTANCE.getPreferences().setHideStories(!enabled);
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.storiesUpdated);
            } else if (position == hideReactionsNotificationRow) {
                enabled = NicegramClientHelper.INSTANCE.getPreferences().getHideReactionsNotification();
                NicegramClientHelper.INSTANCE.getPreferences().setHideReactionsNotification(!enabled);
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
            } else if (position == hideUnreadCounterRow) {
                enabled = NicegramClientHelper.INSTANCE.getPreferences().getHideUnreadCounter();
                NicegramClientHelper.INSTANCE.getPreferences().setHideUnreadCounter(!enabled);
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_CHAT);
            } else if (position == hideMentionNotificationRow) {
                enabled = NicegramClientHelper.INSTANCE.getPreferences().getHideMentionNotification();
                NicegramClientHelper.INSTANCE.getPreferences().setHideMentionNotification(!enabled);
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_CHAT);
            } else if (position == animationsInChatList) {
                enabled = NicegramClientHelper.INSTANCE.getPreferences().getAnimationInChatList();
                NicegramClientHelper.INSTANCE.getPreferences().setAnimationInChatList(!enabled);
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_CHAT);
            } else if (position == fullscreenGrayscale) {
                enabled = NicegramClientHelper.INSTANCE.getPreferences().getGrayscaleFullScreen();
                NicegramClientHelper.INSTANCE.getPreferences().setGrayscaleFullScreen(!enabled);
                ((LaunchActivity) AndroidUtilities.findActivity(context)).tryUpdateGrayscale();
            } else if (position == grayscaleInChatList) {
                enabled = NicegramClientHelper.INSTANCE.getPreferences().getGrayscaleInChatList();
                NicegramClientHelper.INSTANCE.getPreferences().setGrayscaleInChatList(!enabled);
            } else if (position == grayscaleInChat) {
                enabled = NicegramClientHelper.INSTANCE.getPreferences().getGrayscaleInChat();
                NicegramClientHelper.INSTANCE.getPreferences().setGrayscaleInChat(!enabled);
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
            } else if (position == showNgBtnInChatRow) {
                enabled = PrefsHelper.INSTANCE.getShowNgFloatingMenuInChat(currentAccount);
                PrefsHelper.INSTANCE.setShowNgFloatingMenuInChat(currentAccount, !enabled);
            } else if (position == showAiShortcutsRow) {
                enabled = getAiShortcutsSettings().getShowInChat();
                setAiShortcutsStatus(!enabled);
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
                enabled = NicegramClientHelper.INSTANCE.getPreferences().getCanShareChannels();
                NicegramClientHelper.INSTANCE.getPreferences().setCanShareChannels(!enabled);
            } else if (position == shareBotsInfoRow) {
                enabled = NicegramClientHelper.INSTANCE.getPreferences().getCanShareBots();
                NicegramClientHelper.INSTANCE.getPreferences().setCanShareBots(!enabled);
            } else if (position == shareStickersInfoRow) {
                enabled = NicegramClientHelper.INSTANCE.getPreferences().getCanShareStickers();
                NicegramClientHelper.INSTANCE.getPreferences().setCanShareStickers(!enabled);
            } else if (pinSectionRowsMap.contains(position)) {
                PinRow pinRow = pinSectionRowsMap.get(position);
                if (pinRow instanceof PinRow.ChatPlacementPin) {
                    PinnedChatsPlacement placement = ((PinRow.ChatPlacementPin) pinRow).placement;
                    enabled = !NicegramPinChatsPlacementHelper.INSTANCE.isPinnedChatHidden(context, ((PinRow.ChatPlacementPin) pinRow).placement.getId());
                    NicegramPinChatsPlacementHelper.INSTANCE.setPinnedChatHidden(context, placement.getId(), enabled);
                } else if (pinRow instanceof PinRow.PumpPin) {
                    enabled = NicegramPinChatsPlacementHelper.INSTANCE.pumpEnabled(context);
                    NicegramPinChatsPlacementHelper.INSTANCE.setPumpEnabled(context, !enabled);
                }
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
            } else if (position == showHiddenChatsRow) {
                enabled = HiddenChatsHelper.INSTANCE.getShowHiddenChats();
                HiddenChatsHelper.INSTANCE.setShowHiddenChats(!enabled);
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
                case 3:
                    view = new ShadowSectionCell(mContext);
                    break;
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
                    } else if (position == pinSectionHeaderRow) {
                        headerCell.setText(LocaleController.getString("Nicegram_PinSection"));
                    }
                    break;
                }
                case 1: {
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    SharedPreferences preferences = MessagesController.getNicegramSettings(currentAccount);
                    if (position == showNgBtnInChatRow) {
                        checkCell.setTextAndCheck(LocaleController.getString("NicegramShowNgBtnInChats"), PrefsHelper.INSTANCE.getShowNgFloatingMenuInChat(currentAccount), false);
                    } else if(position == showAiShortcutsRow) {
                        checkCell.setTextAndCheck(LocaleController.getString("AiShortcuts_ShowInChat"), getAiShortcutsSettings().getShowInChat(), false);
                    } else if (position == showProfileIdRow) {
                        checkCell.setTextAndCheck(LocaleController.getString("NicegramShowProfileID"), preferences.getBoolean(NicegramPrefs.PREF_SHOW_PROFILE_ID, NicegramPrefs.PREF_SHOW_PROFILE_ID_DEFAULT), false);
                    } else if (position == showRegDateRow) {
                        checkCell.setTextAndCheck(LocaleController.getString("NicegramShowRegistrationDate"), preferences.getBoolean(NicegramPrefs.PREF_SHOW_REG_DATE, NicegramPrefs.PREF_SHOW_REG_DATE_DEFAULT), false);
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
                    } else if (position == hideStoriesRow) {
                        checkCell.setTextAndCheck(mContext.getString(R.string.NicegramHideStories), NicegramClientHelper.INSTANCE.getPreferences().getHideStories(), false);
                    } else if (position == hideReactionsNotificationRow) {
                        checkCell.setTextAndCheck(mContext.getString(R.string.NicegramHideReactionsNotification), NicegramClientHelper.INSTANCE.getPreferences().getHideReactionsNotification(), false);
                    } else if (position == hideUnreadCounterRow) {
                        checkCell.setTextAndCheck(mContext.getString(R.string.NicegramHideUnreadCounter), NicegramClientHelper.INSTANCE.getPreferences().getHideUnreadCounter(), false);
                    } else if (position == hideMentionNotificationRow) {
                        checkCell.setTextAndCheck(mContext.getString(R.string.NicegramHideMentionNotification), NicegramClientHelper.INSTANCE.getPreferences().getHideMentionNotification(), false);
                    } else if (position == animationsInChatList) {
                        checkCell.setTextAndCheck(mContext.getString(R.string.NicegramAnimationsInChatList), NicegramClientHelper.INSTANCE.getPreferences().getAnimationInChatList(), false);
                    } else if (position == fullscreenGrayscale) {
                        checkCell.setTextAndCheck(mContext.getString(R.string.NicegramFullScreenGrayscale), NicegramClientHelper.INSTANCE.getPreferences().getGrayscaleFullScreen(), false);
                    } else if (position == grayscaleInChatList) {
                        checkCell.setTextAndCheck(mContext.getString(R.string.NicegramGrayscaleInChatList), NicegramClientHelper.INSTANCE.getPreferences().getGrayscaleInChatList(), false);
                    } else if (position == grayscaleInChat) {
                        checkCell.setTextAndCheck(mContext.getString(R.string.NicegramGrayscaleInChat), NicegramClientHelper.INSTANCE.getPreferences().getGrayscaleInChat(), false);
                    } else if (position == shareChannelsInfoRow) {
                        checkCell.setTextAndValueAndCheck(LocaleController.getString("NicegramShareChannelInfo"), LocaleController.getString("NicegramShareChannelInfoDesc"), NicegramClientHelper.INSTANCE.getPreferences().getCanShareChannels(), true, false);
                    } else if (position == shareBotsInfoRow) {
                        checkCell.setTextAndValueAndCheck(LocaleController.getString("NicegramShareBotsInfo"), LocaleController.getString("NicegramShareChannelInfoDesc"), NicegramClientHelper.INSTANCE.getPreferences().getCanShareBots(), true, false);
                    } else if (position == shareStickersInfoRow) {
                        checkCell.setTextAndValueAndCheck(LocaleController.getString("NicegramShareStickersInfo"), LocaleController.getString("NicegramShareChannelInfoDesc"), NicegramClientHelper.INSTANCE.getPreferences().getCanShareStickers(), true, false);
                    } else if (position == maxAccountsRow) {
                        checkCell.setTextAndValueAndCheck(LocaleController.getString("NicegramMaxAccount_IncreaseTo"), LocaleController.getString("NicegramMaxAccount_Default"), PrefsHelper.INSTANCE.getMaxAccountCount(getContext()) == NicegramPrefs.PREF_MAX_ACCOUNTS_MAX, true, false);
                        checkCell.setEnabled(false);
                    } else if (position == showHiddenChatsRow) {
                        checkCell.setTextAndCheck(getContext().getString(R.string.NicegramShowHiddenChats), HiddenChatsHelper.INSTANCE.getShowHiddenChats(), false);
                    } else if (pinSectionRowsMap.contains(position)) {
                        PinRow pinRow = pinSectionRowsMap.get(position);
                        boolean checked = false;
                        if (pinRow instanceof PinRow.ChatPlacementPin) {
                            PinnedChatsPlacement placement = ((PinRow.ChatPlacementPin) pinRow).placement;
                            checked = !NicegramPinChatsPlacementHelper.INSTANCE.isPinnedChatHidden(mContext, placement.getId());
                        } else if (pinRow instanceof PinRow.PumpPin) {
                            checked = NicegramPinChatsPlacementHelper.INSTANCE.pumpEnabled(mContext);
                        }
                        checkCell.setTextAndCheck(pinRow.getName(), checked, false);
                    }
                    break;
                }
                case 2: {
                    TextCell textCell = (TextCell) holder.itemView;
                    if (position == unblockGuideRow) {
                        textCell.setText(LocaleController.getString("NicegramUnblockGuide"), false);
                    } else if (position == quickRepliesRow) {
                        textCell.setText(LocaleController.getString("QuickReplies"), false);
                    }
                    break;
                }
                case 3:
                    View sectionCell = holder.itemView;
                    sectionCell.setTag(position);
                    sectionCell.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider, getThemedColor(Theme.key_windowBackgroundGrayShadow)));
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == pinSectionHeaderRow) {
                return 0;
            } else if (position == showRegDateRow || position == showProfileIdRow ||
                    position == startWithRearCameraRow || position == downloadVideosToGallery ||
                    position == hidePhoneNumberRow || position == hideReactionsRow || position == doubleBottomRow ||
                    position == maxAccountsRow || position == shareChannelsInfoRow ||
                    position == shareBotsInfoRow || position == shareStickersInfoRow ||
                    position == showNgBtnInChatRow || position == showHiddenChatsRow ||
                    pinSectionRowsMap.contains(position) || position == hideMentionNotificationRow ||
                    position == hideReactionsNotificationRow || position == hideUnreadCounterRow ||
                    position == hideStoriesRow || position == animationsInChatList ||
                    position == fullscreenGrayscale || position == grayscaleInChatList ||
                    position == grayscaleInChat || position == showAiShortcutsRow
            ) {
                return 1;
            } else if (position == unblockGuideRow || position == quickRepliesRow) {
                return 2;
            } else if (position == nicegramSectionRow) {
                return 3;
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

    public com.appvillis.feature_ai_shortcuts.domain.entitites.Settings getAiShortcutsSettings() {
        return EntryPoints.get(ApplicationLoader.applicationContext, AiShortcutsEntryPoint.class).getSettingsUseCase().getSettings();
    }

    public void setAiShortcutsStatus(boolean enabled) {
        EntryPoints.get(ApplicationLoader.applicationContext, AiShortcutsEntryPoint.class).updateSettingsUseCase().invoke(getAiShortcutsSettings().copy(enabled));
    }
}
