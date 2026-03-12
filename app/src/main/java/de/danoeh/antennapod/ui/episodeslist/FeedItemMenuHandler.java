package de.danoeh.antennapod.ui.episodeslist;

import android.content.Context;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import java.util.ArrayList;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.playback.service.PlaybackServiceInterface;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.preferences.EpisodeGroupPreferences;
import de.danoeh.antennapod.ui.common.IntentUtils;
import de.danoeh.antennapod.playback.service.PlaybackStatus;
import de.danoeh.antennapod.ui.share.ShareUtils;
import de.danoeh.antennapod.ui.share.ShareDialog;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.appstartintent.MediaButtonStarter;
import de.danoeh.antennapod.ui.view.LocalDeleteModal;
import org.greenrobot.eventbus.EventBus;

/**
 * Handles interactions with the FeedItemMenu.
 */
public class FeedItemMenuHandler {

    private static final String TAG = "FeedItemMenuHandler";

    private FeedItemMenuHandler() {
    }

    /**
     * This method should be called in the prepare-methods of menus. It changes
     * the visibility of the menu items depending on a FeedItem's attributes.
     *
     * @param menu          An instance of Menu
     * @param selectedItems The FeedItem for which the menu is supposed to be prepared
     * @param excludeIds Menu item that should be excluded
     * @return Returns true if selectedItem is not null.
     */
    public static boolean onPrepareMenu(Menu menu, List<FeedItem> selectedItems, int... excludeIds) {
        if (menu == null || selectedItems == null || selectedItems.isEmpty() || selectedItems.get(0) == null) {
            return false;
        }
        boolean canSkip = false;
        boolean canRemoveFromQueue = false;
        boolean canAddToQueue = false;
        boolean canVisitWebsite = false;
        boolean canShare = false;
        boolean canRemoveFromInbox = false;
        boolean canMarkPlayed = false;
        boolean canMarkUnplayed = false;
        boolean canResetPosition = false;
        boolean canDelete = false;
        boolean canDownload = false;
        boolean canAddFavorite = false;
        boolean canRemoveFavorite = false;
        boolean canShowTranscript = false;
        boolean canShowSocialInteract = false;
        boolean canAddToGroup = false;
        boolean canRemoveFromGroup = false;
        boolean canQueueGroup = false;

        for (FeedItem item : selectedItems) {
            final boolean hasMedia = item.getMedia() != null;
            final boolean isDownloading = hasMedia
                    && DownloadServiceInterface.get().isDownloadingEpisode(item.getMedia().getDownloadUrl());
            canSkip |= hasMedia && PlaybackStatus.isPlaying(item.getMedia());
            canRemoveFromQueue |= item.isTagged(FeedItem.TAG_QUEUE);
            canAddToQueue |= hasMedia && !item.isTagged(FeedItem.TAG_QUEUE);
            canVisitWebsite |= !item.getFeed().isLocalFeed() && ShareUtils.hasLinkToShare(item);
            canShare |= !item.getFeed().isLocalFeed();
            canRemoveFromInbox |= item.isNew();
            canMarkPlayed |= !item.isPlayed();
            canMarkUnplayed |= item.isPlayed();
            canResetPosition |= hasMedia && item.getMedia().getPosition() != 0;
            canDelete |= item.getFeed().isLocalFeed() || (hasMedia && item.getMedia().isDownloaded()) || isDownloading;
            canDownload |= hasMedia && !item.getMedia().isDownloaded()
                    && !item.getFeed().isLocalFeed() && !isDownloading;
            canAddFavorite |= !item.isTagged(FeedItem.TAG_FAVORITE);
            canRemoveFavorite |= item.isTagged(FeedItem.TAG_FAVORITE);
            canShowTranscript |= item.hasTranscript();
            canShowSocialInteract |= item.getSocialInteractUrl() != null;
        }

        // Group items shown only for single-item selection (context not available here;
        // remove_from_group and queue_group are guarded in the click handler)
        if (selectedItems.size() == 1) {
            canAddToGroup = true;
            canRemoveFromGroup = true;
            canQueueGroup = true;
        }

        if (selectedItems.size() > 1) {
            canVisitWebsite = false;
            canShare = false;
            canShowTranscript = false;
            canShowSocialInteract = false;
        }

        setItemVisibility(menu, R.id.skip_episode_item, canSkip);
        setItemVisibility(menu, R.id.remove_from_queue_item, canRemoveFromQueue);
        setItemVisibility(menu, R.id.add_to_queue_item, canAddToQueue);
        setItemVisibility(menu, R.id.play_next_item, canAddToQueue);
        setItemVisibility(menu, R.id.visit_website_item, canVisitWebsite);
        setItemVisibility(menu, R.id.share_item, canShare);
        setItemVisibility(menu, R.id.remove_inbox_item, canRemoveFromInbox);
        setItemVisibility(menu, R.id.mark_read_item, canMarkPlayed);
        setItemVisibility(menu, R.id.mark_unread_item, canMarkUnplayed);
        setItemVisibility(menu, R.id.reset_position, canResetPosition);
        setItemVisibility(menu, R.id.open_social_interact_url, canShowSocialInteract);

        // Display proper strings when item has no media
        if (selectedItems.size() == 1 && selectedItems.get(0).getMedia() == null) {
            setItemTitle(menu, R.id.mark_read_item, R.string.mark_read_no_media_label);
            setItemTitle(menu, R.id.mark_unread_item, R.string.mark_unread_label_no_media);
        } else {
            setItemTitle(menu, R.id.mark_read_item, R.string.mark_as_played_label);
            setItemTitle(menu, R.id.mark_unread_item, R.string.mark_as_unplayed_label);
        }

        setItemVisibility(menu, R.id.add_to_favorites_item, canAddFavorite);
        setItemVisibility(menu, R.id.remove_from_favorites_item, canRemoveFavorite);
        setItemVisibility(menu, R.id.remove_item, canDelete);
        setItemVisibility(menu, R.id.download_item, canDownload);
        setItemVisibility(menu, R.id.transcript_item, canShowTranscript);
        setItemVisibility(menu, R.id.add_to_group_item, canAddToGroup);
        setItemVisibility(menu, R.id.remove_from_group_item, canRemoveFromGroup);
        setItemVisibility(menu, R.id.queue_group_item, canQueueGroup);

        if (selectedItems.size() == 1 && selectedItems.get(0).getFeed().getState() == Feed.STATE_NOT_SUBSCRIBED) {
            setItemVisibility(menu, R.id.mark_read_item, false);
        }

        if (excludeIds != null) {
            for (int id : excludeIds) {
                setItemVisibility(menu, id, false);
            }
        }
        return true;
    }

