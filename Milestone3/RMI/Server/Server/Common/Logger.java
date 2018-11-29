package Server.Common;

import Server.JSON.*;
import Server.JSON.parser.*;
import Server.Transactions.*;
import java.io.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import Server.Interface.*;

import java.util.*;

public class Logger {
    // File names
    //private String committed_f;
    private String in_progress_f;
    private String master_f;
    private String name;

    private JSONObject committed;
    private JSONObject in_progress;
    private JSONObject master;
    private ResourceManager rm;
    private RMHashMap data;
    private TransactionManager tm;

    private int startXid = 0;

    private static Logger log;

    public Logger(String name, ResourceManager rm, RMHashMap committed_data) {
        String prefix = "Server/Logs/";
        this.name = name;
        //this.committed_f =  prefix + name + "-committed.json";
        this.in_progress_f = prefix + name + "-in-progress.json";
        this.master_f = prefix + name + "-master.json";
        this.rm = rm;
        this.data = committed_data;
        log = this;
    }

    public static Logger getLog() {
        return log;
    }

    public void unlockAll(int xid) {
        String val = String.valueOf(xid);
        JSONObject locks = (JSONObject)this.master.get("locks");
        locks.remove(val);
        this.master.put("locks",locks);
        flush_to_file(this.master, this.master_f);
    }

    public void lock(String xid, String data, String lock_string) {
        JSONObject locks = (JSONObject)this.master.get("locks");
        JSONObject lock = (JSONObject)locks.get(xid);
        if (lock == null) {
            lock = new JSONObject();
            lock.put(data,lock_string);
            locks.put(xid, lock);
        }
        else {
            String lock_type = (lock.get(data) == null) ? lock_string : ((lock.get(data).toString().equals("read")) ? lock_string : "write");
            lock.put(data, lock_type);
        }
        this.master.put("locks", locks);
        flush_to_file(this.master, this.master_f);
    }

    public Set<String> getLocks() {
        JSONObject locks = (JSONObject)this.master.get("locks");
        if (locks == null)
            return new HashSet<String>();
        Set<String> lock_set = new HashSet<String>();
        for (Object _xid : locks.keySet()) {
            String xid = _xid.toString();
            JSONObject data = (JSONObject)locks.get(xid);
            for (Object _data_key : data.keySet()) {
                String data_key = _data_key.toString();
                String a = xid + "," + data_key + "," + data.get(data_key).toString();
                lock_set.add(a);
            }
        }
        return lock_set;
    }

    public void setTM(TransactionManager tm){
        this.tm = tm;
    }

    public void setupEnv() {
        Trace.info("setupEnv called");
        Trace.info(this.master_f);
        if (!initial_setup(this.master_f)) recover_master();
        if (this.master.get("mode") != null && Integer.parseInt(this.master.get("mode").toString()) == 5){
            Trace.info("Mode=" + 5 + " abort during recovery");
            setMode(0);
            System.exit(1);
        }
        Trace.info(this.getCommitted_f());
        if (!initial_setup(this.getCommitted_f())) recover_committed();
        Trace.info(this.in_progress_f);
        if (!initial_setup(this.in_progress_f)) recover_in_progress();

        if (!this.name.equals("Middleware")) {
            Set<Integer> checked = recover_2pc();
            recover_Transactions(checked);
        }
        else {
            recover_middleware();
        }
    }

    private void recover_middleware() {
        for (Object _key : this.master.keySet()) {
            String key = _key.toString();
            if (key.equals("locks") || key.equals("mode") || key.equals("lastCommit"))
                continue;

            int xid = Integer.parseInt(key);
            if (tm.xidActive(xid)) {
                JSONObject obj = (JSONObject)this.master.get(key);
                if (obj.get("Prepared") == null) continue;
                if (obj.get("Votes") != null) {
                    //System.out.println(obj.get("Votes").toString());
                    String[] votes = obj.get("Votes").toString().split(",");
                    for (String vote : votes) {
                        if (vote.equals("")) continue;
                        boolean v = Boolean.parseBoolean(vote);
                        tm.readActiveData(xid).getVotes().add(v);
                    }
                }
//                try { rm.commit(xid); }
//                catch (Exception e) {}
            }
        }
    }

