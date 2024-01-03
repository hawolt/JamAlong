package com.hawolt.source.impl;

import com.hawolt.Soundcloud;
import com.hawolt.audio.AudioFormatConverter;
import com.hawolt.chromium.LocalExecutor;
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

    public SoundcloudAudioSource() {
        Soundcloud.register(Track.class, new TrackManager(this::onTrackData));
        Soundcloud.register(Playlist.class, new PlaylistManager(this::onPlaylistData));
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
        if (playlist.getObjectTimestamp() <= LocalExecutor.RESET_TIMESTAMP) return;
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
        track.retrieveMP3().whenComplete((mp3, throwable) -> {
            if (throwable != null) Logger.error(throwable);
            if (mp3 == null) return;
            mp3.download(this);
        });
    }

    @Override
    public void onCompletion(Track track, byte[] b) {
        if (preload.contains(track.getLink())) {
            this.cache.put(track.getLink(), b);
            this.preload.remove(track.getLink());
        } else {
            onTrack(track, b);
        }
    }

    @Override
    public void onFailure(Track track, int fragment) {
        Logger.debug("Failed to load fragment {} for {}", fragment, track.getPermalink());
    }

    @Override
    public void onLoadFailure(String link, Exception exception) {
        Logger.error("Failed to load track {}: {}", link, exception.getMessage());
    }

    @Override
    public void onTrack(Track track, byte[] bytes) {
        if (track.getObjectTimestamp() <= LocalExecutor.RESET_TIMESTAMP) return;
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
}