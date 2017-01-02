package com.example.android.gradetracker;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.gradetracker.data.AssignmentContract;
import com.example.android.gradetracker.data.AssignmentDbHelper;
import com.example.android.gradetracker.data.AssignmentProvider;

import java.util.ArrayList;
import java.util.HashMap;

import static android.media.CamcorderProfile.get;

/**
 * Created by Anand Kumar on 12/29/2016.
 *
 * This class contains the details for the Course Catalog
 */

public class CourseCatalogActivity extends AppCompatActivity
{
    private HashMap<String, ArrayList<String>> coursetoCategoriesMap;
    private AssignmentDbHelper dbHelper;
    private int longClickedItemPosition;
    private ListView coursesListView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_catalog);
        longClickedItemPosition = -1;

        // set up FAB to open CourseEditorActivity
        // FAB is clicked to create a new course, so no info needs to be sent
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.CourseCatalog_FAB);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(CourseCatalogActivity.this, CourseEditorActivity.class);
                startActivity(intent);
            }
        });

        // initialize mapping between course names and categories
        coursetoCategoriesMap = new HashMap<String, ArrayList<String>>();

        // Find the ListView that will be populated with the courses
        coursesListView = (ListView) findViewById(R.id.Courses_ListView);

        // Enable floating context menu for the list
        registerForContextMenu(coursesListView);

        // Find and set empty view on the ListView
        View emptyCoursesView = findViewById(R.id.emptycourses_view);
        coursesListView.setEmptyView(emptyCoursesView);

        // initialize database helper
        dbHelper = new AssignmentDbHelper(getApplicationContext());


        // SET UP THE LIST
        refreshCourseCatalogList();

        // Handle user clicking list item: user clicks course -> go to course's assignment catalog
        coursesListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long l)
            {
                Intent intent = new Intent(CourseCatalogActivity.this, AssignmentCatalogActivity.class);
                // send course name and categories to the assignment catalog activity

                String currentCourseTitle = (dbHelper.getDistinctCourses()).get(position);
                ArrayList<String> categories = dbHelper.getCategoriesForCourse(currentCourseTitle);

                Log.e("CourseCatalog", "listOnItemClick: course=" + currentCourseTitle
                        + "\ncategory1 = " + categories.get(0));

                ArrayList<String> currentCourseAndCategories = new ArrayList<String>(); // course details
                currentCourseAndCategories.add(currentCourseTitle); // add title
                currentCourseAndCategories.addAll(categories); // add categories

                // put the course info in the intent
                intent.putStringArrayListExtra("currentCourseAndCategories", currentCourseAndCategories);

                // open the AssignmentCatalogActivity
                startActivity(intent);
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
        longClickedItemPosition = acmi.position;
        getMenuInflater().inflate(R.menu.floatingmenu_coursecatalog, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        if (longClickedItemPosition == -1)
            throw new IllegalStateException("longClickedView == -1");

        String currentCourseTitle = (dbHelper.getDistinctCourses()).get(longClickedItemPosition);

        switch (item.getItemId())
        {
            case R.id.floatingaction_editcourse:
                Intent intent = new Intent(CourseCatalogActivity.this, CourseEditorActivity.class);
                // send course name and categories to the assignment catalog activity

                ArrayList<String> categories = dbHelper.getCategoriesForCourse(currentCourseTitle);

                ArrayList<String> currentCourseAndCategories = new ArrayList<String>(); // course details
                currentCourseAndCategories.add(currentCourseTitle); // add title
                currentCourseAndCategories.addAll(categories); // add categories

                // put the course info in the intent
                intent.putStringArrayListExtra("currentCourseAndCategories", currentCourseAndCategories);

                // open the CourseEditorActivity
                startActivity(intent);
                return true;


            case R.id.floatingaction_deletecourse:
                showDeleteConfirmationDialog(currentCourseTitle);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    // Inflate menu options (add the items to the app bar)
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_coursecatalog, menu);
        return true;
    }

    private void refreshCourseCatalogList()
    {
        ArrayAdapter<String> courseListAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                dbHelper.getDistinctCourses());

        coursesListView.setAdapter(courseListAdapter);
    }

    // Handle user selecting menu options
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_delete_all_courses:
                deleteAllCourses();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Run when user clicks on "insert dummy course"
    private void insertDummyCourse()
    {
        String weightSeparation = getString(R.string.categoryweight_separator);
        String[] categories = {"Category A"+weightSeparation + "010",
                "Category B"+weightSeparation + "020",
                "Category C"+weightSeparation + "030",
                "Category D"+weightSeparation + "040"};

        ContentValues courseValues = new ContentValues();
        for (int i = 0; i < categories.length; i++)
        {
            courseValues.put(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_COURSE, "Example Course");
            courseValues.put(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_CATEGORY, categories[i]);

            getContentResolver().insert(AssignmentContract.AssignmentEntry.FULLTABLE_URI, courseValues);
            courseValues.clear();
        }

        refreshCourseCatalogList();
    }

    // Runs when user clicks delete all courses
    private void deleteAllCourses()
    {
        int rowsDeleted = getContentResolver().delete(AssignmentContract.AssignmentEntry.FULLTABLE_URI, null, null);

        // Show a toast message
        if (rowsDeleted == 0)
            Toast.makeText(this, getString(R.string.editor_delete_course_failed),
                    Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, getString(R.string.editor_delete_course_successful),
                    Toast.LENGTH_SHORT).show();
    }

    //showDeleteConfirmatoinDialog
    private void showDeleteConfirmationDialog(final String courseTitle)
    {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.course_delete_dialog_msg);

        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                // User clicked the "Delete" button, so delete the course.
                deleteCourse(courseTitle);
                refreshCourseCatalogList();
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the course.
                if (dialog != null)  dialog.dismiss();
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // delete a course (and all its assignments) from the database
    private void deleteCourse(String courseTitle)
    {
        String selection = AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_COURSE + "=?"; // WHERE course=?
        String[] selectionArgs = new String[] {courseTitle};
        int rowsDeleted = getContentResolver().delete(AssignmentContract.AssignmentEntry.FULLTABLE_URI,
                selection, selectionArgs);

        // Show a toast message depending on whether or not the delete was successful.
        if (rowsDeleted == 0)
            Toast.makeText(this, getString(R.string.editor_delete_course_failed),
                    Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, getString(R.string.editor_delete_course_successful),
                    Toast.LENGTH_SHORT).show();
    }
}
