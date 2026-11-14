#include <Arduino.h>
#include <ArduinoJson.h>
#include <ESPmDNS.h>
#include <IRrecv.h>
#include <IRsend.h>
#include <IRremoteESP8266.h>
#include <IRutils.h>
#include <WebServer.h>
#include <WiFi.h>
#include <algorithm>
#include <cstdio>
#include <vector>

#if __has_include("secrets.h")
#include "secrets.h"
#else
#define NOVAREMOTE_WIFI_SSID ""
#define NOVAREMOTE_WIFI_PASSWORD ""
#define NOVAREMOTE_API_TOKEN ""
#endif

constexpr uint16_t kIrTxPin = 4;
constexpr uint16_t kIrRxPin = 27;
constexpr uint16_t kMaxSendInputTimings = 512;
constexpr uint16_t kMaxExpandedTimings = 1536;
constexpr uint16_t kMaxCapturedTimings = 512;
constexpr uint32_t kLearningTimeoutMs = 20000;
constexpr uint32_t kWifiConnectTimeoutMs = 20000;
constexpr uint32_t kWifiRetryIntervalMs = 15000;
constexpr uint32_t kMinSendIntervalMs = 120;
constexpr uint32_t kMaxFrameDurationMicros = 2000000UL;
constexpr char kFirmwareVersion[] = "0.2.0";

IRsend irsend(kIrTxPin);
IRrecv irrecv(kIrRxPin, 1024, 50, true);
WebServer server(80);
decode_results decoded;

bool learningActive = false;
bool captureReady = false;
bool learningTimedOut = false;
bool httpStarted = false;
bool mdnsStarted = false;
bool hasSentIr = false;
uint32_t learningStartedAtMs = 0;
uint32_t lastIrSendMs = 0;
uint32_t lastWifiRetryMs = 0;
String bridgeId;
String hostname;
std::vector<uint32_t> lastCapture;
const char* kAuthHeaderNames[] = {"Authorization"};

bool constantTimeEquals(const String& left, const String& right) {
  if (left.length() != right.length()) return false;
  uint8_t difference = 0;
  for (size_t i = 0; i < left.length(); ++i) difference |= static_cast<uint8_t>(left[i] ^ right[i]);
  return difference == 0;
}

bool isAuthorized() {
  if (strlen(NOVAREMOTE_API_TOKEN) < 32) return false;
  return constantTimeEquals(server.header("Authorization"), String("Bearer ") + NOVAREMOTE_API_TOKEN);
}

void sendJson(int statusCode, JsonDocument& document) {
  String body;
  serializeJson(document, body);
  server.send(statusCode, "application/json", body);
}

void sendError(int statusCode, const char* message) {
  JsonDocument response;
  response["ok"] = false;
  response["error"] = message;
  sendJson(statusCode, response);
}

bool requireAuthorization() {
  if (isAuthorized()) return true;
  sendError(401, "Unauthorized. Check the bridge token configured in the app and firmware.");
  return false;
}

const char* currentLearningState() {
  if (learningActive) return "listening";
  if (captureReady) return "capture_ready";
  if (learningTimedOut) return "timed_out";
  return "idle";
}

void handleInfo() {
  if (!requireAuthorization()) return;
  JsonDocument response;
  response["ok"] = true;
  response["bridgeId"] = bridgeId;
  response["firmware"] = kFirmwareVersion;
  response["apiVersion"] = 1;
  response["chip"] = ESP.getChipModel();
  response["ip"] = WiFi.localIP().toString();
  response["hostname"] = hostname + ".local";
  response["irTx"] = true;
  response["irRx"] = true;
  response["learningState"] = currentLearningState();
  sendJson(200, response);
}

