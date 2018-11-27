all: java.policy compile-middleware

java.policy: FORCE
	@echo "Creating server java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

RMIInterface.jar: Server/Interface/IResourceManager.java
	@echo "Compiling RMI server interface"
	javac Server/Interface/*.java
	jar cvf RMIInterface.jar Server/Interface/*.class

compile-json: Server/JSON/*.java Server/JSON/parser/*.java
	javac Server/JSON/parser/ParseException.java Server/JSON/parser/JSONParser.java
	javac Server/JSON/*.java
	javac Server/JSON/parser/*.java

compile-middleware: Server/JSON/*.java Server/JSON/parser/*.java Server/LockManager/*.java Server/RMI/*.java Server/Interface/IResourceManager.java Server/Common/*.java Server/Middleware/*.java
	javac Server/JSON/parser/ParseException.java Server/LockManager/*.java Server/JSON/parser/JSONParser.java Server/JSON/*.java Server/JSON/parser/*.java Server/Transactions/*.java Server/RMI/*.java Server/Interface/IResourceManager.java Server/Common/*.java Server/Middleware/*.java

clean:
	rm -f Server/Interface/*.class Server/Common/*.class Server/RMI/*.class Server/Middleware/*.class Server/LockManager/*.class Server/Transactions/*.class
	rm -f Server/JSON/*.class Server/JSON/parser/*.class
	rm -f RMIInterface.jar
	rm -f java.policy