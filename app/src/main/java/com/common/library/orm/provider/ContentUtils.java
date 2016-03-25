package com.common.library.orm.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.RemoteException;

import com.common.library.orm.sqlite.BaseTable;

public class ContentUtils {
	/**
	 * Restore a subclass of EntityContent from the database
	 */
	public static <T extends BaseContent> T findById(Context context, Uri uri, Class<T> tableClass, long id) {
		Uri uriWithId = ContentUris.withAppendedId(uri, id);
		Cursor c = context.getContentResolver().query(uriWithId, null, null, null, null);
		if (c == null) {
			throw new RuntimeException("provider unavailable for " + uri);
		}
		try {
			if (c.moveToFirst()) {
				return getContent(c, tableClass);
			} else {
				return null;
			}
		} finally {
			c.close();
		}
	}
	
	/**
	 * Query and return all subclass of {@link BaseContent} instances as a list
	 */
	public static <T extends BaseContent> List<T> findAll(Context context, Uri uri, Class<T> tableClass){
		Cursor c = context.getContentResolver().query(uri, null, null, null, null);
		if (c == null) {
			throw new RuntimeException("provider unavailable for " + uri);
		}
		try {
			List<T> entities = new ArrayList<T>();
			while (c.moveToNext()) {
				entities.add(getContent(c, tableClass));
			}
			return entities;
		} finally {
			c.close();
		}
	}
	
	/**
	 * Count all record in table.
	 */
	public <T extends BaseTable> int count(Context context, Uri uri){
		Cursor c = context.getContentResolver().query(uri, BaseContent.COUNT_COLUMNS, null, null, null);
		if(c == null){
			throw new RuntimeException();
		}
		try{
			if (c.moveToFirst()) {
				return c.getInt(0);
			}else{
				return 0;
			}
		}finally{
			c.close();
		}
	}
	
	/**
	 * Count records with selections.
	 */
	public <T extends BaseTable> int count(Context context, Uri uri, String selection, String[] selectionArgs){
		Cursor c = context.getContentResolver().query(uri, BaseContent.COUNT_COLUMNS, selection, selectionArgs, null);
		if(c == null){
			throw new RuntimeException();
		}
		try{
			if (c.moveToFirst()) {
				return c.getInt(0);
			}else{
				return 0;
			}
		}finally{
			c.close();
		}
	}

	/**
	 * Query all records in table and return as List without selections.
	 */
	public static <T extends BaseContent> List<T> find(Context context, Uri uri, Class<T> tableClass, String selection,
			String[] selectionArgs, String orderBy) {
		List<T> entities = new ArrayList<T>();
		Cursor c = context.getContentResolver().query(uri, null, selection, selectionArgs, orderBy);
		if(c == null){
			throw new RuntimeException("provider unavailable for " + uri);
		}
		try {
			while (c.moveToNext()) {
				entities.add(getContent(c, tableClass));
			}
		} finally {
			c.close();
		}
		return entities;
	}
	
	public static <T extends BaseContent> T findFirst(Context context, Uri uri, Class<T> tableClass, String selection, 
			String[] selectionArgs, String orderBy){
		Cursor c = context.getContentResolver().query(uri, null, selection, selectionArgs, orderBy);
		if(c == null){
			throw new RuntimeException("provider unavailable for " + uri);
		}
		
		try {
			if (c.moveToFirst()) {
				return getContent(c, tableClass);
			} else {
				return null;
			}
		} finally {
			c.close();
		}
	}

