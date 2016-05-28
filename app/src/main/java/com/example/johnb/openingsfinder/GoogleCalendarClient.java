package com.example.johnb.openingsfinder;

import android.Manifest;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.alamkanak.weekview.WeekViewEvent;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by John B. Lanier on 5/27/2016.
 */
public class GoogleCalendarClient {

    private static final String TAG = "GoogleCalendarClient";

    public ArrayList<WeekViewEvent> mCachedEvents;
    public static ArrayList<Long> mDesiredCalendarIDs;
    private Context mContext;

    private DataChangedListener mDataChangedListener;

    public interface DataChangedListener {
        void onEventsLoaded();
        void onCalendarsLoaded();
    }

    private static GoogleCalendarClient mInstance = null;

    private GoogleCalendarClient() {
        //Constructor is private, call getInstance instead
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
        public String name;
        public long id;
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
               GoogleCalendarClient.getInstance().onEventsQueryComplete(cursor);
           }
        }

    }

    // Projection array. Creating indices for this array instead of doing
    // dynamic lookups improves performance.
    public static final String[] EVENT_PROJECTION = new String[] {
            CalendarContract.Calendars._ID,                           // 0
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 1


    };

    // The indices for the projection array above.
    private static final int PROJECTION_ID_INDEX = 0;
    private static final int PROJECTION_DISPLAY_NAME_INDEX = 1;


    public void loadCalendars() {


        //@@@ TODO:
        // This should actually start an AsyncQueryHandler which calls onCalendarsQueryFinished when its done.

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
                //cur = cr.query(uri, EVENT_PROJECTION, null, null, null);
                GCalendarAsyncQueryHandler queryHandler = new GCalendarAsyncQueryHandler(cr);
                queryHandler.startQuery(GCalendarAsyncQueryHandler.TOKEN_CALENDAR_QUERY,null,uri,EVENT_PROJECTION,null,null,null);

            } else {
                Log.d(TAG, "loadCalendars: Tried to get calendars but permissions were bad.");
            }

        } else {
            Log.d(TAG, "loadCalendars: Tried to get calendars but mContext was null");
        }
    }

    public void onCalendarsQueryComplete(Cursor cur) {

        Log.d(TAG, "loadCalendars: ---------START Calendar Retreived list---------------");

        // Use the cursor to step through the returned records
        while (cur.moveToNext()) {
            long calID = 0;
            String displayName = null;


            // Get the field values
            calID = cur.getLong(PROJECTION_ID_INDEX);
            displayName = cur.getString(PROJECTION_DISPLAY_NAME_INDEX);

            Log.d(TAG, String.format("loadCalendars: CalID %d",calID));
            Log.d(TAG, "loadCalendars: Name: " + displayName);
            //   Log.d(TAG, "loadCalendars: accountName: " + accountName);
            //   Log.d(TAG, "loadCalendars: ownername: " + ownerName);
        }
        Log.d(TAG, "loadCalendars: ---------END Calendar Retreived list---------------");

        //Todo:
        //Set found calendars to some variable that MainActivity can ask for.

        notifyCalendarsLoaded();
    }

    public void onEventsQueryComplete(Cursor cur) {

    }

    public void setDesiredCalendars(ArrayList<GCalendar> desiredCalendars) {
        mDesiredCalendarIDs.clear();
        for (GCalendar calendar : desiredCalendars){
            if (!mDesiredCalendarIDs.contains(calendar.id)) {
                mDesiredCalendarIDs.add(calendar.id);
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

    //// TODO: 5/28/2016
    // Implement these:

    public void loadEventsForYearAndMonth(int Year, int Month) {
        //stub
    }

    public ArrayList<GCalendar> getCalendarCache() {
        // stub
        return new ArrayList<GCalendar>();
    }

    public ArrayList<WeekViewEvent> getCachedEventsForYearAndMonth(int Year, int Month) {
        //stub
        return new ArrayList<WeekViewEvent>();
    }
}
