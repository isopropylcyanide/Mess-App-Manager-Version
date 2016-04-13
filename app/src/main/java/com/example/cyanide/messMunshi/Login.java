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

    private String entered_user;
    private String entered_pass;
    private Firebase ref;
    private ValueEventListener mConnectedListener;
    private SimpleDateFormat format;

    private class firebase_async extends AsyncTask<String, Void, Void> {
        //An Async class to deal with the synchronisation of listener
        private Context async_context;
        private ProgressDialog pd;
        private String actual_pass, last_login, session_val;
        private boolean userExists;
        private String entered_user, entered_pass;

        public firebase_async(Context context){
            this.async_context = context;
            pd = new ProgressDialog(async_context);
            login_status = "0";
            userExists = false;
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

            entered_user = params[0];
            entered_pass = params[1];
            final Object lock = new Object();

            ref.child(entered_user).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    synchronized (lock) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            userExists = true;
                            if (ds.getKey().equals(Constants.PASSWORD_CHILD))
                                actual_pass = ds.getValue().toString();

                            else if (ds.getKey().equals(Constants.SESSION_CHILD))
                                session_val = ds.getValue().toString();

                            else if (ds.getKey().equals(Constants.LAST_LOGIN_CHILD))
                                last_login = ds.getValue().toString();
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
            if (!userExists || !entered_pass.equals(actual_pass))
                login_status = "0";

            else if (entered_pass.equals(actual_pass)){
                if (session_val.equals("1"))
                    login_status = "1";

                else{//session is not valid. Check for timeout maybe
                    login_status = "-1";
                    try {
                        Date last_date = format.parse(last_login);
                        Date curr_date = new Date();

                        if ((curr_date.getTime() - last_date.getTime()) / 1000 >= Constants.SESSION_TIMEOUT)
                            login_status = "1";

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
            //Show the log in progress_bar for at least a few milliseconds
            Handler handler = new Handler();

            handler.postDelayed(new Runnable() {
                public void run() {
                    pd.dismiss();
                    if (login_status.equals("1")) {
                        //Reset the session variable

                        ref.child(entered_user).child(Constants.SESSION_CHILD).setValue("0");
                        ref.child(entered_user).child(Constants.LAST_LOGIN_CHILD).setValue(new Date().toString());

                        /*Crucial Data. If you mess this up, data could be fetched for other users*/
                        StaticUserMap.getInstance().set_password(actual_pass);
                        StaticUserMap.getInstance()._roll = entered_user;
                        /*Crucial Data ends*/

                        Intent launchUser = new Intent(Login.this, UserView.class);
                        startActivity(launchUser);

                        finish();

                    } else if (login_status.equals("-1")) {
                        Snackbar.make(getWindow().getDecorView(), "Maximum Login Limit Reached", Snackbar.LENGTH_SHORT).show();
                        etPass.setText("");

                    } else if (login_status.equals("0")) {
                        Snackbar.make(getWindow().getDecorView(), "Password/User combination doesn't match", Snackbar.LENGTH_SHORT).show();
                        etPass.setText("");
                    } else
                        Snackbar.make(getWindow().getDecorView(), "No internet connection. Try Again Later", Snackbar.LENGTH_SHORT).show();

                }
            }, 100);  // 100 milliseconds
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
                    Snackbar.make(getWindow().getDecorView(),"Connected", Snackbar.LENGTH_SHORT).show();
                }
                else {
                    StaticUserMap.getInstance().setConnectedStatus(false);
                    Snackbar.make(getWindow().getDecorView(), "Disconnected", Snackbar.LENGTH_SHORT).show();
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

        format           = new SimpleDateFormat(Constants.DATE_FORMAT);
        entered_user     = " ";
        entered_pass     = " ";

        Firebase.setAndroidContext(this);
        ref = new Firebase(Constants.DATABASE_URL + Constants.USER_LOGIN_TABLE);

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

                else Snackbar.make(v, "No internet connection", Snackbar.LENGTH_SHORT).show();
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