package me.firas.core;


import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget.TextFieldSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.dropdown.DropdownWidget.DropdownSetting;
import net.labymod.api.configuration.loader.annotation.SpriteSlot;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.settings.annotation.SettingSection;
import net.labymod.api.util.MethodOrder;

@ConfigName("settings")
public class TranslatorConfiguration extends AddonConfig {

  @SwitchSetting
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);

  // Button Settings Section

  @SettingSection(value = "button")
  @TextFieldSetting(maxLength = 10)
  private final ConfigProperty<String> buttonText = new ConfigProperty<>("[T]");

  @SwitchSetting
  private final ConfigProperty<Boolean> showButtonOnOwnMessages = new ConfigProperty<>(false);

  // Translation Settings Section
  @SettingSection("translation")
  @DropdownSetting
  private final ConfigProperty<TranslatorEngine> translatorEngine = new ConfigProperty<>(TranslatorEngine.GOOGLE);

  @DropdownSetting
  private final ConfigProperty<Language> sourceLanguage = new ConfigProperty<>(Language.AUTO);

  @DropdownSetting
  private final ConfigProperty<Language> targetLanguage = new ConfigProperty<>(Language.ENGLISH);

  @SwitchSetting
  private final ConfigProperty<Boolean> enableCache = new ConfigProperty<>(true);

  // DeepL Auth Settings Section
  @SettingSection("deepl")
  @TextFieldSetting
  private final ConfigProperty<String> deeplApiKey = new ConfigProperty<>("");

  @SwitchSetting
  private final ConfigProperty<Boolean> deeplUseFreeApi = new ConfigProperty<>(true);

  // Azure Auth Settings Section
  @SettingSection("azure")
  @TextFieldSetting
  private final ConfigProperty<String> azureApiKey = new ConfigProperty<>("");

  @TextFieldSetting
  private final ConfigProperty<String> azureRegion = new ConfigProperty<>("eastus");

  @TextFieldSetting
  private final ConfigProperty<String> azureEndpoint = new ConfigProperty<>("");

  // LibreTranslate Settings Section
  @SettingSection("libretranslate")
  @TextFieldSetting
  private final ConfigProperty<String> libreTranslateApiKey = new ConfigProperty<>("");

  // Display Settings Section
  @SettingSection("display")
  @SwitchSetting
  private final ConfigProperty<Boolean> showLoadingMessage = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> showTranslatedPrefix = new ConfigProperty<>(false);

  @SwitchSetting
  private final ConfigProperty<Boolean> preserveMessageColors = new ConfigProperty<>(true);

  // Reset Settings Section
  @MethodOrder(after = "preserveMessageColors")
  @ButtonWidget.ButtonSetting
  @SpriteSlot(y = 2, size = 32)
  @SettingSection("reset")
  @SuppressWarnings("unused")
  public void resetToDefaults() {
    // Reset all settings to default values
    this.enabled.set(true);
    this.buttonText.set("[T]");
    this.showButtonOnOwnMessages.set(false);
    this.translatorEngine.set(TranslatorEngine.GOOGLE);
    this.sourceLanguage.set(Language.AUTO);
    this.targetLanguage.set(Language.ENGLISH);
    this.enableCache.set(true);
    this.deeplApiKey.set("");
    this.deeplUseFreeApi.set(true);
    this.azureApiKey.set("");
    this.azureRegion.set("eastus");
    this.azureEndpoint.set("");
    this.libreTranslateApiKey.set("");
    this.showLoadingMessage.set(true);
    this.showTranslatedPrefix.set(false);
    this.preserveMessageColors.set(true);

  }

  @Override
  public ConfigProperty<Boolean> enabled() {
    return this.enabled;
  }

  public ConfigProperty<String> buttonText() {
    return this.buttonText;
  }

  public ConfigProperty<Boolean> showButtonOnOwnMessages() {
    return this.showButtonOnOwnMessages;
  }

  public ConfigProperty<TranslatorEngine> translatorEngine() {
    return this.translatorEngine;
  }

  public ConfigProperty<Language> sourceLanguage() {
    return this.sourceLanguage;
  }

  public ConfigProperty<Language> targetLanguage() {
    return this.targetLanguage;
  }

  public ConfigProperty<Boolean> enableCache() {
    return this.enableCache;
  }

  public ConfigProperty<String> deeplApiKey() {
    return this.deeplApiKey;
  }

  public ConfigProperty<Boolean> deeplUseFreeApi() {
    return this.deeplUseFreeApi;
  }

  public ConfigProperty<String> azureApiKey() {
    return this.azureApiKey;
  }

  public ConfigProperty<String> azureRegion() {
    return this.azureRegion;
  }

  public ConfigProperty<String> azureEndpoint() {
    return this.azureEndpoint;
  }

  public ConfigProperty<String> libreTranslateApiKey() {
    return this.libreTranslateApiKey;
  }

  public ConfigProperty<Boolean> showLoadingMessage() {
    return this.showLoadingMessage;
  }

  public ConfigProperty<Boolean> showTranslatedPrefix() {
    return this.showTranslatedPrefix;
  }

  public ConfigProperty<Boolean> preserveMessageColors() {
    return this.preserveMessageColors;
  }

  public enum TranslatorEngine {
    GOOGLE,
    LIBRETRANSLATE,
    DEEPL,
    AZURE
  }
  @SuppressWarnings("unused")
  public enum Language {
    AUTO("auto"),
    ENGLISH("en"),
    SPANISH("es"),
    FRENCH("fr"),
    GERMAN("de"),
    ITALIAN("it"),
    PORTUGUESE("pt"),
    RUSSIAN("ru"),
    JAPANESE("ja"),
    CHINESE("zh"),
    KOREAN("ko"),
    ARABIC("ar"),
    DUTCH("nl"),
    POLISH("pl"),
    TURKISH("tr"),
    SWEDISH("sv"),
    HINDI("hi"),
    CZECH("cs"),
    DANISH("da"),
    FINNISH("fi"),
    GREEK("el"),
    HUNGARIAN("hu"),
    INDONESIAN("id"),
    NORWEGIAN("no"),
    ROMANIAN("ro"),
    UKRAINIAN("uk"),
    VIETNAMESE("vi");

    private final String code;

    Language(String code) {
      this.code = code;
    }

    public String getCode() {
      return this.code;
    }
  }
}