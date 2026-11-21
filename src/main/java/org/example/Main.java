package org.example;

import net.dv8tion.jda.api.interactions.InteractionContextType;
import club.minnced.discord.jdave.interop.JDaveSessionFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.IntegrationType;

public class Main {
    public static void main(String[] args) throws Exception {
        String token = System.getenv("DISCORD_TOKEN");

        JDA jda = JDABuilder.createDefault(token)
                .setAudioModuleConfig(new AudioModuleConfig()
                        .withDaveSessionFactory(new JDaveSessionFactory()))
                .addEventListeners(new MusicListener())
                .build();

        jda.updateCommands().addCommands(
                Commands.slash("playfile", "Play an mp3 file in your voice channel")
                        .addOption(OptionType.ATTACHMENT, "attach", "Attach your file!", true)
                        .setContexts(InteractionContextType.GUILD).setIntegrationTypes(IntegrationType.GUILD_INSTALL),
                Commands.slash("skip", "Skip the current track")
                        .setContexts(InteractionContextType.GUILD).setIntegrationTypes(IntegrationType.GUILD_INSTALL),
                Commands.slash("queue", "Show what's playing and what's next")
                        .setContexts(InteractionContextType.GUILD).setIntegrationTypes(IntegrationType.GUILD_INSTALL),
                Commands.slash("stop", "Stop playing and leave the voice channel")
                        .setContexts(InteractionContextType.GUILD).setIntegrationTypes(IntegrationType.GUILD_INSTALL)
        ).queue();
    }
}