void handleSendRaw() {
  if (!requireAuthorization()) return;
  if (hasSentIr && millis() - lastIrSendMs < kMinSendIntervalMs) {
    sendError(429, "IR transmitter rate limit. Wait before sending another frame.");
    return;
  }
  if (!server.hasArg("plain")) {
    sendError(400, "Missing JSON body.");
    return;
  }
  const String requestBody = server.arg("plain");
  if (requestBody.length() > 16 * 1024) {
    sendError(413, "Request body is too large.");
    return;
  }
  JsonDocument request;
  if (deserializeJson(request, requestBody)) {
    sendError(400, "Invalid JSON body.");
    return;
  }

  const uint32_t frequencyHz = request["frequencyHz"] | 0U;
  JsonArrayConst timings = request["rawMicros"].as<JsonArrayConst>();
  if (frequencyHz < 30000 || frequencyHz > 60000) {
    sendError(400, "Carrier must be 30000–60000 Hz.");
    return;
  }
  if (timings.size() < 2 || timings.size() > kMaxSendInputTimings) {
    sendError(400, "rawMicros must contain 2–512 values.");
    return;
  }

  uint16_t raw[kMaxExpandedTimings];
  size_t outputIndex = 0;
  uint64_t totalDuration = 0;
  for (JsonVariantConst value : timings) {
    if (!value.is<uint32_t>()) {
      sendError(400, "Every timing must be an unsigned integer.");
      return;
    }
    uint32_t remaining = value.as<uint32_t>();
    if (remaining < 50 || remaining > 131070) {
      sendError(400, "Timing must be 50–131070 microseconds.");
      return;
    }
    totalDuration += remaining;
    if (totalDuration > kMaxFrameDurationMicros) {
      sendError(413, "Frame exceeds two seconds.");
      return;
    }
    while (remaining > UINT16_MAX) {
      if (outputIndex + 2 > kMaxExpandedTimings) {
        sendError(413, "Expanded pattern exceeds firmware limit.");
        return;
      }
      raw[outputIndex++] = UINT16_MAX;
      raw[outputIndex++] = 0;
      remaining -= UINT16_MAX;
    }
    if (outputIndex >= kMaxExpandedTimings) {
      sendError(413, "Expanded pattern exceeds firmware limit.");
      return;
    }
    raw[outputIndex++] = static_cast<uint16_t>(remaining);
  }

  irsend.sendRaw(raw, static_cast<uint16_t>(outputIndex), static_cast<uint16_t>(frequencyHz));
  hasSentIr = true;
  lastIrSendMs = millis();
  JsonDocument response;
  response["ok"] = true;
  response["sent"] = true;
  response["timingCount"] = outputIndex;
  response["durationMicros"] = totalDuration;
  sendJson(200, response);
}

void handleLearnStart() {
  if (!requireAuthorization()) return;
  if (learningActive) {
    sendError(409, "An IR learning session is already active.");
    return;
  }
  lastCapture.clear();
  captureReady = false;
  learningTimedOut = false;
  learningActive = true;
  learningStartedAtMs = millis();
  JsonDocument response;
  response["ok"] = true;
  response["learning"] = true;
  response["state"] = "listening";
  sendJson(200, response);
}

void handleLearnLatest() {
  if (!requireAuthorization()) return;
  JsonDocument response;
  response["ok"] = true;
  response["ready"] = captureReady;
  response["learning"] = learningActive;
  response["timedOut"] = learningTimedOut;
  response["state"] = currentLearningState();
  if (captureReady) {
    JsonArray values = response["rawMicros"].to<JsonArray>();
    for (const uint32_t duration : lastCapture) values.add(duration);
  }
  sendJson(200, response);
}

void handleLearnCancel() {
  if (!requireAuthorization()) return;
  learningActive = false;
  captureReady = false;
  learningTimedOut = false;
  lastCapture.clear();
  JsonDocument response;
  response["ok"] = true;
  response["cancelled"] = true;
  sendJson(200, response);
}

void handleNotFound() { sendError(404, "Unknown NovaRemote endpoint."); }

void startHttpServer() {
  server.on("/api/v1/info", HTTP_GET, handleInfo);
  server.on("/api/v1/ir/send", HTTP_POST, handleSendRaw);
  server.on("/api/v1/ir/learn/start", HTTP_POST, handleLearnStart);
  server.on("/api/v1/ir/learn/latest", HTTP_GET, handleLearnLatest);
  server.on("/api/v1/ir/learn/cancel", HTTP_POST, handleLearnCancel);
  server.onNotFound(handleNotFound);
  server.begin();
  httpStarted = true;
}

