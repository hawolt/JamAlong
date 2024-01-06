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

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.*;

public class SoundcloudAudioSource extends AbstractAudioSource implements DownloadCallback {
    private final Map<String, byte[]> cache = new HashMap<>();
    private final List<String> preload = new ArrayList<>();
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


    @Override
    public void load(String path) {
        Soundcloud.load(path, this);
    }

    @Override
    public void preload(String path) {
        this.preload.add(path);
        this.load(path);
    }

    public void onPlaylistData(String link, Playlist playlist) {
        if (playlist.getLoadReferenceTimestamp() <= timestamp) return;
        List<Long> list = playlist.getList();
        Collections.shuffle(list);
        for (long id : list) {
            try {
                TrackQuery query = new TrackQuery(id, playlist.getId(), playlist.getSecret());
                ObjectCollection<Track> collection = Explorer.browse(query);
                for (Track track : collection) {
                    load(track.getLink());
                }
            } catch (Exception e) {
                Logger.error(e);
            }
        }
    }

    public void onTrackData(String link, Track track) {
        if (cache.containsKey(link)) {
            onTrack(track, cache.get(link));
            cache.remove(link);
            return;
        }

        // PREVENT SONG TO LOAD WHEN IT HAS BEEN CLEARED
        if (track.getLoadReferenceTimestamp() <= timestamp) return;

        track.retrieveMP3().whenComplete((mp3, throwable) -> {
            if (throwable != null) Logger.error(throwable);
            if (mp3 == null) return;
            mp3.download(this);
        });
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
     * @param track     the track object which was unable to fully load
     * @param fragment  the fragment of the track that failed to load
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