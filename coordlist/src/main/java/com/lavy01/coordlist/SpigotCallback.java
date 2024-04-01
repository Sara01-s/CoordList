package com.lavy01.coordlist;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

// original by rodel77, src: https://gist.github.com/rodel77/b6966471d51d5176d0da9bd0120d0a4b
// Modified by Sara01 01/04/2024
public class SpigotCallback {

	private final static HashMap<UUID, Consumer<Player>> Callbacks = new HashMap<>();
	
	public SpigotCallback(Plugin plugin) {
		Bukkit.getPluginManager().registerEvents(new Listener() {

			@EventHandler
			public void command(PlayerCommandPreprocessEvent event) {

				if (event.getMessage().startsWith("/spigot:callback")){
					String[] args = event.getMessage().split(" ");

					if (args.length != 2) return;

					if (args[1].split("-").length == 5){

						var uuid = UUID.fromString(args[1]);
						var consumer = Callbacks.remove(uuid);

						if (consumer != null) {
							consumer.accept(event.getPlayer());
						}

						event.setCancelled(true);
					}
				}
			}
		}, plugin);
	}
	
	public static void createCommand(final TextComponent text, final Consumer<Player> consumer){
		final var uuid = UUID.randomUUID();

		Callbacks.put(uuid, consumer);
		text.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/spigot:callback " + uuid));
	}
}