    public void removePrepared(int xid) {
        this.master.remove(String.valueOf(xid));
        flush_to_file(this.master, this.master_f);
    }

    public void lastCommitted(int xid) {
        String prevCommitted = this.master.get("lastCommit").toString();
        this.master.put("lastCommit", xid);
        flush_to_file(this.master, this.master_f);
        String filePath = this.getCommitted_f(Integer.parseInt(prevCommitted));
        File file = new File(filePath);

        if(file.delete())
        {
            System.out.println("File deleted successfully: " + filePath);
        }
        else
        {
            System.out.println("Failed to delete the file: " + filePath);
        }
    }

    private Set<Integer> recover_2pc() {
        Set<Integer> checked = new HashSet<Integer>();
        Set<Object> keySet = new HashSet<Object>();
        for (Object _key : this.master.keySet()) {
            keySet.add(_key);
        }

        for (Object _key : keySet) {
            String key = _key.toString();
            if (key.equals("locks") || key.equals("mode") || key.equals("lastCommit"))
                continue;

            // deal with prepared transactions first, since these are more time pressing
            int xid = Integer.parseInt(key);
            if (tm.xidActiveAndPrepared(xid)) {
                checked.add(xid);
                JSONObject obj = (JSONObject)this.master.get(key);
                rm.recover(xid, obj.get("Prepared"), obj.get("Decision"), obj.get("Sent"), obj.get("MWDecision"));
            }
        }


        for (Object _key : keySet) {
            String key = _key.toString();
            if (key.equals("locks") || key.equals("mode") || key.equals("lastCommit"))
                continue;

            // deal with non prepared transactions -> invalid transaction or aborted already
            int xid = Integer.parseInt(key);
            if (checked.contains(xid)) continue;
            if (!tm.xidActiveAndPrepared(xid)) {
                JSONObject obj = (JSONObject)this.master.get(key);
                if (obj.get("Prepared") == null) continue;
                checked.add(xid);
                rm.recover(xid, obj.get("Prepared"), obj.get("Decision"), obj.get("Sent"), obj.get("MWDecision"));
            }
        }
        return checked;
    }

    private void recover_Transactions(Set<Integer> checked) {
        // Check to see if transactions failed during regular operations
        for (Integer key : tm.getActiveData().keySet()) {
            if (checked.contains(key)) continue;
            Trace.info(key + " is a current transaction; it could have failed during the middleware coordination; check that it is still active");
            if (!rm.askMWActive(key)) {
                Trace.info(key + " is not active at the middleware, so abort");
                // decision to abort
                try {rm.abort(key);}
                catch(Exception e) {}
            }
            else {
                Trace.info(key + " active at MW; continue");
            }
        }
    }

    public String getCommitted_f() {
        String lastCommitted = this.master.get("lastCommit").toString();
        String f = "Server/Logs/" + this.name + "-committed_" + lastCommitted + ".json";
        return f;
    }

    public String getCommitted_f(int xid) {
        String lastCommitted = this.master.get("lastCommit").toString();
        String f = "Server/Logs/" + this.name + "-committed_" + xid + ".json";
        return f;
    }

    public void flush_committed(int xid) {
        String file_name = this.getCommitted_f(xid);

        if (this.name.equals("Middleware"))
            // Middleware holds the customer resource manager -> flush this
            flush_to_file(generateObj(this.data,true), file_name);

        else
            flush_to_file(generateObj(this.data, false), file_name);
    }

