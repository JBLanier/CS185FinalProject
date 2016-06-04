package com.example.johnb.openingsfinder;

import android.Manifest;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.util.Log;

import com.alamkanak.weekview.WeekViewEvent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Created by John B. Lanier on 5/27/2016.
 */
public class GoogleCalendarClient {

    // Projection array. Creating indices for this array instead of doing
    // dynamic lookups improves performance.
    public static final String[] CALENDAR_PROJECTION = new String[] {
            CalendarContract.Calendars._ID,                           // 0
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 1

    };

    // The indices for the projection array above.
    private static final int CALENDAR_PROJECTION_ID_INDEX = 0;
    private static final int CALENDAR_PROJECTION_DISPLAY_NAME_INDEX = 1;

    public static final String[] EVENTS_PROJECTION = new String[] {
            CalendarContract.Events.TITLE,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.DISPLAY_COLOR,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY

    };

    // The indices for the projection array above.
    private static final int EVENTS_PROJECTION_TITLE_INDEX = 0;
    private static final int EVENTS_PROJECTION_CALENDAR_ID_INDEX = 1;
    private static final int EVENTS_PROJECTION_DISPLAY_COLOR_INDEX = 2;
    private static final int EVENTS_PROJECTION_DTSTART_INDEX = 3;
    private static final int EVENTS_PROJECTION_DTEND_INDEX = 4;
    private static final int EVENTS_PROJECTION_ALL_DAY_INDEX = 5;

    private static final String TAG = "GoogleCalendarClient";

    private static long mIDCounter = 0;

    private ArrayList<ArrayList<WeekViewEvent>> mCachedEvents;
    private ArrayList<GCalendar> mCachedCalendars;
    private static ArrayList<Long> mDesiredCalendarIDs;
    //private Calendar freeSlotMinStartTime;
   // private Calendar freeSlotMaxEndtime;
    private static long mDesiredFreeSlotDuration = 0;
    private boolean mDimEventColors = false;
    private Context mContext;

    private DataChangedListener mDataChangedListener;

    public interface DataChangedListener {
        void onEventsLoaded();
        void onCalendarsLoaded();
    }

    private static GoogleCalendarClient mInstance = null;

    private GoogleCalendarClient() {
        //Constructor is private, call getInstance instead
        mDesiredCalendarIDs = new ArrayList<>();
        mCachedCalendars = new ArrayList<>();
        mCachedEvents = new ArrayList<>();

        /*
        freeSlotMinStartTime = Calendar.getInstance();
        freeSlotMinStartTime.set(2000,1,1,8,0);
        freeSlotMaxEndtime = Calendar.getInstance();
        freeSlotMaxEndtime.set(2000,1,1,23,59);
        */
    }

    public static GoogleCalendarClient getInstance() {
        if (mInstance == null) {
            mInstance = new GoogleCalendarClient();
        }
        return mInstance;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public void setDataChangedListener(DataChangedListener dataChangedListener) {
        mDataChangedListener = dataChangedListener;
    }

    private void notifyEventsLoaded() {
        if (mDataChangedListener != null) {
            mDataChangedListener.onEventsLoaded();
        }
    }

    private void notifyCalendarsLoaded() {
        if (mDataChangedListener != null) {
            mDataChangedListener.onCalendarsLoaded();
        }
    }

    public class GCalendar {

        private String mName;
        private long mId;

        GCalendar(String name, long id) {
            mName = name;
            mId = id;
        }

        public String getName() {
            return mName;
        }

        public long getId() {
            return mId;
        }
    }

    static class GCalendarAsyncQueryHandler extends AsyncQueryHandler {
        public static final int TOKEN_CALENDAR_QUERY = 1;
        public static final int TOKEN_EVENTS_QUERY = 2;


        public GCalendarAsyncQueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
           if (token == TOKEN_CALENDAR_QUERY) {
               GoogleCalendarClient.getInstance().onCalendarsQueryComplete(cursor);
           } else if (token == TOKEN_EVENTS_QUERY) {
               GoogleCalendarClient.getInstance().onEventsQueryComplete(cursor, (String) cookie);
           }
        }

    }

