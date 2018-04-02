package com.google.hany.taskmaker.data;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import static com.google.hany.taskmaker.data.DatabaseContract.TABLE_TASKS;

public class TaskProvider extends ContentProvider {
    private static final String TAG = TaskProvider.class.getSimpleName();

    private static final int CLEANUP_JOB_ID = 43;

    private static final int TASKS = 100;
    private static final int TASKS_WITH_ID = 101;

    private TaskDbHelper mDbHelper;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        // content://com.google.developer.taskmaker/tasks
        sUriMatcher.addURI(DatabaseContract.CONTENT_AUTHORITY,
                TABLE_TASKS,
                TASKS);

        // content://com.google.developer.taskmaker/tasks/id
        sUriMatcher.addURI(DatabaseContract.CONTENT_AUTHORITY,
                TABLE_TASKS + "/#",
                TASKS_WITH_ID);

    }

    @Override
    public boolean onCreate() {
        mDbHelper = new TaskDbHelper(getContext());
        manageCleanupJob();
        return true;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null; /* Not used */
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {


        // Getting access to underlying database (read-only for query)
        final SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Write URI match code and set a variable to return a Cursor
        int match = sUriMatcher.match(uri);
        Cursor retCursor;

        switch (match) {

            case TASKS:
                break;
            case TASKS_WITH_ID:
                long id = ContentUris.parseId(uri);
                selection = String.format("%s = ?", DatabaseContract.TaskColumns._ID);
                selectionArgs = new String[]{String.valueOf(id)};
                break;
            // Default exception
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        retCursor = db.query(DatabaseContract.TABLE_TASKS,projection,selection,selectionArgs,null,null,sortOrder);

        // Set a notification URI on the Cursor and return that Cursor
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the desired Cursor
        return retCursor;

    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Write URI matching code to identify the match for the tasks directory
        int match = sUriMatcher.match(uri);
        Uri returnUri; // URI to be returned

        switch (match) {
            case TASKS:
                // Inserting values into tasks table
                long id = db.insert(DatabaseContract.TABLE_TASKS, null, values);
                if ( id > 0 ) {
                    returnUri = ContentUris.withAppendedId(DatabaseContract.CONTENT_URI, id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Notify the resolver if the uri has been changed, and return the newly inserted URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return constructed uri (this points to the newly inserted row of data)
        return returnUri;

    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Write URI matching code to identify the match for the tasks directory
        int match = sUriMatcher.match(uri);
        int idl =0;
        switch (match) {

            case TASKS_WITH_ID:
                long id = ContentUris.parseId(uri);
                selection = String.format("%s = ?", DatabaseContract.TaskColumns._ID);
                selectionArgs = new String[]{String.valueOf(id)};
                idl = db.update(TABLE_TASKS,values,selection,selectionArgs);
                if ( idl == 0 ) {
                    throw new android.database.SQLException("Failed to update row into " + uri);
                }
                break;
            // Default case throws an UnsupportedOperationException
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Notify the resolver if the uri has been changed, and return the newly inserted URI
        getContext().getContentResolver().notifyChange(uri, null);

        return idl;


}

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        switch (sUriMatcher.match(uri)) {
            case TASKS:
                //Rows aren't counted with null selection
                selection = (selection == null) ? "1" : selection;
                break;
            case TASKS_WITH_ID:
                long id = ContentUris.parseId(uri);
                selection = String.format("%s = ?", DatabaseContract.TaskColumns._ID);
                selectionArgs = new String[]{String.valueOf(id)};
                break;
            default:
                throw new IllegalArgumentException("Illegal delete URI");
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count = db.delete(TABLE_TASKS, selection, selectionArgs);

        if (count > 0) {
            //Notify observers of the change
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    /* Initiate a periodic job to clear out completed items */
    private void manageCleanupJob() {
        Log.d(TAG, "Scheduling cleanup job");
        JobScheduler jobScheduler = (JobScheduler) getContext()
                .getSystemService(Context.JOB_SCHEDULER_SERVICE);

        //Run the job after every hour
        long jobInterval = 3600000L;
        //changed from 900000L to 3600000L to implement deletion on 1hour

        ComponentName jobService = new ComponentName(getContext(), CleanupJobService.class);
        JobInfo task = new JobInfo.Builder(CLEANUP_JOB_ID, jobService)
                .setPeriodic(jobInterval)
                .setPersisted(true)
                .build();

        if (jobScheduler.schedule(task) != JobScheduler.RESULT_SUCCESS) {
            Log.w(TAG, "Unable to schedule cleanup job");
        }
    }
}
