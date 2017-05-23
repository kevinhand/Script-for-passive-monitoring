#while true
#do

ifconfig wlx6c7220cbb867 down
iwconfig wlx6c7220cbb867 mode monitor
ifconfig wlx6c7220cbb867 up
iwconfig wlx6c7220cbb867 channel 1
 
cd /home/honghande/Research/scriptForMonitor/
#java Client piloc.d1.comp.nus.edu.sg 8088 wlan7 &
#java Client 172.26.191.50 8080 wlan6 & 
java DataCollection piloc.d1.comp.nus.edu.sg 8081 wlx6c7220cbb867 &
#while true
#do 
#iwconfig wlan2 essid hande
#ifconfig wlan2 192.168.1.2/24
#sleep 5
#done
#iwconfig wlan1 channel 11
#tcpdump -tttt -i wlan1 -e -s 256 type mgt subtype probe-req > ap1.txt
#tcpdump -tttt -i wlan2 -e -s 256 > ap1.txt
#done

