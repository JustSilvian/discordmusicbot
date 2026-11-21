package org.example;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MusicListener extends ListenerAdapter {
    private final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    private final Map<Long, GuildMusic> guilds = new ConcurrentHashMap<>();

    public MusicListener() {
        playerManager.registerSourceManager(new HttpAudioSourceManager());
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;

        if (guild.isDetached()) {
            event.reply("I need to be added to this server as a bot first. Ask an admin to invite me.")
                    .setEphemeral(true).queue();
            return;
        }

        switch (event.getName()) {
            case "playfile" -> playFile(event, guild);
            case "skip" -> skip(event, guild);
            case "queue" -> showQueue(event, guild);
            case "stop" -> stop(event, guild);
        }
    }

    private void playFile(SlashCommandInteractionEvent event, Guild guild) {
        Message.Attachment file = event.getOption("attach").getAsAttachment();

        if (!"mp3".equalsIgnoreCase(file.getFileExtension())) {
            event.reply("Please attach an .mp3 file.").setEphemeral(true).queue();
            return;
        }

        GuildVoiceState voice = event.getMember().getVoiceState();
        if (voice == null || !voice.inAudioChannel()) {
            event.reply("Join a voice channel first!").setEphemeral(true).queue();
            return;
        }
        AudioChannel channel = voice.getChannel();

        event.deferReply().queue();

        GuildMusic music = guilds.computeIfAbsent(guild.getIdLong(),
                id -> new GuildMusic(playerManager, () -> guild.getAudioManager().closeAudioConnection()));

        guild.getAudioManager().setSendingHandler(music.handler);
        guild.getAudioManager().openAudioConnection(channel);

        playerManager.loadItem(file.getUrl(), new AudioLoadResultHandler() {
            @Override public void trackLoaded(AudioTrack track) {
                track.setUserData(file.getFileName()); // used for /queue and /skip
                music.enqueue(track);
                event.getHook().sendMessage("Queued: **" + file.getFileName() + "**").queue();
            }
            @Override public void playlistLoaded(AudioPlaylist playlist) {
                event.getHook().sendMessage("That doesn't look like a single audio file.").queue();
            }
            @Override public void noMatches() {
                event.getHook().sendMessage("Couldn't read that file.").queue();
            }
            @Override public void loadFailed(FriendlyException e) {
                event.getHook().sendMessage("Failed to load: " + e.getMessage()).queue();
            }
        });
    }

    private void skip(SlashCommandInteractionEvent event, Guild guild) {
        GuildMusic music = guilds.get(guild.getIdLong());
        AudioTrack current = music == null ? null : music.player.getPlayingTrack();
        if (current == null) {
            event.reply("Nothing is playing.").setEphemeral(true).queue();
            return;
        }
        String skipped = GuildMusic.title(current);
        music.skip();
        event.reply("Skipped **" + skipped + "**").queue();
    }

    private void showQueue(SlashCommandInteractionEvent event, Guild guild) {
        GuildMusic music = guilds.get(guild.getIdLong());
        AudioTrack current = music == null ? null : music.player.getPlayingTrack();
        if (current == null) {
            event.reply("Nothing is playing.").setEphemeral(true).queue();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("**Now playing:** ").append(GuildMusic.title(current))
                .append(" `").append(GuildMusic.time(current.getPosition()))
                .append(" / ").append(GuildMusic.time(current.getDuration())).append("`\n");

        List<AudioTrack> upcoming = music.upcoming();
        if (upcoming.isEmpty()) {
            sb.append("\nQueue is empty.");
        } else {
            sb.append("\n**Up next:**\n");
            int shown = Math.min(upcoming.size(), 10);
            for (int i = 0; i < shown; i++) {
                AudioTrack t = upcoming.get(i);
                sb.append(i + 1).append(". ").append(GuildMusic.title(t))
                        .append(" `").append(GuildMusic.time(t.getDuration())).append("`\n");
            }
            if (upcoming.size() > shown) {
                sb.append("...and ").append(upcoming.size() - shown).append(" more");
            }
        }
        event.reply(sb.toString()).queue();
    }

    private void stop(SlashCommandInteractionEvent event, Guild guild) {
        GuildMusic music = guilds.get(guild.getIdLong());
        if (music != null) {
            music.queue.clear();
            music.player.stopTrack();
        }
        guild.getAudioManager().closeAudioConnection();
        event.reply("Stopped.").queue();
    }
}