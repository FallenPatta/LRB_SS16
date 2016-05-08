/*
    This sketch sends data via HTTP GET requests to data.sparkfun.com service.

    You need to get streamId and privateKey at data.sparkfun.com and paste them
    below. Or just customize this script to talk to other HTTP servers.

*/

#include "ESP8266WiFi.h"

#include <Adafruit_NeoPixel.h>
#ifdef __AVR__
  #include <avr/power.h>
#endif

#define GAUSSMASK 7  //ganze ungerade Zahl
#define GAUSSMASK_2 GAUSSMASK/2

const String commands[] = {"SendStatus", "Ok", "TurnOn"};

//const char hotwater[] = "TurnOn\n\0";

const char* ssid     = "GladOS-Net";
const char* password = "thecakeisalie";

const char* host = "192.168.0.49";

long lastSend = 0;
long keepAlive = 0;

//Zustände
int wasserTemp = 0;
bool wasserStatus = false;

volatile char sendQueue[256];

IPAddress ip(192, 168, 0, 69);
IPAddress gate(192, 168, 0, 1);
IPAddress net(255, 255, 255, 0);

#define NUMLED  24

Adafruit_NeoPixel strip = Adafruit_NeoPixel(NUMLED, D3, NEO_GRB + NEO_KHZ800);
Adafruit_NeoPixel statusLED = Adafruit_NeoPixel(1, D2, NEO_GRB + NEO_KHZ800);

boolean convergeLED(int num, double toR, double toG, double toB, int wait, boolean show, double thres = 1, double velo = 1);

double gauss[GAUSSMASK];

//double r, g, b;
double r[NUMLED];
double g[NUMLED];
double b[NUMLED];
double rVal[NUMLED];
double gVal[NUMLED];
double bVal[NUMLED];

const uint32_t ledWarm = strip.Color(120,0,0);
const uint32_t ledKalt = strip.Color(60,60,120);

uint8_t gammatable[] = {
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  1,  1,
    1,  1,  2,  2,  2,  3,  3,  3,  4,  4,  4,  4,  4,  5,  5,  5,
    5,  6,  6,  6,  6,  7,  7,  7,  7,  8,  8,  8,  9,  9,  9, 10,
   10, 10, 11, 11, 11, 12, 12, 13, 13, 13, 14, 14, 15, 15, 16, 16,
   17, 17, 18, 18, 19, 19, 20, 20, 21, 21, 22, 22, 23, 24, 24, 25,
   25, 26, 27, 27, 28, 29, 29, 30, 31, 32, 32, 33, 34, 35, 35, 36,
   37, 38, 39, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 50,
   51, 52, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 66, 67, 68,
   69, 70, 72, 73, 74, 75, 77, 78, 79, 81, 82, 83, 85, 86, 87, 89,
   90, 92, 93, 95, 96, 98, 99,101,102,104,105,107,109,110,112,114,
  115,117,119,120,122,124,126,127,129,131,133,135,137,138,140,142,
  144,146,148,150,152,154,156,158,160,162,164,167,169,171,173,175,
  177,180,182,184,186,189,191,193,196,198,200,203,205,208,210,213,
  215,218,220,223,225,228,231,233,236,239,241,244,247,249,252,255 };

void colorWipe(uint32_t c, uint8_t wait) {
  for (uint16_t i = 0; i < strip.numPixels(); i++) {
    strip.setPixelColor(i, c);
    strip.show();
    delay(wait);
  }
}

boolean convergeLED(int num, double toR, double toG, double toB, int wait, boolean show, double thres, double velo){
  if(thres < velo) thres = velo;
  int thres2 = velo+1;
  boolean diverging = false; 
  double fr=1, fg=1, fb = 1;
  if(abs(toR - r[num]) > thres2) fr *= velo;
  if(abs(toG - g[num]) > thres2) fg *= velo;
  if(abs(toB - b[num]) > thres2) fb *= velo;
  
  if(abs(toR - r[num]) > thres & toR - r[num] > 0){ r[num] += fr; diverging = true;}
  if(abs(toR - r[num]) > thres & toR - r[num] < 0){ r[num] -= fr; diverging = true;}
  
  if(abs(toG - g[num]) > thres & toG - g[num] > 0){ g[num] += fg; diverging = true;}
  if(abs(toG - g[num]) > thres & toG - g[num] < 0){ g[num] -= fg; diverging = true;}
  
  if(abs(toB - b[num]) > thres & toB - b[num] > 0){ b[num] += fb; diverging = true;}
  if(abs(toB - b[num]) > thres & toB - b[num] < 0){ b[num] -= fb; diverging = true;}
  
  return diverging;
}

