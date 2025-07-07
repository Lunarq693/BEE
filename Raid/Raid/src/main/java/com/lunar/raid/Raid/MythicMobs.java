package com.lunar.raid.Raid;

import com.lunar.raid.ExampleCondition;
import com.lunar.raid.ExampleItem;
import com.lunar.raid.ExampleMechanic;
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent;
import io.lumine.mythic.bukkit.events.MythicDropLoadEvent;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;


public class MythicMobs extends JavaPlugin implements Listener {

    private Logger log;

    @Override
    public void onEnable() {
        log = this.getLogger();
        Bukkit.getPluginManager().registerEvents(this, this);

        log.info("MythicMobs API Examples Plugin Enabled!");
    }

    public void onDisable(){
        log.info("MythicMobs API Examples Plugin Disabled!");
    }

    /*
     * Registers all of the custom mechanics when MythicMechanicLoadEvent is called
     */
    @EventHandler
    public void onMythicMechanicLoad(MythicMechanicLoadEvent event)	{
        log.info("MythicMechanicLoadEvent called for mechanic " + event.getMechanicName());

        if(event.getMechanicName().equalsIgnoreCase("EXAMPLE"))	{
            event.register(new ExampleMechanic(event.getConfig()));
            log.info("-- Registered Example mechanic!");
        }
    }

    /*
     * Registers all of the custom conditions when MythicConditionLoadEvent is called
     */
    @EventHandler
    public void onMythicConditionLoad(MythicConditionLoadEvent event)	{
        log.info("MythicConditionLoadEvent called for condition " + event.getConditionName());

        if(event.getConditionName().equalsIgnoreCase("EXAMPLE"))	{
            event.register(new ExampleCondition(event.getConfig()));
            log.info("-- Registered Example condition!");
        }
    }

    /*
     * Registers all of the custom drops when MythicDropLoadEvent is called
     */
    @EventHandler
    public void onMythicDropLoad(MythicDropLoadEvent event)	{
        log.info("MythicDropLoadEvent called for drop " + event.getDropName());

        if(event.getDropName().equalsIgnoreCase("EXAMPLE"))	{
            event.register(new ExampleItem(event.getConfig(), event.getArgument()));
            log.info("-- Registered Excample drop!");
        }
    }
}
