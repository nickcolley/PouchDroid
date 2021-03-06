package com.pouchdb.pouchdroid.example3;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;

import com.pouchdb.pouchdroid.PouchDroidActivity;
import com.pouchdb.pouchdroid.PouchDroid;
import com.pouchdb.pouchdroid.pouch.AsyncPouchDB;
import com.pouchdb.pouchdroid.pouch.PouchDB;
import com.pouchdb.pouchdroid.pouch.callback.AllDocsCallback;
import com.pouchdb.pouchdroid.pouch.callback.BulkCallback;
import com.pouchdb.pouchdroid.pouch.callback.ReplicateCallback;
import com.pouchdb.pouchdroid.pouch.model.AllDocsInfo;
import com.pouchdb.pouchdroid.pouch.model.PouchError;
import com.pouchdb.pouchdroid.pouch.model.PouchInfo;
import com.pouchdb.pouchdroid.pouch.model.ReplicateInfo;
import com.pouchdb.pouchdroid.util.UtilLogger;

public class MainActivity extends PouchDroidActivity {

    private static UtilLogger log = new UtilLogger(MainActivity.class);
    
    private static final String REMOTE_COUCHDB_URL = "http://192.168.0.3:5984/robots";
    
    private AsyncPouchDB<Robot> pouch1, pouch2;
    
    private Handler handler = new Handler();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    protected void onPouchDroidReady(PouchDroid pouchDroid) {
        
        log.i("onPouchDroidReady()");
        
        String random = Integer.toHexString(new Random().nextInt());
        
        pouch1 = PouchDB.newAsyncPouchDB(Robot.class, pouchDroid, "robots-" + random + "-1.db");
        pouch2 = PouchDB.newAsyncPouchDB(Robot.class, pouchDroid, "robots-" + random + "-2.db");
        
        loadIntoPouch1();
    }

    private void loadIntoPouch1() {
        List<Robot> robots = Arrays.asList(
                new Robot("C3P0", "Protocol droid", "George Lucas", 0.4, 200, 
                        Arrays.asList(
                                new RobotFunction("Human-cyborg relations"),
                                new RobotFunction("Losing his limbs")
                                )),
                new Robot("R2-D2", "Astromech droid", "George Lucas", 0.8, 135,
                        Arrays.asList(
                                new RobotFunction("Getting lost"),
                                new RobotFunction("Having a secret jetpack"),
                                new RobotFunction("Showing holographic messages 'n' shit")))        
                );
        
        pouch1.bulkDocs(robots, new BulkCallback() {
            
            @Override
            public void onCallback(PouchError err, List<PouchInfo> info) {
                loadIntoPouch2();
                
            }
        });        
    }

    private void loadIntoPouch2() {
        List<Robot> robots = Arrays.asList(
                new Robot("Mecha Godzilla", "Giant monster", "Toho Co., Ltd.", 0.4, 82, 
                        Arrays.asList(
                                new RobotFunction("Flying through space"),
                                new RobotFunction("Kicking Godzilla's ass"))),
                new Robot("Andy", "Messenger robot", "Stephen King", 0.8, 135,
                        Arrays.asList(
                                new RobotFunction("Relaying messages"),
                                new RobotFunction("Betraying the ka-tet"),
                                new RobotFunction("Many other functions"))),
                new Robot("Bender Bending Rodriguez", "Bending Unit", "Matt Groening", 0.999, 120,
                        Arrays.asList(
                                new RobotFunction("Gettin' drunk"),
                                new RobotFunction("Burping fire"),
                                new RobotFunction("Bending things"),
                                new RobotFunction("Talking about the lustre of his ass")))                                   
                );
        
        pouch2.bulkDocs(robots, new BulkCallback() {
            
            @Override
            public void onCallback(PouchError err, List<PouchInfo> info) {
                replicate();
            }
        }); 
    }

    private void replicate() {
        
        // bi-directional replication on both pouches
        ReplicateCallback onReplicateFrom = new ReplicateCallback() {
            
            @Override
            public void onCallback(PouchError err, ReplicateInfo info) {
                log.i("replicate.from: %s, %s", err, info);
            }
        };
        
        ReplicateCallback onReplicateTo = new ReplicateCallback() {
            
            @Override
            public void onCallback(PouchError err, ReplicateInfo info) {
                log.i("replicate.to  : %s, %s", err, info);
            }
        };
        
        pouch1.replicateFrom(REMOTE_COUCHDB_URL, true, onReplicateFrom);
        pouch1.replicateTo(REMOTE_COUCHDB_URL, true, onReplicateTo);
        pouch2.replicateFrom(REMOTE_COUCHDB_URL, true, onReplicateFrom);
        pouch2.replicateTo(REMOTE_COUCHDB_URL, true, onReplicateTo);
        
        final int DELAY = 30000;
        
        handler.postDelayed(new Runnable() {
            
            @Override
            public void run() {
                checkPouchContents();
            }
        }, DELAY);
        
    }

    private void checkPouchContents() {
        
        pouch1.allDocs(true, new AllDocsCallback<Robot>() {
            
            @Override
            public void onCallback(PouchError err, AllDocsInfo<Robot> info) {
                List<Robot> docs = info.getDocuments();
                log.i("pouch1 contains %s docs: %s", docs.size(), docs);
                if (docs.size() != 5) {
                    throw new RuntimeException("replication failed for pouch1");
                }
            }
        });
        
        
        pouch2.allDocs(true, new AllDocsCallback<Robot>() {
            
            @Override
            public void onCallback(PouchError err, AllDocsInfo<Robot> info) {
                List<Robot> docs = info.getDocuments();
                log.i("pouch2 contains %s docs: %s", docs.size(), docs);
                if (docs.size() != 5) {
                    throw new RuntimeException("replication failed for pouch2");
                }
            }
        });
    }
}