void blurPattern(double * r, double * g, double * b, int numTimes = 1){
  if(numTimes<1) numTimes = 1;
  for(int i = 0; i<numTimes; i++){
     double blurR[NUMLED], blurG[NUMLED], blurB[NUMLED];
     for(int i = 0; i<NUMLED; i++){
       for(int j = 0; j<NUMLED; j++){
        blurR[j] = blurG[j] = blurB[j] = 0; 
       }
     int ringVal[GAUSSMASK];
        for(int j = -(GAUSSMASK_2); j<(GAUSSMASK_2)+1; j++){
            ringVal[j+(GAUSSMASK_2)] = i+j;
            if(ringVal[j+(GAUSSMASK_2)] < 0)ringVal[j+GAUSSMASK_2] += NUMLED;
            ringVal[j+GAUSSMASK_2] = ringVal[j+GAUSSMASK_2] % NUMLED;
        }
        for(int j = 0; j<GAUSSMASK; j++){
            blurR[i] += r[ringVal[j]] * gauss[j];
            blurG[i] += g[ringVal[j]] * gauss[j];
            blurB[i] += b[ringVal[j]] * gauss[j];
        }
        r[i] = blurR[i]; g[i] = blurG[i]; b[i] = blurB[i];
     }
  }
}

void setStatus(uint32_t c){
    statusLED.setPixelColor(0, c);
    statusLED.show();
}

//void setPixelSollWert(int num, uint8_t x, uint8_t y, uint8_t z){
//  rVal[num] = x;
//  gVal[num] = y;
//  bVal[num] = z;
//}

void setPixelSollWert(int num, uint32_t c){
  rVal[num] = ((uint8_t)(c >> 16 & 255));
  gVal[num] = ((uint8_t)(c >> 8 & 255));
  bVal[num] = ((uint8_t)(c & 255));
}

void stripSetup() {

  strip.begin();

  strip.show();
  
  statusLED.begin();
  setStatus(0);

}

void wifiScan() {
  // Set WiFi to station mode and disconnect from an AP if it was previously connected
  WiFi.mode(WIFI_STA);
  //WiFi.disconnect();
  delay(100);

  Serial.println("Setup done");
  Serial.println("scan start");

  // WiFi.scanNetworks will return the number of networks found
  int n = WiFi.scanNetworks();
  Serial.println("scan done");
  if (n == 0)
    Serial.println("no networks found");
  else
  {
    Serial.print(n);
    Serial.println(" networks found");
    for (int i = 0; i < n; ++i)
    {
      // Print SSID and RSSI for each network found
      Serial.print(i + 1);
      Serial.print(": ");
      Serial.print(WiFi.SSID(i));
      Serial.print(" (");
      Serial.print(WiFi.RSSI(i));
      Serial.print(")");
      Serial.println((WiFi.encryptionType(i) == ENC_TYPE_NONE) ? " " : "*");
      delay(10);
    }
  }
  Serial.println("");
}

void wifiConnect() {
  // We start by connecting to a WiFi network

  WiFi.mode(WIFI_STA);
  WiFi.config(ip, gate, net);
  delay(500);

  Serial.println();
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);

  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

void enqueue(const char* s){
    int nullInd = 0;
    for (int i = 0; i < sizeof(sendQueue) & ! sendQueue[i] == '\0'; i++) nullInd = i;
    if(nullInd != 0) nullInd++;
    if (nullInd + strlen(s) < sizeof(sendQueue) - 1) for (int i = 0; i < strlen(s); i++) {
        sendQueue[i + nullInd] = s[i];
        if(s[i] == '\0') Serial.print("NULL");
    }
    sendQueue[nullInd + strlen(s)] = '\0';
/*    Serial.println("enqueued");
    Serial.println("waiting to send:");
    for (int i = 0; i < sizeof(sendQueue) & !sendQueue[i] == '\0'; i++) Serial.print(sendQueue[i]);
    Serial.println();*/
}

void handlePCINT_1() {
//  cli();
  
  if(millis()-lastSend < 100) return;
  uint32_t debouncer = 1;
  long timeout = millis();
  
  while(debouncer != 0 && millis() - timeout < 500){
  delayMicroseconds(1000);
  digitalRead(D1) == HIGH ? debouncer = (debouncer<<1) : debouncer |= 1;
  Serial.println(debouncer);
  }
  lastSend = millis();
  
  if (debouncer == 0) {
    enqueue("TurnOn\n\0");
  }
  
//  sei();
}

void pinSetup() {
  pinMode(D1, INPUT);           // set pin to input
  digitalWrite(D1, LOW);       // turn on pullup resistors
  attachInterrupt(digitalPinToInterrupt(D1), handlePCINT_1, RISING);
}

void setup() {
  if(!Serial) Serial.begin(115200);
  for(int i = -((GAUSSMASK)/2); i<((GAUSSMASK)/2)+1; i++){
   gauss[i+GAUSSMASK_2] =  1/(sqrt(2*PI)) * pow(EULER,(-0.5 * pow(i,2)));
  }
  sendQueue[0] = '\0';
  pinSetup();
  stripSetup();
  wifiScan();
  wifiConnect();
}

