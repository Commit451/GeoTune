package com.jawnnypoo.geotune.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import timber.log.Timber;

/**
 * Content provider
 */
public class GeoTuneContentProvider extends ContentProvider {

    private static final String PACKAGE_NAME = "com.jawnnypoo.geotune";
    private static final String GEOTUNES = "geotunes";
    public static final Uri CONTENT_URI = Uri.parse("content://" + PACKAGE_NAME + "/" + GEOTUNES);

    public static final int MAX_NUM_GEOFENCES = 99;

    /**
     * Defining a UriMatcher to determine if a request is for all elements or a single row
     */
    //Create the constants used to differentiate between the different URI
    //requests.
    private static final int ALLROWS = 1;
    private static final int SINGLE_ROW = 2;

    private static final UriMatcher uriMatcher;

    //Populate the UriMatcher object, where a URI ending in
    //'favorites' will correspond to a request for all items,
    //and 'favorites/[rowID]' represents a single row.
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PACKAGE_NAME,
                GEOTUNES, ALLROWS);
        uriMatcher.addURI(PACKAGE_NAME,
                GEOTUNES + "/#", SINGLE_ROW);
    }

    // The index (key) column name for use in where clauses. (required)
    public static final String KEY_ID = "_id";

    /**
     * The helper (defined as an inner class) that allows db access
     */
    private GeoTuneSQLiteOpenHelper myOpenHelper;

    @Override
    public boolean onCreate() {
        // Construct the underlying database.
        // Defer opening the database until you need to perform
        // a query or transaction.
        myOpenHelper = new GeoTuneSQLiteOpenHelper(getContext(),
                GeoTuneSQLiteOpenHelper.DATABASE_NAME, null,
                GeoTuneSQLiteOpenHelper.DATABASE_VERSION);

        return true;
    }

    /**
     * Query the Content Provider, similar to way you would query a database, using
     * a URI defined within the provider. By using this abstraction, we can allow others
     * to access our databases through an interface that we can make secure
     */
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Open the database.
        SQLiteDatabase db;
        try {
            db = myOpenHelper.getWritableDatabase();
        } catch (SQLiteException ex) {
            db = myOpenHelper.getReadableDatabase();
        }

        // Replace these with valid SQL statements if necessary.
        String groupBy = null;
        String having = null;

        // Use an SQLite Query Builder to simplify constructing the
        // database query.
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // If this is a row query, limit the result set to the passed in row.
        switch (uriMatcher.match(uri)) {
            case SINGLE_ROW:
                String rowID = uri.getPathSegments().get(1);
                queryBuilder.appendWhere(KEY_ID + "=" + rowID);
            default:
                break;
        }

        // Specify the table on which to perform the query. This can
        // be a specific table or a join as required.
        queryBuilder.setTables(GeoTuneSQLiteOpenHelper.DATABASE_TABLE);

        // Return the result Cursor.
        return queryBuilder.query(db, projection, selection,
                selectionArgs, groupBy, having, sortOrder);
    }

    /**
     * Returning a Content Provider MIME type
     */
    @Override
    public String getType(@NonNull Uri uri) {
        // Return a string that identifies the MIME type
        // for a Content Provider URI
        switch (uriMatcher.match(uri)) {
            case ALLROWS:
                return "vnd.android.cursor.dir/vnd.jawnnypoo.geotune";
            case SINGLE_ROW:
                return "vnd.android.cursor.item/vnd.jawnnypoo.geotune";
            default:
                throw new IllegalArgumentException("Unsupported URI: " +
                        uri);
        }
    }

    /**
     * Performs deletions within the database
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Open a read / write database to support the transaction.
        SQLiteDatabase db = myOpenHelper.getWritableDatabase();

        // If this is a row URI, limit the deletion to the specified row.
        switch (uriMatcher.match(uri)) {
            case SINGLE_ROW:
                String rowID = uri.getPathSegments().get(1);
                selection = KEY_ID + "=" + rowID
                        + (!TextUtils.isEmpty(selection) ?
                        " AND (" + selection + ')' : "");
            default:
                break;
        }

        // To return the number of deleted items you must specify a where
        // clause. To delete all rows and return a value pass in "1".
        if (selection == null)
            selection = "1";

        // Perform the deletion.
        int deleteCount = db.delete(GeoTuneSQLiteOpenHelper.DATABASE_TABLE,
                selection, selectionArgs);

        // Notify any observers of the change in the data set.
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the number of deleted items.
        return deleteCount;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        // Open a read / write database to support the transaction.
        SQLiteDatabase db = myOpenHelper.getWritableDatabase();

        // To add empty rows to your database by passing in an empty
        // Content Values object you must use the null column hack
        // parameter to specify the name of the column that can be
        // set to null.
        String nullColumnHack = null;

        // Insert the values into the table
        long id = db.insert(GeoTuneSQLiteOpenHelper.DATABASE_TABLE,
                nullColumnHack, values);

        // Construct and return the URI of the newly inserted row.
        if (id > -1) {
            // Construct and return the URI of the newly inserted row.
            Uri insertedId = ContentUris.withAppendedId(CONTENT_URI, id);

            // Notify any observers of the change in the data set.
            getContext().getContentResolver().notifyChange(insertedId, null);

            return insertedId;
        } else {
            return null;
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // Open a read / write database to support the transaction.
        SQLiteDatabase db = myOpenHelper.getWritableDatabase();

        // If this is a row URI, limit the deletion to the specified row.
        switch (uriMatcher.match(uri)) {
            case SINGLE_ROW:
                String rowID = uri.getPathSegments().get(1);
                selection = KEY_ID + "=" + rowID
                        + (!TextUtils.isEmpty(selection) ?
                        " AND (" + selection + ')' : "");
            default:
                break;
        }

        // Perform the update.
        int updateCount = db.update(GeoTuneSQLiteOpenHelper.DATABASE_TABLE,
                values, selection, selectionArgs);

        // Notify any observers of the change in the data set.
        getContext().getContentResolver().notifyChange(uri, null);

        return updateCount;
    }

    private static class GeoTuneSQLiteOpenHelper extends SQLiteOpenHelper {
        // Database name, version, and table names.
        private static final String DATABASE_NAME = "geotunes.db";
        private static final int DATABASE_VERSION = 1;
        private static final String DATABASE_TABLE = "geotunesTable";

        // SQL Statement to create a new database.
        private static final String DATABASE_CREATE = "create table " +
                DATABASE_TABLE + " (" + KEY_ID +
                " integer primary key autoincrement, " +
                GeoTune.Companion.getKEY_UID() + " text not null, " +
                GeoTune.Companion.getKEY_NAME() + " text not null, " +
                GeoTune.Companion.getKEY_LATITUDE() + " real, " +
                GeoTune.Companion.getKEY_LONGITUDE() + " real, " +
                GeoTune.Companion.getKEY_RADIUS() + " real, " +
                GeoTune.Companion.getKEY_TRANSITION_TYPE() + " integer, " +
                GeoTune.Companion.getKEY_TUNE() + " text, " +
                GeoTune.Companion.getKEY_TUNE_NAME() + " text, " +
                GeoTune.Companion.getKEY_ACTIVE() + " integer);";

        // The name and column index of each column in your database.
        // These should be descriptive.

        public GeoTuneSQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        // Called when no database exists in disk and the helper class needs
        // to create a new one.
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        // Called when there is a database version mismatch meaning that the version
        // of the database on disk needs to be upgraded to the current version.
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Log the version upgrade.
            Timber.w("Upgrading from version " +
                    oldVersion + " to " +
                    newVersion + ", which will destroy all old data");

            // Upgrade the existing database to conform to the new version. Multiple
            // previous versions can be handled by comparing _oldVersion and _newVersion
            // values.

            // The simplest case is to drop the old table and create a new one.
            db.execSQL("DROP TABLE IF IT EXISTS " + DATABASE_TABLE);
            // Create a new one.
            onCreate(db);
        }
    }
}