/*
SHAC
Written by Philip Booysen <philipbooysen@gmail.com>
Tue Sep  6 05:14:48 SAST 2011
Smart House Access Control
Copyright 2011 Philip Booysen <philipbooysen@gmail.com>
Last update : Thu Oct 31 23:09:32 SAST 2013
 - PB made the code Arduino 1.x compatible
 - Accomodate Arduino 1.0 String class reimplementation and in place replacement
 - Dependancies for SHAC :
   - https://github.com/Hardcore-fs/TinyWebServer/archive/master.zip
     -> Extract above zip file to your sketchbook/libraries/TinyWebServer/
   - http://arduiniana.org/Flash/Flash4.zip
     -> See http://arduiniana.org/libraries/flash/
     -> Extract above Flash4.zip file to your sketchbook/libraries/Flash/

The Webserver sub-implementation
Copyright 2010 Ovidiu Predescu <ovidiu@gmail.com>
Date: June 2010

Further weblock code implementations
Written by Schalk Heunis <schalk.heunis@gmail.com>
Sat Aug 13 17:30:02 SAST 2011
*/

#include <SPI.h>
#include <Ethernet.h>
#include <Flash.h>
#include <SD.h>
#include <TinyWebServer.h>

// Two initializers for our RollingCode (PRNG) (not null)
// This is our initial seeding values, they should differ each time we reset/resync
// Philip propose we use H4H initiation date {+,-} days up to today
// Eg: 29 days after H4H initiation date being 20110813 :  m_w = 20110715 ; m_z=20110911 ;
unsigned long m_w = 20110715;
unsigned long m_z = 20110911; 
int SubmittedLed1 = 0;
int SubmittedLed2 = 0;
int SubmittedLed3 = 0;
int SubmittedLed4 = 0;
int SubmittedLed5 = 0;
int SHACLEDbuffer[6];
int SHACLed1 = 0;
int SHACLed2 = 1;
int SHACLed3 = 0;
int SHACLed4 = 1;
int SHACLed5 = 0;
long tstart_led_seq;
long telapsed_led_seq;

boolean index_handler(TinyWebServer& web_server);

static uint8_t mac[] = { 0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xED };

// Don't forget to modify the IP to an available one on your home network
byte ip[] = { 192, 168, 1, 79 };


TinyWebServer::PathHandler handlers[] = {
  {"/" "*", TinyWebServer::GET, &index_handler },
  {NULL},
};

