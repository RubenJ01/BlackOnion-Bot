package com.github.black0nion.blackonionbot.commands.misc;

import com.github.black0nion.blackonionbot.commands.SlashCommand;
import com.github.black0nion.blackonionbot.commands.SlashCommandEvent;
import com.github.black0nion.blackonionbot.utils.Placeholder;
import com.github.black0nion.blackonionbot.wrappers.TranslatedEmbed;
import com.github.black0nion.blackonionbot.wrappers.jda.BlackGuild;
import com.github.black0nion.blackonionbot.wrappers.jda.BlackMember;
import com.github.black0nion.blackonionbot.wrappers.jda.BlackUser;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.stream.IntStream;

public class PollCommand extends SlashCommand {

	// used in unit tests, don't change the visibility
	static final List<String> DIGITS_LIST = Arrays.asList("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten");

	static final List<String> DIGITS_UNICODE = Arrays.asList(
		"\\u0030\\u20E3", // 0
		"\\u0031\\u20E3", // 1
		"\\u0032\\u20E3", // 2
		"\\u0033\\u20E3", // 3
		"\\u0034\\u20E3", // 4
		"\\u0035\\u20E3", // 5
		"\\u0036\\u20E3", // 6
		"\\u0037\\u20E3", // 7
		"\\u0038\\u20E3", // 8
		"\\u0039\\u20E3", // 9
		"\\u1F51F" // 10
	);

	private static final String TOPIC = "topic";
	private static final String OPTION_PREFIX = "option_";

	private static final OptionData[] choices = IntStream.range(0, DIGITS_UNICODE.size())
		.mapToObj(i -> new OptionData(OptionType.STRING, OPTION_PREFIX + DIGITS_LIST.get(i), "Choice #" + i))
		.toArray(OptionData[]::new);

	public PollCommand() {
		super(builder(Commands.slash("poll", "Used to create a poll.")
			.addOption(OptionType.STRING, TOPIC, "Used to set the topic of the poll.", true)
			.addOptions(choices)));
	}

	@Override
	public void execute(SlashCommandEvent cmde, SlashCommandInteractionEvent e, BlackMember member, BlackUser author, BlackGuild guild, TextChannel pollChannel) {
		TranslatedEmbed embed = cmde.success();
		embed.setTitle(e.getOption(TOPIC, OptionMapping::getAsString));
		embed.setFooter("Poll created by " + author.getEscapedEffectiveName() + "#" + author.getDiscriminator(), author.getAvatarUrl());

		boolean hadNullOption = false;
		int index = 0;
		for (OptionData optionData : choices) {
			var option = e.getOption(optionData.getName());
			if (option != null) {
				embed.addField(cmde.getTranslation("optionnumber", new Placeholder("num", index)), DIGITS_UNICODE.get(index));
			} else {
				if (hadNullOption) throw new InputMismatchException("Options have to be filled in order");
				hadNullOption = true;
			}
			index++;
		}
		cmde.reply(embed);
	}
}