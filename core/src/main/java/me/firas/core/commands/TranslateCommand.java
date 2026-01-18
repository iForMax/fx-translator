package me.firas.core.commands;

import me.firas.core.FXTranslatorAddon;
import me.firas.core.listeners.ChatTranslateListener;
import me.firas.core.util.ComponentUtil;
import net.labymod.api.Laby;
import net.labymod.api.client.chat.command.Command;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.component.format.TextColor;

/**
 * Command handler for manual translation requests
 * Usage: /fxtranslate <message>
 * Aliases: /translate
 */
public class TranslateCommand extends Command {

  private final FXTranslatorAddon addon;


  public TranslateCommand(FXTranslatorAddon addon) {
    super("fxtranslate", "translate");
    this.addon = addon;
  }

  @Override
  public boolean execute(String prefix, String[] arguments) {
    // Validate that addon is enabled
    if (!this.addon.configuration().enabled().get()) {
      ChatTranslateListener.skipNextButton();
      this.displayMessage(
          Component.translatable("fxtranslator.command.disabled", NamedTextColor.RED)
      );
      return true;
    }

    // Check for arguments
    if (arguments.length == 0) {
      ChatTranslateListener.skipNextButton();
      this.displayMessage(
          Component.translatable("fxtranslator.command.usage", NamedTextColor.RED)
      );
      return true;
    }

    // Join all arguments to form the message
    String messageToTranslate = String.join(" ", arguments);

    // Remove player name prefix if exists (e.g., "<PlayerName> message" -> "message")
    messageToTranslate = ComponentUtil.stripChatPrefix(messageToTranslate);

    // Validate message is not empty after cleanup
    if (messageToTranslate.trim().isEmpty()) {
      ChatTranslateListener.skipNextButton();
      this.displayMessage(
          Component.translatable("fxtranslator.command.noText", NamedTextColor.RED)
      );
      return true;
    }

    // Show loading message if enabled
    if (this.addon.configuration().showLoadingMessage().get()) {
      ChatTranslateListener.skipNextButton();
      this.displayMessage(
          Component.translatable("fxtranslator.command.translating", NamedTextColor.GRAY)
      );
    }

    // Get language codes
    String sourceLang = this.addon.configuration().sourceLanguage().get().getCode();
    String targetLang = this.addon.configuration().targetLanguage().get().getCode();

    // Store original message component for color preservation
    final Component originalComponent = Component.text(messageToTranslate);

    // Translate asynchronously
    this.addon.getTranslationService().translate(messageToTranslate, sourceLang, targetLang)
        .thenAccept(translatedText -> {
          // Display translated message on main thread
          Laby.labyAPI().minecraft().executeOnRenderThread(() -> displayTranslationResult(translatedText, originalComponent));
        })
        .exceptionally(throwable -> {
          // Display error on main thread
          Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
            ChatTranslateListener.skipNextButton();
            this.displayMessage(
                Component.translatable("fxtranslator.command.failed", NamedTextColor.RED)
                    .append(Component.text(throwable.getMessage(), NamedTextColor.DARK_RED))
            );
          });
          return null;
        });

    // Command executed successfully (response will come asynchronously)
    return true;
  }

  /**
   * Displays the translation result based on user preferences
   *
   * @param translatedText The translated result
   * @param originalComponent The original component with colors/formatting
   */
  private void displayTranslationResult(String translatedText, Component originalComponent) {
    boolean showTranslatedPrefix = this.addon.configuration().showTranslatedPrefix().get();
    boolean preserveColors = this.addon.configuration().preserveMessageColors().get();

    // Build the translated component
    Component translatedComponent;

    if (preserveColors) {
      // Preserve the original message colors by extracting color from original component
      TextColor originalColor = originalComponent.getColor();
      if (originalColor != null) {
        translatedComponent = Component.text(translatedText, originalColor);
      } else {
        translatedComponent = Component.text(translatedText, NamedTextColor.GREEN);
      }
    } else {

      // Use default green color for translated text
      translatedComponent = Component.text(translatedText).color(NamedTextColor.GREEN);


    }

    // Mark to skip button on the translation output
    ChatTranslateListener.skipNextButton();

    // Add "Translated: " prefix if enabled
    if (showTranslatedPrefix) {
      this.displayMessage(
          Component.translatable("fxtranslator.command.translated", NamedTextColor.GRAY)
              .append(translatedComponent)
      );
    } else {
      // Just show the translated text without prefix
      this.displayMessage(translatedComponent);
    }
  }
}