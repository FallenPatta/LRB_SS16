/*
    This sketch sends data via HTTP GET requests to data.sparkfun.com service.

    You need to get streamId and privateKey at data.sparkfun.com and paste them
    below. Or just customize this script to talk to other HTTP servers.

*/

#include "ESP8266WiFi.h"
#include <stdlib.h>

#include <Adafruit_NeoPixel.h>
#ifdef __AVR__
  #include <avr/power.h>
#endif

//#define Reset_AVR() wdt_enable(WDTO_1S); while(1) {}

#define GAUSSMASK 7  //ganze ungerade Zahl
#define GAUSSMASK_2 GAUSSMASK/2

const String commands[] = {"SendStatus", "Ok", "TurnOn"};

const char *ssid = "FEUERTANZ-WLAN";

IPAddress localip(192, 168, 0, 2);
IPAddress gateway(192, 168, 0, 1);
IPAddress subnet(255, 255, 255, 0);
int port = 5000;

WiFiServer server(port);

boolean alreadyConnected = false;

volatile char sendQueue[256];

#define NUMLED  64

Adafruit_NeoPixel strip = Adafruit_NeoPixel(NUMLED, D3, NEO_GRB + NEO_KHZ800);

boolean convergeLED(int num, double toR, double toG, double toB, double thres = 1, double velo = 1);

double gauss[GAUSSMASK];

//double r, g, b;
double r[NUMLED];
double g[NUMLED];
double b[NUMLED];
double rVal[NUMLED];
double gVal[NUMLED];
double bVal[NUMLED];

const uint8_t gammatable[] = {
     0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
     0,   0,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   2,   2,   2,
     2,   2,   2,   2,   3,   3,   3,   3,   3,   3,   4,   4,   4,   4,   5,   5,
     5,   5,   6,   6,   6,   6,   7,   7,   7,   8,   8,   8,   9,   9,   9,  10,
    10,  10,  11,  11,  11,  12,  12,  13,  13,  13,  14,  14,  15,  15,  16,  16,
    17,  17,  18,  18,  19,  19,  20,  20,  21,  21,  22,  23,  23,  24,  24,  25,
    26,  26,  27,  28,  28,  29,  30,  30,  31,  32,  32,  33,  34,  35,  35,  36,
    37,  38,  38,  39,  40,  41,  42,  43,  43,  44,  45,  46,  47,  48,  49,  50,
    50,  51,  52,  53,  54,  55,  56,  57,  58,  59,  60,  61,  62,  63,  64,  65,
    67,  68,  69,  70,  71,  72,  73,  74,  76,  77,  78,  79,  80,  82,  83,  84,
    85,  87,  88,  89,  90,  92,  93,  94,  96,  97,  98, 100, 101, 102, 104, 105,
   107, 108, 110, 111, 112, 114, 115, 117, 118, 120, 121, 123, 125, 126, 128, 129,
   131, 133, 134, 136, 137, 139, 141, 142, 144, 146, 147, 149, 151, 153, 154, 156,
   158, 160, 162, 163, 165, 167, 169, 171, 173, 175, 176, 178, 180, 182, 184, 186,
   188, 190, 192, 194, 196, 198, 200, 202, 204, 206, 208, 210, 213, 215, 217, 219,
   221, 223, 225, 228, 230, 232, 234, 237, 239, 241, 243, 246, 248, 250, 253, 255 };

/*
void colorWipe(uint32_t c) {
  for (uint16_t i = 0; i < strip.numPixels(); i++) {
    strip.setPixelColor(i, c);
  }
  strip.show();
}
*/

