package utils;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import data.Log;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

public class DiscordChatUtils {

	public static void message(MessageChannelUnion p_channel, String p_message) {
		// cut the message into parts discord can send (<4000 characters per message)
		// 3960 gives us some leeway just in case
		for(int i = 0; i < (int) Math.ceil(p_message.length() / 3960f); i++) {
			String cutMessage = p_message;
			
			if(i > 0) {
				if((i + 1) * 3960 > cutMessage.length())
					cutMessage = cutMessage.substring(i * 3960);
				else cutMessage = cutMessage.substring(i * 3960, (i + 1) * 3960);
			}
			
			final String part = cutMessage;
			
			p_channel.sendMessage(part).queue(
					(message) -> Log.log(Level.INFO, "{Message sent in " + 
													 getChannelLogString(p_channel) + "} " + 
													 part),
					(error) -> Log.log(Level.WARNING, "Could not send message", error));
		}
	}
	
	public static void embed(MessageChannelUnion p_channel, MessageEmbed p_embed) {
		p_channel.sendMessageEmbeds(p_embed).queue(
				(message) -> Log.log(Level.INFO, "{Embed sent in " + 
												 getChannelLogString(p_channel) + "} " + 
												 p_embed.getAuthor().getName() + 
												 (p_embed.getTitle() != null ? "\n" + 
												 p_embed.getTitle() : "")),
				(error) -> Log.log(Level.WARNING, "Could not send embed", error));
	}
	
	public static CompletableFuture<Message> embedWithFuture(MessageChannelUnion p_channel, MessageEmbed p_embed) {
		CompletableFuture<Message> future = p_channel.sendMessageEmbeds(p_embed).submit();
		
		return future.whenComplete((message, error) -> {
			if(error != null)
				Log.log(Level.WARNING, "Could not send embed", error);
			else Log.log(Level.INFO, "{Embed sent in " + 
					 getChannelLogString(p_channel) + "} " + 
					 p_embed.getAuthor().getName() + 
					 (p_embed.getTitle() != null ? "\n" + 
					 p_embed.getTitle() : ""));
		});
	}

	public static String getChannelLogString(MessageChannelUnion p_channel) {
		if(p_channel.getType() == ChannelType.PRIVATE)
			return "Private/" + ((PrivateChannel) p_channel).getUser().getId();
		else if(p_channel.getType() == ChannelType.TEXT)
			return ((TextChannel) p_channel).getGuild().getName() + "#" + p_channel.getName();
		else return "";
	}
}
