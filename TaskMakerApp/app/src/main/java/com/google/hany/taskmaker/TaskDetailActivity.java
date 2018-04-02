package com.google.hany.taskmaker;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.hany.taskmaker.data.DatabaseContract;
import com.google.hany.taskmaker.data.Task;
import com.google.hany.taskmaker.data.TaskUpdateService;
import com.google.hany.taskmaker.views.DatePickerFragment;
import com.google.hany.taskmaker.views.TaskTitleView;

import java.util.Calendar;

import static com.google.hany.taskmaker.R.id.action_delete;
import static com.google.hany.taskmaker.R.id.action_reminder;
import static com.google.hany.taskmaker.reminders.AlarmScheduler.scheduleAlarm;

public class TaskDetailActivity extends AppCompatActivity implements
        DatePickerDialog.OnDateSetListener {

    TaskTitleView nameView;
    TextView Duedate;
    ImageView Priority;

    String description;
    int priority,iscomplete;
    long duedate;
    Uri taskUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Task must be passed to this activity as a valid provider Uri
        taskUri = getIntent().getData();
        setContentView(R.layout.activity_detail_task);

        nameView = (TaskTitleView) findViewById(R.id.text_description);
        Duedate= (TextView)findViewById(R.id.duedate);
        Priority = (ImageView)findViewById(R.id.priority);
        //TODO: Display attributes of the provided task in the UI


        Cursor mCursor = getContentResolver().query(taskUri,null,null,null,null);
        if(mCursor!= null) {
            mCursor.moveToFirst();

            description = DatabaseContract.getColumnString(mCursor,DatabaseContract.TaskColumns.DESCRIPTION);
            priority = DatabaseContract.getColumnInt(mCursor,DatabaseContract.TaskColumns.IS_PRIORITY);
            iscomplete = DatabaseContract.getColumnInt(mCursor,DatabaseContract.TaskColumns.IS_COMPLETE);
            duedate = DatabaseContract.getColumnLong(mCursor,DatabaseContract.TaskColumns.DUE_DATE);
       }
        nameView.setText(description);

        Task newTask = new Task(mCursor);

        if(newTask.hasDueDate())
        {
            Duedate.setVisibility(View.VISIBLE);
            String date = (String) DateUtils.getRelativeTimeSpanString(this,duedate,false);
            Duedate.setText(date);
        }
        else
            Duedate.setText(R.string.date_empty);

        if(priority==0)
        {
            Priority.setImageResource(R.drawable.ic_not_priority);

        }
        else
        {
            Priority.setImageResource(R.drawable.ic_priority);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_task_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()){
            case action_reminder:

                DatePickerFragment dialogFragment = new DatePickerFragment();
                dialogFragment.show(getSupportFragmentManager(), "datePicker");
                break;

            case action_delete:
                TaskUpdateService.deleteTask(this,taskUri);
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                break;

            default:break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {

        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 00);
        c.set(Calendar.SECOND, 0);
        //check alarm setting
        if(c.getTimeInMillis() > System.currentTimeMillis())
        {
            scheduleAlarm(this,c.getTimeInMillis(),taskUri);
        }
        else
        {
            Toast.makeText(this,R.string.alarm_error,Toast.LENGTH_LONG).show();
        }
    }
}