void startMdns() {
  const uint64_t mac = ESP.getEfuseMac();
  char buffer[32];
  snprintf(buffer, sizeof(buffer), "novaremote-%06X", static_cast<uint32_t>(mac & 0xFFFFFF));
  hostname = buffer;
  if (!MDNS.begin(hostname.c_str())) {
    Serial.println("mDNS startup failed; enter the IP address manually.");
    return;
  }
  MDNS.setInstanceName("NovaRemote IR Bridge");
  if (!MDNS.addService("novaremote", "tcp", 80)) {
    Serial.println("mDNS service registration failed; use manual IP entry.");
    MDNS.end();
    return;
  }
  MDNS.addServiceTxt("novaremote", "tcp", "api", "1");
  MDNS.addServiceTxt("novaremote", "tcp", "firmware", kFirmwareVersion);
  MDNS.addServiceTxt("novaremote", "tcp", "bridgeId", bridgeId.c_str());
  mdnsStarted = true;
  Serial.print("mDNS ready at http://");
  Serial.print(hostname);
  Serial.println(".local");
}

void ensureNetworkServices() {
  if (WiFi.status() != WL_CONNECTED) return;
  if (!httpStarted) startHttpServer();
  if (!mdnsStarted) startMdns();
}

void connectWifi() {
  if (strlen(NOVAREMOTE_WIFI_SSID) == 0 || strlen(NOVAREMOTE_API_TOKEN) < 32) {
    Serial.println("Configuration missing: copy secrets.example.h to secrets.h and fill local values.");
    return;
  }
  WiFi.mode(WIFI_STA);
  WiFi.setAutoReconnect(true);
  WiFi.persistent(false);
  WiFi.begin(NOVAREMOTE_WIFI_SSID, NOVAREMOTE_WIFI_PASSWORD);
  Serial.print("Connecting to configured Wi-Fi");
  const uint32_t started = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - started < kWifiConnectTimeoutMs) {
    delay(300);
    Serial.print('.');
  }
  Serial.println();
  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("NovaRemote bridge IP: ");
    Serial.println(WiFi.localIP());
    ensureNetworkServices();
  } else {
    Serial.println("Wi-Fi connection pending. Bridge will retry in the background.");
    lastWifiRetryMs = millis();
  }
}

void captureRawIfLearning() {
  if (!irrecv.decode(&decoded)) return;
  if (!learningActive) {
    irrecv.resume();
    return;
  }
  lastCapture.clear();
  const uint16_t count = std::min<uint16_t>(
      decoded.rawlen > 0 ? decoded.rawlen - 1 : 0, kMaxCapturedTimings);
  lastCapture.reserve(count);
  for (uint16_t i = 1; i <= count; ++i) {
    const uint32_t durationMicros = static_cast<uint32_t>(decoded.rawbuf[i]) * kRawTick;
    lastCapture.push_back(durationMicros);
  }
  captureReady = lastCapture.size() >= 2;
  learningActive = false;
  learningTimedOut = false;
  irrecv.resume();
  Serial.printf("IR capture complete: %u timings\n", static_cast<unsigned>(lastCapture.size()));
}

void setup() {
  Serial.begin(115200);
  delay(200);
  bridgeId = String(static_cast<uint32_t>(ESP.getEfuseMac() & 0xFFFFFFFF), HEX);
  server.collectHeaders(kAuthHeaderNames, 1);
  irsend.begin();
  irrecv.enableIRIn();
  connectWifi();
}

void loop() {
  if (WiFi.status() == WL_CONNECTED) {
    ensureNetworkServices();
    server.handleClient();
  } else {
    if (mdnsStarted) {
      MDNS.end();
      mdnsStarted = false;
    }
    if (millis() - lastWifiRetryMs >= kWifiRetryIntervalMs) {
      lastWifiRetryMs = millis();
      Serial.println("Retrying Wi-Fi connection…");
      WiFi.reconnect();
    }
  }
  if (learningActive && millis() - learningStartedAtMs >= kLearningTimeoutMs) {
    learningActive = false;
    learningTimedOut = true;
    Serial.println("IR learning timed out.");
  }
  captureRawIfLearning();
  delay(2);
}
