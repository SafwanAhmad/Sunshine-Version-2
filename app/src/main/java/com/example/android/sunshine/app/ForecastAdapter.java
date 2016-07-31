package com.example.android.sunshine.app;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends CursorAdapter {

    //These are used to decide which type of view to be inflated in newView
    private final int VIEW_TYPE_TODAY = 0;
    private final int VIEW_TYPE_FUTURE_DAY = 1;

    private static int count = 0;

    //In Two pane UI for tablets we will not show today's weather as a
    //special case
    private boolean isTodayViewSpecial;


    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }


    // Helper class for view holder, each view will have one object of this type.
    // While using ListView with adapter, the newView method is not called as many
    // times as there are list items, because the adapter intelligently recycles views
    // that are outside the screen.
    // But when an older view is being reused the bindView method is called. So if
    // we use methods like findViewById there then it will slow down the performance.
    //
    // Hence we find the views inside view holder and instantiating view holder object
    // inside newView.

    private static class ViewHolder
    {
        private ImageView iconView;
        private TextView dateTextView;
        private TextView descriptionView;
        private TextView temperatureMaxView;
        private TextView temperatureMinView;

        public ViewHolder(View view)
        {
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateTextView = (TextView) view.findViewById(R.id.list_item_date_textview);
            descriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            temperatureMaxView = (TextView) view.findViewById(R.id.list_item_high_textview);
            temperatureMinView = (TextView) view.findViewById(R.id.list_item_low_textview);
        }
    }

    //Setter to be used by main activity
    public void setTodayViewSpecial(boolean value)
    {
        isTodayViewSpecial = value;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && isTodayViewSpecial) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    /*
     Remember that these views are reused as needed.
    */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        count++;

        //Find the layout type (today/future day)
        int layoutType = getItemViewType(cursor.getPosition());
        int layoutId = -1;

        //Determine layout id from view type
        if(layoutType == VIEW_TYPE_TODAY)
        {
            layoutId = R.layout.list_item_forecast_today;
        }
        else
        {
            layoutId = R.layout.list_item_forecast;
        }

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);

        //Create view holder for this view
        ViewHolder viewHolder = new ViewHolder(view);

        //Save it inside view
        view.setTag(viewHolder);

        return view;
    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // our view is pretty simple here --- just a text view
        // we'll keep the UI functional with a simple (and slow!) binding.

        //TextView tv = (TextView)view;
        //tv.setText(convertCursorRowToUXFormat(cursor));

        //Get the associated view holder object
        ViewHolder viewHolder = (ViewHolder)view.getTag();

        //Find the layout type (today/future day)
        int layoutType = getItemViewType(cursor.getPosition());
        //Get the weather id for weather condition
        int weatherId = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);


        if(layoutType == VIEW_TYPE_TODAY) {
            int weatherIconId = Utility.getArtResourceForWeatherCondition(weatherId);
            //Set the source for this image view
            viewHolder.iconView.setImageResource(weatherIconId);
        }
        else {
            int weatherIconId = Utility.getIconResourceForWeatherCondition(weatherId);
            //Set the source for this image view
            viewHolder.iconView.setImageResource(weatherIconId);
        }



        //Find date from the cursor
        long dateInMillis = cursor.getLong(ForecastFragment.COL_WEATHER_DATE);

        //In two pane UI don't date with today
        //Set the date in friendly format
        if(isTodayViewSpecial)
        {
            viewHolder.dateTextView.setText(Utility.getFriendlyDayString(context, dateInMillis));
        }
        else
        {
            viewHolder.dateTextView.setText(Utility.getDayName(context, dateInMillis));
        }

        //Find the forecast(description) from the cursor
        String description = cursor.getString(ForecastFragment.COL_WEATHER_DESC);

        //Set the text for this view
        viewHolder.descriptionView.setText(description);


        //For the temperature we need user preference for unit(metric/imperial)
        boolean isMetric = Utility.isMetric(context);

        //Find the high(Max) temperature from cursor
        double temperatureMax = cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP);

        //Set the max temperature
        viewHolder.temperatureMaxView.setText(Utility.formatTemperature(context,temperatureMax,isMetric));

        //Find the low(Min) temperature from cursor
        double temperatureMin = cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP);

        //Set the min temperature
        viewHolder.temperatureMinView.setText(Utility.formatTemperature(context,temperatureMin,isMetric));

    }
}