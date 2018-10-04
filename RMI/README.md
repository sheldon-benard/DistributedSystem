## Milestone 1

To run the Resource Managers (RMI registry port: 1099)

```
cd Server/
make
./run_server.sh Flights
./run_server.sh Cars
./run_server.sh Rooms
```

To run the Middleware

```
cd Server/
make
./run_middleware.sh Flights,<<ip-flights>>,1099 Cars,<<ip-cars>>,1099 Rooms,<<ip-rooms>>,1099
```

Run client

```
cd Client/
make
./run_client.sh <<ip-middleware>>
```

Run test

```
cd Client/
make
./run_client_test.sh <<ip-middleware>>
```

To get IP addresses:

```
hostname -I
```