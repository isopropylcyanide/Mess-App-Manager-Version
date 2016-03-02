package com.example.cyanide.messMunshi;


import com.example.cyanide.messMunshi.background.Constants;
import com.example.cyanide.messMunshi.background.StaticUserMap;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import android.os.Handler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;


public class Login extends AppCompatActivity{

    private Button btnSignIn;
    private EditText etUserName, etPass;
    private String login_status;
    private String user_login_table;
    private String password_child, session_child,last_login_child;
    private String firebase_Url,database_Url;

    private String entered_user;
    private String entered_pass;
    private Firebase ref;
    private ValueEventListener mConnectedListener, validUserListener;
    private long session_timeout;
    private SimpleDateFormat format;
    private CoordinatorLayout coordinatorLayout;

    private class firebase_async extends AsyncTask<String, Void, Void> {
        //An Async class to deal with the synchronisation of listener
        private Context async_context;
        private ProgressDialog pd;
        private String actual_pass;

        public firebase_async(Context context){
            this.async_context = context;
            pd = new ProgressDialog(async_context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd.setMessage("Logging in");
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected Void doInBackground(final String... params) {

            final String entered_user = params[0];
            final String entered_pass = params[1];
            final Object lock = new Object();

            validUserListener = ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    synchronized (lock) {
                        login_status = "0";

                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            HashMap<String, Object> existUser = (HashMap<String, Object>) ds.getValue();

                            if (entered_user.equals(ds.getKey())) {

                                actual_pass = existUser.get(password_child).toString();
                                int session_val = Integer.parseInt(existUser.get(session_child).toString());

                                if (entered_pass.equals(actual_pass)) {
                                    if (session_val == 0) {
                                        login_status = "-1";

                                        /*if difference b/w curr timestamp and stored timestamp
                                        is greater than session_timeout, allow log-in by setting session bit*/

                                        String last_login = existUser.get(last_login_child).toString();
                                        try {
                                            Date last_date = format.parse(last_login);
                                            Date curr_date = new Date();
                                            long diff = (curr_date.getTime() - last_date.getTime())/1000;

                                            if (diff >= session_timeout)
                                                login_status = "1";

                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    else
                                        login_status = "1";
                                    lock.notifyAll();
                                    break;
                                }
                            }
                        }
                        lock.notifyAll();
                    }
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {

                }
            });

            synchronized (lock){
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //Handles the stuff after the synchronisation with the firebase listener has been achieved
            //The main UI is already idle by this moment

            super.onPostExecute(aVoid);
            System.out.println("After async: " + login_status);

            //Show the log in progress_bar for at least a few milliseconds
            Handler handler = new Handler();

            handler.postDelayed(new Runnable() {
                public void run() {
                    pd.dismiss();
                    if (login_status.equals("1")) {

                        //Reset the session variable
                        Firebase updated_ref = new Firebase(firebase_Url).child(entered_user);
                        Map<String, Object> existUser = new HashMap<String, Object>();
                        Map<String, Object> user_args = new HashMap<String, Object>();

                        existUser.put(session_child, "0");
                        existUser.put(password_child, actual_pass);
                        existUser.put(last_login_child, new Date().toString());
                        updated_ref.updateChildren(existUser);

                        user_args.put("EXTRA_FireBase_Node_Ref", database_Url + updated_ref.getPath().toString());
                        user_args.put("EXTRA_Node_Session_Field", session_child);
                        user_args.put("EXTRA_Node_Password_Field", password_child);
                        user_args.put("EXTRA_Node_Last_Log_Field", last_login_child);

                        StaticUserMap.getInstance().setUserMap(existUser);
                        StaticUserMap.getInstance().setUserViewExtras(user_args);
                        StaticUserMap.getInstance()._userName = entered_user;

                        Intent launchUser = new Intent(Login.this, UserView.class);
                        startActivity(launchUser);

                        //Remove listener as it is not required anymore. Also pop off the current activity
                        ref.removeEventListener(validUserListener);
                        finish();

                    } else if (login_status.equals("-1")) {
                        Snackbar.make(coordinatorLayout, "Maximum Login Limit Reached", Snackbar.LENGTH_SHORT).show();
                        etPass.setText("");

                    } else if (login_status.equals("0")) {
                        Snackbar.make(coordinatorLayout, "Password/User combination doesn't match", Snackbar.LENGTH_SHORT).show();
                        etPass.setText("");
                    } else
                        Snackbar.make(coordinatorLayout, "No internet connection. Try Again Later", Snackbar.LENGTH_SHORT).show();

                }
            }, 500);  // 500 milliseconds
        }
        //end firebase_async_class
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ref.getRoot().child(".info/connected").removeEventListener(mConnectedListener);
                        Login.this.finish();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Finally, a little indication of connection status

        mConnectedListener = ref.getRoot().child(".info/connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean connected = (Boolean) dataSnapshot.getValue();
                if (connected) {
                    StaticUserMap.getInstance().setConnectedStatus(true);
                    Snackbar.make(coordinatorLayout,"Connected to Firebase", Snackbar.LENGTH_SHORT).show();
                }
                else {
                    StaticUserMap.getInstance().setConnectedStatus(false);
                    Snackbar.make(coordinatorLayout, "Disconnected from Firebase", Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        });
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        btnSignIn = (Button) findViewById(R.id.btnSignIn);
        etUserName = (EditText) findViewById(R.id.etUserName);
        etPass = (EditText) findViewById(R.id.etPass);
        coordinatorLayout = (CoordinatorLayout)findViewById(R.id.coordinateLayout);

        user_login_table = Constants.USER_LOGIN_TABLE;
        password_child   = Constants.PASSWORD_CHILD;
        session_child    = Constants.SESSION_CHILD;
        last_login_child = Constants.LAST_LOGIN_CHILD;
        database_Url     = Constants.DATABASE_URL;
        firebase_Url     = database_Url + user_login_table;
        session_timeout  = Constants.SESSION_TIMEOUT;
        format           = new SimpleDateFormat(Constants.DATE_FORMAT);
        entered_user     = " ";
        entered_pass     = " ";

        Firebase.setAndroidContext(this);
        ref = new Firebase(firebase_Url);

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                entered_user = etUserName.getText().toString();
                entered_pass = etPass.getText().toString();

                //only if connected
                if (StaticUserMap.getInstance().getConnectedStatus()) {

                    //Let the main UI run independently of the async listener
                    firebase_async authTask = new firebase_async(Login.this);
                    authTask.execute(entered_user, entered_pass);
                }

                else Snackbar.make(coordinatorLayout, "No internet connection", Snackbar.LENGTH_SHORT).show();
                //in this case the main UI does practically nothing
                //but the catch is that it's not waiting for anyone. Fully responsive
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //class ends

}