boolean index_handler(TinyWebServer& web_server) {
  web_server.send_error_code(200);
  web_server.end_headers();
  String path =  web_server.get_path();
  String path1 = path;
  Serial.println("path is: " + path);
  Serial.println("path1 is: " + path1);
  Serial.println("path.substring(0,6) should be /gate? and is: " + path.substring(0,6));
  Serial.println("I repeat, path.substring(0,6) should be /gate? and is: " + path.substring(0,6));
  Serial.println("path1.substring(0,8) should be /resync? and is: " + path1.substring(0,8));
  if (path.substring(0,6) == "/gate?") {
    Serial.println("Someone is trying to open the gate");
    String Submitted_Code_String = path; // PB: To accomodate Arduino 1.0 String class reimplementation and in place replacement
    Submitted_Code_String.replace("/gate?", "");    

    int urlsublen = Submitted_Code_String.length(); //capture string length
    String LED_Sequence_String = Submitted_Code_String.substring(urlsublen-5,urlsublen);
    String Rolling_Code_String = Submitted_Code_String; // PB: To accomodate Arduino 1.0 String class reimplementation and in place replacement
    Rolling_Code_String.replace("&"+LED_Sequence_String, "");
    SubmittedLed1 = stringToInt(LED_Sequence_String.substring(0,1));
    SubmittedLed2 = stringToInt(LED_Sequence_String.substring(1,2));
    SubmittedLed3 = stringToInt(LED_Sequence_String.substring(2,3));
    SubmittedLed4 = stringToInt(LED_Sequence_String.substring(3,4));
    SubmittedLed5 = stringToInt(LED_Sequence_String.substring(4,5));
    Serial.println("Submitted LED1: " + (String)SubmittedLed1);
    Serial.println("Submitted LED2: " + (String)SubmittedLed2);
    Serial.println("Submitted LED3: " + (String)SubmittedLed3);
    Serial.println("Submitted LED4: " + (String)SubmittedLed4);
    Serial.println("Submitted LED5: " + (String)SubmittedLed5);

    SHACLed1 = SHACLEDbuffer[2];
    SHACLed2 = SHACLEDbuffer[3];
    SHACLed3 = SHACLEDbuffer[4];
    SHACLed4 = SHACLEDbuffer[5];
    SHACLed5 = SHACLEDbuffer[6];
    Serial.println("SHAC LED1: " + (String)SHACLed1);
    Serial.println("SHAC LED2: " + (String)SHACLed2);
    Serial.println("SHAC LED3: " + (String)SHACLed3);
    Serial.println("SHAC LED4: " + (String)SHACLed4);
    Serial.println("SHAC LED5: " + (String)SHACLed5);
    
    Serial.println("Submitted Rolling Code: " + Rolling_Code_String);
    Serial.println("Submitted LED Sequence: " + LED_Sequence_String + " ->" + LED_Sequence_String.substring(0,1));
    // Call that damn stringToUnsignedLong I had to design
    unsigned long Rolling_Code = stringToUnsignedLong(Rolling_Code_String);
    if (valid_rolling_code(Rolling_Code) && valid_led_sequence(SubmittedLed1,SubmittedLed2,SubmittedLed3,SubmittedLed4,SubmittedLed5)) {
          web_server << F("<html><body><h1>Welcome the gate is open for 10 seconds</h1></body></html>\n");
          digitalWrite(9,HIGH);
          delay(100);
    }
    else web_server << F("<html><body><h1>Invalid rolling code for gate ... Go away cracker!</h1></body></html>\n");
  }
  else if (path1.substring(0,8) == "/resync?") {
    Serial.println("Someone is trying to resync the arduino rolling codes");
    String Submitted_Seed_String = path1; // PB: To accomodate Arduino 1.0 String class reimplementation and in place replacement
    Submitted_Seed_String.replace("/resync?", "");

    int urlsublena = Submitted_Seed_String.length(); //capture string length
    String LED_Sequence_String = Submitted_Seed_String.substring(urlsublena-5,urlsublena);
    String Rolling_Seed_String = Submitted_Seed_String; // PB: To accomodate Arduino 1.0 String class reimplementation and in place replacement
    Submitted_Seed_String.replace("&"+LED_Sequence_String, "");

    SubmittedLed1 = stringToInt(LED_Sequence_String.substring(0,1));
    SubmittedLed2 = stringToInt(LED_Sequence_String.substring(1,2));
    SubmittedLed3 = stringToInt(LED_Sequence_String.substring(2,3));
    SubmittedLed4 = stringToInt(LED_Sequence_String.substring(3,4));
    SubmittedLed5 = stringToInt(LED_Sequence_String.substring(4,5));
    Serial.println("Submitted LED1: " + (String)SubmittedLed1);
    Serial.println("Submitted LED2: " + (String)SubmittedLed2);
    Serial.println("Submitted LED3: " + (String)SubmittedLed3);
    Serial.println("Submitted LED4: " + (String)SubmittedLed4);
    Serial.println("Submitted LED5: " + (String)SubmittedLed5);

    SHACLed1 = SHACLEDbuffer[2];
    SHACLed2 = SHACLEDbuffer[3];
    SHACLed3 = SHACLEDbuffer[4];
    SHACLed4 = SHACLEDbuffer[5];
    SHACLed5 = SHACLEDbuffer[6];
    Serial.println("SHAC LED1: " + (String)SHACLed1);
    Serial.println("SHAC LED2: " + (String)SHACLed2);
    Serial.println("SHAC LED3: " + (String)SHACLed3);
    Serial.println("SHAC LED4: " + (String)SHACLed4);
    Serial.println("SHAC LED5: " + (String)SHACLed5);
    
    Serial.println("Submitted Rolling Code Seed: " + Rolling_Seed_String);
    Serial.println("Submitted LED Sequence: " + LED_Sequence_String + " ->" + LED_Sequence_String.substring(0,1));
    // Call that damn stringToUnsignedLong I had to design
    unsigned long Rolling_Code = stringToUnsignedLong(Rolling_Seed_String);    
    // Generate the counter Rolling Code Seed supplied by this Arduino
    long arduino_rolling_code_seed = random(0, 2147483647);
    
    if (1==1 | valid_led_sequence(SubmittedLed1,SubmittedLed2,SubmittedLed3,SubmittedLed4,SubmittedLed5)) {
          //String response = "<html><body>Resync Success:" + String(arduino_rolling_code_seed) + "</body></html>\n";
          //web_server << response;
          web_server << "<html><body>Resync Success:" << String(arduino_rolling_code_seed) << "</body></html>\n";
    }
    else web_server << F("<html><body><h1>Invalid LED sequence and/or button not pressed ... Go away cracker!</h1></body></html>\n");
  }
  else web_server << F("<html><body><h1>Invalid url/device ... Go away cracker!</h1></body></html>\n");
  return true;
}

