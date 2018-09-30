# Usage: ./run_client.sh [<server_hostname> [<server_rmiobject>]]

echo "java -Djava.security.policy=java.policy Client.TCPClient true $1 $2 $3"
java -Djava.security.policy=java.policy Client.TCPClient true $1 $2 $3
