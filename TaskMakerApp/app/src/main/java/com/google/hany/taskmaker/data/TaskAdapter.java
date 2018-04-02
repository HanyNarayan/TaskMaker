package com.google.hany.taskmaker.data;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.hany.taskmaker.R;
import com.google.hany.taskmaker.views.TaskTitleView;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskHolder> {

    /* Callback for list item click events */
    public interface OnItemClickListener {
        void onItemClick(View v, int position);

        void onItemToggled(boolean active, int position);
    }

    /* ViewHolder for each task item */
    public class TaskHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TaskTitleView nameView;
        public TextView dateView;
        public ImageView priorityView;
        public CheckBox checkBox;

        public TaskHolder(View itemView) {
            super(itemView);

            nameView = (TaskTitleView) itemView.findViewById(R.id.text_description);
            dateView = (TextView) itemView.findViewById(R.id.text_date);
            priorityView = (ImageView) itemView.findViewById(R.id.priority);
            checkBox = (CheckBox) itemView.findViewById(R.id.checkbox);

            itemView.setOnClickListener(this);
            checkBox.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v == checkBox) {
                completionToggled(this);
            } else {
                postItemClick(this);
            }
        }
    }

    private Cursor mCursor;
    private OnItemClickListener mOnItemClickListener;
    private Context mContext;

    public TaskAdapter(Cursor cursor) {
        mCursor = cursor;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    private void completionToggled(TaskHolder holder) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemToggled(holder.checkBox.isChecked(), holder.getAdapterPosition());
        }
    }

    private void postItemClick(TaskHolder holder) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemClick(holder.itemView, holder.getAdapterPosition());
        }
    }

    @Override
    public TaskHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View itemView = LayoutInflater.from(mContext)
                .inflate(R.layout.list_item_task, parent, false);

        return new TaskHolder(itemView);
    }

    @Override
    public void onBindViewHolder(TaskHolder holder, int position) {


        mCursor.moveToPosition(position);

        final long id = DatabaseContract.getColumnLong(mCursor, DatabaseContract.TaskColumns._ID);
        String description = DatabaseContract.getColumnString(mCursor, DatabaseContract.TaskColumns.DESCRIPTION);
        int priority = DatabaseContract.getColumnInt(mCursor, DatabaseContract.TaskColumns.IS_PRIORITY);
        int iscomplete = DatabaseContract.getColumnInt(mCursor, DatabaseContract.TaskColumns.IS_COMPLETE);
        long duedate = DatabaseContract.getColumnLong(mCursor, DatabaseContract.TaskColumns.DUE_DATE);

        holder.itemView.setTag(id);

        holder.nameView.setText(description);

        if (priority == 0) {
            holder.priorityView.setImageResource(R.drawable.ic_not_priority);
        } else {
            holder.priorityView.setImageResource(R.drawable.ic_priority);
        }

        if (iscomplete == 1) {
            holder.checkBox.setChecked(true);
        } else {
            holder.checkBox.setChecked(false);
        }

        Task newTask = new Task(mCursor);

        if (newTask.hasDueDate()) {
            holder.dateView.setVisibility(View.VISIBLE);
            String date = (String) DateUtils.getRelativeTimeSpanString(mContext,duedate,false);
            holder.dateView.setText(date);
            Log.i("DueDate", date);
        } else
            holder.dateView.setText(null);
            holder.nameView.setState(0);

        if(iscomplete==1)
        {
            holder.nameView.setState(1);
            holder.nameView.setPaintFlags(holder.nameView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        else if (newTask.hasDueDate())
        {
            long currentTime = System.currentTimeMillis();


            if(currentTime > duedate)
                holder.nameView.setState(2);
            else
                holder.nameView.setState(0);
        }
        else
            holder.nameView.setState(0);

    }

    @Override
    public int getItemCount() {
        return (mCursor != null) ? mCursor.getCount() : 0;
    }

    /**
     * Retrieve a {@link Task} for the data at the given position.
     *
     * @param position Adapter item position.
     *
     * @return A new {@link Task} filled with the position's attributes.
     */
    public Task getItem(int position) {
        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("Invalid item position requested");
        }

        return new Task(mCursor);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).id;
    }

    public void swapCursor(Cursor cursor) {
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = cursor;
        notifyDataSetChanged();
    }
}