    public void loadCalendars() {

        if (mContext != null) {
            // Run query
            Cursor cur = null;
            ContentResolver cr = mContext.getContentResolver();
            Uri uri = CalendarContract.Calendars.CONTENT_URI;
            String selection = "((" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?) AND ("
                    + CalendarContract.Calendars.ACCOUNT_TYPE + " = ?) AND ("
                    + CalendarContract.Calendars.OWNER_ACCOUNT + " = ?))";

            // Submit the query and get a Cursor object back.
            if (checkPermissions()) {
                GCalendarAsyncQueryHandler queryHandler = new GCalendarAsyncQueryHandler(cr);
                queryHandler.startQuery(GCalendarAsyncQueryHandler.TOKEN_CALENDAR_QUERY,null,uri,CALENDAR_PROJECTION,null,null,null);

            } else {
                Log.d(TAG, "loadCalendars: Tried to get calendars but permissions were bad.");
            }

        } else {
            Log.d(TAG, "loadCalendars: Tried to get calendars but mContext was null");
        }
    }

    public void onCalendarsQueryComplete(Cursor cur) {

        //Log.d(TAG, "loadCalendars: ---------START Calendar Retrieved list---------------");

        mCachedCalendars = new ArrayList<>();

        // Use the cursor to step through the returned records
        while (cur.moveToNext()) {
            long calID = 0;
            String displayName = null;


            // Get the field values
            calID = cur.getLong(CALENDAR_PROJECTION_ID_INDEX);
            displayName = cur.getString(CALENDAR_PROJECTION_DISPLAY_NAME_INDEX);

            GCalendar gcal = new GCalendar(displayName,calID);

            mCachedCalendars.add(gcal);

            //Log.d(TAG, String.format("loadCalendars: CalID %d",calID));
            //Log.d(TAG, "loadCalendars: Name: " + displayName);
            //   Log.d(TAG, "loadCalendars: accountName: " + accountName);
            //   Log.d(TAG, "loadCalendars: ownername: " + ownerName);
        }

        //Log.d(TAG, "loadCalendars: ---------END Calendar Retrieved list---------------");
        
        notifyCalendarsLoaded();
        loadEventsForYearAndMonth(2016,5);
    }

    public void loadEventsForYearAndMonth(int Year, int Month) {
        Log.d(TAG, "loadEventsForYearAndMonth: called");
        if (mContext != null) {
            // Run query
            Cursor cur = null;
            ContentResolver cr = mContext.getContentResolver();
            Uri uri = CalendarContract.Events.CONTENT_URI;

            //Months entered (from WeekView) start at 1, we want to start at 0
            int correctedMonth = Month -1;

            //Set up endYear and endMonth
            int endYear = Year;
            int endMonth = correctedMonth + 1;
            if (correctedMonth == 11) {
                endYear += 1;
                endMonth = 0;
            }

            Calendar c_start= Calendar.getInstance();
            c_start.set(Year,correctedMonth,1,0,0); //Note that months start from 0 (January), Days SEEM to start at 1 though.
            Calendar c_end= Calendar.getInstance();
            c_end.set(endYear,endMonth,1,0,0); //Note that months start from 0 (January)

            String selection = "((dtstart >= "+c_start.getTimeInMillis()+") AND (dtend <= "+c_end.getTimeInMillis()+"))";

            String yearAndMonth = stringFromYearAndMonth(Year,Month);

            // Submit the query and get a Cursor object back.
            if (checkPermissions()) {
                GCalendarAsyncQueryHandler queryHandler = new GCalendarAsyncQueryHandler(cr);
                queryHandler.startQuery(GCalendarAsyncQueryHandler.TOKEN_EVENTS_QUERY,yearAndMonth,uri,EVENTS_PROJECTION,selection,null,null);

            } else {
                Log.d(TAG, "loadEventsForYearAndMonth: Tried to get events but permissions were bad.");
            }

        } else {
            Log.d(TAG, "loadEventsForYearAndMonth: Tried to get events but mContext was null.");
        }
    }

