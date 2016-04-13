package com.example.cyanide.messMunshi;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.cyanide.messMunshi.background.Constants;
import com.example.cyanide.messMunshi.background.StaticUserMap;
import com.firebase.client.Firebase;
import java.util.Map;


public class ChangePassword extends Fragment {
    View homeview;

    EditText curr_pass;
    EditText new_pass;
    EditText new_pass_again;
    Button change_pass;

    private class Password_Match_Async extends AsyncTask<Void, Void, Void>{
        private Context async_context;
        private Firebase ref;
        private String actual_pass;
        private ProgressDialog pd;
        private  boolean netConnected;

        public Password_Match_Async(Context context){
            this.async_context = context;
            pd = new ProgressDialog(async_context);
            netConnected = StaticUserMap.getInstance().getConnectedStatus();
        }

        protected void onPreExecute() {
            super.onPreExecute();

            String username = StaticUserMap._roll;
            ref = new Firebase(Constants.DATABASE_URL + Constants.USER_LOGIN_TABLE).child(username);
            actual_pass = StaticUserMap.getInstance().get_password();

            pd.setMessage("Requesting Password Update");
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //Once the actual password has been safely found out. Perform changes

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    pd.dismiss();

                    if(!netConnected)
                        Snackbar.make(getView(), "Connection Failed. Please Try Again Later.", Snackbar.LENGTH_SHORT).show();

                    else if (curr_pass.getText().toString().equals(actual_pass)){
                        String pass1 = new_pass.getText().toString();
                        String pass2 = new_pass_again.getText().toString();

                        if(pass1.equals(pass2)){
                            //Firebase Update here
                            ref.child(Constants.PASSWORD_CHILD).setValue(pass1);

                            Snackbar.make(getView(), "Password Updated", Snackbar.LENGTH_SHORT).show();
                            StaticUserMap.getInstance().set_password(pass1);
                            curr_pass.setText("");
                            new_pass.setText("");
                            new_pass_again.setText("");

                        }
                        else{
                            Snackbar.make(getView(), "Passwords do not match", Snackbar.LENGTH_SHORT).show();
                            new_pass_again.setText("");
                        }
                    }
                    else{
                        Snackbar.make(getView(), "Current Password is incorrect", Snackbar.LENGTH_SHORT).show();
                        curr_pass.setText("");
                    }
                }
            }, 800);  // 800 milliseconds
        }

        @Override
        protected Void doInBackground(Void... params) {
            return null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        homeview  = inflater.inflate(R.layout.change_password, container, false);
        curr_pass = (EditText)homeview.findViewById(R.id.curr_pass);
        new_pass = (EditText)homeview.findViewById(R.id.new_pass);
        new_pass_again = (EditText)homeview.findViewById(R.id.new_pass_again);
        change_pass = (Button)homeview.findViewById(R.id.change_pass);

        change_pass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new AlertDialog.Builder(getContext())
                        .setMessage("Are you sure you want to change your password?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Password_Match_Async password_match_async = new Password_Match_Async(ChangePassword.this.getContext());
                                password_match_async.execute();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        return homeview;
    }
}