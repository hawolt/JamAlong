package com.hawolt.media.impl;

import com.hawolt.Soundcloud;
import com.hawolt.audio.AudioFormatConverter;
import com.hawolt.data.media.download.DownloadCallback;
import com.hawolt.data.media.hydratable.impl.playlist.Playlist;
import com.hawolt.data.media.hydratable.impl.playlist.PlaylistManager;
import com.hawolt.data.media.hydratable.impl.track.Track;
import com.hawolt.data.media.hydratable.impl.track.TrackManager;
import com.hawolt.data.media.search.Explorer;
import com.hawolt.data.media.search.query.ObjectCollection;
import com.hawolt.data.media.search.query.impl.TrackQuery;
import com.hawolt.logger.Logger;
import com.hawolt.media.Audio;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.*;

public class SoundcloudAudioSource extends AbstractAudioSource implements DownloadCallback {
    private final Map<String, byte[]> cache = new HashMap<>();
    private final List<String> pending = new LinkedList<>();
    private final List<String> loading = new LinkedList<>();
    private final List<String> preload = new ArrayList<>();
    private final Object lock = new Object();
    private int currentlyLoadingReferences;
    private long timestamp;


    /**
     * Initiating this Class will register HydratableInterface for both Track.class and Playlist.class
     * which are used internally by 'soundcloud-downloader' to forward any data to the appropriate handler.
     */
    public SoundcloudAudioSource() {
        Soundcloud.register(Track.class, new TrackManager(this::onTrackData));
        Soundcloud.register(Playlist.class, new PlaylistManager(this::onPlaylistData));
    }

    /**
     * Configures a timestamp which is internally used to determine if an asynchronously loaded Song is
     * part of a previous party or the current one, any song associated with a timestamp previous to the
     * timestamp set will be dropped and ignored for full loading
     *
     * @param timestamp the exact timestamp of when the last visited party has been left
     */
    @Override
    public void setPartyLeaveTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * loads the given track
     *
     * @param path the track origin
     */
    @Override
    public void load(String path) {
        Soundcloud.load(path, this);
    }

    /**
     * preloads the given track to memory for faster playback
     *
     * @param path the track origin
     */
    @Override
    public void preload(String path) {
        this.preload.add(path);
        this.load(path);
    }

    /**
     * loads the next Song that is currently first in the pending queue
     */
    @Override
    public void loadNextPendingReference() {
        synchronized (lock) {
            if (pending.isEmpty()) return;
            this.incrementCurrentlyLoadingReferences();
            String link = pending.remove(0);
            this.loading.add(link);
            this.load(link);
        }
    }

    /**
     * clears all available queues
     */
    @Override
    public void clear() {
        this.cache.clear();
        this.pending.clear();
        this.loading.clear();
        this.preload.clear();
        this.getCurrentQueue().clear();
    }

    /**
     * @return the counter for currentlyLoadingReferences
     */
    public int getCurrentlyLoadingReferences() {
        synchronized (lock) {
            return this.currentlyLoadingReferences;
        }
    }

    /**
     * used to increment the currentlyLoadingReferences counter
     */
    private void incrementCurrentlyLoadingReferences() {
        synchronized (lock) {
            this.currentlyLoadingReferences += 1;
        }
    }

    /**
     * used to decrement the currentlyLoadingReferences counter
     */
    private void decrementCurrentlyLoadingReferences() {
        synchronized (lock) {
            this.currentlyLoadingReferences -= 1;
        }
    }

    /**
     * used to add a track to the pending queue
     *
     * @param origin the track link
     */
    private void addTrackAsPending(String origin) {
        synchronized (lock) {
            this.pending.add(origin);
        }
    }

    /**
     * used to remove a track from the pending queue
     *
     * @param origin the track link
     */
    private void removePendingLoadReference(String origin) {
        synchronized (lock) {
            this.loading.remove(origin);
        }
    }