    public void flush_in_progress() {
        boolean customer = this.name.equals("Middleware");

        JSONObject transactions = new JSONObject();

        // Inactive first
        JSONObject inactive = new JSONObject();
        HashMap<Integer, Boolean> inact = tm.getInactiveData();

        for (Integer key : inact.keySet()) {
            Boolean v = inact.get(key);
            inactive.put(key,v);
        }

        transactions.put("inactive", inactive);

        JSONObject active = new JSONObject();

        HashMap<Integer, Transaction> act = tm.getActiveData();
        for (Integer key : act.keySet()) {
            JSONObject t_obj = new JSONObject();
            Transaction v = act.get(key);
            if (v == null) continue;
            int timetolive = v.getTimeToLive();
            boolean prepared = v.getIsPrepared();
            RMHashMap d = v.getData();
            String rms = String.join(",",v.getResourceManagers());

            t_obj.put("rms", rms);
            t_obj.put("timetolive", timetolive);
            t_obj.put("isprepared", prepared);
            t_obj.put("data", generateObj(d, customer));
            active.put(key, t_obj);
        }
        transactions.put("active", active);

        flush_to_file(transactions, this.in_progress_f);

    }

    private JSONObject generateObj(RMHashMap data, boolean customer) {
        JSONObject obj = new JSONObject();
        synchronized (data) {
            if (!customer) {
                for (String key : data.keySet()) {
                    JSONObject o = new JSONObject();
                    ReservableItem item = (ReservableItem)data.get(key);
                    if (item == null) continue;
                    o.put("count", item.getCount());
                    o.put("price", item.getPrice());
                    o.put("reserved", item.getReserved());
                    o.put("location", item.getLocation());
                    obj.put(key, o);
                }
            }
            else {
                for (String key : data.keySet()) {
                    JSONObject o = new JSONObject();
                    Customer item = (Customer)data.get(key);
                    if (item == null) continue;
                    o.put("id", item.getID());
                    JSONObject reservation_obj = new JSONObject();
                    RMHashMap reservations = item.getReservations();
                    for (String reservation : reservations.keySet()) {
                        JSONObject ro = new JSONObject();
                        ReservedItem r_item = (ReservedItem)reservations.get(reservation);
                        ro.put("count", r_item.getCount());
                        ro.put("key", r_item.getReservableItemKey());
                        ro.put("location", r_item.getLocation());
                        ro.put("price", r_item.getPrice());
                        reservation_obj.put(reservation, ro);
                    }
                    o.put("reservations", reservation_obj);
                    obj.put(key, o);
                }
            }
        }
        return obj;
    }


    private void recover_committed() {
        //this.committed is file object
        JSONObject obj = this.committed;

        if (obj.toString().equals("{}")) return;

        synchronized (data) {
            if (!this.name.equals("Middleware")) {
                // Thus, Flight,Car,or Room
                for (Object key_o : obj.keySet()) {
                    String key = key_o.toString();
                    if (key.contains("flight")) {
                        JSONObject o = (JSONObject)obj.get(key);
                        String location = o.get("location").toString();
                        int count = Integer.parseInt(o.get("count").toString());
                        int price = Integer.parseInt(o.get("price").toString());
                        int reserved = Integer.parseInt(o.get("reserved").toString());

                        Flight flight = new Flight(Integer.parseInt(location), count, price);
                        flight.setReserved(reserved);
                        data.put(key, flight);
                    }
                    else if (key.contains("car")) {
                        JSONObject o = (JSONObject)obj.get(key);
                        String location = o.get("location").toString();
                        int count = Integer.parseInt(o.get("count").toString());
                        int price = Integer.parseInt(o.get("price").toString());
                        int reserved = Integer.parseInt(o.get("reserved").toString());

                        Car car = new Car(location, count, price);
                        car.setReserved(reserved);
                        data.put(key, car);
                    }
                    else if (key.contains("room")) {
                        JSONObject o = (JSONObject)obj.get(key);
                        String location = o.get("location").toString();
                        int count = Integer.parseInt(o.get("count").toString());
                        int price = Integer.parseInt(o.get("price").toString());
                        int reserved = Integer.parseInt(o.get("reserved").toString());

                        Room room = new Room(location, count, price);
                        room.setReserved(reserved);
                        data.put(key, room);
                    }
                }
            }
            else {
                for (Object key_o : obj.keySet()) {
                    String key = key_o.toString();
                    JSONObject o = (JSONObject)obj.get(key);
                    int id = Integer.parseInt(o.get("id").toString());
                    Customer customer = new Customer(id);
                    JSONObject reservations = (JSONObject)o.get("reservations");

                    for (Object res_o : reservations.keySet()){
                        String rkey = res_o.toString();
                        JSONObject ro = (JSONObject)reservations.get(rkey);
                        String location = ro.get("location").toString();
                        int count = Integer.parseInt(ro.get("count").toString());
                        int price = Integer.parseInt(ro.get("price").toString());
                        String k = ro.get("key").toString();
                        customer.reserve(k, location, count, price);
                    }

                    data.put(key, customer);
                }
            }

        }

    }

