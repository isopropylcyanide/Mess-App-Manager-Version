package com.example.cyanide.messMunshi;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.example.cyanide.messMunshi.background.Constants;
import com.example.cyanide.messMunshi.background.StaticUserMap;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.nio.channels.CancelledKeyException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;



public class Home extends Fragment {
    View homeview;
    Spinner rollSpin;
    EditText roomText;
    private Firebase item_ref;
    private Firebase student_app_diet_ref;
    private Firebase messOffRef;
    private boolean date_in_mess_off_range;

    private int currentMonth, currentDay, currentYear;
    private HashMap<String, Integer> dietIntMap;
    private ArrayList<CheckBox> intCheck;

    //For mess off table
    private String initOffDate, endOffDate, initOffDiet, endOffDiet;

    private CheckBox lunchCb, dinnerCb, bfastCb;
    private EditText extrasEt, guestsEt;
    private Button saveBt;

    //A map that stores roll no per room no
    private HashMap<String, ArrayList<String>> rollRoomMap;

    //An Async class to deal with the synchronisation of listener
    private class databaseItems extends AsyncTask<Void, Void, Void> {

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

    //Async class to get the details of the user based on the current date
    private class roll_diet_record_async extends AsyncTask<Void, Void, Void>{
        private  String roll;
        private Firebase dietDayToday;

        public roll_diet_record_async(String curRoll){
            this.roll = curRoll;
            dietDayToday = student_app_diet_ref.child(roll).child(currentYear + "").child(currentMonth + "").child(currentDay +"");
        }

