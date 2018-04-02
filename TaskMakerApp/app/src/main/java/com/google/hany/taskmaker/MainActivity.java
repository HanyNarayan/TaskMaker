package com.google.hany.taskmaker;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.hany.taskmaker.data.DatabaseContract;
import com.google.hany.taskmaker.data.TaskAdapter;


public class MainActivity extends AppCompatActivity implements
        TaskAdapter.OnItemClickListener,
        View.OnClickListener,LoaderManager.LoaderCallbacks<Cursor>,SharedPreferences.OnSharedPreferenceChangeListener {

    private TaskAdapter mAdapter;
    private static final int TASK_LOADER_ID = 0;
    private static String SORT_ORDER = null;
    private static final String TAG = MainActivity.class.getSimpleName();
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab);
        setSupportActionBar(toolbar);

        fab.setOnClickListener(this);
        mAdapter = new TaskAdapter(null);
        mAdapter.setOnItemClickListener(this);

        // setting vies in recycler view
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        getSupportLoaderManager().initLoader(TASK_LOADER_ID, null, this);
        // Shared Preference for SORT ORDER
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String DefSP = sharedPreferences.getString(getString(R.string.pref_sortBy_key), getString(R.string.pref_sortBy_default));
        SORT_ORDER = DefSP.equals("default") ? DatabaseContract.DEFAULT_SORT : DatabaseContract.DATE_SORT;
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();

        // re-queries for all tasks
        getSupportLoaderManager().restartLoader(TASK_LOADER_ID, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* Click events in Floating Action Button */
    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, AddTaskActivity.class);
        startActivity(intent);
    }

    /* Click events in RecyclerView items */
    @Override
    public void onItemClick(View v, int position) {

        Cursor mCursor =  getContentResolver().query(DatabaseContract.CONTENT_URI,null,null,null,SORT_ORDER);
        mCursor.moveToPosition(position);
        //getting data from cursor
        final long id = DatabaseContract.getColumnLong(mCursor,DatabaseContract.TaskColumns._ID);
        Uri uri = ContentUris.withAppendedId(DatabaseContract.CONTENT_URI,id);
        Intent intent = new Intent(this, TaskDetailActivity.class);
        //setting Uri in Data
        intent.setData(uri);
        startActivity(intent);
    }

    /* Click events on RecyclerView item checkboxes */
    @Override
    public void onItemToggled(boolean active, int position) {

        Cursor mCursor = getContentResolver().query(DatabaseContract.CONTENT_URI,null,null,null,SORT_ORDER);
        mCursor.moveToPosition(position);
        //getting data from cursor
        final long id =  DatabaseContract.getColumnLong(mCursor,DatabaseContract.TaskColumns._ID);
        String description =DatabaseContract.getColumnString(mCursor,DatabaseContract.TaskColumns.DESCRIPTION);
        int priority = DatabaseContract.getColumnInt(mCursor,DatabaseContract.TaskColumns.IS_PRIORITY);
        long dueDate = DatabaseContract.getColumnLong(mCursor,DatabaseContract.TaskColumns.DUE_DATE);
        //setting content values for update
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseContract.TaskColumns.IS_PRIORITY,priority);
        contentValues.put(DatabaseContract.TaskColumns.DESCRIPTION,description);
        contentValues.put(DatabaseContract.TaskColumns._ID,id);
        contentValues.put(DatabaseContract.TaskColumns.IS_COMPLETE,active);
        contentValues.put(DatabaseContract.TaskColumns.DUE_DATE,dueDate);

        Uri uri = ContentUris.withAppendedId(DatabaseContract.CONTENT_URI,id);
        int rows = getContentResolver().update(uri,contentValues,null,null);
        Log.i("Row Updated", String.valueOf(rows));
//        TaskUpdateService.updateTask(this,uri,contentValues);
        //publish change
        getSupportLoaderManager().restartLoader(TASK_LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        return new AsyncTaskLoader<Cursor>(this) {

            // Initialize a Cursor, this will hold all the task data
            Cursor mTaskData = null;

            // onStartLoading() is called when a loader first starts loading data
            @Override
            protected void onStartLoading() {
                if (mTaskData != null) {
                    // Delivers any previously loaded data immediately
                    deliverResult(mTaskData);
                } else {
                    // Force a new load
                    forceLoad();
                }
            }

            // loadInBackground() performs asynchronous loading of data
            @Override
            public Cursor loadInBackground() {
                // Will implement to load data

                // Query and load all task data in the background; sort by SORT ORDER
                try {
                    return getContentResolver().query(DatabaseContract.CONTENT_URI,
                            null,
                            null,
                            null,
                            SORT_ORDER);

                } catch (Exception e) {
                    Log.e(TAG, "Failed to asynchronously load data.");
                    e.printStackTrace();
                    return null;
                }
            }

            // deliverResult sends the result of the load, a Cursor, to the registered listener
            public void deliverResult(Cursor data) {
                mTaskData = data;
                super.deliverResult(data);
            }
        };

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        mAdapter.swapCursor(null);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String sortorder = sharedPreferences.getString(getString(R.string.pref_sortBy_key),
                getString(R.string.pref_sortBy_default));

        switch (sortorder)
        {
            case "default":
            SORT_ORDER = DatabaseContract.DEFAULT_SORT;
                break;

            case "due":
            SORT_ORDER = DatabaseContract.DATE_SORT;
                break;

            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister VisualizerActivity as an OnPreferenceChangedListener to avoid any memory leaks.
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
