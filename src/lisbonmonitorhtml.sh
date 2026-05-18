#!/bin/sh
accessKey=eyJhbGciOiJIUzI1NiJ9.eyJ4cC51Ijo0MiwieHAucCI6MywieHAubSI6MTcwMTAwMjYwNzIxMiwiZXhwIjoyMDE2MzYyNjA3LCJpc3MiOiJjb20uZXhwZXJpdGVzdCJ9.SMGYMR4IFha22QTlFFHg9UdyayG4kx4VcRHg7PjsYBY
dateval=$(date)
deviceArray=()

echo "start-here";
echo "<html><body><table border=1>"

echo "<tr><td> Lisbon cloud monitoring report : </td><td> $dateval </td></tr>"

porto=$(/usr/local/bin/sshpass -p Ab123456 ssh -o StrictHostKeyChecking=no auto@192.168.2.80 'df -h | grep disk1s1')
IFS=' ' read -ra deviceSplit <<< "$porto"
portofs=${deviceSplit[3]}
echo "<tr><td> Porto free space </td><td> $portofs </td></tr>"


sintra=$(/usr/local/bin/sshpass -p Ab123456 ssh -o StrictHostKeyChecking=no auto@192.168.4.22 'df -h | grep disk1s5')
IFS=' ' read -ra deviceSplit <<< "$sintra"
sintrafs=${deviceSplit[3]}
echo "<tr><td> Sintra free space </td><td> $sintrafs </td></tr>"

elvas=$(/usr/local/bin/sshpass -p Ab123456 ssh -o StrictHostKeyChecking=no auto@192.168.2.150 'df -h | grep disk3s5')
IFS=' ' read -ra deviceSplit <<< "$elvas"
elvasfs=${deviceSplit[3]}
echo "<tr><td> Elvas free space </td><td> $elvasfs </td></tr>"

lisbon=$(/usr/local/bin/sshpass -p Ab123456 ssh -o StrictHostKeyChecking=no auto@192.168.6.105 'df -h | grep lv')
IFS=' ' read -ra deviceSplit <<< "$lisbon"
lisbonfs=${deviceSplit[3]}
echo "<tr><td> Lisbon free space </td><td> $lisbonfs </td></tr>"

reporter=$(/usr/local/bin/sshpass -p Ab123456 ssh -o StrictHostKeyChecking=no auto@192.168.6.106 'df -h | grep lv')
IFS=' ' read -ra deviceSplit <<< "$reporter"
reporterfs=${deviceSplit[3]}
echo "<tr><td> Reporter free space </td><td> $reporterfs </td></tr>"

uploads=$(/usr/local/bin/sshpass -p Ab123456 ssh -o StrictHostKeyChecking=no auto@192.168.6.106 'du -sh /var/lib/Experitest/reporter/uploads/')
IFS='/' read -ra deviceSplit <<< "$uploads"
uploadsSize=${deviceSplit[0]}
echo "<tr><td> Reporter uploads folder size </td><td> $uploadsSize </td></tr>"

tcagent=$(/usr/local/bin/sshpass -p Ab123456 ssh -o StrictHostKeyChecking=no auto@192.168.4.60 'df -h | grep disk1s1')
IFS=' ' read -ra deviceSplit <<< "$tcagent"
tcagentfs=${deviceSplit[3]}
echo "<tr><td> Teamcity agent free space </td><td>$tcagentfs</td></tr>"

nvserverArray=$(curl -s GET 'https://lisbon.experitest.com/api/v2/nv-servers' -H "Authorization: Bearer $accessKey" | jq -rj '.[]|"\(.name): \(.status);"')
IFS=';' eval 'array=($nvserverArray)'
echo "<tr><td>NV server 1 </td><td> ${array[0]} </td></tr>"
echo "<tr><td>NV server 2 </td><td> ${array[1]} </td></tr>"

agentsArray=$(curl -s GET 'https://lisbon.experitest.com/api/v2/agents' -H "Authorization: Bearer $accessKey" | jq -rj '.[]|"\(.name): \(.statusForDisplay): \(.nvServerName);"')
IFS=';' eval 'array=($agentsArray)'
echo "<tr><td>Agent 1 </td><td> ${array[0]} </td></tr>"
echo "<tr><td>Agent 2 </td><td> ${array[1]} </td></tr>"
echo "<tr><td>Agent 3 </td><td> ${array[2]} </td></tr>"
echo "<tr><td>Agent 4 </td><td> ${array[3]} </td></tr>"
echo "<tr><td>Agent 5 </td><td> ${array[4]} </td></tr>"


regionArray=$(curl -s GET 'https://lisbon.experitest.com/api/v2/regions' -H "Authorization: Bearer $accessKey" | jq -rj '.[]|"\(.name): \(.status);"')
IFS=';' eval 'array=($regionArray)'
echo

