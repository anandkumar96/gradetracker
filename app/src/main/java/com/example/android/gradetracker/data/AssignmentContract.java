package com.example.android.gradetracker.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Anand Kumar on 12/29/2016.
 *
 * This class contains details and format of the database
 */

// Connection between Java and SQLite
public final class AssignmentContract
{
    private AssignmentContract() {}
    public static final String CONTENT_AUTHORITY = "com.example.android.gradetracker"; // content authority
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY); // base URI
    public static final String PATH_ASSIGNMENTS_TABLE = "assignments"; // table name path

    // one class per table. here there is only one table
    public static final class AssignmentEntry implements BaseColumns
    {
        public static final String TABLE_NAME = "assignments";

        // Describe each column
        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_ASSIGNMENT_NAME = "name";
        public static final String COLUMN_ASSIGNMENT_COURSE = "course";
        public static final String COLUMN_ASSIGNMENT_CATEGORY = "category";
        public static final String COLUMN_ASSIGNMENT_SCORE = "score";
        public static final String COLUMN_ASSIGNMENT_MAXSCORE = "maxScore";



        // Complete Content URI
        public static final Uri FULLTABLE_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_ASSIGNMENTS_TABLE);

        // MIME type of a list of assignments
        public static final String ASSIGNMENTSTABLE_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ASSIGNMENTS_TABLE;

        // MIME type of a single assignment
        public static final String ASSIGNMENTITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ASSIGNMENTS_TABLE;
    }
}