    private void recover_in_progress() {
        //this.in_progress is file object
        JSONObject obj = this.in_progress;

        if (obj.toString().equals("{}")) return;

        for (Object inac_key : ((JSONObject)obj.get("inactive")).keySet()){
            int k = Integer.parseInt(inac_key.toString());
            boolean b = Boolean.parseBoolean(((JSONObject)obj.get("inactive")).get(inac_key).toString());
            tm.writeInactiveData(k,b);
        }

        //active data
        JSONObject active = (JSONObject)obj.get("active");

        for (Object a : active.keySet()) {
            int tid = Integer.parseInt(a.toString());
            int timetolive = Integer.parseInt(((JSONObject)active.get(a)).get("timetolive").toString()) / 1000; // expects seconds
            boolean isprepared = Boolean.parseBoolean(((JSONObject)active.get(a)).get("isprepared").toString());
            Transaction t = new Transaction(tid, timetolive);
            t.setIsPrepared(isprepared);
            for (String rm : ((JSONObject)active.get(a)).get("rms").toString().split(","))
                t.addResourceManager(rm);

            if (!this.name.equals("Middleware")) {
                // Thus, Flight,Car,or Room
                JSONObject temp = (JSONObject)((JSONObject)((JSONObject)active.get(a))).get("data");
                for (Object key_o : temp.keySet()) {
                    String key = key_o.toString();
                    if (key.contains("flight")) {
                        JSONObject o = (JSONObject)((JSONObject)((JSONObject)active.get(a)).get("data")).get(key);
                        String location = o.get("location").toString();
                        int count = Integer.parseInt(o.get("count").toString());
                        int price = Integer.parseInt(o.get("price").toString());
                        int reserved = Integer.parseInt(o.get("reserved").toString());

                        Flight flight = new Flight(Integer.parseInt(location), count, price);
                        flight.setReserved(reserved);
                        t.writeData(tid, key, flight);
                    }
                    else if (key.contains("car")) {
                        JSONObject o = (JSONObject)((JSONObject)((JSONObject)active.get(a)).get("data")).get(key);
                        String location = o.get("location").toString();
                        int count = Integer.parseInt(o.get("count").toString());
                        int price = Integer.parseInt(o.get("price").toString());
                        int reserved = Integer.parseInt(o.get("reserved").toString());

                        Car car = new Car(location, count, price);
                        car.setReserved(reserved);
                        t.writeData(tid, key, car);
                    }
                    else if (key.contains("room")) {
                        JSONObject o = (JSONObject)((JSONObject)((JSONObject)active.get(a)).get("data")).get(key);
                        String location = o.get("location").toString();
                        int count = Integer.parseInt(o.get("count").toString());
                        int price = Integer.parseInt(o.get("price").toString());
                        int reserved = Integer.parseInt(o.get("reserved").toString());

                        Room room = new Room(location, count, price);
                        room.setReserved(reserved);
                        t.writeData(tid, key, room);
                    }
                }
            }
            else {
                JSONObject temp = (JSONObject)((JSONObject)((JSONObject)active.get(a))).get("data");
                for (Object key_o : temp.keySet()) {
                    String key = key_o.toString();
                    JSONObject o = (JSONObject)((JSONObject)((JSONObject)active.get(a)).get("data")).get(key);
                    int id = Integer.parseInt(o.get("id").toString());
                    Customer customer = new Customer(id);
                    JSONObject reservations = (JSONObject)o.get("reservations");

                    for (Object res_o : reservations.keySet()){
                        String rkey = res_o.toString();
                        JSONObject ro = (JSONObject)reservations.get(rkey);
                        String location = ro.get("location").toString();
                        int count = Integer.parseInt(ro.get("count").toString());
                        int price = Integer.parseInt(ro.get("price").toString());
                        String k = ro.get("key").toString();
                        customer.reserve(k, location, count, price);
                    }

                    data.put(key, customer);
                }
            }
            //System.out.println(tm);
            //System.out.println(tid);
            //System.out.println(t);
            this.tm.writeActiveData(tid, t);
        }


    }

