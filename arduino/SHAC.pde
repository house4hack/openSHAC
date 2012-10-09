/*
SHAC
Written by Philip Booysen <philipbooysen@gmail.com>
Tue Sep  6 05:14:48 SAST 2011
Smart House Access Control
Copyright 2011 Philip Booysen <philipbooysen@gmail.com>

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
#include <SdFatUtil.h>
#include <TinyWebServer.h>

// Two initializers for our RollingCode (PRNG) (not null)
// This is our initial seeding values, they should differ each time we reset/resync
// Philip propose we use H4H initiation date {+,-} days up to today
// Eg: 29 days after H4H initiation date being 20110813 :  m_w = 20110715 ; m_z=20110911 ;
unsigned long m_w = 20110715;
unsigned long m_z = 20110911; 
int Debug_Verbose = 1;
int Debug_Millis = 1;
long start;
long time_taken;

boolean index_handler(TinyWebServer& web_server);

static uint8_t mac[] = { 0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xED };

// Don't forget to modify the IP to an available one on your home network
byte ip[] = { 192, 168, 1, 80 };


TinyWebServer::PathHandler handlers[] = {
  {"/" "*", TinyWebServer::GET, &index_handler },
  {NULL},
};

boolean index_handler(TinyWebServer& web_server) {
  web_server.send_error_code(200);
  web_server.end_headers();
  String path =  web_server.get_path();
  
  if (path.substring(0,6) == "/gate?") {
    Serial.println("Someone is trying to open the gate");
    String Submitted_Rolling_Code_String = path.replace("/gate?", "");
    Serial.println("Rolling code submitted is" + Submitted_Rolling_Code_String);
    // Call that damn stringToUnsignedLong I had to design
    unsigned long Submitted_Rolling_Code = stringToUnsignedLong(Submitted_Rolling_Code_String);    
    if (valid_rolling_code(Submitted_Rolling_Code)) {
          web_server << F("<html><body><h1>Welcome the gate is open for 10 seconds</h1></body></html>\n");
          digitalWrite(9,HIGH);
          delay(100);
    }
    else web_server << F("<html><body><h1>Invalid rolling code for gate ... Go away cracker!</h1></body></html>\n");
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

void setup()
{
  Serial.begin(9600);
  pinMode(9, OUTPUT);

  pinMode(10, OUTPUT); // set the SS pin as an output (necessary!)
  digitalWrite(10, HIGH); // but turn off the W5100 chip!
  pinMode(4, OUTPUT); // set the SS pin as an output (necessary!)
  digitalWrite(4, HIGH); // but turn off the SD card functionality!

  Serial << F("Free RAM: ") << FreeRam() << "\n";

  Serial << F("Setting up the Ethernet card...\n");
  Ethernet.begin(mac, ip);

  // Start the web server.
  Serial << F("Web server starting...\n");
  web.begin();

  Serial << F("Ready to accept HTTP requests.\n\n");
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
    //Serial.println("Next generated rolling code: " + (String)next_rolling_code);
    if(submitted_rolling_code == next_rolling_code){
      // new seed values auto-resynced as m_w and m_z
      // We return true, code matched and gate may be opened
      //Serial.println("The rolling code matched!");
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

void loop(){
  web.process();
  digitalWrite(9,LOW);
}