	protected static <T extends BaseContent> T getContent(Cursor cursor, Class<T> tableClass) {
		try {
			T content = tableClass.newInstance();
			content.mId = cursor.getLong(0);
			content.restore(cursor);
			return content;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * A convenient method for transform content values to cursor.
	 * @param contentValues
	 * @return Cursor include data in content values.
	 */
	public static Cursor getCursor(ContentValues[] contentValues) {
		if(contentValues.length > 0) {
	        final Set<Entry<String, Object>> valueSet = contentValues[0].valueSet();
	        int colSize = valueSet.size();
	        final String[] keys = new String[colSize];
	
	        int i = 0;
	        for (Entry<String, Object> entry : valueSet) {
	            keys[i] = entry.getKey();
	            i++;
	        }
	
	        final MatrixCursor cursor = new MatrixCursor(keys);
	        for (ContentValues cv : contentValues) {
		        final Object[] values = new Object[colSize];
		        i = 0;
		        for (Entry<String, Object> entry : cv.valueSet()) {
		        	values[i] = entry.getValue();
		            i++;
		        }
	            cursor.addRow(values);
	        }
	        return cursor;
		}
		return null;
    }

	/**
	 * Insert table with module's properties.
	 */
	public static <T extends BaseContent> Uri save(Context context, Uri uri, T content) {
		if (content.isSaved()) {
			throw new UnsupportedOperationException("record has been saved before for:" + content.toString());
		}
		return context.getContentResolver().insert(uri, content.toContentValues());
	}

	/**
	 * Insert records in batch mode.
	 */
	public static <T extends BaseContent> Uri[] saveAll(Context context, String authority, Uri uri, List<T> contents) {
		if(contents == null || contents.size() == 0){
			return null;
		}
		ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
		for (T item : contents) {
			operations.add(ContentProviderOperation.newInsert(uri).withValues(item.toContentValues()).build());
		}
		try {
			ContentProviderResult[] results = context.getContentResolver().applyBatch(authority, operations);

			// prepare values to return.
			Uri[] uris = new Uri[results.length];
			for (int i = 0; i < results.length; i++) {
				uris[i] = results[i].uri;
			}
			return uris;
		} catch (RemoteException e) {
			e.printStackTrace();
			return null;
		} catch (OperationApplicationException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static <T extends BaseContent> int update(Context context, Uri uri, T t){
		if(t == null){
			throw new RuntimeException("Content to be update cannot be null");
		}
		
		if(t.mId == BaseContent.NOT_SAVED){
			throw new RuntimeException("Record is not exist in table");
		}
		return update(context, uri, t.mId, t.toContentValues());
	}
	
	/**
	 * Update record with the subclass's properties.
	 */
	public static int update(Context context, Uri uri, long id, ContentValues values) {
		if (id == BaseContent.NOT_SAVED) {
			throw new UnsupportedOperationException("Record is not exist in table");
		}
		return context.getContentResolver().update(ContentUris.withAppendedId(uri, id), values, null, null);
	}

	/**
	 * Update records with selections.
	 */
	public static int update(Context context, Uri uri, String where, String[] selectionArgs, ContentValues values) {
		return context.getContentResolver().update(uri, values, where, selectionArgs);
	}

	/**
	 * Update records in batch mode and update records with explicit primary key.
	 * @param klass
	 * @param beans
	 * @return updated records' count
	 */
	public static int update(Context context, String authority, Uri uri, Map<Long, ContentValues> valueMap) {
		ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
		for (long id : valueMap.keySet()) {
			ContentValues values = valueMap.get(id);
			operations.add(ContentProviderOperation.newUpdate(uri)
					.withSelection(BaseContent._ID + "=?",
							new String[] { String.valueOf(id) }).withValues(values).build());
		}

		try {
			ContentProviderResult[] results = context.getContentResolver().applyBatch(authority, operations);
			return results.length;
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * Delete record with its id.
	 */
	public static int delete(Context context, Uri uri, long id) {
		return context.getContentResolver().delete(ContentUris.withAppendedId(uri, id), null, null);
	}

	/**
	 * Delete records with selections.
	 */
	public static int delete(Context context, Uri uri, String where, String[] selectionArgs) {
		return context.getContentResolver().delete(uri, where, selectionArgs);
	}

	/**
	 * Delete records in batch mode and delete with primary key.
	 * @param context
	 * @param authority
	 * @param uri
	 * @param ids
	 * @return delete records count.
	 */
	public static int delete(Context context, String authority, Uri uri, List<Long> ids) {
		if(ids == null || ids.size() == 0){
			return 0;
		}
		ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
		for (long id : ids) {
			operations.add(ContentProviderOperation.newDelete(uri)
					.withSelection(BaseContent._ID + "=?", new String[] { String.valueOf(id) }).build());
		}

		try {
			ContentProviderResult[] results = context.getContentResolver().applyBatch(authority, operations);
			return results.length;
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			e.printStackTrace();
		}
		return 0;
	}

}
