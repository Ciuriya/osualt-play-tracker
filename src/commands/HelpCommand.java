package commands;

import java.util.List;

import data.CommandCategory;
import help.HelpBlurb;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import utils.Constants;
import utils.DiscordChatUtils;

public class HelpCommand extends Command {

	public HelpCommand() {
		super(null, false, true, CommandCategory.GENERAL, new String[]{"help"}, 
			  "Shows command list or specific command information.", 
			  "Shows every available command to the user using this command.\n" +
			  "Also allows the user to get more information about specific commands.", 
			  new String[]{"help", "Shows the command list."}, 
			  new String[]{"help <command>", "Displays a detailed help page for the specified command.\n" +
			  								 "Example: **`{prefix}help osuset`**"});
	}

	@Override
	public void onCommand(MessageReceivedEvent p_event, String[] p_args) {
		EmbedBuilder builder = new EmbedBuilder();
		
		builder.setColor(m_category.getColor());
		builder.setFooter(Constants.DEFAULT_FOOTER);
		
		if(p_args.length > 0) {
			Command cmd = Command.findCommand(p_args[0]);
			
			if(cmd != null) {
				if(!cmd.canUse(p_event.getAuthor(), p_event.getChannel())) return;
				
				FillCommandDescription(p_event, cmd, builder);
			} else {
				HelpBlurb blurb = HelpBlurb.findBlurb(p_args[0]);
				
				if (blurb != null) {
					FillHelpBlurbDescription(p_event, blurb, builder);
				} else {
					DiscordChatUtils.message(p_event.getChannel(), "Command not found!\n" + 
											 "Use `" + Constants.DEFAULT_PREFIX + 
											 "help` to get the full command list!");
					
					return;
				}
			}
		} else {
			FillCommandsListDescription(p_event, builder);
		}
		
		DiscordChatUtils.embed(p_event.getChannel(), builder.build());
	}
	
	private void FillCommandDescription(MessageReceivedEvent p_event, Command p_cmd, EmbedBuilder p_builder) {
		p_builder.setAuthor("Command information for " + p_cmd.getTriggers()[0],
							  Constants.SUPPORT_SERVER_LINK,
							  p_event.getJDA().getSelfUser().getAvatarUrl());
		p_builder.setColor(p_cmd.getCategory().getColor());
		
		String description = "**__" + p_cmd.getDescription() + "__**\n\n";
		String[][] usages = p_cmd.getUsages();
		
		for(int i = 0; i < usages.length; i++) {
			String[] usage = usages[i];
			
			description += "```\n" + Constants.DEFAULT_PREFIX + usage[0] + "```\n";
			description += usage[1].replaceAll("\\{prefix}", Constants.DEFAULT_PREFIX) + "\n";
			
			if(i + 1 < usages.length) description += "\n";
		}
		
		String[] triggers = p_cmd.getTriggers();
		
		if(triggers.length > 1) {
			description += "\n**Alias" + (triggers.length > 2 ? "es" : "") + "**:";
			
			for(int i = 1; i < triggers.length; i++)
				description += " `" + triggers[i] + "`";
		}
		
		p_builder.setDescription(description);
	}
	
	private void FillCommandsListDescription(MessageReceivedEvent p_event, EmbedBuilder p_builder) {
		p_builder.setAuthor("o!alt tracker command list", Constants.SUPPORT_SERVER_LINK,
				  p_event.getJDA().getSelfUser().getAvatarUrl());

		String description = "**__Use `" + Constants.DEFAULT_PREFIX + 
							 "help <command>` for details about specific commands.__**\n\n";
		
		for(CommandCategory category : CommandCategory.values()) {
			List<Command> commands = Command.findCommandsInCategory(category);
		
			if(commands.size() > 0) {
				String categoryText = "";
		
				for(Command cmd : commands)
					if(cmd.canUse(p_event.getAuthor(), p_event.getChannel()))
						categoryText += " `" + cmd.getTriggers()[0] + "`";
				
				if(categoryText.length() > 0) {
					categoryText = categoryText.substring(1);
					p_builder.addField(category.getName(), categoryText, true);
				}
			}
		}
		
		p_builder.setDescription(description);
	}
	
	private void FillHelpBlurbDescription(MessageReceivedEvent p_event, HelpBlurb p_blurb, EmbedBuilder p_builder) {
		p_builder.setAuthor("Help information for " + p_blurb.getNames()[0],
				  Constants.SUPPORT_SERVER_LINK,
				  p_event.getJDA().getSelfUser().getAvatarUrl());
		p_builder.setColor(getCategory().getColor());
		
		String description = p_blurb.getDescription() + "\n\n";
		
		p_builder.setDescription(description);
	}
}
