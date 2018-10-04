## Milestone 1

To run the Resource Managers (Flights port:6667, Cars port:6668, Rooms port:6669)

```
cd Server/
make
./run_server.sh Flights,6667
./run_server.sh Cars,6668
./run_server.sh Rooms,6669
```

To run the Middleware (server socket on port 6666)

```
cd Server/
make
./run_middleware.sh <<ip-flight>>,6667 <<ip-car>>,6668 <<ip-room>>,6669
```

Run client

```
cd Client/
make
./run_client.sh <<ip-middleware>> 6666
```

Run test

```
cd Client/
make
./run_client_test.sh <<ip-middleware>> 6666
```

To get IP addresses:

```
hostname -I
```