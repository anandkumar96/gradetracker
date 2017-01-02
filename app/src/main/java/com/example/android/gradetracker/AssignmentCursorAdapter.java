package com.example.android.gradetracker;

/**
 * Created by Anand Kumar on 12/29/2016.
 *
 * This class is the CursorAdapter for the database
 */

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.android.gradetracker.data.AssignmentContract;
import com.example.android.gradetracker.data.AssignmentDbHelper;

/**
 * AssignmentCursorAdapter is an adapter for a list or grid view
 * that uses a Cursor of assignment data as its data source. This adapter knows
 * how to create list items for each row of assignment data in the Cursor
 */
public class AssignmentCursorAdapter extends CursorAdapter
{
    /* Constructor
     * Parameter Context: the context
     * Parameter Cursor: the cursor from which to get the data
     */
    public AssignmentCursorAdapter(Context context, Cursor c)
    {
        super(context, c, 0 /* flags */);
    }

    /* Inflate the new list item view
     * Parameter Context: app context
     * Parameter Cursor: the cursor from which to get the data
     * Parameter: ViewGroup: parent view group to which the new view is attached
     * Return: newly created list item view
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent)
    {
        return LayoutInflater.from(context).inflate(R.layout.assignmentlist_item, parent, false);
    }

    /* This method binds the assignment data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current assignment can be set on the name TextView
     * in the list item layout.
     *
     * Parameter: View: existing view returned from newView()
     * Parameter Context: app context
     * Parameter Cursor: the cursor from which to get the data
     * Return: newly created list item view
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor)
    {
        // Get the text views to update
        TextView assignmentNameTextView = (TextView) view.findViewById(R.id.AssignmentName_TextView);
        TextView assignmentCategoryTextView = (TextView) view.findViewById(R.id.AssignmentCategory_TextView);
        TextView assignmentScoreTextView = (TextView) view.findViewById(R.id.AssignmentScore_TextView);
        TextView assignmentMaxScoreTextView = (TextView) view.findViewById(R.id.AssignmentMaxScore_TextView);

        // Get the values from the cursor.
        String assignmentName = cursor.getString(
                cursor.getColumnIndexOrThrow(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_NAME));
        String assignmentCategory = cursor.getString(
                cursor.getColumnIndexOrThrow(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_CATEGORY));
        Double assignmentScore = cursor.getDouble(
                cursor.getColumnIndexOrThrow(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_SCORE));
        Double assignmentMaxScore = cursor.getDouble(
                cursor.getColumnIndexOrThrow(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_MAXSCORE));


        // Attach values to respective views.
        assignmentNameTextView.setText(assignmentName);
        AssignmentDbHelper dbHelper = new AssignmentDbHelper(context);
        assignmentCategoryTextView.setText(dbHelper.getNameFromCategory(assignmentCategory, context));

        if ((assignmentScore == null || assignmentScore == 0) &&
                assignmentMaxScore == null || assignmentMaxScore == 0)
        {
            TextView slashTextView = (TextView) view.findViewById(R.id.AssignmentScoreSlash_TextView);
            slashTextView.setVisibility(View.INVISIBLE);
            assignmentScoreTextView.setVisibility(View.INVISIBLE);
            assignmentMaxScoreTextView.setVisibility(View.INVISIBLE);
            return;
        }

        if (assignmentScore != null)
        {
            if (Math.floor(assignmentScore) == assignmentScore)
                assignmentScoreTextView.setText(String.valueOf(Integer.valueOf(assignmentScore.intValue())));
            else
                assignmentScoreTextView.setText(String.valueOf(assignmentScore));
        }
        else assignmentScoreTextView.setText(String.valueOf(0));

        if (assignmentMaxScore != null)
        {
            if (Math.floor(assignmentMaxScore) == assignmentMaxScore)
                assignmentMaxScoreTextView.setText(String.valueOf(Integer.valueOf(assignmentMaxScore.intValue())));
            else
                assignmentMaxScoreTextView.setText(String.valueOf(assignmentMaxScore));
        }
        else assignmentMaxScoreTextView.setText(String.valueOf(0));
    }
}