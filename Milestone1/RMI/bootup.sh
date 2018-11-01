osascript -e 'tell application "Terminal" to do script "cd ~/Desktop/Github/DistributedSystem/RMI/Server; make clean; make; ./run_middleware.sh Flights,localhost,1099 Cars,localhost,1099 Rooms,localhost,1099"'

sleep 5

osascript -e 'tell application "Terminal" to do script "cd ~/Desktop/Github/DistributedSystem/RMI/Server; ./run_server.sh Flights"'
osascript -e 'tell application "Terminal" to do script "cd ~/Desktop/Github/DistributedSystem/RMI/Server; ./run_server.sh Cars"'
osascript -e 'tell application "Terminal" to do script "cd ~/Desktop/Github/DistributedSystem/RMI/Server; ./run_server.sh Rooms"'

osascript -e 'tell application "Terminal" to do script "cd ~/Desktop/Github/DistributedSystem/RMI/Client; make clean; make; ./run_client.sh"'