    public void onEventsQueryComplete(Cursor cur, String yearandMonth) {
        //Log.d(TAG, "onEventsQueryComplete: ---------START Events Retrieved list---------------");

        ArrayList<WeekViewEvent> events = new ArrayList<>();

        // Use the cursor to step through the returned records
        while (cur.moveToNext()) {
            long calID = 0;
            String displayName = null;
            int colorInt = 0;
            long DTStart = 0;
            long DTEnd = 0;
            int AllDay = -1;

            // Get the field values
            calID = cur.getLong(EVENTS_PROJECTION_CALENDAR_ID_INDEX);
            displayName = cur.getString(EVENTS_PROJECTION_TITLE_INDEX);
            colorInt = cur.getInt(EVENTS_PROJECTION_DISPLAY_COLOR_INDEX);
            DTStart = cur.getLong(EVENTS_PROJECTION_DTSTART_INDEX);
            DTEnd = cur.getLong(EVENTS_PROJECTION_DTEND_INDEX);
            AllDay = cur.getInt(EVENTS_PROJECTION_ALL_DAY_INDEX);

            //Change color to hexadecimal
            String hexColor = String.format("#%06X", (0xFFFFFF & colorInt));

            //Create Calendars for dates
            Calendar startTime = Calendar.getInstance();
            startTime.setTimeInMillis(DTStart);

            Calendar endTime = Calendar.getInstance();
            endTime.setTimeInMillis(DTEnd);

            //AllDay events go from 4pm of the previous day to 4pm of the actual events' day.
            //Fix them so they go from 12AM actual event day to 12AM next day
            if (AllDay == 1) {
                startTime.setTimeInMillis(endTime.getTimeInMillis());
                startTime.set(Calendar.HOUR_OF_DAY, 0);
                startTime.set(Calendar.MINUTE, 0);
                startTime.set(Calendar.SECOND, 0);
                startTime.set(Calendar.MILLISECOND, 0);

                endTime.setTimeInMillis(startTime.getTimeInMillis() + TimeUnit.DAYS.toMillis(1));

            } else if (DTEnd-DTStart < 600000) {
                endTime.setTimeInMillis(DTStart+600000);
            }

            //Log Stuff
          //  Log.d(TAG, String.format(": CalID %d",calID));
           // Log.d(TAG, ": EVENT Name: " + displayName);
            //Log.d(TAG, ":  color: " + hexColor);
            //Log.d(TAG, "onEventsQueryComplete: StartTime: " + DateFormat.getDateTimeInstance().format(startTime.getTime()));
            //Log.d(TAG, "onEventsQueryComplete: EndTime: " + DateFormat.getDateTimeInstance().format(endTime.getTime()));
           // Log.d(TAG, "onEventsQueryComplete: AllDay: " + AllDay);


            //Create WeekViewEvent

            WeekViewEvent event = new WeekViewEvent(getNewID(calID),displayName,startTime,endTime);
            //Todo
            //change to actual color

            event.setColor(colorInt);
            events.add(event);
        }
        //Log.d(TAG, "OnEventsQueryComplete: ---------END Events Retrieved list---------------");

        // Add stub event at end or array with Year and Month information
        Calendar stubCalender = Calendar.getInstance();
        stubCalender.setTimeInMillis(1);
        WeekViewEvent stubEvent = new WeekViewEvent(getNewID(-1),yearandMonth,stubCalender,stubCalender);
        events.add(stubEvent);

        addMonthToEventsCache(events);
        notifyEventsLoaded();
    }