    /**
     * Used to set the viability of a menu item.
     * This method also does some null-checking so that neither menu nor the menu item are null
     * in order to prevent nullpointer exceptions.
     * @param menu The menu that should be used
     * @param menuId The id of the menu item that will be used
     * @param visibility The new visibility status of given menu item
     * */
    private static void setItemVisibility(Menu menu, int menuId, boolean visibility) {
        if (menu == null) {
            return;
        }
        MenuItem item = menu.findItem(menuId);
        if (item != null) {
            item.setVisible(visibility);
        }
    }

    /**
     * This method allows to replace to String of a menu item with a different one.
     * @param menu Menu item that should be used
     * @param id The id of the string that is going to be replaced.
     * @param noMedia The id of the new String that is going to be used.
     * */
    public static void setItemTitle(Menu menu, int id, int noMedia) {
        MenuItem item = menu.findItem(id);
        if (item != null) {
            item.setTitle(noMedia);
        }
    }

    /**
     * Default menu handling for the given FeedItem.
     * A Fragment instance, (rather than the more generic Context), is needed as a parameter
     * to support some UI operations, e.g., creating a Snackbar.
     */
    public static boolean onMenuItemClicked(@NonNull Fragment fragment, int menuItemId,
                                            @NonNull FeedItem selectedItem) {

        @NonNull Context context = fragment.requireContext();
        if (menuItemId == R.id.skip_episode_item) {
            context.sendBroadcast(MediaButtonStarter.createIntent(context, KeyEvent.KEYCODE_MEDIA_NEXT));
        } else if (menuItemId == R.id.remove_item) {
            LocalDeleteModal.showLocalFeedDeleteWarningIfNecessary(context, Arrays.asList(selectedItem),
                    () -> DBWriter.deleteFeedMediaOfItem(context, selectedItem.getMedia()));
        } else if (menuItemId == R.id.remove_inbox_item) {
            removeNewFlagWithUndo(fragment, selectedItem);
        } else if (menuItemId == R.id.mark_read_item) {
            new EpisodeMultiSelectActionHandler(fragment.getActivity(), R.id.mark_read_item)
                    .handleAction(Collections.singletonList(selectedItem));
        } else if (menuItemId == R.id.mark_unread_item) {
            new EpisodeMultiSelectActionHandler(fragment.getActivity(), R.id.mark_unread_item)
                    .handleAction(Collections.singletonList(selectedItem));
        } else if (menuItemId == R.id.play_next_item) {
            DBWriter.addQueueItemNext(context, selectedItem);
        } else if (menuItemId == R.id.add_to_queue_item) {
            DBWriter.addQueueItemToEnd(context, selectedItem);
        } else if (menuItemId == R.id.remove_from_queue_item) {
            DBWriter.removeQueueItem(context, true, selectedItem);
        } else if (menuItemId == R.id.add_to_favorites_item) {
            DBWriter.addFavoriteItem(selectedItem);
        } else if (menuItemId == R.id.remove_from_favorites_item) {
            DBWriter.removeFavoriteItem(selectedItem);
        } else if (menuItemId == R.id.reset_position) {
            selectedItem.getMedia().setPosition(0);
            if (PlaybackPreferences.getCurrentlyPlayingFeedMediaId() == selectedItem.getMedia().getId()) {
                PlaybackPreferences.writeNoMediaPlaying();
                IntentUtils.sendLocalBroadcast(context, PlaybackServiceInterface.ACTION_SHUTDOWN_PLAYBACK_SERVICE);
            }
            DBWriter.markItemPlayed(FeedItem.UNPLAYED, true, selectedItem);
        } else if (menuItemId == R.id.visit_website_item) {
            IntentUtils.openInBrowser(context, selectedItem.getLinkWithFallback());
        } else if (menuItemId == R.id.open_social_interact_url) {
            new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.visit_social_interact_confirm_dialog_title)
                    .setMessage(context.getString(R.string.visit_social_interact_confirm_dialog_message,
                            selectedItem.getSocialInteractUrl()))
                    .setPositiveButton(R.string.confirm_label, (dialog, which) ->
                            IntentUtils.openInBrowser(context, selectedItem.getSocialInteractUrl()))
                    .setNegativeButton(R.string.cancel_label, null)
                    .show();
        } else if (menuItemId == R.id.share_item) {
            ShareDialog shareDialog = ShareDialog.newInstance(selectedItem);
            shareDialog.show((fragment.getActivity().getSupportFragmentManager()), "ShareEpisodeDialog");
        } else if (menuItemId == R.id.add_to_group_item) {
            showAddToGroupDialog(fragment, selectedItem);
        } else if (menuItemId == R.id.remove_from_group_item) {
            String existingGroup = EpisodeGroupPreferences.getGroup(context, selectedItem.getId());
            if (existingGroup != null) {
                EpisodeGroupPreferences.setGroup(context, selectedItem.getId(), null);
                EventBus.getDefault().post(new MessageEvent(
                        context.getString(R.string.removed_from_group_message, existingGroup)));
            }
        } else if (menuItemId == R.id.queue_group_item) {
            String groupName = EpisodeGroupPreferences.getGroup(context, selectedItem.getId());
            if (groupName == null) {
                // Not in a group — just add this episode
                DBWriter.addQueueItemToEnd(context, selectedItem);
            } else {
                new MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.queue_group_label)
                        .setMessage(context.getString(R.string.queue_group_question, groupName))
                        .setPositiveButton(R.string.queue_group_add_all, (dialog, which) -> {
                            java.util.List<Long> groupIds =
                                    EpisodeGroupPreferences.getEpisodesInGroup(context, groupName);
                            java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                                java.util.List<FeedItem> groupItems = new ArrayList<>();
                                for (long id : groupIds) {
                                    FeedItem fi = DBReader.getFeedItem(id);
                                    if (fi != null) {
                                        groupItems.add(fi);
                                    }
                                }
                                // Sort by publication date so multi-part episodes queue in order
                                groupItems.sort((a, b) -> {
                                    if (a.getPubDate() == null || b.getPubDate() == null) return 0;
                                    return a.getPubDate().compareTo(b.getPubDate());
                                });
                                DBWriter.addQueueItemToEnd(context,
                                        groupItems.toArray(new FeedItem[0]));
                            });
                        })
                        .setNegativeButton(R.string.queue_group_add_one,
                                (dialog, which) -> DBWriter.addQueueItemToEnd(context, selectedItem))
                        .show();
            }
        } else {
            Log.d(TAG, "Unknown menuItemId: " + menuItemId);
            return false;
        }
        // Refresh menu state

        return true;
    }

    /**
     * Remove new flag with additional UI logic to allow undo with Snackbar.
     *
     * Undo is useful for Remove new flag, given there is no UI to undo it otherwise
     * ,i.e., there is (context) menu item for add new flag
     */
    public static void markReadWithUndo(@NonNull Fragment fragment, FeedItem item,
                                        int playState, boolean showSnackbar) {
        if (item == null) {
            return;
        }

        Log.d(TAG, "markReadWithUndo(" + item.getId() + ")");
        // we're marking it as unplayed since the user didn't actually play it
        // but they don't want it considered 'NEW' anymore
        DBWriter.markItemPlayed(playState, false, item);

        final Handler h = new Handler(fragment.requireContext().getMainLooper());
        final Runnable r = () -> {
            FeedMedia media = item.getMedia();
            if (media == null) {
                return;
            }
            boolean shouldAutoDelete = UserPreferences.isAutoDelete()
                    && (!item.getFeed().isLocalFeed() || UserPreferences.isAutoDeleteLocal());
            int smartMarkAsPlayedSecs = UserPreferences.getSmartMarkAsPlayedSecs();
            boolean almostEnded = media.getDuration() > 0
                    && media.getPosition() >= media.getDuration() - smartMarkAsPlayedSecs * 1000;
            if (almostEnded && shouldAutoDelete) {
                DBWriter.deleteFeedMediaOfItem(fragment.requireContext(), media);
            }
        };

        String message;
        switch (playState) {
            default:
            case FeedItem.UNPLAYED:
                if (item.getPlayState() == FeedItem.NEW) {
                    //was new
                    message = fragment.getString(R.string.removed_from_inbox_message);
                } else {
                    //was played
                    message = fragment.getResources().getQuantityString(
                            R.plurals.marked_as_unplayed_message, 1);
                }
                break;
            case FeedItem.PLAYED:
                message = fragment.getResources().getQuantityString(
                        R.plurals.marked_as_played_message, 1);
                break;
        }

        if (showSnackbar) {
            EventBus.getDefault().post(new MessageEvent(message,
                    context -> {
                        DBWriter.markItemPlayed(item.getPlayState(), false, item);
                        // don't forget to cancel the thing that's going to remove the media
                        h.removeCallbacks(r);
                    }, fragment.getString(R.string.undo)));
        }
        h.postDelayed(r, 2000);
    }

    public static void removeNewFlagWithUndo(@NonNull Fragment fragment, FeedItem item) {
        markReadWithUndo(fragment, item, FeedItem.UNPLAYED, false);
    }

    private static void showAddToGroupDialog(@NonNull Fragment fragment, @NonNull FeedItem item) {
        Context context = fragment.requireContext();
        String currentGroup = EpisodeGroupPreferences.getGroup(context, item.getId());

        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(R.string.group_name_hint);
        if (currentGroup != null) {
            input.setText(currentGroup);
        }

        // Suggest existing group names as a subtitle
        java.util.List<String> existing = EpisodeGroupPreferences.getAllGroupNames(context);
        String subtitle = existing.isEmpty() ? null
                : context.getString(R.string.group_name_existing) + " " + String.join(", ", existing);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.group_name_dialog_title)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    EpisodeGroupPreferences.setGroup(context, item.getId(),
                            name.isEmpty() ? null : name);
                })
                .setNegativeButton(android.R.string.cancel, null);

        if (subtitle != null) {
            builder.setMessage(subtitle);
        }
        builder.show();
    }

}
