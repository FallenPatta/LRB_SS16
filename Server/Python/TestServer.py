#!/usr/bin/env pythons
import time
import datetime
import os
import socket
import thread
import threading

#globale Variablen
numMsgs = 0             #Anzahl gesendeter Nachrichten seit Server-Start (fuer Statistik/Debugging)
status = False          #Status des Duschwassers (Momentat binaer: True: warm, False: kalt)
wasserTemperatur = 0    #Verfuegbares Warmwasser (darf Werte zwischen 0 und 255 annehmen)
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
                        
                        
                if tDiff.seconds >= ty and status == True:
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
                res = res + "<Temperatur>"
                res = res + wasserTemperatur
                res = res + "</Temperatur>"
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

# mapping socket -> (host, port) on which the client is running
clients = []
print("HI")
class client_socket:
        numMsgs = 0
        def __init__(self, client_sock):
                self.client_sock = client_sock
                client_sock.sendall("connected\n")
                self.numMsgs = self.numMsgs +1
                print "sent"
                t = threading.Thread(target=listen())
                t.start()

        def listen():
                while 1:
                        data = self.client_sock.recv(1024)
                        print "got: %s" % data
                        if not data: break
                        response = calculate_Response(data, numMsgs)
                        self.client_sock.sendall(response)
                        self.numMsgs = self.numMsgs +1
                        print self.numMsgs
                conn.close()

        #Setzt einen Server auf die hostIP der auf den Port 50007 hoert
        #hoert jeweils nur einem Client gleichzeitig zu, laesst sich aber mit relativ wenig Aufwand erweitern
def listener_socket(hostIP):
        HOST = hostIP
        PORT = 50007
        print "setting up"
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.bind((HOST, PORT))
        numMsgs = 0
        print "done"
        while 1:
                s.listen(1)
                print "connecting"
                print s.getsockname()
                conn, addr = s.accept()
                print "accepted"
                conn.sendall("connected\n")
                print addr
                client = client_socket(conn)
                clients.append(client)
        s.close()


        #Watchdog Thread
        #Kontrolliert alle 5 Sekunden ob der Server online ist
        #Die hostIP muss hier noch manuell als args={'IP'} gesetzt werden
        #aktiviert ausserdem den Service-Thread der die Heizung auf Kommando steuert
def Thread_checkup():
        print "Starting"
        status = False
        t = threading.Thread(target=listener_socket, args={'192.168.0.51'})
        t.start()
        t2 = threading.Thread(target=warmwasser_Timer, args=())
        t2.start()
        while 1:
                if not t.isAlive():
                        t = threading.Thread(target=listener_socket, args={'192.168.0.51'})
                        t.start()
                if not t2.isAlive():
                        t2 = threading.Thread(target=warmwasser_Timer, args=())
                        t2.start()
                while t.isAlive() and t2.isAlive():
                        time.sleep(5)
                print("offline\nreconnecting...")

checkupThread = threading.Thread(target=Thread_checkup())
checkupThread.start
