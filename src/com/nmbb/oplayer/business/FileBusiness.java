package com.nmbb.oplayer.business;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.nmbb.oplayer.database.SQLiteHelper;
import com.nmbb.oplayer.database.TableColumns.FilesColumns;
import com.nmbb.oplayer.po.PFile;
import com.nmbb.oplayer.util.PinyinUtils;

public final class FileBusiness {

	private static final String TABLE_NAME = "files";

	/** 获取所有已经排好序的列表 */
	public static ArrayList<PFile> getAllSortFiles(final Context ctx) {
		ArrayList<PFile> result = new ArrayList<PFile>();
		SQLiteHelper sqlite = new SQLiteHelper(ctx);
		SQLiteDatabase db = sqlite.getReadableDatabase();
		Cursor c = null;
		try {
			c = db.rawQuery("SELECT " + FilesColumns.COL_ID + "," + FilesColumns.COL_TITLE + "," + FilesColumns.COL_TITLE_PINYIN + "," + FilesColumns.COL_PATH + "," + FilesColumns.COL_DURATION + "," + FilesColumns.COL_POSITION + "," + FilesColumns.COL_LAST_ACCESS_TIME + "," + FilesColumns.COL_THUMB + "," + FilesColumns.COL_FILE_SIZE + " FROM files", null);
			while (c.moveToNext()) {
				PFile po = new PFile();
				int index = 0;
				po._id = c.getLong(index++);
				po.title = c.getString(index++);
				po.title_pinyin = c.getString(index++);
				po.path = c.getString(index++);
				po.duration = c.getInt(index++);
				po.position = c.getInt(index++);
				po.last_access_time = c.getLong(index++);
				po.thumb = c.getString(index++);
				po.file_size = c.getLong(index++);
				result.add(po);
			}
		} finally {
			if (c != null)
				c.close();
		}
		db.close();

		Collections.sort(result, new Comparator<PFile>() {

			@Override
			public int compare(PFile f1, PFile f2) {
				char c1 = f1.title_pinyin.charAt(0);
				char c2 = f2.title_pinyin.charAt(0);
				return c1 == c2 ? 0 : (c1 > c2 ? 1 : -1);
			}//相等返回0，-1 f2 > f2，-1

		});
		return result;
	}

	public static void insertFile(final Context ctx, final PFile p) {
		SQLiteHelper sqlite = new SQLiteHelper(ctx);
		SQLiteDatabase db = sqlite.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(FilesColumns.COL_TITLE, p.title);
			values.put(FilesColumns.COL_TITLE_PINYIN, PinyinUtils.chineneToSpell(p.title.charAt(0) + ""));
			values.put(FilesColumns.COL_PATH, p.path);
			values.put(FilesColumns.COL_LAST_ACCESS_TIME, System.currentTimeMillis());
			values.put(FilesColumns.COL_FILE_SIZE, p.file_size);
			db.insert(TABLE_NAME, "", values);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				db.close();
			} catch (Exception e) {
			}
		}
	}

	/** 批量插入数据 */
	public static void batchInsertFiles(final Context ctx, final ArrayList<File> files) {
		SQLiteHelper sqlite = new SQLiteHelper(ctx);
		SQLiteDatabase db = sqlite.getWritableDatabase();
		try {
			db.beginTransaction();

			SQLiteStatement stat = db.compileStatement("INSERT INTO files(" + FilesColumns.COL_TITLE + "," + FilesColumns.COL_TITLE_PINYIN + "," + FilesColumns.COL_PATH + "," + FilesColumns.COL_LAST_ACCESS_TIME + "," + FilesColumns.COL_FILE_SIZE + ") VALUES(?,?,?,?,?)");
			for (File f : files) {
				String name = f.getName();
				int index = 1;
				stat.bindString(index++, name);// title
				stat.bindString(index++, PinyinUtils.chineneToSpell(name.charAt(0) + ""));// title_pinyin
				stat.bindString(index++, f.getPath());// path
				stat.bindLong(index++, System.currentTimeMillis());// last_access_time
				stat.bindLong(index++, f.length());
				stat.execute();
			}
			db.setTransactionSuccessful();
		} catch (BadHanyuPinyinOutputFormatCombination e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
			try {
				db.close();
			} catch (Exception e) {
			}
		}
	}
}