        @Override
        protected Void doInBackground(Void... params) {
            final Object lock = new Object();
            //refer to the currentMonth and currentDate
            //note that variables from database are static and should be uneditable
            //edit is only allowed when the mess is off and user wants to edit

            dietDayToday.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    synchronized (lock) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            System.out.println("\nFound : " + ds.getKey() +" => " + ds.getValue());
                            Boolean isSelected;

                            if (ds.getKey().equals(Constants.STUDENT_DIET_BFAST)){
                                isSelected = (Boolean)ds.getValue();
                                bfastCb.setEnabled(true);
                                bfastCb.setChecked(isSelected);
                                bfastCb.setEnabled(false);
                            }
                            else if (ds.getKey().equals(Constants.STUDENT_DIET_LUNCH)){
                                isSelected = (Boolean)ds.getValue();
                                lunchCb.setEnabled(true);
                                lunchCb.setChecked(isSelected);
                                lunchCb.setEnabled(false);
                            }

                            else if (ds.getKey().equals(Constants.STUDENT_DIET_DINNER)){
                                isSelected = (Boolean)ds.getValue();
                                dinnerCb.setEnabled(true);
                                dinnerCb.setChecked(isSelected);
                                dinnerCb.setEnabled(false);
                            }

                            else if (ds.getKey().equals(Constants.STUDENT_DIET_EXTRAS)){
                                extrasEt.setText(ds.getValue().toString());
                            }
                            else if (ds.getKey().equals(Constants.STUDENT_DIET_GUEST)){
                                guestsEt.setText(ds.getValue().toString());
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
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    //Async class to deal with the mess off table values if current date is a part
    private class mess_off_async extends AsyncTask<Void, Void, Void>{
        private  String roll;
        private boolean recordExists;

        public mess_off_async(String curRoll) {
            super();
            this.roll = curRoll;
            date_in_mess_off_range = this.recordExists = false;
            initOffDate = endOffDate =  initOffDiet = endOffDiet = "";
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //initially all fields are as they were received before
            saveBt.setBackgroundResource(android.R.drawable.btn_default);
            extrasEt.setEnabled(true);
            guestsEt.setEnabled(true);
            extrasEt.setBackgroundResource(android.R.drawable.edit_text);
            guestsEt.setBackgroundResource(android.R.drawable.edit_text);
        }

        @Override
        protected Void doInBackground(Void... params) {
            final Object lock = new Object();
            //refer to the currentMonth and currentDate

            messOffRef.child(roll).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    synchronized (lock) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            recordExists = true;
                            if (ds.getKey() == Constants.STUDENT_MESS_OFF_START_DAY)
                                initOffDate = ds.getValue().toString();

                            else if (ds.getKey() == Constants.STUDENT_MESS_OFF_END_DAY)
                                endOffDate = ds.getValue().toString();

                            else if (ds.getKey() == Constants.STUDENT_MESS_OFF_END_DIET)
                                endOffDiet = ds.getValue().toString();

                            else if (ds.getKey() == Constants.STUDENT_MESS_OFF_START_DIET)
                                initOffDiet = ds.getValue().toString();
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
            super.onPostExecute(aVoid);
            //Mess hasn't been set off
            if (recordExists == false) {
                System.out.println("Mess is not off for this user");
                return;
            }
            //else there's a record. Now carefully enable the required checkbox and
            //set them as unchecked

            String initDateExp[] = initOffDate.split("-");
            int initYear = Integer.parseInt(initDateExp[0]);
            int initMonth = Integer.parseInt(initDateExp[1]);
            int initDay = Integer.parseInt(initDateExp[2]);

            String endDateExp[] = endOffDate.split("-");
            int endYear = Integer.parseInt(endDateExp[0]);
            int endMonth = Integer.parseInt(endDateExp[1]);
            int endDay = Integer.parseInt(endDateExp[2]);


            if (currentYear >= endYear && currentMonth >= endMonth && currentDay > endDay) {
                System.out.println("Old record");
                //Possible cleanup of old mess rRecord
                return;
            }

            if (currentYear <= initYear && currentMonth <= initMonth && currentDay < initDay) {
                System.out.println("record ahead in time");
                //Too early for a change
                return;
            }

            StringBuffer dialogText = new StringBuffer();

            if (currentYear >= initYear && currentYear <= endYear &&
                    currentMonth >= initMonth && currentMonth <= endMonth){
                //We are either on start day, end day or in between
                //Display an alert dialog regarding the dates and press ok to modify else not
                date_in_mess_off_range = true;

                if (currentDay == initDay){
                    System.out.println("We are on start day");
                    dialogText.append("User's Mess started today\n");
                    //then from start diet to dinner , set everything off

                    setMessDiet(initOffDiet, Constants.STUDENT_DIET_DINNER, false);
                }

                else if (currentDay == endDay){
                    System.out.println("We are on end day");
                    dialogText.append("User's Mess ends today\n");
                    //then from bfast to end Diet , set everything off
                    setMessDiet(Constants.STUDENT_DIET_BFAST, endOffDiet, false);
                }

                else{
                    System.out.println("We are in between");
                    dialogText.append("User's Mess is currently off\n");
                    //set everything from bfast to dinner
                    setMessDiet(Constants.STUDENT_DIET_BFAST, Constants.STUDENT_DIET_DINNER, false);
                }
            }

            dialogText.append("\n From: " + initOffDate + " (" + initOffDiet + ")\n");
            dialogText.append("\n To: " + endOffDate + " (" + endOffDiet + ")");


            new AlertDialog.Builder(getContext())
                    .setMessage(dialogText)
                    .setCancelable(false)
                    .setPositiveButton("Edit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Allow munshi to edit field and (most likely) include a meal which was previously
                            //off. Notice that once you open breakfast then all will get open
                            //only enable those fields that are disabled
                            //don't enable set checkboxes as this isn't allowed
                            if(!bfastCb.isEnabled())
                                bfastCb.setEnabled(true);

                            if(!lunchCb.isEnabled())
                                lunchCb.setEnabled(true);

                            if (!dinnerCb.isEnabled())
                                dinnerCb.setEnabled(true);

                            //allow guest diet fields and extra fields
                            extrasEt.setEnabled(true);
                            guestsEt.setEnabled(true);
                        }
                    })
                    .setNegativeButton("Okay", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //No edits
                            date_in_mess_off_range = false;
                            saveBt.setBackgroundColor(Color.parseColor("#D3D3D3"));
                            extrasEt.setBackgroundColor(Color.parseColor("#D3D3D3"));
                            guestsEt.setBackgroundColor(Color.parseColor("#D3D3D3"));
                            extrasEt.setEnabled(false);
                            guestsEt.setEnabled(false);
                            Snackbar.make(homeview, "No edits allowed", Snackbar.LENGTH_SHORT).show();
                            //still you have to upload this data for consistency
                            saveBt.performClick();
                        }
                    })
                    .show();

        }
    }//end mess_off_async