echo "<tr><td>Region 1 </td><td> ${array[0]} </td></tr>"
echo "<tr><td>Region 2 </td><td> ${array[1]} </td></tr>"

servicesArray=$(curl -s GET 'https://lisbon.experitest.com/api/v2/region-services' -H "Authorization: Bearer $accessKey" | jq -rj '.[]|"\(.hostOrIp) \(.type): \(.status);"')
IFS=';' eval 'array=($servicesArray)'
echo "<tr><td>Service 1 </td><td> ${array[0]} </td></tr>"
echo "<tr><td>Service 2 </td><td> ${array[1]} </td></tr>"
echo "<tr><td>Service 3 </td><td> ${array[2]} </td></tr>"
echo "<tr><td>Service 4 </td><td> ${array[3]} </td></tr>"
echo "<tr><td>Service 5 </td><td> ${array[4]} </td></tr>"
echo "<tr><td>Service 6 </td><td> ${array[5]} </td></tr>"
echo "<tr><td>Service 7 </td><td> ${array[6]} </td></tr>"
echo "</table></body></html>";
echo "<p>       </p>"

deviceArray=$(curl -s GET 'https://lisbon.experitest.com/api/v1/devices' -H "Authorization: Bearer $accessKey" | jq -rj '.data[]|"\(.id)%\(.deviceName)%\(.region)%\(.bluetooth)%\(.deviceOs)%\(.osVersion)%\(.agentName);"')
IFS=';' eval 'array=($deviceArray)'
ai=1
i=1

echo "<html><body><table border=1>"

for element in ${array[@]}; do
  max=0
  extraChars=""
  IFS='%' read -ra deviceSplit <<< "$element"
  deviceID=${deviceSplit[0]}
  deviceName=${deviceSplit[1]}
  deviceRegion=${deviceSplit[2]}
  btStatus=${deviceSplit[3]}
  deviceOS=${deviceSplit[4]}
  OSVersion=${deviceSplit[5]}
  agentName=${deviceSplit[6]}
  max="$((17-${#deviceName}))"
  deviceName=$(echo $deviceName | tr '[:lower:]' '[:upper:]')

if [ ! -z "$deviceRegion" -a "$deviceRegion" != " " ]; then
  deviceIP=$(curl -s GET https://lisbon.experitest.com/api/v1/devices/$deviceID/WifiIPAddress -H "Authorization: Bearer $accessKey" | jq -rj '(.v4)," ",(.ssid)')
  IFS=' ' read -ra deviceIPSplit <<< "$deviceIP"
  deviceIPAddr=${deviceIPSplit[0]}
  deviceWiFiName=${deviceIPSplit[1]}
if [ $deviceOS == Android ]; then
  curlOP=$(curl -s -L -X GET https://lisbon.experitest.com/api/v1/devices/$deviceID/cacerts -H "Authorization: Bearer $accessKey")
  if [[ $curlOP == *"mitmproxy"* ]]; then
  mitmCertsAvailable="MITM"
  else
  mitmCertsAvailable="   "
  fi
  echo "<tr><td> ${ai}"." </td><td> $deviceName </td><td> ${deviceID} </td><td> ${agentName:0:5} </td><td> ${OSVersion} </td><td> ${mitmCertsAvailable} </td><td> ${deviceIPAddr} </td><td> ${deviceWiFiName} </td></tr>"
  ai=$((ai+1))
fi
fi
done
echo "</table></body></html>";
echo "<p>       </p>"
echo "<html><body><table border=1>"
for element in ${array[@]}; do
  IFS='%' read -ra deviceSplit <<< "$element"
  deviceID=${deviceSplit[0]}
  deviceName=${deviceSplit[1]}
  deviceRegion=${deviceSplit[2]}
  btStatus=${deviceSplit[3]}
  deviceOS=${deviceSplit[4]}
  OSVersion=${deviceSplit[5]}
  agentName=${deviceSplit[6]}
if [ ! -z "$deviceRegion" -a "$deviceRegion" != " " ]; then
  deviceIP=$(curl -s GET https://lisbon.experitest.com/api/v1/devices/$deviceID/WifiIPAddress -H "Authorization: Bearer $accessKey" | jq -rj '(.v4)," ",(.ssid)')
  IFS=' ' read -ra deviceIPSplit <<< "$deviceIP"
if [ $deviceOS == iOS ]; then
  echo "<tr><td> ${i}"."   </td><td> ${deviceName}        </td><td> ${deviceID} </td><td> ${agentName:0:5} </td><td> ${OSVersion} </td><td> ${deviceIP} </td><td> ${btStatus//null/} </td></tr>"
  i=$((i+1))
 fi
 fi
done

echo "</table></body></html>";
echo "end-here\n";