    /**
     * @param origin the track origin
     * @return if the track origin is part of the pending queue or not
     */
    private boolean isCurrentlyPending(String origin) {
        synchronized (lock) {
            return this.loading.contains(origin);
        }
    }

    /**
     * This method is called internally by 'soundcloud-downloader' when a playlist is loaded
     *
     * @param link     the playlist origin
     * @param playlist a reference Object containing bare minimum track metadata
     */
    public void onPlaylistData(String link, Playlist playlist) {
        if (playlist.getLoadReferenceTimestamp() <= timestamp) return;
        List<Long> list = playlist.getList();
        Collections.shuffle(list);
        for (long id : list) {
            try {
                TrackQuery query = new TrackQuery(id, playlist.getId(), playlist.getSecret());
                ObjectCollection<Track> collection = Explorer.browse(query);
                for (Track track : collection) {
                    addTrackAsPending(track.getLink());
                }
            } catch (Exception e) {
                Logger.error(e);
            }
        }
    }

    /**
     * This method is called internally to speed up playback by returning a preloaded cached version of the track
     *
     * @param link  the track link to be handled by the application cache
     * @param track the reference object containing metadata for the specified track
     */
    private void handleAudioTrackCache(String link, Track track) {
        byte[] b = cache.get(link);
        cache.remove(link);
        onTrack(track, b);
    }

    /**
     * This method is called internally by 'soundcloud-downloader' when metadata for a track is loaded
     *
     * @param link  the track link
     * @param track the reference object containing metadata for the specified track
     */
    public void onTrackData(String link, Track track) {
        if (isCurrentlyPending(link)) {
            this.decrementCurrentlyLoadingReferences();
            this.removePendingLoadReference(link);
        }
        boolean isAudioCached = cache.containsKey(link);
        if (isAudioCached) handleAudioTrackCache(link, track);
        else if (track.getLoadReferenceTimestamp() > timestamp) {
            track.retrieveMP3().whenComplete((mp3, throwable) -> {
                if (throwable != null) Logger.error(throwable);
                if (mp3 == null) return;
                mp3.download(this);
            });
        }
    }

    /**
     * Enqueues a song for playback given the Track Object was initiated after the last Party has been left
     *
     * @param track the track that should be enqueued for playback
     * @param bytes the audio data (mp3) associated with the track
     */
    @Override
    public void onTrack(Track track, byte[] bytes) {
        if (track.getLoadReferenceTimestamp() <= timestamp) return;
        try {
            push(
                    new SimpleAudio(
                            track.getLink(),
                            track.getTitle(),
                            AudioFormatConverter.convertToWaveFormatFromMP3(bytes).toByteArray()
                    )
            );
        } catch (UnsupportedAudioFileException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method is called internally by 'soundcloud-downloader' when it successfully loads
     * the audio data (mp3) for a given Track, which is then either cached for preloading
     * or forwarded to the audio queue for playback.
     *
     * @param track the Track that has been fully loaded
     * @param b     the audio data (mp3) associated with the Track
     */
    @Override
    public void onCompletion(Track track, byte[] b) {
        if (preload.contains(track.getLink())) {
            this.cache.put(track.getLink(), b);
            this.preload.remove(track.getLink());
        } else {
            onTrack(track, b);
        }
    }

    /**
     * This method is called internally by 'soundcloud-downloader' when it fails to load
     * a fragment of a Track
     *
     * @param track    the track object which was unable to fully load
     * @param fragment the fragment of the track that failed to load
     */
    @Override
    public void onFailure(Track track, int fragment) {
        Logger.debug("Failed to load fragment {} for {}", fragment, track.getPermalink());
    }

    /**
     * This method is called internally by 'soundcloud-downloader' when it fails to load metadata
     * for the given origin
     *
     * @param link      the track origin that failed to be loaded
     * @param exception the exception that occured which prevented the song from loading
     */
    @Override
    public void onLoadFailure(String link, Exception exception) {
        Logger.error("Failed to load track {}: {}", link, exception.getMessage());
    }
}