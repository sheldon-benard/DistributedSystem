#Usage: ./run_server.sh [<rmi_name>]

java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ Server.TCP.TCPResourceManager $1
