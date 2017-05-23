cd /root/
./stopMonitor.sh
echo $(date) >> log.txt
echo "stop monitor" >> log.txt
sleep 5
ifconfig wlan6 down
iwconfig wlan6 mode managed
ifconfig wlan6 up
sleep 5
wpa_supplicant -B -i wlan6 -D nl80211 -c /etc/wpa_supplicant/wpa_supplicant.conf
dhclient wlan6
service ntp stop
ntpdate us.pool.ntp.org
echo "time synchronise at $(date)" >> log.txt
sleep 5
./startMonitor.sh &
echo "restart monitor" >> log.txt

