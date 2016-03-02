package com.example.cyanide.messMunshi;


import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.example.cyanide.messMunshi.background.Constants;
import com.example.cyanide.messMunshi.background.StaticUserMap;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;


public class Home extends Fragment {
    View homeview;
    Spinner rollSpin;
    EditText roomText;
    private Firebase item_ref;
    private HashMap<String, ArrayList<String>> rollRoomMap;

    private class databaseItems extends AsyncTask<Void, Void, Void> {
        //An Async class to deal with the synchronisation of listener
        private Context async_context;
        private ProgressDialog pd;

        public databaseItems(Context context){
            this.async_context = context;
            pd = new ProgressDialog(async_context);

            item_ref = new Firebase(Constants.DATABASE_URL + Constants.USER_PROFILE_TABLE);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd.setMessage("Fetching Student Lists");
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            final Object lock = new Object();


            item_ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    synchronized (lock) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            HashMap<String, Object> currUser = (HashMap<String, Object>) ds.getValue();
                            ArrayList<String> currRoom = rollRoomMap.get(currUser.get(Constants.USER_PROFILE_ROOM));

                            if (currRoom == null)
                                currRoom = new ArrayList<String>();

                            currRoom.add(ds.getKey());
                            rollRoomMap.put(currUser.get(Constants.USER_PROFILE_ROOM).toString(), currRoom);
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

            //Show the log in progress_bar for at least a few milliseconds
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    pd.dismiss();
                }
            }, 500);  // 100 milliseconds
        }
        //end firebase_async_class
    }


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        homeview  = inflater.inflate(R.layout.home, container, false);
        rollSpin = (Spinner)homeview.findViewById(R.id.rollSpin);
        rollRoomMap = StaticUserMap.rollRoomMap;
        roomText = (EditText)homeview.findViewById(R.id.roomText);

        //Fetch from internet only when required
        if (rollRoomMap.size() == 0)
            new databaseItems(getContext()).execute();


        roomText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                final String roomNo = roomText.getText().toString();
                ArrayList<String> allRoll = new ArrayList<String>();
                ArrayAdapter<String> adapter;

                if (rollRoomMap.containsKey(roomNo) == false)
                    allRoll.add("None");
                else
                    allRoll = rollRoomMap.get(roomNo);

                adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, allRoll);
                rollSpin.setAdapter(adapter);
                return false;
            }
        });

        return homeview;
    }

}
