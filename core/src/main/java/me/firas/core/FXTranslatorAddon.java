package me.firas.core;

import me.firas.core.commands.TranslateCommand;
import me.firas.core.listeners.ChatTranslateListener;
import me.firas.core.service.TranslationService;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.models.addon.annotation.AddonMain;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@AddonMain
public class FXTranslatorAddon extends LabyAddon<TranslatorConfiguration> {

  private TranslationService translationService;

  @Override
  protected void enable() {
    this.registerSettingCategory();

    // Initialize translation service
    this.translationService = new TranslationService(this);

    // Register chat listener
    this.registerListener(new ChatTranslateListener(this));

    // Register command
    this.registerCommand(new TranslateCommand(this));

    // Schedule periodic cache cleanup to prevent memory issues (guideline #1)
    ScheduledExecutorService cacheCleanupScheduler = Executors.newSingleThreadScheduledExecutor();
    cacheCleanupScheduler.scheduleAtFixedRate(
        () -> this.translationService.cleanExpiredCache(),
        5, // Initial delay
        5, // Period
        TimeUnit.MINUTES
    );

    this.logger().info("FX Translator Addon enabled!");
    this.logger().info("Using translation engine: " +
        this.configuration().translatorEngine().get().name());
  }


  @Override
  protected Class<TranslatorConfiguration> configurationClass() {
    return TranslatorConfiguration.class;
  }

  public TranslationService getTranslationService() {
    return this.translationService;
  }
}