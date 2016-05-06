# -*- coding: cp1252 -*-

#!/usr/bin/env python
from copy import deepcopy
from time import *
import datetime
import os
import rrdtool
import RPi.GPIO as GPIO
import socket
import thread
import threading

#globale Variablen
numMsgs = 0             #Anzahl gesendeter Nachrichten seit Server-Start (fuer Statistik/Debugging)
status = False          #Status des Duschwassers (Momentat binaer: True: warm, False: kalt)
anforderung = False     #Ist warmes Wasser gefordert: "True" wenn nicht: "False"

#Momentan ist der Timer eine Statemachine mit sehr beschränkten Optionen
#Wenn wir das System ein wenig besser kennen und wissen wie sich Wassertemperatur/Zeit verhaelt
#koennte ich mir auch eine Art Fuzzy Logik um den Zusammenhang herum vorstellen, die dann in den
#Grenzbereichen eine ungefaehre Aussage zu Wassertemperatur und zu bevorzugender Duschdauer machen kann.
#Dafuer ist die Datenlage jetzt aber noch zu duenn.
def warmwasser_Timer():
        global status
        global anforderung
        lastRequest = datetime.datetime.strptime("14 Nov 93", "%d %b %y")   #Zeitpunkt relativ weit in der Vergangenheit, sodass im Status "AUS" gestartet wird - funktioniert nur die naechsten 90 Jahre
        tx = 60                                 #Zeit bis Wasser warm ist (in Sekunden)
        ty = 900                                #Zeit bis Pumpe wieder ausgeschaltet wird (in Sek.)
        tz = 1200                               #Zeit bis Wasser wieder kalt ist (in Sek.)
        while True:
                time.sleep(10)
                if anforderung == True:
                        #TODO: Heizung fuer Warmwasser anschalten
                        print "EINSCHALTEN"
                        anforderung = False
                        lastRequest = datetime.datetime.now()
                        
                tDiff = datetime.datetime.now() - lastRequest
                
                if tDiff.seconds >= tx and status == False and tDiff.seconds < tz:
                        #Koennte statt durch einen Boolean-Wert auch zusaetzlich durch einen Float-Wert repraesentiert werden
                        #Dadurch koennte man die Guete des vorhandenen Duschwassers angeben (noch nicht warm, ausreichend warm, warm, durchgeheizt)
                        #Wird aber momentan auch von der App noch nicht unterstuetzt
                        status = True
                        
                if tDiff.seconds >= ty and status = True:
                        print "AUSSCHALTEN"
                        #TODO: Warmwasser "AUS" schalten wenn es "AN" ist
                        
                if tDiff.seconds >= tz:
                        #Hier koennte man z.B. eine Temperaturpruefung einbauen,
                        #die feststellt wann das Wasser wirklich zu kalt zum duschen ist
                        status = False
                

#Berechnet Antwort
#Momentates Protokoll
# Abfrage fuer binaere Wassertemperatur: "SendStatus\n"
# Anfrage fuer Warmwasser:               "TurnOn\n"
# Bestaetigung fuer Synchronitaet:       "OK\n"          -Wie "ACK" nur etwas klobiger, stellt sicher, dass Client bemerkt wenn Server waehrend der Verarbeitung der vorherigen Anfrage abgeschmiert ist
# Connect Signal:                        "connecting\n"  -Wird waehrend des Connects genutzt. Soll spaeter z.B. mit Stockwerksnummer, oder User-ID kombiniert werden.

# TODO: Wassertemperatur muss besser spezifiziert werden und sollte nicht direkt durch "TurnOn\n" gesetzt werden
# Dazu fehlt mir aber der Einblick in das restliche Projekt und die Moeglichkeit die zu timen wann wo warmes Wasser ankommt
def calculate_Response(data, numMs):
        global status
        global anforderung
        global numMsgs
        print "calc With: %s" % data
        res = "OK"
        if data == "OK\n":
                res = "OK" 
        if data == "SendStatus\n":
                print "init"
                res = "Status "
                res = res + "%d " % numMs
                res = res + "Messages "
                if status == True:
                        res = res + "Wasser:An"         # - Warm
                else:
                        res = res + "Wasser:Aus"        # - Kalt
        if data == "TurnOn\n":
                #Mommentan noch nicht wirklich funktional (siehe Oben)
                #status = not status #Einkommentieren um Konnektivitaet testen zu koennen
                anforderung = True
                res = "OK"
        if data == "connecting\n":
                res = "connected"

        res = res + "\n"
        print "response: %s" % res
        return res
                
                
