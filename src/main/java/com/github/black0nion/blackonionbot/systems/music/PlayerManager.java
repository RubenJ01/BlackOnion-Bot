package com.github.black0nion.blackonionbot.systems.music;

import com.github.black0nion.blackonionbot.bot.Bot;
import com.github.black0nion.blackonionbot.systems.language.LanguageSystem;
import com.github.black0nion.blackonionbot.utils.EmbedUtils;
import com.github.black0nion.blackonionbot.utils.Utils;
import com.github.black0nion.blackonionbot.config.api.Config;
import com.github.black0nion.blackonionbot.wrappers.jda.BlackGuild;
import com.github.black0nion.blackonionbot.wrappers.jda.BlackUser;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import se.michaelthelin.spotify.requests.data.tracks.GetTrackRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PlayerManager {
	private static PlayerManager instance;
	private SpotifyApi spotifyApi;
	private final Map<Long, GuildMusicManager> musicManagers;
	private final AudioPlayerManager audioPlayerManager;
	private final Config config;

	public PlayerManager(Config config) {
		instance = this;
		this.musicManagers = new HashMap<>();
		this.audioPlayerManager = new DefaultAudioPlayerManager();
		this.config = config;

		AudioSourceManagers.registerRemoteSources(this.audioPlayerManager);
		AudioSourceManagers.registerLocalSource(this.audioPlayerManager);
	}

	public void init() {
		if (config.getSpotifyClientId() == null || config.getSpotifyClientSecret() == null) {
			LoggerFactory.getLogger(PlayerManager.class).warn("Spotify client ID or secret is null, disabling Spotify integration");
			return;
		}

		spotifyApi = new SpotifyApi.Builder().setClientId(config.getSpotifyClientId()).setClientSecret(config.getSpotifyClientSecret()).build();

		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
			try {
				final ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials().build();
				final ClientCredentials credentials = clientCredentialsRequest.execute();
				spotifyApi.setAccessToken(credentials.getAccessToken());
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}, 0, 58, TimeUnit.MINUTES);
	}

	@Nullable
	public static PlayerManager getInstance() {
		return instance;
	}

	public GuildMusicManager getMusicManager(final TextChannel channel) {
		return this.musicManagers.computeIfAbsent(channel.getGuild().getIdLong(), guildId -> {
			final GuildMusicManager guildMusicManager = new GuildMusicManager(this.audioPlayerManager, BlackGuild.from(channel.getGuild()));

			channel.getGuild().getAudioManager().setSendingHandler(guildMusicManager.getSendHandler());

			return guildMusicManager;
		});
	}

	public void loadAndPlay(final BlackUser author, final TextChannel channel, @Nonnull String trackUrl, final AudioManager manager, final AudioChannel vc) {
		final GuildMusicManager musicManager = this.getMusicManager(channel);
		final BlackGuild guild = BlackGuild.from(channel.getGuild());
		MusicSystem.channels.put(guild.getIdLong(), channel.getIdLong());

		if (trackUrl.contains("spotify.com")) {
			final String[] parsed = trackUrl.split("/track/");
			if (parsed.length == 2) {
				final GetTrackRequest request = spotifyApi.getTrack(parsed[1]).build();
				try {
					trackUrl = "ytsearch:" + request.execute().getName();
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}

		this.audioPlayerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
			@Override
			public void trackLoaded(final AudioTrack track) {
				musicManager.scheduler.queueTrack(track, manager, vc);
				channel.sendMessageEmbeds(EmbedUtils.getSuccessEmbed(author, guild).addField("addedtoqueue", track.getInfo().title + " by " + track.getInfo().author, false).build()).queue();
			}

			@Override
			public void playlistLoaded(final AudioPlaylist playlist) {
				final List<AudioTrack> tracks = playlist.getTracks();

				if (playlist.isSearchResult()) {
					final EmbedBuilder builder = EmbedUtils.getSuccessEmbed(author, guild);
					final List<AudioTrack> trackz = playlist.getTracks().subList(0, (playlist.getTracks().size() > 10 ? 9 : playlist.getTracks().size()));
					for (int i = 0; i < trackz.size(); i++) {
						final AudioTrack track = trackz.get(i);
						builder.addField(Utils.EMOJIS[i] + " " + track.getInfo().title, "By: " + track.getInfo().author, false);
					}
					channel.sendMessageEmbeds(builder.build()).queue(msg -> {
						for (int i = 0; i < trackz.size(); i++) {
							msg.addReaction(Emoji.fromUnicode(Utils.NUMBERS_UNICODE.get(i))).queue();
						}
						PlayerManager.this.wait(author, msg, trackz, musicManager, manager, vc);
					});
				} else {
					final EmbedBuilder builder = EmbedUtils.getSuccessEmbed(author, guild);

					builder.setTitle("addedtoqueue");

					if (tracks.size() <= 10) {
						tracks.forEach(track -> {
							builder.addField(track.getInfo().title, "By: " + track.getInfo().author, false);
							musicManager.scheduler.queueTrack(track, manager, vc);
						});
					} else {
						builder.setDescription(LanguageSystem.getTranslation("thistracksplusadded", author, guild).replace("%tracks%", String.valueOf(tracks.size() - 10)));
						for (int i = 0; i < tracks.size(); i++) {
							final AudioTrack track = tracks.get(i);
							musicManager.scheduler.queueTrack(track, manager, vc);
							if (i < 10) {
								builder.addField(track.getInfo().title, "By: " + track.getInfo().author, false);
							}
						}
					}

					channel.sendMessageEmbeds(builder.build()).queue();
				}
			}

			@Override
			public void noMatches() {
				channel.sendMessageEmbeds(EmbedUtils.getErrorEmbed(author, guild).addField("notfound", "musicnotfound", false).build()).queue();
			}

			@Override
			public void loadFailed(final FriendlyException exception) {
				channel.sendMessageEmbeds(EmbedUtils.getErrorEmbed(author, guild).addField("errorhappened", "somethingwentwrong", false).build()).queue();
				exception.printStackTrace();
			}
		});
	}

	private void wait(final BlackUser author, final Message msg, final List<AudioTrack> tracks, final GuildMusicManager musicManager, final AudioManager manager, final AudioChannel vc) {
		Bot.getInstance().getEventWaiter().waitForEvent(MessageReactionAddEvent.class, event -> event.getChannelType() == ChannelType.TEXT && msg.getIdLong() == event.getMessageIdLong() && !Objects.requireNonNull(event.getUser()).isBot(), event -> {
			event.getReaction().removeReaction(Objects.requireNonNull(event.getUser())).queue();
			if (event.getEmoji().getType() != Emoji.Type.UNICODE || !Utils.NUMBERS_UNICODE.containsValue(event.getEmoji().getAsReactionCode()) || tracks.size() < Utils.NUMBERS_UNICODE.entrySet().stream().filter(entry -> entry.getValue().equals(event.getEmoji().getAsReactionCode())).findFirst().orElseThrow().getKey()) {
				this.wait(author, msg, tracks, musicManager, manager, vc);
				return;
			}
			final AudioTrack track = tracks.get(Utils.NUMBERS_UNICODE.entrySet().stream().filter(entry -> entry.getValue().equals(event.getEmoji().getAsReactionCode())).findFirst().orElseThrow().getKey());
			musicManager.scheduler.queueTrack(track, manager, vc);
		}, 1, TimeUnit.MINUTES, () -> msg.editMessageEmbeds(EmbedUtils.getErrorEmbed(author, BlackGuild.from(msg.getGuild())).addField("timeout", "tooktoolong", false).build()).queue());
	}
}
