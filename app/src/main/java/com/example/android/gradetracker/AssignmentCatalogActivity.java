package com.example.android.gradetracker;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.gradetracker.data.AssignmentContract;
import com.example.android.gradetracker.data.AssignmentDbHelper;

import java.util.ArrayList;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;
import static com.example.android.gradetracker.R.id.fab;
import static java.security.AccessController.getContext;

public class AssignmentCatalogActivity extends AppCompatActivity
        implements android.app.LoaderManager.LoaderCallbacks<Cursor>
{
    private AssignmentCursorAdapter assignmentAdapter;
    private TextView cat1Name;
    private TextView cat1Grade;
    private TextView cat2Name;
    private TextView cat2Grade;
    private TextView cat3Name;
    private TextView cat3Grade;
    private TextView cat4Name;
    private TextView cat4Grade;
    private TextView catTotalName;
    private TextView catTotalGrade;
    private static final int URI_LOADER = 0;

    private AssignmentDbHelper dbHelper;
    private ArrayList<String> currentCourseAndCategories; // {CourseTitle, Categ1, Categ2, ... }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment_catalog);
        currentCourseAndCategories = getIntent().getStringArrayListExtra("currentCourseAndCategories");

        //set title to course name
        if (currentCourseAndCategories != null)
            setTitle(currentCourseAndCategories.get(0));

        // Setup FAB to open AssignmentEditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(AssignmentCatalogActivity.this, AssignmentEditorActivity.class);
                intent.putStringArrayListExtra("currentCourseAndCategories", currentCourseAndCategories);
                startActivity(intent);
            }
        });

        // Initialize database helper
        dbHelper = new AssignmentDbHelper(this);

        ArrayList<String> courseCategories = dbHelper.getCategoriesForCourse(currentCourseAndCategories.get(0));

        // Initialize text views
        cat1Name = (TextView) findViewById(R.id.Category1Name_TextView);
        cat1Grade = (TextView) findViewById(R.id.Category1Score_TextView);
        cat1Name.setText(dbHelper.getNameFromCategory(courseCategories.get(0), getApplicationContext()));
        cat1Grade.setText(String.valueOf(dbHelper.getCategoryGradePercentage(
                currentCourseAndCategories.get(0), courseCategories.get(0))));

        cat2Name = (TextView) findViewById(R.id.Category2Name_TextView);
        cat2Grade = (TextView) findViewById(R.id.Category2Score_TextView);
        cat2Name.setText(dbHelper.getNameFromCategory(courseCategories.get(1), getApplicationContext()));
        cat2Grade.setText(String.valueOf(dbHelper.getCategoryGradePercentage(
                currentCourseAndCategories.get(0), courseCategories.get(1))));

        cat3Name = (TextView) findViewById(R.id.Category3Name_TextView);
        cat3Grade = (TextView) findViewById(R.id.Category3Score_TextView);
        cat3Name.setText(dbHelper.getNameFromCategory(courseCategories.get(2), getApplicationContext()));
        cat3Grade.setText(String.valueOf(dbHelper.getCategoryGradePercentage(
                currentCourseAndCategories.get(0), courseCategories.get(2))));

        cat4Name = (TextView) findViewById(R.id.Category4Name_TextView);
        cat4Grade = (TextView) findViewById(R.id.Category4Score_TextView);
        cat4Name.setText(dbHelper.getNameFromCategory(courseCategories.get(3), getApplicationContext()));
        cat4Grade.setText(String.valueOf(dbHelper.getCategoryGradePercentage(
                currentCourseAndCategories.get(0), courseCategories.get(3))));

        catTotalName = (TextView) findViewById(R.id.CategoryTotalName_TextView);
        catTotalGrade = (TextView) findViewById(R.id.CategoryTotalScore_TextView);
        catTotalName.setText(getString(R.string.course_total));
        catTotalGrade.setText(String.valueOf(dbHelper.getCourseWeightedGradePercent(
                currentCourseAndCategories.get(0), getApplicationContext())));



        // Find the ListView which will be populated with the assignment data
        final ListView assignmentListView = (ListView) findViewById(R.id.Assignments_ListView);


        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        assignmentListView.setEmptyView(emptyView);

        // initialize the AssignmentCursorAdapter
        assignmentAdapter = new AssignmentCursorAdapter(this, null);
        assignmentListView.setAdapter(assignmentAdapter);


        // Listens for user clicking on list item --> EDIT ASSIGNMENT
        assignmentListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Intent intent = new Intent(AssignmentCatalogActivity.this, AssignmentEditorActivity.class);

                // Get the assignment's uri to send to the editor via the intent
                Uri clickedAssignmentUri =
                        ContentUris.withAppendedId(AssignmentContract.AssignmentEntry.FULLTABLE_URI, id);
                intent.setData(clickedAssignmentUri);
                intent.putStringArrayListExtra("currentCourseAndCategories", currentCourseAndCategories);


                // open the editor
                startActivity(intent);
            }
        });

        //initialize the cursor loader
        getLoaderManager().initLoader(URI_LOADER, null, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (data == null) throw new IllegalStateException("onActivityResult: Intent data = null");
        if (requestCode == 0x0000abcd)
            currentCourseAndCategories = data.getStringArrayListExtra("currentCourseAndCategories");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu options from the res/menu/menu_assignmentcatalogentcatalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_assignmentcatalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            case R.id.action_insert_dummy_assignment:
                insertDummyAssignment();
                return true;

            case R.id.action_delete_all_entries:
                deleteAllAssignments();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Runs when user clicks on menu item: "Insert Dummy Data"
    private void insertDummyAssignment()
    {
        if (currentCourseAndCategories.size() <= 5)
            Log.e("AssgnCatalogActv", "\ncourse=" + currentCourseAndCategories.get(0) +
                    "\ncat1 = " + currentCourseAndCategories.get(1) + "  " +
                    "\ncat2 = " + currentCourseAndCategories.get(2) + "  " +
                    "\ncat3 = " + currentCourseAndCategories.get(3) + "  " +
                    "\ncat4 = " + currentCourseAndCategories.get(4) + "  " );

        // Put fake values into a ContentValues object
        ContentValues assignmentValues = new ContentValues();
        assignmentValues.put(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_NAME, "AssignmentName");
        assignmentValues.put(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_COURSE, currentCourseAndCategories.get(0));
        assignmentValues.put(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_CATEGORY, currentCourseAndCategories.get(1));
        assignmentValues.put(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_SCORE, 99.9);
        assignmentValues.put(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_MAXSCORE, 100.1);

        // insert fake assignment
        Uri insertionResult = getContentResolver().insert(
                AssignmentContract.AssignmentEntry.FULLTABLE_URI, assignmentValues);
        if (insertionResult == null)
            Toast.makeText(getApplicationContext(),
                    R.string.toast_assignmentInsertionError, Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(getApplicationContext(),
                    R.string.toast_assignmentInsertionSuccess, Toast.LENGTH_SHORT).show();

    }

    //Runs when user clicks delete all assignments FOR THIS COURSE
    private void deleteAllAssignments()
    {
        // WHERE statement values
        String selection = AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_COURSE+ "=?";
        String[] selectionArgs = new String[] {currentCourseAndCategories.get(0)};

        int rowsDeleted = getContentResolver().delete(
                AssignmentContract.AssignmentEntry.FULLTABLE_URI, selection, selectionArgs);

        // Show a toast message depending on whether or not the delete was successful.
        if (rowsDeleted == 0)
            Toast.makeText(this, getString(R.string.editor_delete_assignment_failed),
                    Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, getString(R.string.editor_delete_assignment_successful),
                    Toast.LENGTH_SHORT).show();
    }

    @Override
    // When the loader is created, all the info needed to be displayed is gathered
    public android.content.Loader<Cursor> onCreateLoader(int id, Bundle args)
    {
        if (currentCourseAndCategories == null)
            throw new IllegalArgumentException("onCreateLoader: currentCourseAndCategories == null");

        switch(id)
        {
            case URI_LOADER:
                // return a new cursor loader to view details of all assignments IN THIS COURSE
                String[] projection = {AssignmentContract.AssignmentEntry._ID,
                        AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_NAME,
                        AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_CATEGORY,
                        AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_SCORE,
                        AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_MAXSCORE,
                }; // array of columns to use.

                String selection = AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_COURSE + "=?"
                        + " AND " + AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_NAME + " IS NOT NULL "
                        + " AND " + AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_MAXSCORE + " IS NOT NULL ";
                String[] selectionArgs = new String[] {currentCourseAndCategories.get(0)};
                String sortOrder = null;
                return new android.content.CursorLoader(getApplicationContext(),
                        AssignmentContract.AssignmentEntry.FULLTABLE_URI,
                        projection, selection, selectionArgs, sortOrder);

            default:
                return null; // invalid id
        }
    }

    @Override
    //when load is finished, the query results should be put into the adapter
    // then, listview can be refreshed
    public void onLoadFinished(android.content.Loader<Cursor> loader, Cursor data)
    {
        assignmentAdapter.swapCursor(data);
    }

    @Override
    // avoid memory leaks. clear the cursor.
    public void onLoaderReset(android.content.Loader<Cursor> loader)
    {
        assignmentAdapter.swapCursor(null);
    }
}