uint32_t processReply(String s) {
  uint32_t color1 = 0;
  String comp = "<Wasser>";
  int num = s.indexOf("<Wasser>");
  if (num >= 0) {
    num = num + comp.length();
    if(s.indexOf("<Wasser>An") >= 0){
      Serial.println("Wasser:AN");
      wasserStatus = true;
    }
    if(s.indexOf("<Wasser>Aus") >= 0){
      Serial.println("Wasser:AUS");
      wasserStatus = false;
    }
  }
  comp = "<Temperatur>";
  num = s.indexOf("<Temperatur>");
  if(num >= 0){
    num = num + comp.length();
    int tEnd = s.indexOf("</Temperatur>");
    String temperatur = s.substring(num, tEnd);
    Serial.print("Temperatur: ");
    wasserTemp = temperatur.toInt();
    Serial.println(wasserTemp);
  }
  else {
  }

  comp = "<Messages>";
  int stat = s.indexOf("<Messages>");
  int mess = s.indexOf("</Messages>");
  if (mess >= 0 && stat >= 0) {
    stat = stat + comp.length();
    String msgNum = s.substring(stat, mess);
    int numMsgs = msgNum.toInt();
    Serial.print("Message Index: ");
    Serial.println(numMsgs);
  }

  return color1;
}

bool isAvailable(String s){
  int ind = s.indexOf("\n");
  String comp = s.substring(0,ind);
  for(int i = 0; i<sizeof(commands); i++){
    if(comp.equals(commands[i])) return true;
  }
  return false;
}

float tmpLast = 0;
bool wasserLast = true;

void loop() {
  delay(500);

  Serial.print("connecting to ");
  Serial.println(host);

  WiFiClient client;

  // Use WiFiClient class to create TCP connections
  setStatus(statusLED.Color(30,30,30));
  const int httpPort = 50007;
  if (!client.connect(host, httpPort)) {
    Serial.println("connection failed");
    if(WiFi.status() != WL_CONNECTED){
      setup();
      return;
    }
    return;
  }

  String req = "SendStatus\n";

  //Connecting################################

  client.print("connected\n");
  unsigned long timeout = millis();
  while (client.available() == 0) {
    if (millis() - timeout > 1000) {
      Serial.println(">>> Client Timeout !");
      client.stop();
      break;
    }
  }
  // Read all the lines of the reply from server and print them to Serial
  while (client.available()) {
    String line = client.readStringUntil('\n');
    Serial.println(line);
  }

  //##########################################

  // This will send the request to the server
  while (client.connected()) {
    while (client.available() == 0) {
      
      if (sendQueue[0] != '\0') {
        cli();
        Serial.println("SENDING:");
        for(int i = 0; i<sizeof(sendQueue) & sendQueue[i]!='\0'; i++) Serial.print(sendQueue[i]);
        String request = "";
        for (int i = 0; i < sizeof(sendQueue); i++) {
          if (sendQueue[i] != '\0') request += sendQueue[i];
          if (sendQueue[i] == '\n') {
            sendQueue[0] = '\0';
            if(isAvailable(request)) client.print(request);
            setStatus(statusLED.Color(0,0,30));
            request = "";
          }
        }
        sei();
      }
      
      if (millis() - timeout > 10000) {
        Serial.println(">>> Client Timeout !");
        client.stop();
        break;
      }
      
      if(millis()-keepAlive >= 500 && client.connected()){
        cli();
        delayMicroseconds(10000);
        enqueue("SendStatus\n\0");
        delayMicroseconds(10000);
        sei();
        keepAlive = millis();
      }
      
      setStatus(statusLED.Color(0,30,0));
      float tmp = (float)wasserTemp;
      tmp /= 255.0f;
      tmp *= NUMLED;
      if((int)tmpLast != (int)tmp | wasserLast != wasserStatus){
        for(int i = 0; i<NUMLED; i++){
            if(i<(int)tmp){ setPixelSollWert(i, ledWarm); }
            else if(!wasserStatus & i>=(int)tmp) { setPixelSollWert(i, ledKalt); }
            else { setPixelSollWert(i, ledKalt); }
        }
        blurPattern(rVal, gVal, bVal, 1);
        tmpLast = tmp;
      }
      for(int i =0; i<NUMLED; i++){
          convergeLED(i, rVal[i], gVal[i], bVal[i], 0, false, 2, 1);
          strip.setPixelColor(i,strip.Color(gammatable[(uint8_t)r[i]],gammatable[(uint8_t)g[i]],gammatable[(uint8_t)b[i]]));
      }
      strip.show();
      delay(20);
      
      if(!client.connected()) break;
    }
    // Lesen und verarbeiten
    while (client.available()) {
      setStatus(statusLED.Color(30,0,0));
      timeout = millis();
      //Serial.println("receiving Response");
      String line = client.readStringUntil('\n');
      //Serial.println(line);
      Serial.println("Message: \n---------->");
      processReply(line);
      Serial.println("<----------\n");
    }
    //    lastStatus = thisStatus;
  }

  Serial.println();
  Serial.println("closing connection");
}

