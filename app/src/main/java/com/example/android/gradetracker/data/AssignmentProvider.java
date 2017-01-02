package com.example.android.gradetracker.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import static android.R.attr.id;

/**
 * Created by Anand Kumar on 12/29/2016.
 *
 * This class is the ContentProvider for the database
 */

public class AssignmentProvider extends ContentProvider
{
    private AssignmentDbHelper dbHelper; // database helper object
    private static final int ASSIGNMENTS_TABLE_CASE = 1; // case ID (accessing the whole table)
    private static final int ASSIGNMENT_ID_CASE = 2; // case ID (accessing one row)

    // create uri matcher
    private static final UriMatcher providerUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static
    {
        // Add full table case
        providerUriMatcher.addURI(AssignmentContract.CONTENT_AUTHORITY,
                AssignmentContract.PATH_ASSIGNMENTS_TABLE,
                ASSIGNMENTS_TABLE_CASE);

        // Add individual row case
        providerUriMatcher.addURI(AssignmentContract.CONTENT_AUTHORITY,
                AssignmentContract.PATH_ASSIGNMENTS_TABLE + "/#",
                ASSIGNMENT_ID_CASE);
    }

    // Tag for the log messages
    public static final String LOG_TAG = AssignmentProvider.class.getSimpleName();


    // Initialize the provider and database helper object
    @Override
    public boolean onCreate()
    {
        dbHelper = new AssignmentDbHelper(getContext());
        return true;
    }


