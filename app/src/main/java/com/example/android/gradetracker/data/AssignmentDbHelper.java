package com.example.android.gradetracker.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.example.android.gradetracker.R;
import java.util.ArrayList;

/**
 * Created by Anand Kumar on 12/29/2016.
 *
 * This class is the SQLiteOpenHelper for the database
 */

public class AssignmentDbHelper extends SQLiteOpenHelper
{
    // database details (constants)
    public static final String DATABASE_NAME = "assignments.db";
    public static final int DATABASE_VERSION = 1;
    private static final String COMMA_SEPARATION = ", ";

    // Constructor
    public AssignmentDbHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db)
    {
        // make the CREATE TABLE statement for SQL
        String SQL_CREATE_ASSIGNMENTS_TABLE = "CREATE TABLE " + AssignmentContract.AssignmentEntry.TABLE_NAME + "("
                + AssignmentContract.AssignmentEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA_SEPARATION
                + AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_NAME + " TEXT" + COMMA_SEPARATION
                + AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_COURSE + " TEXT NOT NULL" + COMMA_SEPARATION
                + AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_CATEGORY + " TEXT NOT NULL" + COMMA_SEPARATION
                + AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_SCORE + " REAL" + COMMA_SEPARATION
                + AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_MAXSCORE + " REAL"
                + ");";

        // Execute
        db.execSQL(SQL_CREATE_ASSIGNMENTS_TABLE);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        // Version 1 for now, so do nothing...
    }


    // get list of distinct courses
    public ArrayList<String> getDistinctCourses()
    {
        String query = "SELECT DISTINCT "
                + AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_COURSE
                + " FROM " + AssignmentContract.AssignmentEntry.TABLE_NAME;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor;
        cursor = db.rawQuery(query, null);

        ArrayList<String> courses = new ArrayList<String>();

        if (cursor.moveToFirst())
        {
            for (int i = 0; i < cursor.getCount(); i++)
            {
                String courseName = cursor.getString(cursor.getColumnIndex(
                        AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_COURSE));

                Log.d("AssignmentDbHelper", "getCoursesArray: courses[" + i + "] = " + courseName);
                courses.add(courseName);
                cursor.moveToNext();
            }
        }


        cursor.close();
        return courses;
    }

    // get list of categories for a course
    public ArrayList<String> getCategoriesForCourse(String courseName)
    {
        String query = "SELECT DISTINCT "
                + AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_CATEGORY
                + " FROM " + AssignmentContract.AssignmentEntry.TABLE_NAME
                + " WHERE " + AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_COURSE
                + "=\"" + courseName + "\"";


        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        ArrayList<String> categories = new ArrayList<String>();

        if (cursor.moveToFirst())
        {
            for (int i = 0; i < cursor.getCount(); i++)
            {
                String categoryName = cursor.getString(cursor.getColumnIndex(
                        AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_CATEGORY));

                Log.d("AssignmentDbHelper", "getCategoriesForCourse: categ[" + i + "] = " + categoryName);
                categories.add(categoryName);
                cursor.moveToNext();
            }
        }

        cursor.close();
        return categories;
    }

    // Split Category into Name and Weight (categories listed in that order)
    public String getNameFromCategory(String category, Context context)
    {
        String[] splitCategory = category.split(context.getString(R.string.categoryweight_separator));
        return splitCategory[0];
    }
    public String getWeightFromCategory(String category, Context context)
    {
        String[] splitCategory = category.split(context.getString(R.string.categoryweight_separator));
        double weight = Double.valueOf(splitCategory[1]);
        Log.e("CourseEditor", "str/double/retval weight = " + splitCategory[1] + "/" + weight + "/"
                + String.valueOf(weight));
        return String.valueOf(weight);
    }

    public double getCategoryGradePercentage(String courseName, String category)
    {
        // select name, score, maxscore from table WHERE course=COURSENAME AND category=CATEGORYNAME
        String query = "SELECT "
                + AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_NAME + COMMA_SEPARATION
                + AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_SCORE + COMMA_SEPARATION
                + AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_MAXSCORE
                + " FROM " + AssignmentContract.AssignmentEntry.TABLE_NAME
                + " WHERE " + AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_COURSE
                + "=\"" + courseName + "\""
                + " AND " + AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_CATEGORY
                + "=\"" + category + "\""
                + " AND " + AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_NAME + " IS NOT NULL ";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        Log.d("AssignmentDbHelper", "getCatGrade%: category = " + category);

        double totalScore = 0.0;
        double totalMaxScore = 0.0;

        if (cursor.moveToFirst())
        {
            for (int i = 0; i < cursor.getCount(); i++)
            {
                String assignmentName = cursor.getString(cursor.getColumnIndex(
                        AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_NAME));

                Double assignmentScore = cursor.getDouble(cursor.getColumnIndex(
                        AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_SCORE));

                Double assignmentMaxScore = cursor.getDouble(cursor.getColumnIndex(
                        AssignmentContract.AssignmentEntry.COLUMN_ASSIGNMENT_MAXSCORE));


                Log.d("AssignmentDbHelper", "getCatGrade%: assignment " + assignmentName +
                        "  = " + assignmentScore + "/" + assignmentMaxScore);

                totalScore += assignmentScore;
                totalMaxScore += assignmentMaxScore;

                cursor.moveToNext();
            }
        }

        double grade;
        if (totalMaxScore == 0) grade = 0.0;
        else grade = totalScore/totalMaxScore;
        Log.d("AssignmentDbHelper.java", "getCatGrade%: category " + category + " grade = " + grade);
        return (grade * 100);
    }

    public double getCourseWeightedGradePercent(String courseName, Context context)
    {
        ArrayList<String> categories = getCategoriesForCourse(courseName);

        double totalGrade = 0.0;
        double totalWeight = 0.0;

        for (int i = 0; i < categories.size(); i++)
        {
            String categoryName = categories.get(i);
            double categoryGrade = getCategoryGradePercentage(courseName, categoryName);
            if (categoryGrade == 0) continue;

            totalGrade +=
                    (Double.parseDouble(getWeightFromCategory(categoryName, context))/100.0 * categoryGrade/100.0);
            totalWeight +=
                    (Double.parseDouble(getWeightFromCategory(categoryName, context))/100.0);
        }

        if (totalWeight == 0) return 0.0;
        return Math.round(totalGrade/totalWeight * 100 * 1000)/1000;
    }

    // VALIDITY CHECKS
    public boolean isValidName(String name)
    {
        return true;
    }
    public boolean isValidCourse(String course)
    {
        return course != null;
    }
    public boolean isValidCategory(String category)
    {
        return category != null;
    }
    public boolean isValidPoints(Double pointValue)
    {
        return (pointValue == null || pointValue >= 0.0);
    }
}