boolean convergeLED(int num, double toR, double toG, double toB,double thres, double velo){
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

/*
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
*/

void setPixelSollWert(int num, uint32_t c){
  rVal[num] = ((uint8_t)(c >> 16 & 255));
  gVal[num] = ((uint8_t)(c >> 8 & 255));
  bVal[num] = ((uint8_t)(c & 255));
}

void stripSetup() {
  strip.begin();
        for(int i = 0; i<NUMLED; i++){
          setPixelSollWert(i, 0);
        }
  strip.show();
}

void apSetup(){
  WiFi.mode(WIFI_AP);
  WiFi.softAPConfig(localip, gateway, subnet);
  WiFi.softAP(ssid);
  IPAddress myIP = WiFi.softAPIP();
  Serial.print("AP IP address: ");
  Serial.println(myIP);
  Serial.print("Port: ");
  Serial.println(port);
  server.begin();
}

void processRequest(String s){
  Serial.print("input: ");
  Serial.println(s);
  if(s.length() != 6) return;
  String red = s.substring(0,2);
  String green = s.substring(2,4);
  String blue = s.substring(4);
  int hexRed = strtol(red.c_str(),NULL,16);
  int hexGreen = strtol(green.c_str(),NULL,16);
  int hexBlue = strtol(blue.c_str(),NULL,16);
  uint32_t col = ((uint32_t) hexRed << 16) + ((uint32_t) hexGreen << 8) + ((uint32_t) hexBlue);
  Serial.print(hexRed, HEX);
  Serial.print(hexGreen, HEX);
  Serial.println(hexBlue, HEX);
  //for(int i = 0; i<NUMLED; i++) setPixelSollWert(i, col);
  setPixelSollWert(0, col);
}

void setup() {
  if(!Serial) Serial.begin(115200);
  for(int i = -((GAUSSMASK)/2); i<((GAUSSMASK)/2)+1; i++){
   gauss[i+GAUSSMASK_2] =  1/(sqrt(2*PI)) * pow(EULER,(-0.5 * pow(i,2)));
  }
  sendQueue[0] = '\0';
  stripSetup();
  apSetup();
}

float tmpLast = 0;
int timeoutConnection = 0;

void loop() {
  Serial.println("Started");
  int wasserTemp = 0;
  long tStart = micros();
  long tEnd = micros();
  float average = 0;
  long cnt = 0;
  float integral = 0;
  long last_average = 0;
  int colChCnt = 0;
  long timeout = 2000;
  long watcher = millis();
  bool reconnect = true;

  WiFiClient client;

  long uniStart = millis();
  while(true){
    //<connect routine>
    if(reconnect){
      client = server.available();
      delay(5);
      if(client){
        reconnect = false;
        watcher = millis();
      }
    }

   if(client & (!client.available() || abs(millis()-watcher) > timeout)){
    Serial.println("reset");
    client.stop();
    reconnect = true;
    watcher = millis();
   }
   //</connect routine>

   //<arbeitsroutine>
    while (client.available()) 
    {
      watcher = millis();
      String input = "";
      char value;
      while(client.available()){
        value = (char)client.read();
        input += value;
        if(input.length() > 50){
          client.flush();
          break;
        }
      }
      client.print(".");
      while(input.indexOf('\n') > 0){
        String request = input.substring(0, input.indexOf('\n'));
        input = input.substring(input.indexOf('\n')+1);
        Serial.println(request);
        processRequest(request);
        watcher = millis();
      }
    }
   //</arbeitsroutine>
    
    if(!cnt) {
      tStart = micros();
    }
      convergeLED(0, rVal[0], gVal[0], bVal[0], 0, 5);
      for(int i =0; i<NUMLED; i++){
          strip.setPixelColor(i,strip.Color(gammatable[(uint8_t)r[0]],gammatable[(uint8_t)g[0]],gammatable[(uint8_t)b[0]]));
      }
            
      //for(int i =0; i<NUMLED; i++){
      //    convergeLED(i, rVal[i], gVal[i], bVal[i], 0, 5);
      //    strip.setPixelColor(i,strip.Color(gammatable[(uint8_t)r[i]],gammatable[(uint8_t)g[i]],gammatable[(uint8_t)b[i]]));
      //}
      strip.show();
  if(cnt){
      long error = (20000 - (long)average);
      float prop = error * 2;
      float diff = (average - last_average) * 0.2;
      integral += error*0.2;
      long k = (long)prop+(long)integral-(long)diff;
      if(k < 0) {k = 0; Serial.println("rate missed");}
      long waitTmp = micros();
      while(micros()-waitTmp < k){
        delay(0);
      }
  }
      cnt++;
      tEnd = micros();
      last_average = average;
      if(abs(tEnd - tStart) < 100000) average -= (average + (tStart-tEnd))/(float)cnt;
      if(cnt) tStart = tEnd;
  }

  Serial.println();
  Serial.println("closing");
}

