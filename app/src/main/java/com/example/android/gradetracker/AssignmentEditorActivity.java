package com.example.android.gradetracker;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import java.util.ArrayList;
import com.example.android.gradetracker.data.AssignmentContract;
import com.example.android.gradetracker.data.AssignmentDbHelper;



public class AssignmentEditorActivity extends AppCompatActivity
        implements android.app.LoaderManager.LoaderCallbacks<Cursor>
{
    // Editor Fields
    private EditText nameEditText;
    private EditText scoreEditText;
    private EditText maxScoreEditText;
    private Spinner categorySpinner;
    private String chosenCategory;

    // Current Course and Assignment details
    private static final int URI_LOADER = 0;
    private ArrayList<String> currentCourseDetails;
    private String currentCourseTitle;
    private Uri currentAssignmentUri; // current assignment
    private boolean currentAssignmentHasChanged; // flag


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment_editor);
        currentAssignmentHasChanged = false;


        // get associated assignment uri or course title string
        currentAssignmentUri = getIntent().getData();
        currentCourseDetails = getIntent().getStringArrayListExtra("currentCourseAndCategories");

        // currentCourseDetails is a list of {CourseTitle, Category1, Category2, ....}
        // Separate the title so the list only contains categories
        if (currentCourseDetails != null)
        {
            currentCourseTitle = currentCourseDetails.get(0);
            currentCourseDetails.remove(0);
        }

        // set title accordingly
        if (currentAssignmentUri == null)
        {
            setTitle(R.string.editor_activity_title_new_assignment);
            invalidateOptionsMenu(); // change menu to hide delete option for nonexistent assignment
        }
        else
            setTitle(R.string.editor_activity_title_edit_assignment);


        // Find all relevant views that we will need to read user input from
        nameEditText = (EditText) findViewById(R.id.AssignmentName_EditText);
        scoreEditText = (EditText) findViewById(R.id.AssignmentScore_EditText);
        maxScoreEditText = (EditText) findViewById(R.id.AssignmentMaxScore_EditText);
        categorySpinner = (Spinner) findViewById(R.id.AssignmentCategory_Spinner);

        // Listen for user making edits
        nameEditText.setOnTouchListener(mTouchListener);
        scoreEditText.setOnTouchListener(mTouchListener);
        maxScoreEditText.setOnTouchListener(mTouchListener);
        categorySpinner.setOnTouchListener(mTouchListener);

        // Set up the category spinner
        setupSpinner();


        //initialize the cursor loader
        getLoaderManager().initLoader(URI_LOADER, null, this);
    }

    // gather the updated course information to send back to the assignment catalog
    private void finishThisActivity()
    {
        ArrayList<String> currentCourseAndCategories = new ArrayList<String>();
        currentCourseAndCategories.add(currentCourseTitle);
        currentCourseAndCategories.addAll(currentCourseDetails);

        Intent intent = new Intent(AssignmentEditorActivity.this, AssignmentCatalogActivity.class);
        intent.putStringArrayListExtra("currentCourseAndCategories", currentCourseAndCategories);
        startActivity(intent);
    }


    @Override
    // called on invalidateOptionsMenu();
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);

        // If this is a new assignment, hide the "Delete" menu item.
        if (currentAssignmentUri == null)
        {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }


    // Listen for user making edits
    private View.OnTouchListener mTouchListener = new View.OnTouchListener()
    {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent)
        {
            currentAssignmentHasChanged = true;
            return false;
        }
    };


    // Set up dropdown spinner for assignment category
    private void setupSpinner()
    {
        AssignmentDbHelper dbHelper = new AssignmentDbHelper(this);
        ArrayList<String> categoryNames = new ArrayList<String>();
        for (int i = 0; i < currentCourseDetails.size(); i++)
            categoryNames.add(dbHelper.getNameFromCategory(currentCourseDetails.get(i), getApplicationContext()));

        // Create adapter for spinner. The list options are from the String arraylist it will use
        // the spinner will use the default layout
        ArrayAdapter<String> categorySpinnerAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_dropdown_item, categoryNames);

        // Specify dropdown layout style - simple list view with 1 item per line
        categorySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        categorySpinner.setAdapter(categorySpinnerAdapter);

        // Get the chosen value
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                chosenCategory = currentCourseDetails.get(position);
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                chosenCategory = null;
            }
        });
    }

    // Get user input from editor and save assignment into database.
    private void saveAssignment()
    {
        String name = nameEditText.getText().toString().trim();
        // currentCourseTitle is the course
        // chosenCategory is the category. variable is set in spinner's onOptionsItemSelected
        String scoreStr = scoreEditText.getText().toString();
        String maxScoreStr = maxScoreEditText.getText().toString();

        Log.d("AssignEditorActivity", "saveAssgn(): user input: name=" + name + " course=" + currentCourseTitle
                + " categ="+chosenCategory + " scoreStr="+scoreStr + " maxScoreStr="+maxScoreStr);

        // If user has made no changes to the editor, do not create a new assignment with default result
        if (name.isEmpty() && chosenCategory == currentCourseDetails.get(0) && scoreStr.isEmpty() && maxScoreStr.isEmpty())
        {
            Log.d("AssignEditorActivity", "user has made no edits");
            return;
        }

        Double score = 0.0;
        Double maxScore = 0.0;
        if (!scoreStr.isEmpty())
            score = Double.parseDouble(scoreStr);
        if (!maxScoreStr.isEmpty())
            maxScore = Double.parseDouble(maxScoreStr);

        System.out.println("\n\nSAVE ASSIGN: score = " + score + "     max = " + maxScore + "\n\n");
        Log.d("AssignEditorActivity", "saveAssgn(): \n\ndouble values score=" + score
                + " maxscore=" + maxScore + "\n\n");

        AssignmentDbHelper dbHelper = new AssignmentDbHelper(this);
        ContentValues values = new ContentValues();
        values.put(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_NAME, name);
        values.put(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_COURSE, currentCourseTitle);
        values.put(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_CATEGORY, chosenCategory);
        values.put(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_SCORE, score);
        values.put(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_MAXSCORE, maxScore);

        if (currentAssignmentUri == null)
        {
            Uri insertionResult = getContentResolver().insert(
                    AssignmentContract.AssignmentEntry.FULLTABLE_URI, values);

            if (insertionResult == null)
                Toast.makeText(getApplicationContext(),
                        R.string.toast_assignmentInsertionError, Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(getApplicationContext(),
                        R.string.toast_assignmentInsertionSuccess, Toast.LENGTH_SHORT).show();
        }
        else
        {
            int rowsChanged = getContentResolver().update(currentAssignmentUri, values, null, null);
            if (rowsChanged > 0)
                Toast.makeText(getApplicationContext(),
                    R.string.toast_assignmentUpdateSuccess, Toast.LENGTH_SHORT).show();
        }
    }


    // Perform a deletion of an assignment
    private void deleteAssignment()
    {
        if (currentAssignmentUri == null) return;
        int rowsDeleted = getContentResolver().delete(currentAssignmentUri, null, null);

        // Show a toast message depending on whether or not the delete was successful.
        if (rowsDeleted == 0)
            Toast.makeText(this, getString(R.string.editor_delete_assignment_failed),
                    Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, getString(R.string.editor_delete_assignment_successful),
                    Toast.LENGTH_SHORT).show();
    }



    // Inflate the menu options from the res/menu/menu_assignmenteditor.xml file.
    // This adds menu items to the app bar.
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_assignmenteditor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId())
        {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                saveAssignment();   // Save assignment to database
                finishThisActivity();  // exit the editor
                return true;


            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog(); // contains conditional finish statement
                return true;


            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the assignment hasn't been edited, continue with navigating up to parent
                // activity (which is the AssignmentCatalogActivity}.
                if (!currentAssignmentHasChanged)
                {
                    //NavUtils.navigateUpFromSameTask(AssignmentEditorActivity.this);
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
                        //NavUtils.navigateUpFromSameTask(AssignmentEditorActivity.this);
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

    @Override
    // gather all info needed for EditText fields
    public Loader<Cursor> onCreateLoader(int id, Bundle args)
    {
        if (currentAssignmentUri == null) return null;

        switch (id)
        {
            case URI_LOADER:
                String[] projection = {AssignmentContract.AssignmentEntry._ID,
                        AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_NAME,
                        AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_COURSE,
                        AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_CATEGORY,
                        AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_SCORE,
                        AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_MAXSCORE
                }; // array of columns to use.

                /// WHERE statement and sort order
                String selection = null;
                String[] selectionArgs = null;
                String sortOrder = null;

                return new android.content.CursorLoader(this, currentAssignmentUri,
                        projection, selection, selectionArgs, sortOrder);
        }
        return null;
    }

    @Override
    // Set the EditText fields and gender Spinner with the current assignment info
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
    {
        if (!cursor.moveToFirst()) return;

        nameEditText.setText(cursor.getString(cursor.getColumnIndexOrThrow(
                AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_NAME)));
        String assignmentCategory = cursor.getString(cursor.getColumnIndexOrThrow(
                AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_CATEGORY));
        scoreEditText.setText(cursor.getString(cursor.getColumnIndexOrThrow(
                AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_SCORE)));
        maxScoreEditText.setText(cursor.getString(cursor.getColumnIndexOrThrow(
                AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_MAXSCORE)));

        int spinnerIndex = currentCourseDetails.indexOf(assignmentCategory);
        if (spinnerIndex == -1) throw new IllegalArgumentException("Chosen category does not exist");
        else categorySpinner.setSelection(spinnerIndex);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {
        nameEditText = null;
        scoreEditText = null;
        maxScoreEditText = null;
        categorySpinner = null;
    }


    // Warn the user when pressing the up button while editing
    @Override
    public void onBackPressed()
    {
        // If the assignment hasn't changed, continue with handling back button press
        if (!currentAssignmentHasChanged)
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


    // User warning when exiting while editing:
    // Create an AlertDialog.Builder and set the message, and click listeners
    // for the positive and negative buttons on the dialog.
    // PARAMETER: clicklistener of how to handle positive button
    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);

        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener()
        {
            // User clicked the "Keep editing" button, so dismiss the dialog
            // and continue editing the assignment.
            public void onClick(DialogInterface dialog, int id)
            {
                if (dialog != null)   dialog.dismiss();
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    // User warning when deleting:
    // Create an AlertDialog.Builder and set the message, and click listeners
    // for the positive and negative buttons on the dialog.
    private void showDeleteConfirmationDialog()
    {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.assignment_delete_dialog_msg);

        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                // User clicked the "Delete" button, so delete the assignment.
                deleteAssignment();
                finishThisActivity();
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the assignment.
                if (dialog != null)  dialog.dismiss();
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}