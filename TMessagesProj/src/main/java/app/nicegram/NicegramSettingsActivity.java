/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package app.nicegram;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appvillis.nicegram.NicegramPrefs;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

public class NicegramSettingsActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter adapter;

    private int nicegramSectionRow;
    private int unblockGuideRow;
    private int mentionAllRow;
    private int skipReadHistoryRow;
    private int showProfileIdRow;
    private int showRegDateRow;
    private int openLinksRow;
    private int rowCount = 0;

    @Override
    public boolean onFragmentCreate() {
        nicegramSectionRow = -1;
        unblockGuideRow = rowCount++;
        mentionAllRow = rowCount++;
        skipReadHistoryRow = rowCount++;
        showProfileIdRow = rowCount++;
        showRegDateRow = rowCount++;
        openLinksRow = rowCount++;

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
            if (position == mentionAllRow) {
                SharedPreferences preferences = MessagesController.getNicegramSettings(currentAccount);
                SharedPreferences.Editor editor = preferences.edit();
                enabled = preferences.getBoolean(NicegramPrefs.PREF_MENTION_ALL_ENABLED, NicegramPrefs.PREF_MENTION_ALL_ENABLED_DEFAULT);
                editor.putBoolean(NicegramPrefs.PREF_MENTION_ALL_ENABLED, !enabled);
                editor.apply();
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
            } else if (position == openLinksRow) {
                SharedPreferences preferences = MessagesController.getNicegramSettings(currentAccount);
                SharedPreferences.Editor editor = preferences.edit();
                enabled = preferences.getBoolean(NicegramPrefs.PREF_OPEN_LINKS_IN_BROWSER, NicegramPrefs.PREF_OPEN_LINKS_IN_BROWSER_DEFAULT);
                editor.putBoolean(NicegramPrefs.PREF_OPEN_LINKS_IN_BROWSER, !enabled);
                editor.apply();
            } else if (position == unblockGuideRow) {
                Browser.openUrl(getParentActivity(), NicegramConsts.UNBLOCK_URL);
            }
            if (view instanceof TextCheckCell) {
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
                    if (position == mentionAllRow) {
                        checkCell.setTextAndValueAndCheck(LocaleController.getString("ShowMentionAll"), LocaleController.getString("For20OrLessChats"), preferences.getBoolean(NicegramPrefs.PREF_MENTION_ALL_ENABLED, NicegramPrefs.PREF_MENTION_ALL_ENABLED_DEFAULT), true, false);
                    } else if (position == skipReadHistoryRow) {
                        checkCell.setTextAndCheck(LocaleController.getString("NicegramSkipReadHistory"), preferences.getBoolean(NicegramPrefs.PREF_SKIP_READ_HISTORY, NicegramPrefs.PREF_SKIP_READ_HISTORY_DEFAULT), false);
                    } else if (position == showProfileIdRow) {
                        checkCell.setTextAndCheck(LocaleController.getString("NicegramShowProfileID"), preferences.getBoolean(NicegramPrefs.PREF_SHOW_PROFILE_ID, NicegramPrefs.PREF_SHOW_PROFILE_ID_DEFAULT), false);
                    } else if (position == showRegDateRow) {
                        checkCell.setTextAndCheck(LocaleController.getString("NicegramShowRegistrationDate"), preferences.getBoolean(NicegramPrefs.PREF_SHOW_REG_DATE, NicegramPrefs.PREF_SHOW_REG_DATE_DEFAULT), false);
                    } else if (position == openLinksRow) {
                        checkCell.setTextAndCheck(LocaleController.getString("NicegramOpenLinksInBrowser"), preferences.getBoolean(NicegramPrefs.PREF_OPEN_LINKS_IN_BROWSER, NicegramPrefs.PREF_OPEN_LINKS_IN_BROWSER_DEFAULT), false);
                    }
                    break;
                }
                case 2: {
                    TextCell textCell = (TextCell) holder.itemView;
                    textCell.setText(LocaleController.getString("NicegramUnblockGuide"), false);
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == nicegramSectionRow) {
                return 0;
            } else if (position == mentionAllRow || position == skipReadHistoryRow || position == openLinksRow ||
                    position == showRegDateRow || position == showProfileIdRow) {
                return 1;
            } else if (position == unblockGuideRow) {
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
