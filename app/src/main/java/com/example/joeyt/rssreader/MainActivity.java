package com.example.joeyt.rssreader;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Bundle;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TwoLineListItem;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ListActivity {

    private RSSListAdapter rssList; // Custom list adapter that fits the RSS data into the list.
    private EditText rssURL; // URL edit text field.
    private Handler mHandler; // Handler used to post runnables to the UI thread.
    private RSSWorker bgThread; // Currently running background network thread.
    public static final int SNIPPET_LENGTH = 90; // Take this many chars from the front of the description.

    // Keys used for data in the onSaveInstanceState() method.
    public static final String STRINGS_KEY = "strings";
    public static final String SELECTION_KEY = "selection";
    public static final String URL_KEY = "url";

    // Called when the activity starts up.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Install our custom RSSListAdapter.
        List<RSSItem> items = new ArrayList<RSSItem>();
        rssList = new RSSListAdapter(this, items);
        getListView().setAdapter(rssList);
        rssURL = (EditText)findViewById(R.id.urltext);
        Button download = (Button)findViewById(R.id.download);
        download.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doRSS(rssURL.getText());
            }
        });
        // Need one of these to post things back to the UI thread.
        mHandler = new Handler();
    }

    // Hold RSSItems and display them
    private class RSSListAdapter extends ArrayAdapter<RSSItem> {
        private LayoutInflater inflater;
        public RSSListAdapter(Context context, List<RSSItem> objects) {
            super(context, 0, objects);
            inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        // Render a particular item for the screen from the list
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TwoLineListItem view;
            // Make a new view for the list, or reuse the old view
            if (convertView == null) {
                view = (TwoLineListItem) inflater.inflate(android.R.layout.simple_list_item_2,
                        null);
            }
            else {
                view = (TwoLineListItem) convertView;
            }
            RSSItem item = this.getItem(position);
            // Set the item title and description into the view.
            view.getText1().setText(item.getTitle());
            String descr = item.getDescription().toString();
            descr = removeTags(descr);
            view.getText2().setText(descr.substring(0, Math.min(descr.length(), SNIPPET_LENGTH)));
            return view;
        }
    }

    // Code to strip out <tag>s
    public String removeTags(String str) {
        str = str.replaceAll("<.*?>", " ");
        str = str.replaceAll("\\s+", " ");
        return str;
    }

    // Called when clicking an item in the list. Starts an activity to open the URL for that item.
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        RSSItem item = rssList.getItem(position);
        // Creates and starts an intent to open the item.link url.
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getLink().toString()));
        startActivity(intent);
    }

    // Resets the output UI to make the list text empty.
    public void resetUI() {
        // Reset the list to be empty.
        List<RSSItem> items = new ArrayList<RSSItem>();
        rssList = new RSSListAdapter(this, items);
        getListView().setAdapter(rssList);
        rssURL.requestFocus();
    }

    // Sets the current active background thread
    public synchronized void setCurrentWorker(RSSWorker worker) {
        if (bgThread != null) bgThread.interrupt();
        bgThread = worker;
    }

    // Once there is a RSS URL, start the thread to download the RSS feed
    private void doRSS(CharSequence rssUrl) {
        RSSWorker worker = new RSSWorker(rssUrl);
        setCurrentWorker(worker);
        resetUI();
        worker.start();
    }

    // Post RSS Items to the UI via mHandler
    private class ItemAdder implements Runnable {
        RSSItem item;
        ItemAdder(RSSItem item) {
            this.item = item;
        }
        public void run() {
            rssList.add(item);
        }
    }

    // Thread takes in the RSS URL, downloads the data and then parses it
    private class RSSWorker extends Thread {
        private CharSequence mUrl;
        public RSSWorker(CharSequence url) {
            mUrl = url;
        }
        @Override
        public void run() {
            try {
                // Standard code to make an HTTP connection.
                URL url = new URL(mUrl.toString());
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(10000);
                connection.connect();
                InputStream in = connection.getInputStream();
                parseRSS(in, rssList);
            } catch (Exception e) {
                e.getMessage();
            }
        }
    }

    // Called to save the current state
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Make a List of all the RSSItem data for saving
        int count = rssList.getCount();
        // Save the items as a list of CharSequence objects
        ArrayList<CharSequence> strings = new ArrayList<CharSequence>();
        for (int i = 0; i < count; i++) {
            RSSItem item = rssList.getItem(i);
            strings.add(item.getTitle());
            strings.add(item.getLink());
            strings.add(item.getDescription());
        }
        outState.putSerializable(STRINGS_KEY, strings);
        // Save current selection index
        if (getListView().hasFocus()) {
            outState.putInt(SELECTION_KEY, Integer.valueOf(getListView().getSelectedItemPosition()));
        }
        // Save URL
        outState.putString(URL_KEY, rssURL.getText().toString());
    }

    // Brings app to the previous state saved in onSaveInstanceState
    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        if (state == null) return;
        // Restore items from the list of CharSequence objects
        List<CharSequence> strings = (ArrayList<CharSequence>)state.getSerializable(STRINGS_KEY);
        List<RSSItem> items = new ArrayList<RSSItem>();
        for (int i = 0; i < strings.size(); i += 4) {
            items.add(new RSSItem(strings.get(i), strings.get(i + 1), strings.get(i + 2), strings.get(i + 3)));
        }
        // Reset the list view to show this data.
        rssList = new RSSListAdapter(this, items);
        getListView().setAdapter(rssList);
        // Restore selection
        if (state.containsKey(SELECTION_KEY)) {
            getListView().requestFocus(View.FOCUS_FORWARD);
            getListView().setSelection(state.getInt(SELECTION_KEY));
        }
        // Restore URL
        rssURL.setText(state.getCharSequence(URL_KEY));
    }

    // Does the RSS parsing and posts RSS items to the UI. Uses Android's XmlPullParser facility.
    void parseRSS(InputStream in, RSSListAdapter adapter) throws IOException,
            XmlPullParserException {
        XmlPullParser xpp = Xml.newPullParser();
        xpp.setInput(in, null);  // null = default to UTF-8
        int eventType;
        String title = "";
        String link = "";
        String description = "";
        String image = "";
        eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tag = xpp.getName();
                if (tag.equals("item")) {
                    title = link = description = image = "";
                } else if (tag.equals("title")) {
                    xpp.next(); // Skip to next element assumes text is directly inside the tag
                    title = xpp.getText();
                } else if (tag.equals("link")) {
                    xpp.next();
                    link = xpp.getText();
                } else if (tag.equals("description")) {
                    xpp.next();
                    description = xpp.getText();
                }
                else if (tag.equals("image")){
                    xpp.next();
                    image = xpp.getText();
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                // If everything is there, post it back to the UI
                String tag = xpp.getName();
                if (tag.equals("item")) {
                    RSSItem item = new RSSItem(title, link, description, image);
                    mHandler.post(new ItemAdder(item));
                }
            }
            eventType = xpp.next();
        }
    }
}