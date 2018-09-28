# Usage: ./run_client.sh [<server_hostname> [<server_rmiobject>]]

echo "$1"
echo "$2"
echo "$3"

java -Djava.security.policy=java.policy Client.TCPClient $1 $2 $3
