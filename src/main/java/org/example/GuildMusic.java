package org.example;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GuildMusic extends AudioEventAdapter {
    final AudioPlayer player;
    final AudioPlayerSendHandler handler;
    final BlockingQueue<AudioTrack> queue = new LinkedBlockingQueue<>();
    private final Runnable onFinished;

    GuildMusic(AudioPlayerManager manager, Runnable onFinished) {
        this.player = manager.createPlayer();
        this.player.addListener(this);
        this.handler = new AudioPlayerSendHandler(player);
        this.onFinished = onFinished;
    }

    void enqueue(AudioTrack track) {
        if (!player.startTrack(track, true)) queue.offer(track);
    }

    /** Skips to the next track, or stops and leaves if the queue is empty. */
    void skip() {
        AudioTrack next = queue.poll();
        if (next != null) {
            player.startTrack(next, false);
        } else {
            player.stopTrack();
            onFinished.run();
        }
    }

    List<AudioTrack> upcoming() {
        return new ArrayList<>(queue);
    }

    @Override
    public void onTrackEnd(AudioPlayer p, AudioTrack track, AudioTrackEndReason reason) {
        if (!reason.mayStartNext) return;
        AudioTrack next = queue.poll();
        if (next != null) p.startTrack(next, false);
        else onFinished.run();
    }

    // --- helpers ---

    static String title(AudioTrack track) {
        Object name = track.getUserData();
        return name != null ? name.toString() : track.getInfo().title;
    }

    static String time(long ms) {
        long s = ms / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }
}