#Setzt einen Server auf die hostIP der auf den Port 50007 hoert
#hoert jeweils nur einem Client gleichzeitig zu, laesst sich aber mit relativ wenig Aufwand erweitern
def listener_socket(hostIP):
        print "setting up"
        HOST = hostIP
        PORT = 50007
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.bind((HOST, PORT))
        numMsgs = 0
        print "done"
        while 1:
                s.listen(1)
                print "connecting"
                conn, addr = s.accept()
                print "accepted"
                print addr
		conn.sendall("connected\n")
		numMsgs = numMsgs +1
		print "sent"
                while 1:
                        data = conn.recv(1024)
                        print "got: %s" % data
                        if not data: break
                        response = calculate_Response(data, numMsgs)
                        conn.sendall(response)
                        numMsgs = numMsgs +1
                        print numMsgs
                conn.close()

#Watchdog Thread
#Kontrolliert alle 5 Sekunden ob der Server online ist
#Die hostIP muss hier noch manuell als args={'IP'} gesetzt werden
#aktiviert ausserdem den Service-Thread der die Heizung auf Kommando steuert
def Thread_checkup():
        print "Starting"
        status = False
        t = threading.Thread(target=listener_socket, args={'172.16.28.49'})
        t.start()
        t2 = threading.Thread(target=warmwasser_Timer, args=())
        t2.start()
        while 1:
                if not t.isAlive():
                        t = threading.Thread(target=listener_socket, args={'172.16.28.49'})
                        t.start()
                if not t2.isAlive():
                        t2 = threading.Thread(target=warmwasser_Timer, args=())
                        t2.start()
                while t.isAlive() and t2.isAlive():
                        time.sleep(5)
                print("offline\nreconnecting...")

#Anlegen und Starten des Watchdogs
checkupThread = threading.Thread(target=Thread_checkup())
checkupThread.start()



def Datei_schreiben(Datei,Dat,Uhr,Zustand,Start):
    Datei=Datei+".dat"
    fobj_out = open("/home/pi/Daten/"+Datei,"a")
    fobj_out.write(Dat + "\t " + Uhr +"\t " +str(Zustand)+"\t"+Start+ "\n")
    fobj_out.close()
    return

def Pumpenzustand(Datei,Dat,Uhr,Pumpen):
    fobj_out = open("/home/pi/Daten/"+Datei,"w")
    fobj_out.write(Dat + "\t " + Uhr +"\tBrenner\t" +str(Pumpen[0])+"\n")
    fobj_out.write(Dat + "\t " + Uhr +"\tHeizung\t" +str(Pumpen[1])+"\n")
    fobj_out.write(Dat + "\t " + Uhr +"\tBrauchwasser\t" +str(Pumpen[2])+"\n")
    fobj_out.write(Dat + "\t " + Uhr +"\tLade\t" +str(Pumpen[3])+"\n")

    fobj_out.close()
    return

def Dauer(Datei,Ein,Aus,Dat,Uhr):
    fobj_out = open("/home/pi/Daten/"+Datei,"a")
    Dauer1=Aus-Ein
    fobj_out.write(Dat+"\t" + Uhr + "\t%u" % Dauer1+"\n")
    fobj_out.close()
    return