    protected void setMessDiet(String start, String end, boolean value){
        //A helper method that sets the diets from start to end as false
        //Marks them as uneditable (Mess Off)
        int startDiet = dietIntMap.get(start);
        int endDiet = dietIntMap.get(end);
        for (int i = startDiet; i <= endDiet; i++) {
            CheckBox cur = intCheck.get(i);
            cur.setEnabled(true);
            intCheck.get(i).setChecked(value);
            cur.setEnabled(false);
        }

    }


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        homeview  = inflater.inflate(R.layout.home, container, false);
        rollSpin = (Spinner)homeview.findViewById(R.id.spinner);
        rollRoomMap = StaticUserMap.rollRoomMap;
        roomText = (EditText)homeview.findViewById(R.id.roomText);

        bfastCb  = (CheckBox)homeview.findViewById(R.id.breakFastCb);
        lunchCb  = (CheckBox)homeview.findViewById(R.id.lunchCb);
        dinnerCb = (CheckBox)homeview.findViewById(R.id.dinnerCb);
        extrasEt = (EditText)homeview.findViewById(R.id.extrasEt);
        guestsEt = (EditText)homeview.findViewById(R.id.guestEt);
        saveBt   = (Button) homeview.findViewById(R.id.saveBt);

        /*-------Helpful to set required checkboxes off when mess is off*/
        intCheck = new ArrayList<CheckBox>();
        intCheck.add(bfastCb);
        intCheck.add(lunchCb);
        intCheck.add(dinnerCb);

        dietIntMap = new HashMap<String, Integer>();
        dietIntMap.put(Constants.STUDENT_DIET_BFAST, 0);
        dietIntMap.put(Constants.STUDENT_DIET_LUNCH, 1);
        dietIntMap.put(Constants.STUDENT_DIET_DINNER, 2);
        /*------------------------*/

        //Set class variables
        Calendar localCalendar = Calendar.getInstance(TimeZone.getDefault());
        currentDay = localCalendar.get(Calendar.DATE);
        currentMonth = localCalendar.get(Calendar.MONTH) + 1; //this is 0 based
        currentYear = localCalendar.get(Calendar.YEAR);

        //Fetch from internet only when required
        if (rollRoomMap.size() == 0)
            new databaseItems(getContext()).execute();

        //But create a reference to the student app database table as soon as possible without intrusion
        student_app_diet_ref = new Firebase(Constants.STUDENT_DATABASE_URL + Constants.STUDENT_DATABASE_DIET_TABLE);

        //Get a reference to the mess off table beforehand
        messOffRef = new Firebase(Constants.STUDENT_DATABASE_URL + Constants.STUDENT_MESS_OFF_TABLE);

        //Allow display of roll number for a  given room through arrayAdapter
        roomText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final String roomNo = roomText.getText().toString();
                ArrayList<String> allRoll = new ArrayList<String>();
                ArrayAdapter<String> adapter;

