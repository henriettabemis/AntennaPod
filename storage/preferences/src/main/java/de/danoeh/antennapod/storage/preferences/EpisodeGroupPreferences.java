package de.danoeh.antennapod.storage.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores episode group assignments and per-item queue lock state.
 * Groups allow multi-part episodes to be queued together in order.
 */
public class EpisodeGroupPreferences {

    private static final String PREFS_NAME = "episode_groups";
    private static final String KEY_GROUP_PREFIX = "group_";
    private static final String KEY_LOCKED_PREFIX = "locked_";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Assigns an episode to a named group. Pass null to remove from any group. */
    public static void setGroup(Context context, long episodeId, String groupName) {
        SharedPreferences prefs = getPrefs(context);
        String key = KEY_GROUP_PREFIX + episodeId;
        if (groupName == null || groupName.trim().isEmpty()) {
            prefs.edit().remove(key).apply();
        } else {
            prefs.edit().putString(key, groupName.trim()).apply();
        }
    }

    /** Returns the group name for an episode, or null if not in any group. */
    public static String getGroup(Context context, long episodeId) {
        return getPrefs(context).getString(KEY_GROUP_PREFIX + episodeId, null);
    }

    /** Returns all episode IDs that belong to the given group name. */
    public static List<Long> getEpisodesInGroup(Context context, String groupName) {
        SharedPreferences prefs = getPrefs(context);
        Map<String, ?> all = prefs.getAll();
        List<Long> ids = new ArrayList<>();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            if (entry.getKey().startsWith(KEY_GROUP_PREFIX)
                    && groupName.equals(entry.getValue())) {
                String idStr = entry.getKey().substring(KEY_GROUP_PREFIX.length());
                try {
                    ids.add(Long.parseLong(idStr));
                } catch (NumberFormatException ignored) { }
            }
        }
        return ids;
    }

    /** Returns all distinct group names currently stored. */
    public static List<String> getAllGroupNames(Context context) {
        SharedPreferences prefs = getPrefs(context);
        Map<String, ?> all = prefs.getAll();
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            if (entry.getKey().startsWith(KEY_GROUP_PREFIX)) {
                String name = (String) entry.getValue();
                if (!names.contains(name)) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    /** Locks an episode's position in the queue so it cannot be dragged. */
    public static void setQueueLocked(Context context, long episodeId, boolean locked) {
        String key = KEY_LOCKED_PREFIX + episodeId;
        if (locked) {
            getPrefs(context).edit().putBoolean(key, true).apply();
        } else {
            getPrefs(context).edit().remove(key).apply();
        }
    }

    /** Returns true if this episode's queue position is locked. */
    public static boolean isQueueItemLocked(Context context, long episodeId) {
        return getPrefs(context).getBoolean(KEY_LOCKED_PREFIX + episodeId, false);
    }
}