def Grafik_in_var_www():
    rrdtool.graph("/var/www/Brenner.png",
                 # '--width', '1024',
                 # '--height', '768',
                  "--start",
                  "-1d",
                  '--vertical-label', 'Brenner an',
                  '--title', 'Brenner',
                  "DEF:Brenner=/home/pi/Kosten.rrd:Brenner:AVERAGE",
                  "LINE2:Brenner#FF0000:Brenner an",
                  "VDEF:Brennerlast=Brenner,LAST")
 
    rrdtool.graph("/var/www/Brauchwasserpumpe.png",
                 # '--width', '1024',
                 # '--height', '768',
                  "--start",
                  "-1d",
                  '--vertical-label', 'Brauchwassserpumpe an',
                  '--title', 'Brauchwasserpumpe',
                  "DEF:Brauchwasser=/home/pi/Kosten.rrd:Brauchwasser:AVERAGE",
                  "LINE2:Brauchwasser#FF0000:Brauchwasserpumpe an",
                  "VDEF:Brauchwasserlast=Brauchwasser,LAST")


    rrdtool.graph("/var/www/Ladepumpe.png",
                 # '--width', '1024',
                 # '--height', '768',
                  "--start",
                  "-1d",
                  '--vertical-label', 'Ladepumpe an',
                  '--title', 'Ladepumpe',
                  "DEF:Lade=/home/pi/Kosten.rrd:Lade:AVERAGE",
                  "LINE2:Lade#FF0000:Ladepumpe an",
                  "VDEF:Ladelast=Lade,LAST")

    rrdtool.graph("/var/www/Umwaelzpumpe.png",
                 # '--width', '1024',
                 # '--height', '768',
                  "--start",
                  "-1d",
                  '--vertical-label', 'Umwaelzpumpe an',
                  '--title', 'Umwaelzpumpe',
                  "DEF:Heizung=/home/pi/Kosten.rrd:Heizung:AVERAGE",
                  "LINE2:Heizung#FF0000:Umwaelzpumpe an",
                  "VDEF:Heizunglast=Heizung,LAST")


#Layoutverwenden (wie Pin-Nummer)
GPIO.setmode(GPIO.BOARD)
GPIO.setup(29, GPIO.OUT)
GPIO.setup(31, GPIO.OUT)
GPIO.setup(33, GPIO.OUT)
GPIO.setup(35, GPIO.OUT)
GPIO.setup(12, GPIO.IN,GPIO.PUD_OFF)
GPIO.setup(13, GPIO.IN,GPIO.PUD_OFF)
GPIO.setup(18, GPIO.IN,GPIO.PUD_OFF)
GPIO.setup(16, GPIO.IN,GPIO.PUD_OFF)

#alles aus
GPIO.output(29, GPIO.LOW)
GPIO.output(31, GPIO.LOW)
GPIO.output(33, GPIO.LOW)
GPIO.output(35, GPIO.LOW)



# Variablen deklaration
Brenner= GPIO.input(13)
Brauch=GPIO.input(16)
Lade=GPIO.input(18)
Heizung=GPIO.input(12)

Brenner_an=time()
Brenner_aus=time()
Heiz_an=time()
Heiz_aus=time()
Brauch_an=time()
Brauch_aus=time()
Lade_an=time()
Lade_aus=time()	

o=0
a=0
Pumpen_neu=[Brenner, Brauch, Lade, Heizung ]
Pumpen_alt =[1,0,0,0]
Datum = strftime("%d.%m.%Y")
Uhrzeit = strftime("%H:%M:%S")

Pumpenzustand("Pumpen.dat",Datum,Uhrzeit,Pumpen_neu)

Datei_schreiben("Brenner",Datum,Uhrzeit,Brenner,"S")
Datei_schreiben("Brauchwasser",Datum,Uhrzeit,Brauch,"S")
Datei_schreiben("Ladepumpe",Datum,Uhrzeit,Lade,"S")
Datei_schreiben("Heizungspumpe",Datum,Uhrzeit,Heizung,"S")


# Dauerschleife

