package com.github.black0nion.blackonionbot.api.impl.post;

import com.github.black0nion.blackonionbot.api.BlackSession;
import com.github.black0nion.blackonionbot.api.routes.IPostRoute;
import com.github.black0nion.blackonionbot.bot.Bot;
import com.github.black0nion.blackonionbot.commands.bot.ConfigCommand;
import com.github.black0nion.blackonionbot.oauth.DiscordUser;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.util.HashMap;

// TODO: what even is this smh
public class UpdateValue implements IPostRoute {

	@Override
	public String handle(final Request request, final Response response, final JSONObject body, final HashMap<String, String> headers, final BlackSession session, DiscordUser user) {
		final String newValue = headers.get("newValue");
		final String databaseKey = headers.get("databaseKey");
		final String guildId = headers.get("guild");
		Guild guild;
		try {
			guild = Bot.jda.getGuildById(guildId);
		} catch (final Exception e) {
			return "bing bong request gone";
		}
		if (ConfigCommand.setters.containsKey(databaseKey)) {
			try {
				ConfigCommand.setters.get(databaseKey).getKey().invoke(guild, newValue);
				return "worked (very pog)";
			} catch (Exception e) {
				e.printStackTrace();
				return "error bruhh";
			}
		}
		return "ding dong done";
	}

	@Override
	public String url() {
		return "updatevalue";
	}

	@Override
	public String[] requiredParameters() {
		return new String[]{"databaseKey", "newValue"};
	}

	@Override
	public boolean requiresLogin() {
		return false;
	}
}