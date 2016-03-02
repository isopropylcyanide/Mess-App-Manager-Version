package com.example.cyanide.messMunshi;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
        private String firebase_change_url;
        private String password_valid;
        private Map<String, Object> userChange,user_args;
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
            userChange = StaticUserMap.getInstance().getUserMap();
            user_args = StaticUserMap.getInstance().getUserViewExtras();

            firebase_change_url = user_args.get("EXTRA_FireBase_Node_Ref").toString();
            password_valid  = user_args.get("EXTRA_Node_Password_Field").toString();
            actual_pass = userChange.get(password_valid).toString();

            ref = new Firebase(firebase_change_url);
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

                    if(!netConnected){
                        Toast.makeText(getActivity().getApplicationContext(), "Internet Disconnected. Try Again", Toast.LENGTH_SHORT).show();
                    }

                    else if (curr_pass.getText().toString().equals(actual_pass)){
                        String pass1 = new_pass.getText().toString();
                        String pass2 = new_pass_again.getText().toString();

                        if(pass1.equals(pass2)){
                            //Firebase Update here
                            userChange.put(password_valid, pass1);
                            ref.updateChildren(userChange);
                            StaticUserMap.getInstance().setUserMap(userChange);

                            Toast.makeText(getActivity().getApplicationContext(), "Password Updated", Toast.LENGTH_SHORT).show();
                            curr_pass.setText("");
                            new_pass.setText("");
                            new_pass_again.setText("");

                        }
                        else{
                            Toast.makeText(getActivity().getApplicationContext(), "Password do not match", Toast.LENGTH_SHORT).show();
                            new_pass_again.setText("");
                        }
                    }
                    else{
                        Toast.makeText(getActivity().getApplicationContext(), "Current Password is incorrect", Toast.LENGTH_SHORT).show();
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
                Password_Match_Async password_match_async = new Password_Match_Async(ChangePassword.this.getContext());
                password_match_async.execute();
            }
        });

        return homeview;
    }
}
