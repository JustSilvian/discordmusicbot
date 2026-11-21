# discordmusicbot

A small, self-hosted Discord bot written in Java that plays **mp3 files you upload** in a voice channel. Run `/playfile`, attach an mp3, and the bot joins your voice channel and plays it.

## Motivation

I put this Discord bot together because I wanted to be able to play stuff in voice chat, like sound effects, downloaded songs, and whatever other mp3 files I have on my PC. There are other bots that can do this, but the functionality is usually behind a paywall, so I decided to make and host my own. I wrote it in Java because that made it easier for me to deploy on an Oracle Cloud AMD micro VM instance running Ubuntu.

## Features

- Play any mp3 straight from a Discord attachment with a slash command
- Per-server queue, so several files can be lined up
- Skip the current track and view what's playing and what's next
- Leaves the voice channel automatically when the queue is empty
- Single runnable jar, easy to deploy on Windows or Linux

## Commands

| Command | Description |
|---|---|
| `/playfile attach:<file>` | Upload an mp3 and play it (or add it to the queue) in your current voice channel |
| `/queue` | Show the current track with its progress and the next tracks in the queue |
| `/skip` | Skip the current track (leaves the channel if nothing is left) |
| `/stop` | Stop playback, clear the queue and leave the voice channel |

You need to be in a voice channel when you use `/playfile`.

## Tech stack

- [JDA](https://github.com/discord-jda/JDA) 6.x for the Discord API
- [Lavaplayer](https://github.com/lavalink-devs/lavaplayer) for decoding and streaming audio
- [JDAVE](https://github.com/MinnDevelopment/jdave) for the DAVE end-to-end encryption protocol, which Discord now requires for voice
- Gradle, plus the Shadow plugin to build a single jar

## Requirements

- **Java 25 or newer** (required by JDAVE), both for building and for running
- A Discord application with a bot token (see below)
- Windows or Linux. The build includes JDAVE natives for Windows x64 and Linux x64/ARM64. For macOS, add `jdave-native-darwin` to `build.gradle`.

## Discord setup

1. Go to the [Discord Developer Portal](https://discord.com/developers/applications) and create a **New Application**.
2. Open the **Bot** tab and copy the bot token. Keep it secret, and never commit it to Git. If it leaks, use **Reset Token**.
3. No privileged gateway intents are needed.
4. Open the **Installation** tab:
   - Under *Installation Contexts*, enable only **Guild Install**.
   - Under *Default Install Settings → Guild Install*, set the scopes `applications.commands` and `bot`, and the permissions **Connect** and **Speak**.
5. Copy the **Install Link** and open it to add the bot to your server.

> The tick boxes in the permissions calculator on the Bot tab are only a calculator. They aren't saved. Set the permissions on the Installation page or in the invite link instead.

## Configuration

The bot reads its token from an environment variable:

| Variable | Description |
|---|---|
| `DISCORD_TOKEN` | Your bot token |

## Build

Clone the repo and build the jar:

```bash
git clone https://github.com/<your-username>/discordmusicbot.git
cd discordmusicbot
./gradlew shadowJar        # on Windows: .\gradlew shadowJar
```

The result is `build/libs/discordmusicbot.jar`.

## Run locally

**Windows (PowerShell):**

```powershell
$env:DISCORD_TOKEN="your_token"
java -jar build\libs\discordmusicbot.jar
```

**Linux / macOS:**

```bash
export DISCORD_TOKEN="your_token"
java -jar build/libs/discordmusicbot.jar
```

**From IntelliJ IDEA:** run `Main` and set the environment variable `DISCORD_TOKEN=your_token` in the run configuration. Add `--enable-native-access=ALL-UNNAMED` to the VM options to avoid native-access warnings.

When the console prints `Finished Loading!`, the bot is online. Global slash commands can take a minute to appear the first time. Press Ctrl+R in Discord to refresh if they don't show up.

## Deployment

The bot just needs a machine that is always on, has **Java 25 or newer**, and has internet access. It only makes outbound connections, so you don't need to open any ports. How you host it is up to you: a VPS, a home server, a Raspberry Pi, or simply your own PC.

The general idea:

1. [Build](#build) the jar and copy `discordmusicbot.jar` to the machine that will host it.
2. Install Java 25+ there.
3. Set the `DISCORD_TOKEN` environment variable and start the jar (see [Run locally](#run-locally)).
4. Keep it running in the background with whatever you prefer: systemd, tmux/screen, Docker, a hosting panel, etc.

I run it as a systemd service on an Oracle Cloud AMD (x86-64) micro VM with Ubuntu, but anything that can run Java will work.

A couple of tips:

- Small VMs have little RAM. If memory gets tight, limit the JVM heap by adding a flag like `-Xmx300m` before `-jar`.
- If you write a start script on Windows and run it on Linux, convert it to Unix line endings first (`sed -i 's/\r$//' start.sh`), otherwise you'll get a `bad interpreter` error.

## Troubleshooting

| Problem | Fix |
|---|---|
| `DetachedEntityException` when running a command | The bot isn't actually in the server. Reinstall it with the **Guild Install** link (scopes `bot` and `applications.commands`) |
| Bot joins but there's no sound, or voice fails to connect | Check that you're on Java 25+ and that the JDAVE native for your OS is in `build.gradle`. Read the console for the real error |
| Slash commands don't appear | Wait a minute, restart Discord with Ctrl+R, and make sure the bot was invited with `applications.commands` |
| `bad interpreter: /bin/bash^M` | The script has Windows line endings. Convert it with `sed -i 's/\r$//' yourscript.sh` |
| `Couldn't read that file.` | The file may be corrupted or use an unusual encoding. Try re-exporting it |
| `UnsupportedClassVersionError` | The machine's Java is too old. Install Java 25+ |

## Limitations

- Only `.mp3` attachments are accepted
- Queues live in memory, so they are cleared when the bot restarts
- Playback is per server, in the voice channel of the person who ran the command

## Project structure

```
src/main/java/org/example/
├── Main.java                  # starts the bot and registers slash commands
├── MusicListener.java         # handles /playfile, /skip, /queue, /stop
├── GuildMusic.java            # per-server audio player and queue
└── AudioPlayerSendHandler.java # bridges Lavaplayer audio into JDA
```

## Security notes

- Never commit your token. Add `.env` and any start script that contains it to `.gitignore`.
- If your token ever ends up in a screenshot, a commit, or a log, reset it in the Developer Portal right away.
