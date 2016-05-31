package com.example.johnb.openingsfinder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.SubMenu;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Toast;

import com.alamkanak.weekview.DateTimeInterpreter;
import com.alamkanak.weekview.MonthLoader;
import com.alamkanak.weekview.WeekView;
import com.alamkanak.weekview.WeekViewEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements
        NavigationView.OnNavigationItemSelectedListener,
        WeekView.EventClickListener,
        WeekView.EventLongPressListener,
        WeekView.EmptyViewLongPressListener,
        MonthLoader.MonthChangeListener,
        GoogleCalendarClient.DataChangedListener {

    private static final String TAG = "MainActivity";

    private static final int PERMISSIONS_REQUEST_READ_WRITE_CALENDAR = 1234;

    private static final int TYPE_DAY_VIEW = 1;
    private static final int TYPE_THREE_DAY_VIEW = 2;
    private static final int TYPE_WEEK_VIEW = 3;
    private int mWeekViewType = TYPE_THREE_DAY_VIEW;
    private static boolean findTimes = false;
    private static final int MENU_TODAY = Menu.FIRST;
    private static final int MENU_LIST = Menu.FIRST + 1;
    private static final int MENU_EXIT_EDIT = Menu.FIRST + 2;

    private WeekView mWeekView;
    private boolean inEditMode = false;


    ///////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////

    // Setup:

    ///////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setUpStandardNavigationViewObjects();

        if (checkCalendarPermissions()) {
           setUpGoogleCalendarClient();
        }
        setUpWeekView();
        //addDrawerItems();




    }


    /*private void addDrawerItems() {
        String[] osArray = { "Bluetooth", "Reply to Calls", "Reply to sms", "customise message"};
        ListView mDrawerList = (ListView) findViewById(R.id.listView);

        ArrayAdapter mAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_multiple_choice, osArray);

        mDrawerList.setAdapter(mAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                CheckedTextView ctv = (CheckedTextView)view;
                if (ctv.isChecked()){
                    Toast.makeText(getApplicationContext(),"uncheckd",Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(getApplicationContext(),"checked",Toast.LENGTH_LONG).show();
                }

            }
        });
    }


*/




    private void setUpStandardNavigationViewObjects() {

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setImageResource(R.drawable.ic_menu_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(inEditMode){
                    GoogleCalendarClient.getInstance().setDuration(0);
                    mWeekView.notifyDatasetChanged();
                    exitEditMode();
                }
                else{
                    onDurationSet(view);
                }
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setUpGoogleCalendarClient() {
        GoogleCalendarClient.getInstance().setContext(this);
        GoogleCalendarClient.getInstance().setDataChangedListener(this);
        GoogleCalendarClient.getInstance().loadCalendars();

    }

    void setUpWeekView() {
        // Get a reference for the week view in the layout.
        mWeekView = (WeekView) findViewById(R.id.weekView);

        // Show a toast message about the touched event.
        mWeekView.setOnEventClickListener(this);

        // The week view has infinite scrolling horizontally. We have to provide the events of a
        // month every time the month changes on the week view.
        mWeekView.setMonthChangeListener(this);

        // Set long press listener for events.
        mWeekView.setEventLongPressListener(this);

        // Set long press listener for empty view
        mWeekView.setEmptyViewLongPressListener(this);
        // Set up a date time interpreter to interpret how the date and time will be formatted in
        // the week view. This is optional.
        setupDateTimeInterpreter(false);
    }

    public boolean checkCalendarPermissions() {
        // Return true if permissions are good, if not, we ask for them and return false.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Log.d("PermissionsReqResult", "Checking FOR PERMISSIONS");
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                //Log.d("Permissions", "Permissions not already granted, asking...");
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR},
                        PERMISSIONS_REQUEST_READ_WRITE_CALENDAR);
                return false;
            } else {
                //Log.d("PermissionsReqResult", "PERMISSIONS are good");
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_WRITE_CALENDAR: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    setUpGoogleCalendarClient();
                } else {
                    Log.d("PermissionsReqResult", "Permissions Denied");
                    Toast.makeText(this, "Calendar Permissions Denied - We should show an alert and quit I think", Toast.LENGTH_LONG).show();
                    // permission denied, Disable the functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    /**
     * Set up a date time interpreter which will show short date values when in week view and long
     * date values otherwise.
     * @param shortDate True if the date values should be short.
     */
    private void setupDateTimeInterpreter(final boolean shortDate) {
        mWeekView.setDateTimeInterpreter(new DateTimeInterpreter() {
            @Override
            public String interpretDate(Calendar date) {
                SimpleDateFormat weekdayNameFormat = new SimpleDateFormat("EEE", Locale.getDefault());
                String weekday = weekdayNameFormat.format(date.getTime());
                SimpleDateFormat format = new SimpleDateFormat(" M/d", Locale.getDefault());

                // All android api level do not have a standard way of getting the first letter of
                // the week day name. Hence we get the first char programmatically.
                // Details: http://stackoverflow.com/questions/16959502/get-one-letter-abbreviation-of-week-day-of-a-date-in-java#answer-16959657
                if (shortDate)
                    weekday = String.valueOf(weekday.charAt(0));
                return weekday.toUpperCase() + format.format(date.getTime());
            }

            @Override
            public String interpretTime(int hour) {
                return hour > 11 ? (hour - 12) + " PM" : (hour == 0 ? "12 AM" : hour + " AM");
            }
        });
    }


    ///////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////

    // Interaction With View Objects:

    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id){
            case R.id.action_today:
                mWeekView.goToToday();
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        setupDateTimeInterpreter(id == R.id.week);

        if (id == R.id.single_day) {
            if (mWeekViewType != TYPE_DAY_VIEW) {
                item.setChecked(!item.isChecked());
                mWeekViewType = TYPE_DAY_VIEW;
                mWeekView.setNumberOfVisibleDays(1);

                // Lets change some dimensions to best fit the view.
                mWeekView.setColumnGap((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
                mWeekView.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
                mWeekView.setEventTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
            }
        } else if (id == R.id.three_day) {
            if (mWeekViewType != TYPE_THREE_DAY_VIEW) {
                item.setChecked(!item.isChecked());
                mWeekViewType = TYPE_THREE_DAY_VIEW;
                mWeekView.setNumberOfVisibleDays(3);

                // Lets change some dimensions to best fit the view.
                mWeekView.setColumnGap((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
                mWeekView.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
                mWeekView.setEventTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
            }
        } else if (id == R.id.week) {
            if (mWeekViewType != TYPE_WEEK_VIEW) {
                item.setChecked(!item.isChecked());
                mWeekViewType = TYPE_WEEK_VIEW;
                mWeekView.setNumberOfVisibleDays(7);

                // Lets change some dimensions to best fit the view.
                mWeekView.setColumnGap((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()));
                mWeekView.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, getResources().getDisplayMetrics()));
                mWeekView.setEventTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, getResources().getDisplayMetrics()));
            }
        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    protected String getEventTitle(Calendar time) {
        return String.format("Event of %02d:%02d %s/%d", time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE), time.get(Calendar.MONTH)+1, time.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void onEventClick(WeekViewEvent event, RectF eventRect) {
        Toast.makeText(this, "Clicked " + event.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEventLongPress(WeekViewEvent event, RectF eventRect) {
        Toast.makeText(this, "Long pressed event: " + event.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEmptyViewLongPress(Calendar time) {
        Toast.makeText(this, "Empty view long pressed: " + getEventTitle(time), Toast.LENGTH_SHORT).show();
    }

    @Override
    public List<? extends WeekViewEvent> onMonthChange(int newYear, int newMonth) {
        // Populate the week view with some placeholder events.

        //// TODO: 5/29/2016 Remove this line at some point:
        //Toast.makeText(this, String.format("Year :%d, Month: %d", newYear, newMonth),Toast.LENGTH_SHORT).show();

        ArrayList<WeekViewEvent> events = GoogleCalendarClient.getInstance().getCachedEventsForYearAndMonth(newYear,newMonth);

        if (events.size() == 0) {
            //This month isn't in the cache yet, load it.
            GoogleCalendarClient.getInstance().loadEventsForYearAndMonth(newYear, newMonth);
        } else {
            //The last event in a group is a sentinel with the year and month for the name, remove it before displaying stuff.
            events.remove(events.size() - 1);
        }
        Log.d(TAG, "onMonthChange: Requested " + newYear + ", " + newMonth + ", got: ");
        Log.d(TAG, GoogleCalendarClient.getInstance().WeekViewEventListToString(events));

        return events;
    }


    ///////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////

    // Interaction With GoogleCalendarClient:

    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public void onEventsLoaded() {
        mWeekView.notifyDatasetChanged();
    }

    @Override
    public void onCalendarsLoaded() {
        //// TODO: 5/29/2016 Do something with this:
        ArrayList<GoogleCalendarClient.GCalendar> calendars = GoogleCalendarClient.getInstance().getCalendarCache();
        //this is to initially show all calendars so you know everything working, change this to work how you want.
        GoogleCalendarClient.getInstance().setDesiredCalendars(calendars);
        mWeekView.notifyDatasetChanged();
        NavigationView navView = (NavigationView)findViewById(R.id.nav_view);
        Menu menu = navView.getMenu();
        SubMenu subMenu = menu.getItem(1).getSubMenu();

        for(GoogleCalendarClient.GCalendar g : calendars ){
            subMenu.add(g.getName());
        }

    }

    public void onDurationSet(final View view) {
        NewEventFragment eventFragment = new NewEventFragment();
        eventFragment.setDoneListener(new NewEventFragment.OnDoneListener(){
            @Override
            public void OnDone(int durationInMinutes){
                long durationInMillis = durationInMinutes*60*1000;
                GoogleCalendarClient.getInstance().setDuration(durationInMillis);
                mWeekView.notifyDatasetChanged();
                if(durationInMillis != 0)
                    enterEditMode();
            }
        });
        eventFragment.show(getFragmentManager(),"newEvent");


    }

    private void enterEditMode(){
        this.inEditMode = true;
        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab);
        fab.setImageResource(R.drawable.ic_menu_exit);
    }

    private void exitEditMode(){
        this.inEditMode = false;
        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab);
        fab.setImageResource(R.drawable.ic_menu_add);
    }
}

