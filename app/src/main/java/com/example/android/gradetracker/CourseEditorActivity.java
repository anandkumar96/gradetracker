package com.example.android.gradetracker;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.android.gradetracker.data.AssignmentContract;
import com.example.android.gradetracker.data.AssignmentDbHelper;

import java.util.ArrayList;

/**
 * Created by Anand Kumar on 12/29/2016.
 *
 * This class contains the details for the Course Editor
 */

public class CourseEditorActivity extends AppCompatActivity
{
    // Editor Fields
    private EditText nameEditText;
    private EditText cat1NameEditText;
    private EditText cat1WeightEditText;
    private EditText cat2NameEditText;
    private EditText cat2WeightEditText;
    private EditText cat3NameEditText;
    private EditText cat3WeightEditText;
    private EditText cat4NameEditText;
    private EditText cat4WeightEditText;

    private String courseTitle;
    private ArrayList<String> courseDetails;
    private boolean currentCourseHasChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_editor);
        courseDetails = getIntent().getStringArrayListExtra("currentCourseAndCategories");

        nameEditText = (EditText) findViewById(R.id.CourseName_EditText);
        cat1NameEditText = (EditText) findViewById(R.id.Category1Name_EditText);
        cat1WeightEditText = (EditText) findViewById(R.id.Category1Weight_EditText);
        cat2NameEditText = (EditText) findViewById(R.id.Category2Name_EditText);
        cat2WeightEditText = (EditText) findViewById(R.id.Category2Weight_EditText);
        cat3NameEditText = (EditText) findViewById(R.id.Category3Name_EditText);
        cat3WeightEditText = (EditText) findViewById(R.id.Category3Weight_EditText);
        cat4NameEditText = (EditText) findViewById(R.id.Category4Name_EditText);
        cat4WeightEditText = (EditText) findViewById(R.id.Category4Weight_EditText);

        nameEditText.setOnTouchListener(mTouchListener);
        cat1NameEditText.setOnTouchListener(mTouchListener);
        cat1WeightEditText.setOnTouchListener(mTouchListener);
        cat2NameEditText.setOnTouchListener(mTouchListener);
        cat2WeightEditText.setOnTouchListener(mTouchListener);
        cat3NameEditText.setOnTouchListener(mTouchListener);
        cat3WeightEditText.setOnTouchListener(mTouchListener);
        cat4NameEditText.setOnTouchListener(mTouchListener);
        cat4WeightEditText.setOnTouchListener(mTouchListener);

        currentCourseHasChanged = false;

        if (courseDetails != null)
        {
            // the course already exists
            setTitle(R.string.editor_activity_title_edit_course);
            courseTitle = courseDetails.get(0);
            courseDetails.remove(0); // now it's a list of just categories
            Log.e("CourseEditorActv", "courseDetails != null; courseTitle = " + courseTitle);
            populateFields();
        }
        else
        {
            setTitle(R.string.editor_activity_title_new_course);
        }
    }

    // Handling the end of the activity: gather course info to pass to CourseCatalogActivity
    private void finishThisActivity()
    {
        Intent intent = new Intent(CourseEditorActivity.this, CourseCatalogActivity.class);
        startActivity(intent);
    }

    // Listen for user making edits
    private View.OnTouchListener mTouchListener = new View.OnTouchListener()
    {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent)
        {
            currentCourseHasChanged = true;
            return false;
        }
    };


    // save the course details
    private void saveCourse()
    {
        String name = nameEditText.getText().toString().trim();
        String cat1Name = cat1NameEditText.getText().toString().trim();
        String cat1Weight = cat1WeightEditText.getText().toString().trim();
        String cat2Name = cat2NameEditText.getText().toString().trim();
        String cat2Weight = cat2WeightEditText.getText().toString().trim();
        String cat3Name = cat3NameEditText.getText().toString().trim();
        String cat3Weight = cat3WeightEditText.getText().toString().trim();
        String cat4Name = cat4NameEditText.getText().toString().trim();
        String cat4Weight = cat4WeightEditText.getText().toString().trim();

        // return if user has made no changes
        if (name.isEmpty()  && cat1Name.isEmpty() && cat2Name.isEmpty() && cat3Name.isEmpty() &&
                cat3Weight.isEmpty() && cat4Name.isEmpty() && cat4Weight.isEmpty())
        {
            Log.d("CourseEditorActv", "saveCourse() return b/c user made no changes");
            Toast.makeText(this, getString(R.string.empty_course),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!cat1Weight.isEmpty() && cat1Name.isEmpty() ||
                !cat2Weight.isEmpty() && cat2Name.isEmpty() ||
                !cat3Weight.isEmpty() && cat3Name.isEmpty() ||
                !cat4Weight.isEmpty() && cat4Name.isEmpty())
        {
            Toast.makeText(this, getString(R.string.unnamed_weight),
                    Toast.LENGTH_SHORT).show();
            Log.d("CourseEditorActv", "saveCourse() return b/c unnamed weight");
            return;
        }

        // check for total weight == 100%
        double totalWeight = 0.0;
        if (!cat1Weight.isEmpty()) totalWeight += Double.parseDouble(cat1Weight);
        if (!cat2Weight.isEmpty()) totalWeight += Double.parseDouble(cat2Weight);
        if (!cat3Weight.isEmpty()) totalWeight += Double.parseDouble(cat3Weight);
        if (!cat4Weight.isEmpty()) totalWeight += Double.parseDouble(cat4Weight);


        if (totalWeight != 100.0)
        {
            Toast.makeText(this, getString(R.string.invalid_total_weight),
                    Toast.LENGTH_SHORT).show();
            Log.e("CourseEditorActv", "saveCourse() return b/c weight total != 100%");
            return;
        }

        ArrayList<String> categories = new ArrayList<String>();
        categories.add(cat1Name + getString(R.string.categoryweight_separator) + cat1Weight);
        categories.add(cat2Name + getString(R.string.categoryweight_separator) + cat2Weight);
        categories.add(cat3Name + getString(R.string.categoryweight_separator) + cat3Weight);
        categories.add(cat4Name + getString(R.string.categoryweight_separator) + cat4Weight);

        for (int i = 0; i < categories.size(); i++)
            Log.d("CourseEditorActivity", "categories[" + i + "] = " + categories.get(i));

        AssignmentDbHelper dbHelper = new AssignmentDbHelper(this);
        ContentValues values = new ContentValues();
        int rowsChanged = 0;

        for (int i = 0; i < categories.size(); i++)
        {
            values.put(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_COURSE, name);
            values.put(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_CATEGORY, categories.get(i));


            if (courseDetails == null)
            {
                getContentResolver().insert(AssignmentContract.AssignmentEntry.FULLTABLE_URI, values);
                continue;
            }


            String selection = AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_COURSE + "=? AND " +
                    AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_CATEGORY + "=?";

            String[] selectionArgs = new String[] {courseTitle, courseDetails.get(i)};

            rowsChanged += getContentResolver().update(AssignmentContract.AssignmentEntry.FULLTABLE_URI,
                    values, selection, selectionArgs);
        }
        courseTitle = name;
        courseDetails = categories;
        finishThisActivity();
        if (rowsChanged > 0)
            Toast.makeText(getApplicationContext(),
                    R.string.toast_courseUpdateSuccess, Toast.LENGTH_SHORT).show();
    }



    // Inflate the menu options from the res/menu/menu_editor.xml file.
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_courseeditor, menu);
        return true;
    }

    //onOptionsItemSelected for selecting menu options
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId())
        {
            // Respond to a click on the "Save" menu option
            case R.id.action_savecourse:
                saveCourse();
                return true;


            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the assignment hasn't changed, continue with navigating up to parent activity
                // which is the assignment catalog.
                if (!currentCourseHasChanged)
                {
                    //NavUtils.navigateUpFromSameTask(CourseEditorActivity.this);
                    finishThisActivity();
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener = new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        // User clicked "Discard" button, navigate to parent activity.
                        //NavUtils.navigateUpFromSameTask(CourseEditorActivity.this);
                        finishThisActivity();
                    }
                };

                // Show a dialog that notifies the user they have unsaved changes
                // parameter is how to respond on positive button press
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Populate Editor Fields (if not creating a new course)
    private void populateFields()
    {
        AssignmentDbHelper dbHelper = new AssignmentDbHelper(this);
        if (courseDetails == null) return;
        Log.e("CourseEditorActv", "nameEditText.getCurrentTextColor() =" + nameEditText.getCurrentTextColor());
        Log.e("CourseEditorActv", "course title =" + courseTitle);
        nameEditText.setText(courseTitle);

        if (courseDetails.size() < 1) return;
        String category1 = courseDetails.get(0);
        cat1NameEditText.setText(dbHelper.getNameFromCategory(category1, getApplicationContext()));
        cat1WeightEditText.setText(dbHelper.getWeightFromCategory(category1, getApplicationContext()));

        if (courseDetails.size() < 2) return;
        String category2 = courseDetails.get(1);
        cat2NameEditText.setText(dbHelper.getNameFromCategory(category2, getApplicationContext()));
        cat2WeightEditText.setText(dbHelper.getWeightFromCategory(category2, getApplicationContext()));

        if (courseDetails.size() < 3) return;
        String category3 = courseDetails.get(2);
        cat3NameEditText.setText(dbHelper.getNameFromCategory(category3, getApplicationContext()));
        cat3WeightEditText.setText(dbHelper.getWeightFromCategory(category3, getApplicationContext()));

        if (courseDetails.size() < 4) return;
        String category4 = courseDetails.get(3);
        cat4NameEditText.setText(dbHelper.getNameFromCategory(category4, getApplicationContext()));
        cat4WeightEditText.setText(dbHelper.getWeightFromCategory(category4, getApplicationContext()));
    }

    // Warn the user when pressing the up button while editing
    @Override
    public void onBackPressed()
    {
        // If the assignment hasn't changed, continue with handling back button press
        if (!currentCourseHasChanged)
        {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener = new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                // User clicked "Discard" button, close the current activity.
                finishThisActivity();
            }
        };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }


    /* User warning when exiting while editing:::
     * Create an AlertDialog.Builder and set the message, and click listeners
     * for the positive and negative buttons on the dialog.
     * PARAMETER: clicklistener of how to handle positive button
     */
    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);

        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener()
        {
            // User clicked the "Keep editing" button, so dismiss the dialog
            // and continue editing the course.
            public void onClick(DialogInterface dialog, int id)
            {
                if (dialog != null)   dialog.dismiss();
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}
