package com.example.cyanide.messMunshi;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.cyanide.messMunshi.background.Constants;
import com.example.cyanide.messMunshi.background.StaticUserMap;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TodayConsumption extends Fragment {

    View homeview;
    private Button submit;
    private Spinner items;
    private EditText newQtyField;
    private TextView currCummQty;
    private String itemTable;
    private Firebase item_ref;
    private HashMap<String, StaticUserMap.Rate_Quantity> menuItems;

    //Data members for spinnerUtil
    Set<String> allKeys;
    ArrayList<String> itemList;
    ArrayAdapter<String> adapter;

    private class databaseItems extends AsyncTask<Void, Void, Void> {
        //An Async class to deal with the synchronisation of listener
        private Context async_context;
        private ProgressDialog pd;

        public databaseItems(Context context){
            this.async_context = context;
            pd = new ProgressDialog(async_context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd.setMessage("Getting Current Quantities. (Enter : Daily)");
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

            allKeys = menuItems.keySet();
            itemList =  new ArrayList<String>();
            itemList.addAll(allKeys);
            adapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item,
                    itemList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            items.setAdapter(adapter);
        }
        //end firebase_async_class
    }


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        homeview = inflater.inflate(R.layout.todayconsumption, container, false);
        itemTable = Constants.DATABASE_URL + Constants.MESS_ITEM_TABLE;
        item_ref = new Firebase(itemTable);

        items = (Spinner)homeview.findViewById(R.id.items);
        currCummQty = (TextView)homeview.findViewById(R.id.prevQty);
        newQtyField = (EditText)homeview.findViewById(R.id.newQty);

        submit=(Button)homeview.findViewById(R.id.submit);

        menuItems = StaticUserMap.getInstance().menuItems;

        if (menuItems.size() == 0)
            new databaseItems(getContext()).execute();


        else{
            //Set adapter for spinner based on arrayList of all keys in menuItems
            allKeys = menuItems.keySet();
            itemList =  new ArrayList<String>();
            itemList.addAll(allKeys);
            adapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item,
                    itemList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            items.setAdapter(adapter);
        }

        items.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String curr_item = items.getSelectedItem().toString();
                currCummQty.setText(menuItems.get(curr_item).getCummQuantity());
                newQtyField.setText("");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String curr_item = items.getSelectedItem().toString();
                StaticUserMap.Rate_Quantity currQty = menuItems.get(curr_item);

                int prevQtyInt = Integer.parseInt(currQty.getCummQuantity());
                int qtyToday = Integer.parseInt(newQtyField.getText().toString());
                int newQty = prevQtyInt + qtyToday;

                currQty.setCummQuantity(Integer.toString(newQty));
                menuItems.put(curr_item, currQty);

                Map<String, Object> temp = new HashMap<String, Object>();
                temp.put(Constants.MESS_ITEM_QTY, currQty.getCummQuantity());
                temp.put(Constants.MESS_ITEM_RATE, currQty.getRate());

                try {
                    item_ref.child(curr_item).setValue(temp);
                    Snackbar.make(homeview, "New value submitted : " + newQty, Snackbar.LENGTH_LONG).show();
                } catch (NullPointerException e) {
                    Snackbar.make(homeview, "NullPointer exception occured", Snackbar.LENGTH_LONG).show();
                }
            }
        });

        return homeview;
    }



}