    public void prepare(int _xid, String state, String data){
        String xid = String.valueOf(_xid);

        if (this.master.get(xid) == null)
            this.master.put(xid, new JSONObject());
        ((JSONObject)this.master.get(xid)).put(state, data);
        flush_to_file(this.master, this.master_f);
    }

    public void setMode(int mode){
        this.master.put("mode",mode);
        flush_to_file(this.master, this.master_f);
    }


    private void recover_master() {
        //this.in_progress is file object
        if (this.master.get("locks") == null)
            this.master.put("locks",new JSONObject());

        if (this.master.get("lastCommit") == null)
            this.master.put("lastCommit",0);

        if (this.master.get("mode") == null)
            this.master.put("mode",0);
    }

//    public void write_committed(int xid) throws InvalidTransactionException{
//        // Grab in progress array
//        JSONArray a = (JSONArray)this.in_progress.get(xid);
//        JSONArray b = (JSONArray)this.committed.get(xid);
//        if (a == null) throw new InvalidTransactionException(xid, "Transaction not in progress");
//        if (b != null) throw new InvalidTransactionException(xid, "Transaction already committed");
//
//        this.committed.put(xid, a);
//        this.in_progress.remove(xid);
//        flush_to_file(this.committed, this.committed_f);
//        flush_to_file(this.in_progress, this.in_progress_f);
//    }

    // True if file setup for the first time; False otherwise
    private boolean initial_setup(String file) {
        // Check for existence of files
        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(new FileReader(file));

            JSONObject jsonObject = (JSONObject) obj;

            if (file.contains("committed") && file.contains(".json"))
                this.committed = jsonObject;
            else if (file.contains("in-progress.json"))
                this.in_progress = jsonObject;
            else if (file.contains("master.json")) {
                this.master = jsonObject;
            }
            else {
                Trace.error("Unknown file=" + file);
                System.exit(-1);
            }
            Trace.info(file + " found: Recovery process initiated");
            return false;

        } catch (FileNotFoundException e) {
            JSONObject o = new JSONObject();
            Trace.info(file + " not found: creating log file");
            if (file.contains("committed") && file.contains(".json")) {
                this.committed = new JSONObject();
                flush_to_file(o, this.getCommitted_f());
            }
            else if (file.contains("in-progress.json")) {
                this.in_progress = new JSONObject();
                flush_to_file(o, this.in_progress_f);
            }
            else if (file.contains("master.json")) {
                this.master = new JSONObject();
                this.master.put("locks", new JSONObject());
                this.master.put("lastCommit", 0);
                this.master.put("mode",0);
                flush_to_file(this.master, this.master_f);
            }
            else {
                Trace.error("Unknown file=" + file);
                System.exit(-1);
            }
        } catch (IOException e) {
            Trace.error("IOException while setting up: " + file);
            e.printStackTrace();
            System.exit(-1);
        } catch (ParseException e) {
            Trace.error("ParseException while setting up: " + file);
            e.printStackTrace();
            System.exit(-1);
        }
        return true;
    }

    private void flush_to_file(JSONObject obj, String file) {
        try (FileWriter f = new FileWriter(file)) {
            f.write(obj.toJSONString());
            f.flush();
        } catch (IOException e) {
            Trace.error("IOException in new_file; file=" + file);
            e.printStackTrace();
            System.exit(-1);
        }
    }


}