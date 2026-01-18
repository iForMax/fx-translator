package me.firas.core.listeners;

import me.firas.core.FXTranslatorAddon;
import me.firas.core.util.ComponentUtil;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.event.ClickEvent;
import net.labymod.api.client.component.event.HoverEvent;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.chat.ChatReceiveEvent;
import net.labymod.api.event.client.chat.ChatMessageSendEvent;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Listener for chat messages to add translation functionality
 * Adds clickable buttons to chat messages for translation
 */
public class ChatTranslateListener {

  private final FXTranslatorAddon addon;
  private static final ThreadLocal<Boolean> SKIP_BUTTON = ThreadLocal.withInitial(() -> false);

  // Track if we're expecting the player's own message to arrive
  private static final AtomicBoolean expectingOwnMessage = new AtomicBoolean(false);

  public ChatTranslateListener(FXTranslatorAddon addon) {
    this.addon = addon;
  }

  /**
   * Mark the next chat message to skip button addition
   * Used for translation output messages
   */
  public static void skipNextButton() {
    SKIP_BUTTON.set(true);
  }

  /**
   * Track when the player sends a message
   */
  @Subscribe()
  public void onChatSend(ChatMessageSendEvent event) {
    if (!this.addon.configuration().enabled().get()) {
      return;
    }

    String message = event.getMessage();

    // Don't track commands
    if (message.startsWith("/")) {
      return;
    }

    // Set flag that we're expecting the player's message to be echoed back
    expectingOwnMessage.set(true);
  }

  @Subscribe
  public void onChatReceive(ChatReceiveEvent event) {
    if (!this.addon.configuration().enabled().get()) {
      return;
    }

    // Check if we should skip button for this message
    if (SKIP_BUTTON.get()) {
      SKIP_BUTTON.set(false);
      return;
    }

    Component originalMessage = event.message();

    String messageText = ComponentUtil.getPlainText(originalMessage);

    // Skip empty messages
    if (messageText.trim().isEmpty()) {
      return;
    }

    // Check if this message was received shortly after the player sent a message
    boolean isOwnMessage = expectingOwnMessage.compareAndSet(true, false);

    if (isOwnMessage && !this.addon.configuration().showButtonOnOwnMessages().get()) {
      return;
    }

    // Add clickable translate button
    addClickableButton(event, originalMessage, messageText);
  }


  /**
   * Adds a clickable translate button to the message
   */
  private void addClickableButton(ChatReceiveEvent event, Component originalMessage, String messageText) {
    String buttonText = this.addon.configuration().buttonText().get();
    if (!this.addon.configuration().preserveMessageColors().get()){
      messageText = stripColors(messageText);
    }
    // Create clickable translate button
    Component translateButton = Component.text(" ")
        .append(Component.text(buttonText, NamedTextColor.GOLD)
            .clickEvent(ClickEvent.runCommand("/fxtranslate " + messageText))
            .hoverEvent(HoverEvent.showText(
                Component.translatable("fxtranslator.chat.hoverText", NamedTextColor.GRAY)
            )));

    Component modifiedMessage = originalMessage.append(translateButton);
    event.setMessage(modifiedMessage);
  }


  public static String stripColors(String text) {
    if (text == null) return null;

    text = text.replaceAll("(?i)§x(§[0-9a-f]){6}", "");

    text = text.replaceAll("(?i)§[0-9a-fk-or]", "");

    return text;
  }

}