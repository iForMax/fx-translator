package me.firas.core.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.firas.core.FXTranslatorAddon;
import me.firas.core.TranslatorConfiguration.TranslatorEngine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling translation requests
 * Implements caching and async processing to optimize performance
 * Guideline-compliant: Uses caching to avoid performance impact
 */
public class TranslationService {

  private final FXTranslatorAddon addon;
  private final ExecutorService executorService;
  private final Map<String, CachedTranslation> translationCache;

  // Cache expiration time (30 minutes)
  private static final long CACHE_EXPIRATION_MS = 30 * 60 * 1000;

  // Connection timeout settings
  private static final int CONNECT_TIMEOUT_MS = 5000;
  private static final int READ_TIMEOUT_MS = 10000;

  // DeepL API endpoints
  private static final String DEEPL_FREE_API = "https://api-free.deepl.com/v2/translate";
  private static final String DEEPL_PRO_API = "https://api.deepl.com/v2/translate";

  public TranslationService(FXTranslatorAddon addon) {
    this.addon = addon;
    // Fixed thread pool size to prevent resource exhaustion (guideline #1)
    this.executorService = Executors.newFixedThreadPool(3);
    this.translationCache = new ConcurrentHashMap<>();
  }

  /**
   * Translates text asynchronously using the selected translation engine
   *
   * @param text The text to translate
   * @param sourceLang Source language code
   * @param targetLang Target language code
   * @return CompletableFuture with translated text
   */
  public CompletableFuture<String> translate(String text, String sourceLang, String targetLang) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        // Validate input
        if (text == null || text.trim().isEmpty()) {
          throw new IllegalArgumentException("Text to translate cannot be empty");
        }

        // Check cache first (guideline #1 - performance optimization)
        String cacheKey = buildCacheKey(sourceLang, targetLang, text);
        if (this.addon.configuration().enableCache().get()) {
          CachedTranslation cached = this.translationCache.get(cacheKey);
          if (cached != null && !cached.isExpired()) {
            return cached.getTranslation();
          }
        }

        // Select translation engine
        TranslatorEngine engine = this.addon.configuration().translatorEngine().get();
        String translatedText = switch (engine) {
          case GOOGLE -> translateWithGoogle(text, sourceLang, targetLang);
          case DEEPL -> translateWithDeepL(text, sourceLang, targetLang);
          case AZURE -> translateWithAzure(text, sourceLang, targetLang);
          case LIBRETRANSLATE -> translateWithLibreTranslate(text, sourceLang, targetLang);
          default -> throw new RuntimeException("Unknown translation engine: " + engine);
        };

        // Cache the result if enabled
        if (this.addon.configuration().enableCache().get()) {
          this.translationCache.put(cacheKey, new CachedTranslation(translatedText));
        }

