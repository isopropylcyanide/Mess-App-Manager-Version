package com.example.cyanide.messMunshi.background;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StaticUserMap {

    public class Rate_Quantity {

        private String cummQuantity;
        private String rate;

        public Rate_Quantity(){
            cummQuantity = "0";
            rate = "0";
        }

        public void setCummQuantity(String qty){ this.cummQuantity = qty;}
        public void setRate(String qtyRate){    this.rate = qtyRate;}

        public String getCummQuantity()   { return this.cummQuantity; }
        public String getRate() { return this.rate; }
    }

    public static String _roll;
    private static String _password;//private Data

    public String get_password(){
        return _password;
    }

    public void set_password(String pass){
        _password = new String(pass);
    }

    public static HashMap<String, Rate_Quantity> menuItems;
    public static HashMap<String, ArrayList<String> > rollRoomMap;

    static {
        menuItems = new HashMap<String, Rate_Quantity>();
        rollRoomMap = new HashMap<String, ArrayList<String>>();
    }

    private static boolean netConnected;
    public boolean getConnectedStatus(){return netConnected;}

    public void setConnectedStatus(boolean flag){netConnected = flag;}
    public static StaticUserMap getInstance(){
        return holder;
    }

    private static final StaticUserMap holder = new StaticUserMap();
}