    // Perform Query (Selection and selection args are used for the WHERE statement)
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder)
    {
        Cursor cursor = null; // database subset to be returned
        SQLiteDatabase db = dbHelper.getReadableDatabase(); // get the database
        int match = providerUriMatcher.match(uri); // match parameter uri to one of the options from static{} (above)

        switch(match)
        {
            case ASSIGNMENTS_TABLE_CASE:
                // Perform database query on the assignments table
                cursor = db.query(AssignmentContract.AssignmentEntry.TABLE_NAME,
                        projection, selection, selectionArgs, null, null, sortOrder);
                break;

            case ASSIGNMENT_ID_CASE:
                // Perform database query on specific row
                selection = AssignmentContract.AssignmentEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};
                cursor = db.query(AssignmentContract.AssignmentEntry.TABLE_NAME,
                        projection, selection, selectionArgs, null, null, sortOrder);
                break;

            default: // invalid case
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // set notification uri on the cursor. if the data at this uri changes,
        // the cursor needs an update. then return the cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }


    // Perform insertion of new data with the given content values
    @Override
    public Uri insert(Uri uri, ContentValues contentValues)
    {
        final int match = providerUriMatcher.match(uri);
        switch(match)
        {
            case ASSIGNMENTS_TABLE_CASE:
                return insertAssignment(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for URI " + uri);
        }
    }

    // helper method that inserts assignment
    // returns uri of created and inserted assignment
    private Uri insertAssignment(Uri uri, ContentValues contentValues)
    {
        // Check values
        Log.d("AssignmentProvider.java",
                "insertAssignment(): Starting validity checks for assignment fields");

        String assignmentName = contentValues.getAsString(
                AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_NAME);
        Log.d("AssignProvider.java", "insertAssign(): assignmentName = " + assignmentName);
        if (!dbHelper.isValidName(assignmentName))
            throw new IllegalArgumentException("ERROR: Assignment requires a name");

        String assignmentCourse = contentValues.getAsString(
                AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_COURSE);
        if (!dbHelper.isValidName(assignmentCourse))
            throw new IllegalArgumentException("ERROR: Assignment requires a corresponding course");
        Log.d("AssignProvider.java", "insertAssign(): assignmentCourse = " + assignmentCourse);

        String assignmentCategory = contentValues.getAsString(
                AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_CATEGORY);
        Log.d("AssignProvider.java", "insertAssign(): assignmentCategory = " + assignmentCategory);
        if (!dbHelper.isValidName(assignmentCategory))
            throw new IllegalArgumentException("ERROR: Assignment requires a category");

        Double assignmentScore = contentValues.getAsDouble(
                AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_SCORE);
        Log.d("AssignProvider.java", "insertAssign(): assignmentScore = " + assignmentScore);

        Double assignmentMaxScore = contentValues.getAsDouble(
                AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_MAXSCORE);
        Log.d("AssignProvider.java", "insertAssign(): assignmentMaxScore = " + assignmentMaxScore);
        if (!dbHelper.isValidPoints(assignmentMaxScore))
            throw new IllegalArgumentException("ERROR: Invalid Maximum Points");

        // Insert a new assignment into the given database
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowID = db.insert(AssignmentContract.AssignmentEntry.TABLE_NAME, null, contentValues);

        if (rowID == -1)
        {
            Log.e("AssignmentProvider.java", "insertAssignment(): insertion error");
            return null; // for invalid insertion
        }
        else
        {
            Log.d("AssignmentProvider.java", "insertAssignment(): successfully inserted at row" + rowID
                    + "\nName = " + assignmentName
                    + "\nCourse = " + assignmentCourse
                    + "\nCategory = " + assignmentCategory
                    + "\nEarned Score = " + assignmentScore
                    + "\nMax Possible = " + assignmentMaxScore
            );

            // Check for any changes in the relevant parts of the database
            getContext().getContentResolver().notifyChange(uri, null);

            // return the uri of the new insertion
            return ContentUris.withAppendedId(uri, id);
        }


    }

    // Update the data at the given selection and selection arguments, with the new ContentValues.
    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs)
    {
        final int match = providerUriMatcher.match(uri);
        switch(match)
        {
            case ASSIGNMENTS_TABLE_CASE: // user wants to update all rows
                return updateAssignment(uri, contentValues, selection, selectionArgs);
            case ASSIGNMENT_ID_CASE: // user wants to update one specific row
                selection = AssignmentContract.AssignmentEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};
                return updateAssignment(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update not supported for " + uri);
        }
    }

    // Update given assignments
    // Returns # of rows affected
    private int updateAssignment(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs)
    {
        // Check if there is anything to update
        if (contentValues.size() == 0) return 0;

        // Validate values
        Log.d("AssignmentProvider.java", "updateAssignment(): Starting validity checks for assignment fields");
        if (contentValues.containsKey(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_NAME) &&
                !dbHelper.isValidName(contentValues.getAsString(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_NAME)))
            throw new IllegalArgumentException("ERROR: Assignment requires a name");

        if (contentValues.containsKey(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_COURSE) &&
                !dbHelper.isValidName(contentValues.getAsString(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_COURSE)))
            throw new IllegalArgumentException("ERROR: Assignment requires a corresponding course");

        if (contentValues.containsKey(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_CATEGORY) &&
                !dbHelper.isValidName(contentValues.getAsString(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_CATEGORY)))
            throw new IllegalArgumentException("ERROR: Assignment requires a category");

        if (contentValues.containsKey(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_SCORE) &&
                !dbHelper.isValidPoints(contentValues.getAsDouble(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_SCORE)))
            throw new IllegalArgumentException("ERROR: Invalid Score");

        if (contentValues.containsKey(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_MAXSCORE) &&
                !dbHelper.isValidPoints(contentValues.getAsDouble(AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_MAXSCORE)))
            throw new IllegalArgumentException("ERROR: Invalid Maximum Score");

        // Update the database
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsChanged = db.update(AssignmentContract.AssignmentEntry.TABLE_NAME, contentValues,
                selection, selectionArgs);
        Log.d("AssignmentProvider.java", "updateAssignment(): changed " + rowsChanged + " rows.");


        // Check for any changes in the relevant parts of the database and then return
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsChanged;
    }

    /**
     * Delete the data at the given selection and selection arguments.
     * Returns the number of rows deleted
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs)
    {
        int numDeletedRows = 0;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // check if user wants to change full table or specific row
        int match = providerUriMatcher.match(uri);
        switch(match)
        {
            case ASSIGNMENTS_TABLE_CASE: // full table case
                numDeletedRows = db.delete(AssignmentContract.AssignmentEntry.TABLE_NAME, selection, selectionArgs);
                Log.d("AssignmentProvider.java", "delete(): deleted " + numDeletedRows + " rows.");

                // Check for any changes in the relevant parts of the database
                getContext().getContentResolver().notifyChange(uri, null);
                return numDeletedRows;

            case ASSIGNMENT_ID_CASE: // specific row case
                selection = AssignmentContract.AssignmentEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};
                numDeletedRows = db.delete(AssignmentContract.AssignmentEntry.TABLE_NAME, selection, selectionArgs);
                Log.d("AssignmentProvider.java", "delete(): deleted " + numDeletedRows + " rows.");

                // Check for any changes in the relevant parts of the database
                getContext().getContentResolver().notifyChange(uri, null);
                return numDeletedRows;

            default:
                throw new IllegalArgumentException("Deletion not supported for " + uri);
        }
    }

    /**
     * Returns the MIME type of data for the content URI.
     */
    @Override
    public String getType(Uri uri)
    {
        final int match = providerUriMatcher.match(uri);
        switch(match)
        {
            case ASSIGNMENTS_TABLE_CASE:
                return AssignmentContract.AssignmentEntry.ASSIGNMENTSTABLE_TYPE;
            case ASSIGNMENT_ID_CASE:
                return AssignmentContract.AssignmentEntry.ASSIGNMENTITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown uri " + uri + " with match " + match);
        }
    }
}