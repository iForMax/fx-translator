package me.firas.core.util;

import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.TextComponent;
import net.labymod.api.client.component.TranslatableComponent;
import net.labymod.api.client.component.format.Style;
import net.labymod.api.client.component.format.TextColor;
import net.labymod.api.client.component.format.TextDecoration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for working with LabyMod Components
 * Provides methods for text extraction, formatting preservation, and message parsing
 */
public class ComponentUtil {

  private static final Pattern CHAT_PREFIX_PATTERN = Pattern.compile("^<[^>]+>\\s*");

  /**
   * Extracts plain text from a Component (recursively)
   * Compliant with LabyMod guidelines - no legacy color codes used
   *
   * @param component The component to extract text from
   * @return Plain text string without formatting
   */
  public static String getPlainText(Component component) {
    if (component == null) {
      return "";
    }

    StringBuilder text = new StringBuilder();

    if (component instanceof TextComponent textComponent) {
      text.append(textComponent.getText());
    } else if (component instanceof TranslatableComponent translatableComponent) {
      text.append(translatableComponent.getKey());
    }

    // Recursively get text from children
    for (Component child : component.getChildren()) {
      text.append(getPlainText(child));
    }

    return text.toString();
  }

  /**
   * Strips chat prefix like "<PlayerName> " from message
   *
   * @param message The message to strip
   * @return Message without prefix
   */
  public static String stripChatPrefix(String message) {
    if (message == null || message.isEmpty()) {
      return message;
    }

    Matcher matcher = CHAT_PREFIX_PATTERN.matcher(message);
    if (matcher.find()) {
      return message.substring(matcher.end());
    }
    return message;
  }

  /**
   * Preserves the formatting (colors, styles) from the original component
   * and applies it to the new text

   * This is compliant with LabyMod guidelines as it uses proper Component API
   * instead of legacy color codes
   *
   * @param originalComponent The original component with formatting
   * @param newText The new text to apply formatting to
   * @return New component with preserved formatting
   */
  public static Component preserveFormatting(Component originalComponent, String newText) {
    if (originalComponent == null || newText == null) {
      return Component.text(newText != null ? newText : "");
    }

    // Extract the style from the original component
    Style originalStyle = originalComponent.style();

    // Create new component with the same style - using non-deprecated methods
    Component newComponent = Component.text(newText)
        .color(originalStyle.getColor());

    // Apply decorations individually (non-deprecated approach)
    for (TextDecoration decoration : TextDecoration.getValues()) {
      if (originalStyle.hasDecoration(decoration)) {
        newComponent = newComponent.decorate(decoration);
      }
    }

    // Preserve click and hover events if they exist
    if (originalStyle.getClickEvent() != null) {
      newComponent = newComponent.clickEvent(originalStyle.getClickEvent());
    }

    if (originalStyle.getHoverEvent() != null) {
      newComponent = newComponent.hoverEvent(originalStyle.getHoverEvent());
    }

    return newComponent;
  }

  /**
   * Extracts the first color from a component hierarchy
   * Used to maintain consistent coloring for translated messages
   *
   * @param component The component to extract color from
   * @return The text color, or null if no color is set
   */
  public static TextColor extractColor(Component component) {
    if (component == null) {
      return null;
    }

    TextColor color = component.style().getColor();
    if (color != null) {
      return color;
    }

    // Check children recursively
    for (Component child : component.getChildren()) {
      TextColor childColor = extractColor(child);
      if (childColor != null) {
        return childColor;
      }
    }

    return null;
  }

  /**
   * Checks if a message is likely a player chat message
   * (as opposed to system messages, server announcements, etc.)
   *
   * @param component The component to check
   * @return true if it appears to be a player message
   */
  public static boolean isPlayerMessage(Component component) {
    String text = getPlainText(component);
    return text.matches("^<[^>]+>.*");
  }

  /**
   * Extracts the player name from a chat message component
   *
   * @param component The chat message component
   * @return The player name, or null if not found
   */
  public static String extractPlayerName(Component component) {
    String text = getPlainText(component);
    Matcher matcher = Pattern.compile("^<([^>]+)>").matcher(text);

    if (matcher.find()) {
      return matcher.group(1);
    }

    return null;
  }

  /**
   * Creates a styled component for translated text
   * Maintains consistency across all translation displays
   *
   * @param translatedText The translated text
   * @param originalComponent The original component for color reference
   * @return Styled component with translated text
   */
  public static Component createTranslatedComponent(String translatedText, Component originalComponent) {
    TextColor color = extractColor(originalComponent);

    if (color != null) {
      return Component.text(translatedText, color);
    }

    // Default to green if no color found
    return Component.text(translatedText, TextColor.color(0x55FF55));
  }
}