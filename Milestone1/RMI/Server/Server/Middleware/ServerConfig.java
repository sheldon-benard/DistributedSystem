package Server.Middleware;

public class ServerConfig {
    public String host;
    public int port;
    public String name;

    public ServerConfig(String name,String host,String port){
        this.name = name;
        this.host = host;
        this.port = Integer.parseInt(port);
    }

}