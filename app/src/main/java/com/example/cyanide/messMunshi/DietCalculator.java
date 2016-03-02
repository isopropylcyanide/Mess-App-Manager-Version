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
import android.widget.*;

import com.example.cyanide.messMunshi.background.Constants;
import com.example.cyanide.messMunshi.background.StaticUserMap;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.*;

public class DietCalculator extends Fragment {

    View homeview;
    TableLayout table;
    TextView dietText;
    Firebase item_ref;
    Iterator it;
    TableRow row;
    private HashMap<String, StaticUserMap.Rate_Quantity> menuItems;

    int total,dietRate,noOfDays;


    private class databaseItems extends AsyncTask<Void, Void, Void> {
        //An Async class to deal with the synchronisation of listener
        private Context async_context;
        private ProgressDialog pd;

        public databaseItems(Context context){
            this.async_context = context;
            pd = new ProgressDialog(async_context);
            menuItems = StaticUserMap.getInstance().menuItems;
            item_ref = new Firebase(Constants.DATABASE_URL + Constants.MESS_ITEM_TABLE);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd.setMessage("Getting all diet status from the cloud. Wait");
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (menuItems.size() != 0)
                return null;

            final Object lock = new Object();

            item_ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    synchronized (lock) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            HashMap<String, String> temp = (HashMap<String, String>) ds.getValue();

                            StaticUserMap.Rate_Quantity newQuantity = new StaticUserMap().new Rate_Quantity();
                            newQuantity.setCummQuantity(temp.get(Constants.MESS_ITEM_QTY));
                            newQuantity.setRate(temp.get(Constants.MESS_ITEM_RATE));
                            menuItems.put(ds.getKey(), newQuantity);
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

            it = menuItems.entrySet().iterator();

            while (it.hasNext()){
                Map.Entry pair = (Map.Entry)it.next();
                row = (TableRow)LayoutInflater.from(getActivity()).inflate(R.layout.item_row, null);
                StaticUserMap.Rate_Quantity qty = menuItems.get(pair.getKey().toString());

                String objVal = qty.getCummQuantity() + " -> " + qty.getRate();
                ((TextView) row.findViewById(R.id.itemName)).setText(pair.getKey().toString());
                ((TextView)row.findViewById(R.id.itemAmount)).setText(objVal);

                table.addView(row);
            }
        }
        //end firebase_async_class
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        homeview = inflater.inflate(R.layout.activity_diet_calculator, container, false);
        menuItems = StaticUserMap.getInstance().menuItems;
        table = (TableLayout)homeview.findViewById(R.id.expenditureTable);
        dietText= (TextView)homeview.findViewById(R.id.dietRate);

        row = (TableRow)LayoutInflater.from(getActivity()).inflate(R.layout.item_row, null);
        new databaseItems(getContext()).execute();

        dietText.setText("--To calculate--"); //instead of 21 dietRate aayega yahan
        return homeview;
    }



}