boolean has_ip_address = false;
TinyWebServer web = TinyWebServer(handlers, NULL);

const char* ip_to_str(const uint8_t* ipAddr)
{
  static char buf[16];
  sprintf(buf, "%d.%d.%d.%d\0", ipAddr[0], ipAddr[1], ipAddr[2], ipAddr[3]);
  return buf;
}

unsigned long stringToUnsignedLong(String s)
{
    char arr[12];
    s.toCharArray(arr, sizeof(arr));
    return (unsigned long)atol(arr);
} 

int stringToInt(String s)
{
    char arr[12];
    s.toCharArray(arr, sizeof(arr));
    return (int)atoi(arr);
} 

void setup()
{
  Serial.begin(9600);
  pinMode(9, OUTPUT);

//  pinMode(10, OUTPUT); // set the SS pin as an output (necessary!)
//  digitalWrite(10, HIGH); // but turn off the W5100 chip!
//  pinMode(4, OUTPUT); // set the SS pin as an output (necessary!)
//  digitalWrite(4, HIGH); // but turn off the SD card functionality!

// Initial LED seqeuence
    for (int thisPin = 2; thisPin < 7; thisPin++) {
      long rand = random(0, 2);
      Serial.println("LED" + (String)thisPin + " get to be " + rand);
      SHACLEDbuffer[thisPin] = rand;
      if ( rand == 1 ) {
        // turn the pin on:
        digitalWrite(thisPin, HIGH);  
      }
      else {      
        digitalWrite(thisPin, LOW);  
      }
    }

  Serial << F("Free RAM: ") << FreeRam() << "\n";

  Serial << F("Setting up the Ethernet card...\n");
  Ethernet.begin(mac, ip);

  // Start the web server.
  Serial << F("Web server starting...\n");
  web.begin();

  Serial << F("Ready to accept HTTP requests.\n\n");
  generate_led_sequence();
}

// PRNG Multiply-with-carry method invented by George Marsaglia.
unsigned long getRollingCode()
{
    m_z = 36969L * (m_z & 65535L) + (m_z >> 16);
    m_w = 18000L * (m_w & 65535L) + (m_w >> 16);
    return (m_z << 16) + m_w;  /* 32-bit result */
}

int valid_rolling_code(unsigned long submitted_rolling_code)
{
  unsigned long initial_m_w = m_w;
  unsigned long initial_m_z = m_z;
  unsigned long next_rolling_code;
  Serial.println("Submitted rolling code: " + (String)submitted_rolling_code);
  for (int x=0; x<256; x++) {
    next_rolling_code=(getRollingCode());
    Serial.println("Next generated rolling code: " + (String)next_rolling_code);
    if(submitted_rolling_code == next_rolling_code){
      // new seed values auto-resynced as m_w and m_z
      // We return true, code matched and gate may be opened
      Serial.println("The rolling code matched!");
      return 1;
    }
    //Serial.println("No match on rolling code yet...");
  }
  //Serial.println("No match on rolling code after 256 iterations, go away cracker!");
  // reset seed values to initial_values, no valid rolling code matched
  m_w = initial_m_w;
  m_z = initial_m_z;
  // Return false
  return 0;
}


int valid_led_sequence(int sLed1, int sLed2, int sLed3, int sLed4, int sLed5)
{
  SHACLed1 = SHACLEDbuffer[2];
  SHACLed2 = SHACLEDbuffer[3];
  SHACLed3 = SHACLEDbuffer[4];
  SHACLed4 = SHACLEDbuffer[5];
  SHACLed5 = SHACLEDbuffer[6];
  if ( (sLed1 == SHACLed1) && (sLed2 == SHACLed2) && (sLed3 == SHACLed3) && (sLed4 == SHACLed4) && (sLed5 == SHACLed5) ) {
      Serial.println("The led sequence matched!");
      return 1;
  }
  else {
    Serial.println("The led sequence DID NOT match!");
    return 0;
  }  
}

void generate_led_sequence() {
  telapsed_led_seq = millis() - tstart_led_seq;
  // loop
  if (telapsed_led_seq > 30000) {
    Serial.println("Time for a new LED sequence");
    for (int thisPin = 2; thisPin < 7; thisPin++) {
      long rand = random(0, 2);
      Serial.println("LED" + (String)thisPin + " get to be " + rand);
      SHACLEDbuffer[thisPin] = rand;
      if ( rand == 1 ) {
        // turn the pin on:
        digitalWrite(thisPin, HIGH);  
      }
      else {      
        digitalWrite(thisPin, LOW);  
      }
    }
    tstart_led_seq = millis();
  }
}

void loop(){
  web.process();
  digitalWrite(9,LOW);
  generate_led_sequence();
}
