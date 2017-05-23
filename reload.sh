nmcli nm wifi off
rfkill unblock wlan
/etc/init.d/networking restart
ifconfig wlan0 192.168.1.1/24 up
sleep 1
service isc-dhcp-server restart
service hostapd restart