                if (rollRoomMap.containsKey(roomNo) == false)
                    allRoll.add("None");
                else
                    allRoll = rollRoomMap.get(roomNo);
                adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, allRoll);
                rollSpin.setAdapter(adapter);

            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        rollSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Next step is to get the current day's report from the database of the selected user
                // Cache if necessary since this will happen a lot of time.
                String curRoll = rollSpin.getSelectedItem().toString();
                if (curRoll.equals("None") == false) {
                    new roll_diet_record_async(curRoll).execute();
                    //After the basic fetch, get async details of the mess off status and update variables.
                    new mess_off_async(curRoll).execute();
                }
                else {
                    bfastCb.setChecked(false);
                    dinnerCb.setChecked(false);
                    lunchCb.setChecked(false);
                    extrasEt.setText("\t None");
                    guestsEt.setText("\t None");
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        saveBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (rollSpin.getSelectedItem() == null)
                    return;

                String curRoll = rollSpin.getSelectedItem().toString();
                if (curRoll.equals("None"))
                    return;

                //check if we are coming from a Mess off edit
                //if yes, then we might need to upate mess off table
                if (date_in_mess_off_range == true) {
                    System.out.println("We are coming from an edit");
                    String lowestMeal = "", lastMeal = "";
                    //if lowest meal was breakfast then set off for the dinner of day before
                    //else a diet before the current day

                    if (bfastCb.isEnabled() && bfastCb.isChecked()) {
                        lowestMeal = Constants.STUDENT_DIET_BFAST;
                        lastMeal = Constants.STUDENT_DIET_DINNER;
                    } else if (lunchCb.isEnabled() && lunchCb.isChecked()) {
                        lowestMeal = Constants.STUDENT_DIET_LUNCH;
                        lastMeal = Constants.STUDENT_DIET_BFAST;
                    } else if (dinnerCb.isEnabled() && dinnerCb.isChecked()) {
                        lowestMeal = Constants.STUDENT_DIET_DINNER;
                        lastMeal = Constants.STUDENT_DIET_LUNCH;
                    }

                    //if still none of them are checked, then no change is necessary
                    //else set required diets off
                    //But extras Fields may be updated, may be not
                    if (!lowestMeal.equals("")) {
                        //Show a progress bar acknowledging new diet in mess off
                        //and update of the mess off table
                        setMessDiet(lowestMeal, Constants.STUDENT_DIET_DINNER, true);

                        StringBuffer newMessOffNotify = new StringBuffer();

                        String newEndDate = currentYear + "-" + currentMonth + "-" + currentDay;
                        String newEndDiet = lastMeal;

                        if (lastMeal == Constants.STUDENT_DIET_DINNER) {
                            System.out.println("Calc previous day: ");
                            Calendar cal = Calendar.getInstance();
                            cal.add(Calendar.DATE, -1);
                            newEndDate = cal.get(Calendar.YEAR) + "" +  '-' + (cal.get(Calendar.MONTH) + 1) + ""
                                    + "-" + cal.get(Calendar.DATE);
                        }

                        newMessOffNotify.append("\n From: " + initOffDate + " (" + initOffDiet + ")\n");
                        newMessOffNotify.append("\n To: " + newEndDate + " (" + newEndDiet + ")");

                        new AlertDialog.Builder(getContext())
                                .setTitle("Mess schedule changed.")
                                .setMessage(newMessOffNotify.toString())
                                .setPositiveButton("okay", null)
                                .show();

                        //Now update the mess ref table
                        HashMap<String, Object> uploadMessOff = new HashMap<String, Object>();
                        uploadMessOff.put(Constants.STUDENT_MESS_OFF_START_DAY, initOffDate);
                        uploadMessOff.put(Constants.STUDENT_MESS_OFF_END_DAY, newEndDate);
                        uploadMessOff.put(Constants.STUDENT_MESS_OFF_START_DIET, initOffDiet);
                        uploadMessOff.put(Constants.STUDENT_MESS_OFF_END_DIET, newEndDiet);

                        Firebase newMessOffUrl = messOffRef.child(curRoll);
                        newMessOffUrl.updateChildren(uploadMessOff);

                    }

                }

                //Carefully upload the data to the servers
                HashMap<String, Object> uploadDiet = new HashMap<String, Object>();
                uploadDiet.put(Constants.STUDENT_DIET_BFAST, bfastCb.isChecked());
                uploadDiet.put(Constants.STUDENT_DIET_LUNCH, lunchCb.isChecked());
                uploadDiet.put(Constants.STUDENT_DIET_DINNER, dinnerCb.isChecked());
                uploadDiet.put(Constants.STUDENT_DIET_EXTRAS, extrasEt.getText().toString());
                uploadDiet.put(Constants.STUDENT_DIET_GUEST, guestsEt.getText().toString());

                Firebase dietUrl = student_app_diet_ref.child(curRoll).child(currentYear + "").child(currentMonth + "");
                HashMap<String, Object> dayUpload = new HashMap<String, Object>();
                HashMap<String, Object> monthUpload = new HashMap<String, Object>();
                dayUpload.put(currentDay + "", uploadDiet);
                monthUpload.put(currentMonth + "", uploadDiet);
                dietUrl.updateChildren(dayUpload);

                Snackbar.make(homeview, "Uploaded Data for: " + curRoll, Snackbar.LENGTH_SHORT).show();

            }
        });

        return homeview;
    }

}
