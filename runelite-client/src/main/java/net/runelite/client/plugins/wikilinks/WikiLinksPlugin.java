/*
 * Copyright (c) 2018, Joshua Filby <joshua@filby.me>
 * Copyright (c) 2018, Jordan Atwood <jordan.atwood423@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.wikilinks;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.IconID;
import net.runelite.api.MessageNode;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.QuantityFormatter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
	name = "Wiki Links",
	description = "Shows virtual levels (beyond 99) and virtual skill total on the skills tab.",
	tags = {"skill", "total", "max"},
	enabledByDefault = false
)
public class WikiLinksPlugin extends Plugin
{
	private static final String TOTAL_LEVEL_TEXT_PREFIX = "Total level:<br>";
	private final Pattern LINK_PATTERN = Pattern.compile("\\[\\[(\\w[\\w\\s\\/'-]*)\\]\\]");

	@Inject
	private WikiLinksConfig config;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Provides
	WikiLinksConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WikiLinksConfig.class);
	}

	@Override
	protected void shutDown()
	{
		clientThread.invoke(this::simulateSkillChange);
	}

	@Subscribe
	public void onPluginChanged(PluginChanged pluginChanged)
	{
		// this is guaranteed to be called after the plugin has been registered by the eventbus. startUp is not.
		if (pluginChanged.getPlugin() == this)
		{
			clientThread.invoke(this::simulateSkillChange);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!configChanged.getGroup().equals("wikilinks"))
		{
			return;
		}

		clientThread.invoke(this::simulateSkillChange);
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage) {
		final ChatMessageType messageType = chatMessage.getType();
		if (messageType != ChatMessageType.PUBLICCHAT) {
			return;
		}

//		Matcher m = LINK_PATTERN.matcher(chatMessage.getMessage());

		final MessageNode node = chatMessage.getMessageNode();

		final String linked = chatMessage.getMessage().replace("\\[\\[(\\w[\\w\\s\\/'-]*)\\]\\]", "<query=https://google.com><u><colHIGHLIGHT><img=12>$1</u><colNORMAL>");
		node.setRuneLiteFormatMessage(linked);

		Widget chatbox = client.getWidget(WidgetInfo.CHATBOX_MESSAGE_LINES);

		Widget a = chatbox.createChild(-1, WidgetType.RECTANGLE);
		a.setTextColor(0x113399);
		a.setFilled(true);
		a.setOriginalX(0);
		a.setOriginalY(0);
		a.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER);
		a.setYPositionMode(WidgetPositionMode.ABSOLUTE_CENTER);
		a.setOriginalWidth(200);
		a.setOriginalHeight(20);
		a.revalidate();

		return;


//		while (m.find()) {
//			final StringBuilder builder = new StringBuilder()
//					.append("my test message ")
//					.append("<u>")
//					.append("<col")
//					.append(ChatColorType.HIGHLIGHT.name())
//					.append(">")
//					.append("<img=")
//					.append(IconID.CHAIN_LINK.getIndex())
//					.append(">")
//					.append("link")
//					.append("</u>")
//					.append("<col")
//					.append(ChatColorType.NORMAL.name())
//					.append(">")
//					.append(" hello");
//
//			final ChatMessageBuilder msgBuilder = new ChatMessageBuilder()
//					.append("my test message ")
//					.append(ChatColorType.HIGHLIGHT)
//					.img(IconID.CHAIN_LINK.getIndex())
//					.append("link")
//					.append(ChatColorType.NORMAL)
//					.append(" hello");
//
//			log.debug(builder.toString());
//			log.debug(msgBuilder.build());
//			log.debug(chatMessage.getMessage());
//
//
//			node.setRuneLiteFormatMessage(linked);
////			chatMessageManager.queue(QueuedMessage.builder()
////					.type(chatMessage.getType())
////					.runeLiteFormattedMessage(builder.toString())
////					.build());
//			log.debug(m.group(1));
//			log.debug("matched 1");
//		}
	}

	private void onClick(Widget w) {

	}

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent e)
	{
		final String eventName = e.getEventName();

		final int[] intStack = client.getIntStack();
		final int intStackSize = client.getIntStackSize();
		final String[] stringStack = client.getStringStack();
		final int stringStackSize = client.getStringStackSize();

		switch (eventName)
		{
			case "skillTabBaseLevel":
				final int skillId = intStack[intStackSize - 2];
				final Skill skill = Skill.values()[skillId];
				final int exp = client.getSkillExperience(skill);

				// alter the local variable containing the level to show
				intStack[intStackSize - 1] = Experience.getLevelForXp(exp);
				break;
			case "skillTabMaxLevel":
				// alter max level constant
				intStack[intStackSize - 1] = Experience.MAX_VIRT_LEVEL;
				break;
			case "skillTabTotalLevel":
				if (!config.virtualTotalLevel())
				{
					break;
				}
				int level = 0;

				for (Skill s : Skill.values())
				{
					level += Experience.getLevelForXp(client.getSkillExperience(s));
				}

				stringStack[stringStackSize - 1] = TOTAL_LEVEL_TEXT_PREFIX + level;
				break;
		}
	}

	private void simulateSkillChange()
	{
		// this fires widgets listening for all skill changes
		for (Skill skill : Skill.values())
		{
			client.queueChangedSkill(skill);
		}
	}
}
