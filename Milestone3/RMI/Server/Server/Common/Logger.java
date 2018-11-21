package Server.Common;

import Server.JSON.*;
import Server.JSON.parser.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import Server.Interface.*;

public class Logger {
    // File names
    private String committed_f;
    private String in_progress_f;
    private String master_f;

    private JSONObject committed;
    private JSONObject in_progress;
    private JSONObject master;
    private ResourceManager rm;

    public Logger(String name, rm) {
        String prefix = "Server/Logs/";
        this.committed_f =  prefix + name + "-committed.json";
        this.in_progress_f = prefix + name + "-in-progress.json";
        this.master_f = prefix + name + "-master.json";
        this.rm = rm;
        setupEnv();
    }

    private void setupEnv() {
        if (!initial_setup(this.committed_f)) recover_committed();
        if (!initial_setup(this.in_progress_f)) recover_in_progress();
        if (!initial_setup(this.master_f)) recover_master();
    }

    private void recover_committed() {}
    private void recover_in_progress() {}
    private void recover_master() {}

    public void write_committed(int xid) throws InvalidTransactionException{
        // Grab in progress array
        JSONArray a = (JSONArray)this.in_progress.get(xid);
        JSONArray b = (JSONArray)this.committed.get(xid);
        if (a == null) throw new InvalidTransactionException(xid, "Transaction not in progress");
        if (b != null) throw new InvalidTransactionException(xid, "Transaction already committed");

        this.committed.put(xid, a);
        this.in_progress.remove(xid);
        flush_to_file(this.committed, this.committed_f);
        flush_to_file(this.in_progress, this.in_progress_f);
    }

    // True if file setup for the first time; False otherwise
    private boolean initial_setup(String file) {
        // Check for existence of files
        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(new FileReader(file));

            JSONObject jsonObject = (JSONObject) obj;

            if (file.contains("committed.json"))
                this.committed = jsonObject;
            else if (file.contains("in-progress.json"))
                this.in_progress = jsonObject;
            else if (file.contains("master.json"))
                this.master = jsonObject;
            else {
                Trace.error("Unknown file");
                System.exit(-1);
            }

            return false;

        } catch (FileNotFoundException e) {
            Trace.info(file + " not found: creating log file");
            if (file.contains("committed.json")) {
                this.committed = new JSONObject();
                flush_to_file(this.committed, this.committed_f);
            }
            else if (file.contains("in-progress.json")) {
                this.in_progress = new JSONObject();
                flush_to_file(this.in_progress, this.in_progress_f);
            }
            else if (file.contains("master.json")) {
                this.master = new JSONObject();
                flush_to_file(this.master, this.master_f);
            }
            else {
                Trace.error("Unknown file");
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