    public void setDesiredCalendars(ArrayList<GCalendar> desiredCalendars) {
        mDesiredCalendarIDs.clear();
        for (GCalendar calendar : desiredCalendars){
            if (!mDesiredCalendarIDs.contains(calendar.getId())) {
                mDesiredCalendarIDs.add(calendar.getId());
            }
        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean eventMatchesYearAndMonth(WeekViewEvent event, int year, int month) {
        return (event.getStartTime().get(Calendar.YEAR) == year && event.getStartTime().get(Calendar.MONTH) == month - 1)
                || (event.getEndTime().get(Calendar.YEAR) == year && event.getEndTime().get(Calendar.MONTH) == month - 1);
    }

    public ArrayList<GCalendar> getCalendarCache() {
        return new ArrayList<>(mCachedCalendars);
    }

    public ArrayList<WeekViewEvent> getCachedEventsForYearAndMonth(int Year, int Month) {
        for (ArrayList<WeekViewEvent> monthArray : mCachedEvents) {
            String requestMonth = stringFromYearAndMonth(Year,Month);
            String cacheMonth = monthArray.get(monthArray.size()-1).getName();

            if (cacheMonth.equalsIgnoreCase(requestMonth)) {

                ArrayList<WeekViewEvent> returnArray;


                if (mDesiredFreeSlotDuration != 0) {
                    Log.d(TAG, "changing event colors");

                    returnArray = new ArrayList<>();

                    for (WeekViewEvent event : monthArray) {
                        int origColor = event.getColor();
                        int newColor = ColorUtils.blendARGB(origColor,Color.BLACK,0.5f);
                        returnArray.add(new WeekViewEvent(event.getId(),event.getName(),event.getStartTime(),event.getEndTime()));
                        returnArray.get(returnArray.size()-1).setColor(newColor);

                    }


                } else {
                    returnArray = new ArrayList<>(monthArray);
                }
                returnArray = filterEventsForDesiredCalendars(returnArray);



                ArrayList<WeekViewEvent> freeslots = getFreeSlotsForMonth(returnArray);

                Log.d(TAG, "DX" + WeekViewEventListToString(freeslots));

                freeslots.addAll(returnArray);


                return freeslots;
            }
        }
        return new ArrayList<WeekViewEvent>();
    }

    private void addMonthToEventsCache(ArrayList<WeekViewEvent> newMonthArray) {

        //Log.d(TAG, "addMonthToEventsCache: \n\nADDING MONTH TO CACHE:\n");
        //Log.d(TAG, WeekViewEventListToString(newMonthArray));

        if (mCachedEvents.size() == 0) {
            //Log.d(TAG, "addMonthToEventsCache: Cache is empty, adding month without comparing anything");
            mCachedEvents.add(newMonthArray);
        } else {

            int removeIndex = -1;
            for (ArrayList<WeekViewEvent> monthArray : mCachedEvents) {

                    String oldMonth = monthArray.get(monthArray.size() - 1).getName();
                    String newMonth = newMonthArray.get(newMonthArray.size() - 1).getName();

                    if (oldMonth.equalsIgnoreCase(newMonth)) {
                        removeIndex = mCachedEvents.indexOf(monthArray);
                        //Log.d(TAG, "addMonthToEventsCache: Month was already present, removed old one");
                    }

            }

            if (removeIndex != -1) {
                mCachedEvents.remove(removeIndex);
            }

            //Add new month to cache
            mCachedEvents.add(newMonthArray);

        }
        //Log.d(TAG, "NEW CACHE:");
        /*for (ArrayList<WeekViewEvent> monthArray : mCachedEvents) {
            Log.d(TAG, WeekViewEventListToString(monthArray));
        }*/
    }

    private long getNewID(long CalID) {
        mIDCounter ++;
        String idString = String.format("%d00000%d",CalID,mIDCounter);
        long newID = Long.parseLong(idString);
        return newID;
    }

    private long calIDFromWeekViewEvent(WeekViewEvent event) {
        long eventID = event.getId();
        String eventIDString = String.format("%d",eventID);
        String[] s = eventIDString.split("00000");
        String CalIdString = s[0];
        long CalID = Long.parseLong(CalIdString);
        return CalID;
    }

    public String WeekViewEventListToString(ArrayList<WeekViewEvent> a) {
        String s = "\n----ArrayList----\n";
        if (a.size() == 0) {
            s += "EMPTY ARRAYLIST!!!";
        }
        for (WeekViewEvent event: a) {
           s += event.getName();
           s += ",\n";
        }
        return s;
    }

    private String stringFromYearAndMonth(int year, int month) {
        return String.format("%d %d", year, month);
    }

    private ArrayList<WeekViewEvent> filterEventsForDesiredCalendars(ArrayList<WeekViewEvent> events) {
        ArrayList<WeekViewEvent> filteredEvents = new ArrayList<>();

            for (WeekViewEvent event : events) {
                long eventCalID = calIDFromWeekViewEvent(event);
                //Log.d(TAG, "filterEventsForDesiredCalendars: CALID: " + eventCalID);
                if(mDesiredCalendarIDs.contains(eventCalID) || eventCalID == -1) {
                    filteredEvents.add(event);
                }
            }

        return filteredEvents;
    }

    public void setDuration(long millis) {
        mDesiredFreeSlotDuration = millis;
    }

    private ArrayList<WeekViewEvent> getFreeSlotsForMonth(ArrayList<WeekViewEvent> events) {
        ArrayList<WeekViewEvent> freeSlots = new ArrayList<>();
        if(mDesiredFreeSlotDuration != 0 && !(getFreeSlotMaxEndTime().getTimeInMillis()-getFreeSlotMinStartTime().getTimeInMillis() < mDesiredFreeSlotDuration)) {

            String yearAndMonth = events.get(events.size() - 1).getName();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM");
            Calendar c = Calendar.getInstance();
            try {
                c.setTime(sdf.parse(yearAndMonth));
            } catch (ParseException e) {
                Log.d(TAG, "getFreeSlotsForMonth: PARSE EXCEPTION: " + e);
            }
            c.set(Calendar.DAY_OF_MONTH, 1);

            Calendar freeSlotMinStartTime = getFreeSlotMinStartTime();

            c.set(Calendar.HOUR_OF_DAY, freeSlotMinStartTime.get(Calendar.HOUR_OF_DAY));
            c.set(Calendar.MINUTE, freeSlotMinStartTime.get(Calendar.MINUTE));

            Calendar freeSlotStartTime = Calendar.getInstance();
            freeSlotStartTime.setTime(c.getTime());

            Calendar freeSlotEndtime = Calendar.getInstance();
            freeSlotEndtime.setTime(c.getTime());

            Calendar freeSlotMaxEndtime = getFreeSlotMaxEndTime();

            freeSlotEndtime.set(Calendar.HOUR_OF_DAY, freeSlotMaxEndtime.get(Calendar.HOUR_OF_DAY));
            freeSlotEndtime.set(Calendar.MINUTE, freeSlotMaxEndtime.get(Calendar.MINUTE));


            int maxDay = c.getActualMaximum(Calendar.DAY_OF_MONTH);

            //Log.d(TAG, "KQ getFreeSlotsForMonth: NEW MONTH SET---------------------------------------------");
            for (int co = 0; co < maxDay; co++) {

                //Log.d(TAG, String.format("KQ Year: %d, Month %d, Day %d", freeSlotStartTime.get(Calendar.YEAR), freeSlotStartTime.get(Calendar.MONTH), freeSlotEndtime.get(Calendar.DAY_OF_MONTH)));
                //Log.d(TAG, String.format("KQ Start time: Hour: %d, Minutes: %d", freeSlotStartTime.get(Calendar.HOUR_OF_DAY), freeSlotStartTime.get(Calendar.MINUTE)));
                //Log.d(TAG, String.format("KQ End time: Hour: %d, Minutes: %d, Day: %d", freeSlotEndtime.get(Calendar.HOUR_OF_DAY), freeSlotEndtime.get(Calendar.MINUTE), freeSlotEndtime.get(Calendar.DAY_OF_MONTH)));

                Calendar s = Calendar.getInstance();
                s.setTime(freeSlotStartTime.getTime());

                Calendar e = Calendar.getInstance();
                e.setTime(freeSlotEndtime.getTime());

                WeekViewEvent freeSlot = new WeekViewEvent(getNewID(-1), "Free Slot", s, e);

                freeSlot.setColor(mContext.getColor(R.color.free_slot_color));

                freeSlots.add(freeSlot);

                freeSlotStartTime.add(Calendar.DAY_OF_MONTH, 1);
                freeSlotEndtime.add(Calendar.DAY_OF_MONTH, 1);
            }

            // Split up slots to fit around the actual events

            for (WeekViewEvent event : events) {

                if (getIgnoreAllDayEvents()) {
                    //Check for All-Day Event
                    boolean startsAtMintime = false;
                    if (event.getStartTime().get(Calendar.HOUR_OF_DAY) == 0 && event.getStartTime().get(Calendar.MINUTE) == 0) {
                        startsAtMintime = true;
                    }

                    boolean twentyFourHours = false;
                    if (event.getEndTime().getTimeInMillis() == event.getStartTime().getTimeInMillis() + TimeUnit.DAYS.toMillis(1)) {
                        twentyFourHours = true;
                    }

                    if (startsAtMintime && twentyFourHours) {
                        continue;
                    }
                }
                ArrayList<WeekViewEvent> toRemove = new ArrayList<>();
                ArrayList<WeekViewEvent> toAdd = new ArrayList<>();

                for (WeekViewEvent freeSlot : freeSlots) {
                    boolean ESBSS = event.getStartTime().before(freeSlot.getStartTime());
                    boolean EEBSS = event.getEndTime().before(freeSlot.getStartTime());
                    boolean EEBSE = event.getEndTime().before(freeSlot.getEndTime());
                    boolean ESBSE = event.getStartTime().before(freeSlot.getEndTime());

                    boolean ESASE = event.getStartTime().after(freeSlot.getEndTime());
                    boolean EEASE = event.getEndTime().after(freeSlot.getEndTime());
                    boolean EEASS = event.getEndTime().after(freeSlot.getStartTime());
                    boolean ESASS = event.getStartTime().after(freeSlot.getStartTime());

                    if (ESBSS && EEBSS) {
                        //If event is completely before free slot : DO NOTHING
                    }
                    if (ESASE && EEASE) {
                        //If event is completely after free slot : DO NOTHING
                    }
                    if ((ESBSS && EEASS && EEBSE) || (!ESBSS && !ESASS && EEASS && EEBSE)) {
                        //Event begins before free slot and ends during free slot
                        freeSlot.setStartTime(event.getEndTime());
                        long duration = freeSlot.getEndTime().getTimeInMillis() - freeSlot.getStartTime().getTimeInMillis();
                        if (duration < mDesiredFreeSlotDuration) {
                            toRemove.add(freeSlot);
                        }
                    }
                    if ((ESBSE && ESASS && EEASE) || (ESBSE && ESASS && !EEASE && !EEBSE)) {
                        //Event begins during free slot and ends after free slot
                        freeSlot.setEndTime(event.getStartTime());
                        long duration = freeSlot.getEndTime().getTimeInMillis() - freeSlot.getStartTime().getTimeInMillis();
                        if (duration < mDesiredFreeSlotDuration) {
                            toRemove.add(freeSlot);
                        }
                    }
                    if ((ESBSS && EEASE) || (ESBSS && !EEASE && !EEBSE) || (!ESBSS && !ESASS && EEASE) || (!ESBSS && !ESASS && !EEASE && !EEBSE)) {
                        //Event begins before free slot and ends after free slot : REMOVE FREE SLOT
                        toRemove.add(freeSlot);
                    }
                    if (ESASS && EEBSE) {
                        //Event begins and ends during free slot : REMOVE AND SPLIT FREE SLOT
                        toRemove.add(freeSlot);
                        WeekViewEvent beforeSlot = new WeekViewEvent(getNewID(-1), "Free Slot", freeSlot.getStartTime(), event.getStartTime());

                        beforeSlot.setColor(mContext.getColor(R.color.free_slot_color));

                        WeekViewEvent afterSlot = new WeekViewEvent(getNewID(-1), "Free Slot", event.getEndTime(), freeSlot.getEndTime());
                        afterSlot.setColor(mContext.getColor(R.color.free_slot_color));


                        long Bduration = beforeSlot.getEndTime().getTimeInMillis() - beforeSlot.getStartTime().getTimeInMillis();
                        if (Bduration >= mDesiredFreeSlotDuration) {
                            toAdd.add(beforeSlot);
                        }

                        long Aduration = afterSlot.getEndTime().getTimeInMillis() - afterSlot.getStartTime().getTimeInMillis();
                        if (Aduration >= mDesiredFreeSlotDuration) {
                            toAdd.add(afterSlot);
                        }
                    }
                }

                for (WeekViewEvent removeSlot : toRemove) {
                    freeSlots.remove(removeSlot);
                }

                for (WeekViewEvent addSlot : toAdd) {
                    freeSlots.add(addSlot);
                }

            }
        }
        return freeSlots;
    }

    public void setDimEventColors(boolean dimEventColors) {
        this.mDimEventColors = dimEventColors;
    }

    public Calendar getFreeSlotMinStartTime() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        String syncConnPref = sharedPref.getString(SettingsActivity.KEY_MIN_OPENING_START_TIME, "");
        Calendar returnCal = Calendar.getInstance();
        returnCal.set(2000,1,1,0,0);
        int hour = Integer.parseInt(syncConnPref);
        returnCal.set(Calendar.HOUR_OF_DAY, hour);

        return returnCal;

    }

    public Calendar getFreeSlotMaxEndTime() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        String syncConnPref = sharedPref.getString(SettingsActivity.KEY_MAX_OPENING_END_TIME, "");
        Calendar returnCal = Calendar.getInstance();
        returnCal.set(2000,1,1,23,0);
        int hour = Integer.parseInt(syncConnPref);
        if (hour != 0) {
            returnCal.set(Calendar.HOUR_OF_DAY, hour);
        } else {
            returnCal.set(2000,1,1,23,59);
        }
        return returnCal;
    }

    private boolean getIgnoreAllDayEvents() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean returnBool = sharedPref.getBoolean(SettingsActivity.KEY_IGNORE_ALL_DAY_EVENTS, true);
        return returnBool;
    }
}
