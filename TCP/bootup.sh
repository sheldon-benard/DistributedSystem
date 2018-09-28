osascript -e 'tell application "Terminal" to do script "cd ~/Desktop/Github/DistributedSystem/TCP/Server; make clean; make; ./run_middleware.sh localhost,6667 localhost,6668 localhost,6669"'

sleep 5

osascript -e 'tell application "Terminal" to do script "cd ~/Desktop/Github/DistributedSystem/TCP/Server; ./run_server.sh Flights,6667"'
osascript -e 'tell application "Terminal" to do script "cd ~/Desktop/Github/DistributedSystem/TCP/Server; ./run_server.sh Cars,6668"'
osascript -e 'tell application "Terminal" to do script "cd ~/Desktop/Github/DistributedSystem/TCP/Server; ./run_server.sh Rooms,6669"'

osascript -e 'tell application "Terminal" to do script "cd ~/Desktop/Github/DistributedSystem/TCP/Client; make clean; make; ./run_client.sh"'