        return translatedText;
      } catch (Exception e) {
        throw new RuntimeException("Translation error: " + e.getMessage(), e);
      }
    }, this.executorService);
  }

  /**
   * Builds a cache key for the translation
   * Guideline-compliant: Caches objects instead of creating new ones every tick
   */
  private String buildCacheKey(String sourceLang, String targetLang, String text) {
    return sourceLang + ":" + targetLang + ":" + text.hashCode();
  }

  /**
   * Google Translate (Unofficial API)
   * Uses free Google Translate API endpoint
   *
   * @param text Text to translate
   * @param sourceLang Source language code
   * @param targetLang Target language code
   * @return Translated text
   * @throws Exception If translation fails
   */
  private String translateWithGoogle(String text, String sourceLang, String targetLang) throws Exception {
    String source = sourceLang.equals("auto") ? "auto" : sourceLang;
    String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
    String urlString = "https://translate.googleapis.com/translate_a/single?client=gtx&sl="
        + source + "&tl=" + targetLang + "&dt=t&q=" + encodedText;

    URL url = new URL(urlString);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.setRequestProperty("User-Agent", "Mozilla/5.0");
    connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
    connection.setReadTimeout(READ_TIMEOUT_MS);

    int responseCode = connection.getResponseCode();
    if (responseCode != HttpURLConnection.HTTP_OK) {
      throw new RuntimeException("Google Translate API error: HTTP " + responseCode);
    }

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

      StringBuilder response = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        response.append(line);
      }

      // Parse Google Translate response
      JsonArray jsonResponse = JsonParser.parseString(response.toString()).getAsJsonArray();
      JsonArray translations = jsonResponse.get(0).getAsJsonArray();

      StringBuilder translatedText = new StringBuilder();
      for (int i = 0; i < translations.size(); i++) {
        JsonArray translation = translations.get(i).getAsJsonArray();
        translatedText.append(translation.get(0).getAsString());
      }

      return translatedText.toString();
    }
  }

  /**
   * DeepL Translate (Official API)
   * Uses DeepL API with authentication key
   *
   * @param text Text to translate
   * @param sourceLang Source language code
   * @param targetLang Target language code
   * @return Translated text
   * @throws Exception If translation fails
   */
  private String translateWithDeepL(String text, String sourceLang, String targetLang) throws Exception {
    // Get API key from configuration
    String apiKey = this.addon.configuration().deeplApiKey().get();
    if (apiKey == null || apiKey.trim().isEmpty()) {
      throw new RuntimeException("DeepL API key is not configured. Please add your API key in settings.");
    }

    // Determine API endpoint (free or pro)
    boolean useFreeApi = this.addon.configuration().deeplUseFreeApi().get();
    String apiEndpoint = useFreeApi ? DEEPL_FREE_API : DEEPL_PRO_API;

    // Convert language codes to DeepL format
    String deeplSourceLang = convertToDeeplLangCode(sourceLang);
    String deeplTargetLang = convertToDeeplLangCode(targetLang);

    // Build request body
    JsonObject requestBody = new JsonObject();
    JsonArray textArray = new JsonArray();
    textArray.add(text);
    requestBody.add("text", textArray);
    requestBody.addProperty("target_lang", deeplTargetLang.toUpperCase());

    // Only add source_lang if not auto-detect
    if (!deeplSourceLang.equals("auto")) {
      requestBody.addProperty("source_lang", deeplSourceLang.toUpperCase());
    }

    // Make API request
    URL url = new URL(apiEndpoint);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Authorization", "DeepL-Auth-Key " + apiKey);
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setDoOutput(true);
    connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
    connection.setReadTimeout(READ_TIMEOUT_MS);

    // Send request
    try (OutputStream os = connection.getOutputStream()) {
      byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }

    int responseCode = connection.getResponseCode();
    if (responseCode != HttpURLConnection.HTTP_OK) {
      // Read error message
      try (BufferedReader errorReader = new BufferedReader(
          new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
        StringBuilder errorResponse = new StringBuilder();
        String line;
        while ((line = errorReader.readLine()) != null) {
          errorResponse.append(line);
        }
        throw new RuntimeException("DeepL API error (HTTP " + responseCode + "): " + errorResponse.toString());
      }
    }

    // Read response
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

      StringBuilder response = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        response.append(line);
      }

      // Parse DeepL response
      JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
      JsonArray translations = jsonResponse.getAsJsonArray("translations");

      if (translations.size() == 0) {
        throw new RuntimeException("DeepL API returned empty translation");
      }

      return translations.get(0).getAsJsonObject().get("text").getAsString();
    }
  }

  /**
   * Azure Cognitive Services Translator (Official API)
   * Uses Microsoft Azure Translator API with authentication key
   *
   * @param text Text to translate
   * @param sourceLang Source language code
   * @param targetLang Target language code
   * @return Translated text
   * @throws Exception If translation fails
   */
  private String translateWithAzure(String text, String sourceLang, String targetLang) throws Exception {
    // Get API credentials from configuration
    String apiKey = this.addon.configuration().azureApiKey().get();
    if (apiKey == null || apiKey.trim().isEmpty()) {
      throw new RuntimeException("Azure API key is not configured. Please add your API key in settings.");
    }

    String region = this.addon.configuration().azureRegion().get();
    String endpoint = this.addon.configuration().azureEndpoint().get();

    // Build API URL
    String apiVersion = "3.0";
    StringBuilder urlBuilder = new StringBuilder(endpoint);
    urlBuilder.append("/translate?api-version=").append(apiVersion);
    urlBuilder.append("&to=").append(targetLang);

    // Add source language if not auto-detect
    if (!sourceLang.equals("auto")) {
      urlBuilder.append("&from=").append(sourceLang);
    }

    // Build request body
    JsonArray requestBody = new JsonArray();
    JsonObject textObject = new JsonObject();
    textObject.addProperty("Text", text);
    requestBody.add(textObject);

    // Make API request
    URL url = new URL(urlBuilder.toString());
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Ocp-Apim-Subscription-Key", apiKey);
    connection.setRequestProperty("Ocp-Apim-Subscription-Region", region);
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setRequestProperty("X-ClientTraceId", UUID.randomUUID().toString());
    connection.setDoOutput(true);
    connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
    connection.setReadTimeout(READ_TIMEOUT_MS);

    // Send request
    try (OutputStream os = connection.getOutputStream()) {
      byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }

    int responseCode = connection.getResponseCode();
    if (responseCode != HttpURLConnection.HTTP_OK) {
      // Read error message
      try (BufferedReader errorReader = new BufferedReader(
          new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
        StringBuilder errorResponse = new StringBuilder();
        String line;
        while ((line = errorReader.readLine()) != null) {
          errorResponse.append(line);
        }
        throw new RuntimeException("Azure Translator API error (HTTP " + responseCode + "): " + errorResponse.toString());
      }
    }

    // Read response
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

      StringBuilder response = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        response.append(line);
      }

      // Parse Azure response
      JsonArray jsonResponse = JsonParser.parseString(response.toString()).getAsJsonArray();

      if (jsonResponse.size() == 0) {
        throw new RuntimeException("Azure Translator API returned empty translation");
      }

      JsonObject firstResult = jsonResponse.get(0).getAsJsonObject();
      JsonArray translations = firstResult.getAsJsonArray("translations");

      if (translations.size() == 0) {
        throw new RuntimeException("Azure Translator API returned no translations");
      }

      return translations.get(0).getAsJsonObject().get("text").getAsString();
    }
  }

  /**
   * LibreTranslate (Open Source API)
   * Uses public LibreTranslate instance at https://libretranslate.com
   *
   * @param text Text to translate
   * @param sourceLang Source language code
   * @param targetLang Target language code
   * @return Translated text
   * @throws Exception If translation fails
   */
  private String translateWithLibreTranslate(String text, String sourceLang, String targetLang) throws Exception {
    String apiKey = this.addon.configuration().libreTranslateApiKey().get();
    if (apiKey == null || apiKey.trim().isEmpty()) {
      throw new RuntimeException("LibreTranslate API key is not configured. Please add your API key in settings.");
    }
    // Build request body
    JsonObject requestBody = new JsonObject();
    requestBody.addProperty("q", text);
    requestBody.addProperty("source", sourceLang.equals("auto") ? "auto" : sourceLang);
    requestBody.addProperty("target", targetLang);
    requestBody.addProperty("format", "text");
    requestBody.addProperty("alternatives", 3);

    requestBody.addProperty("api_key", apiKey);

    // Make API request to public LibreTranslate instance
    URL url = new URL("https://libretranslate.com/translate");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setDoOutput(true);
    connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
    connection.setReadTimeout(READ_TIMEOUT_MS);

    // Send request
    try (OutputStream os = connection.getOutputStream()) {
      byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }

    int responseCode = connection.getResponseCode();
    if (responseCode != HttpURLConnection.HTTP_OK) {
      // Read error message
      try (BufferedReader errorReader = new BufferedReader(
          new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
        StringBuilder errorResponse = new StringBuilder();
        String line;
        while ((line = errorReader.readLine()) != null) {
          errorResponse.append(line);
        }
        throw new RuntimeException("LibreTranslate API error (HTTP " + responseCode + "): " + errorResponse.toString());
      }
    }

    // Read response
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

      StringBuilder response = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        response.append(line);
      }

      // Parse LibreTranslate response
      JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();

      if (!jsonResponse.has("translatedText")) {
        throw new RuntimeException("LibreTranslate API returned invalid response");
      }

      return jsonResponse.get("translatedText").getAsString();
    }
  }

  /**
   * Converts language codes to DeepL format
   * DeepL uses uppercase 2-letter codes (EN, DE, FR, etc.)
   */
  private String convertToDeeplLangCode(String langCode) {
    // Handle special cases
    if (langCode.equals("auto")) {
      return "auto";
    }

    // DeepL uses specific codes for some languages
    switch (langCode.toLowerCase()) {
      case "en":
        return "EN";
      case "zh":
        return "ZH";
      case "pt":
        return "PT";
      default:
        return langCode.toUpperCase();
    }
  }

  /**
   * Clears the translation cache
   * Can be called manually or on configuration changes
   */

  public void clearCache() {
    this.translationCache.clear();
  }

  /**
   * Removes expired entries from cache
   * Should be called periodically to prevent memory buildup
   */
  public void cleanExpiredCache() {
    this.translationCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
  }

  /**
   * Shuts down the executor service gracefully
   * Should be called when addon is disabled
   */
  public void shutdown() {
    this.executorService.shutdown();
    try {
      if (!this.executorService.awaitTermination(5, TimeUnit.SECONDS)) {
        this.executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      this.executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Inner class to store cached translations with expiration
   * Guideline-compliant: Prevents memory leaks by using time-based cache
   */
  private static class CachedTranslation {
    private final String translation;
    private final long timestamp;

    public CachedTranslation(String translation) {
      this.translation = translation;
      this.timestamp = System.currentTimeMillis();
    }

    public String getTranslation() {
      return this.translation;
    }

    public boolean isExpired() {
      return (System.currentTimeMillis() - this.timestamp) > CACHE_EXPIRATION_MS;
    }
  }
}