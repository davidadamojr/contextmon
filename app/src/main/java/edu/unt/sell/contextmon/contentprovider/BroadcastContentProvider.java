package edu.unt.sell.contextmon.contentprovider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;

import edu.unt.sell.contextmon.db.BroadcastDatabaseHelper;
import edu.unt.sell.contextmon.db.BroadcastTable;

public class BroadcastContentProvider extends ContentProvider {

    // database
    private BroadcastDatabaseHelper database;

    // Used for the UriMatcher
    private static final int BROADCASTS = 10;
    private static final int BROADCAST_ID = 20;

    private static final String AUTHORITY = "edu.unt.sell.contextmon";

    private static final String BASE_PATH = "broadcasts";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/broadcasts";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/broadcast";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, BROADCASTS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", BROADCAST_ID);
    }

    @Override
    public boolean onCreate(){
        database = new BroadcastDatabaseHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder){
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // Check if the caller has requested a column which does not exist
        checkColumns(projection);

        // Set the table
        queryBuilder.setTables(BroadcastTable.TABLE_NAME);

        int uriType = sURIMatcher.match(uri);
        switch (uriType){
            case BROADCASTS:
                break;
            case BROADCAST_ID:
                // Adding the ID to the original query
                queryBuilder.appendWhere(BroadcastTable.COLUMN_ID + "=" + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = database.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        // Make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    private void checkColumns(String[] projection){
        // checks if the caller has requested a column that does not exist in the database

        String[] available = {
            BroadcastTable.COLUMN_ACTION, BroadcastTable.COLUMN_EXTRAS,
            BroadcastTable.COLUMN_TIMESTAMP, BroadcastTable.COLUMN_ID,
            BroadcastTable.COLUMN_UPLOADED
        };
        if (projection != null){
            HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));

            // Check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)){
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }

    @Override
    public String getType(Uri uri) { return null; }

    @Override
    public Uri insert(Uri uri, ContentValues values){
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        long id = 0;
        switch (uriType) {
            case BROADCASTS:
                id = sqlDB.insert(BroadcastTable.TABLE_NAME, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_PATH + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs){
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsDeleted = 0;
        switch (uriType){
            case BROADCASTS:
                rowsDeleted = sqlDB.delete(BroadcastTable.TABLE_NAME, selection, selectionArgs);
                break;
            case BROADCAST_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)){
                    rowsDeleted = sqlDB.delete(BroadcastTable.TABLE_NAME, BroadcastTable.COLUMN_ID + "=" + id, null);
                } else {
                    rowsDeleted = sqlDB.delete(BroadcastTable.TABLE_NAME, BroadcastTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs){
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsUpdated = 0;
        switch (uriType) {
            case BROADCASTS:
                rowsUpdated = sqlDB.update(BroadcastTable.TABLE_NAME, values, selection, selectionArgs);
                break;
            case BROADCAST_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)){
                    rowsUpdated = sqlDB.update(BroadcastTable.TABLE_NAME, values, BroadcastTable.COLUMN_ID + "=" + id, null);
                } else {
                    rowsUpdated = sqlDB.update(BroadcastTable.TABLE_NAME, values, BroadcastTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }
}