# Liste Pumpe_alt und Pumpe_neu nur bei einer Aenderung wird die Pumpendatei neu geschrieben.
while 1:
 
 sleep(0.01)
 os.system('clear')
 if Pumpen_neu==Pumpen_alt:
    print ("Listen sind gleich \n")
 else:  
    print ("Listen sind ungleich \n")
    Datum = strftime("%d.%m.%Y")
    Uhrzeit = strftime("%H:%M:%S")       
    Pumpenzustand("Pumpen.dat",Datum,Uhrzeit,Pumpen_neu)
    Pumpen_alt = deepcopy(Pumpen_neu)
 #print Pumpen_neu
 #print ("\n")
 #print Pumpen_alt

 #####################################
 #Steuerung Anzeige LED's
 if GPIO.input(13) == GPIO.HIGH:
    # Brenner an
    GPIO.output(29, GPIO.HIGH)
    print("Brenner an")
    Pumpen_neu[0]=1
    
 else:
    # Brenner aus
    GPIO.output(29, GPIO.LOW)
    print("Brenner aus")
    Pumpen_neu[0]=0
           
 if GPIO.input(12) == GPIO.HIGH:
    # Heizungsumwaelzung  an
    GPIO.output(31, GPIO.HIGH)
    print("Heizungumwaelzung an")
    Pumpen_neu[1]=1
    
 else:
    # Heizungumwaelzung aus
    GPIO.output(31, GPIO.LOW)
    print("Heizungumwaelzung aus")
    Pumpen_neu[1]=0
    

 if GPIO.input(16) == GPIO.HIGH:
    # Brauchwasser an
    GPIO.output(33, GPIO.HIGH)
    print("Brauchwasserumwaelzung an")
    Pumpen_neu[2]=1
    
 else:
    # Brauchwasser aus
    GPIO.output(33, GPIO.LOW)
    print("Brauchwasserumwaelzung aus")
    Pumpen_neu[2]=0
    

 if GPIO.input(18) == GPIO.HIGH:
    # Ladepumpe an
    GPIO.output(35, GPIO.HIGH)
    print("Ladepumpe an")
    Pumpen_neu[3]=1
    
 else:
    # Ladepumpe aus
    GPIO.output(35, GPIO.LOW)
    print("Ladepumpe aus")
    Pumpen_neu[3]=0
    

# Aufzeichnen wann der Zustand wechselt
 if Brenner != GPIO.input(13):
    Brenner =  GPIO.input(13)
    Datum = strftime("%d.%m.%Y")
    Uhrzeit = strftime("%H:%M:%S")
    Datei_schreiben("Brenner",Datum,Uhrzeit,Brenner,"N")
    if Brenner==GPIO.HIGH:
       Brenner_an=time()
    if Brenner==GPIO.LOW:
       Brenner_aus=time()
       Dauer("Brenner-an.dat",Brenner_an, Brenner_aus,Datum, Uhrzeit)

 if Brauch != GPIO.input(16):
    Brauch =  GPIO.input(16)
    Datum = strftime("%d.%m.%Y")
    Uhrzeit = strftime("%H:%M:%S")
    Datei_schreiben("Brauchwasser",Datum,Uhrzeit,Brauch,"N")
    if Brauch==GPIO.HIGH:
       Brauch_an=time()
    if Brauch==GPIO.LOW:
       Brauch_aus=time()  
       Dauer("Brauchwasser-an.dat",Brauch_an, Brauch_aus,Datum, Uhrzeit)
    
 if Lade != GPIO.input(18):
    Lade =  GPIO.input(18)
    Datum = strftime("%d.%m.%Y")
    Uhrzeit = strftime("%H:%M:%S")
    Datei_schreiben("Ladepumpe",Datum,Uhrzeit,Lade,"N")
    if Lade==GPIO.HIGH:
       Lade_an=time()
    if Lade==GPIO.LOW:
       Lade_aus=time()
       Dauer("Ladepumpe-an.dat",Lade_an, Lade_aus,Datum, Uhrzeit)

 
 if Heizung != GPIO.input(12):
    Heizung =  GPIO.input(12)
    Datum = strftime("%d.%m.%Y")
    Uhrzeit = strftime("%H:%M:%S")
    Datei_schreiben("Heizungspumpe",Datum,Uhrzeit,Heizung,"N")
    if Heizung==GPIO.HIGH:
       Heizung_an=time()
    if Heizung==GPIO.LOW:
       Heizung_aus=time()
       Dauer("Heizpumpe-an.dat",Heiz_an, Heiz_aus,Datum, Uhrzeit)
 
 o=o+1
 print (o)
 if o>100:
     o=0
     Daten_rrd="N"
     Daten_rrd+=":"+str(Brenner)
     Daten_rrd+=":"+str(Brauch)
     Daten_rrd+=":"+str(Lade)
     Daten_rrd+=":"+str(Heizung)
     rrdtool.update(  "%s/Kosten.rrd" % (os.path.dirname(os.path.abspath(__file__))),  Daten_rrd)
     print "rrd werte geschrieben."
     #sleep(0.2)

 a=a+1
 print (a)
 if a>1000:
     a=0
     Grafik_in_var_www()


