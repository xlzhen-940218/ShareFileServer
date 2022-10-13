package com.evgenii.jsevaluator.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.xlzhen.sharefileserver.Application;

public class SQLUtils {
    private static String dbPath;

    /**
     * 创建数据库 可重复调用添加表格
     * @param dbName 数据库名称
     * @param createTables 创建表格
     * @return 返回是否创建成功
     */
    public static boolean createDb(String dbName,String[] createTables){
        if(dbPath==null){
            dbPath= Application.getContext().getExternalFilesDir("db")+"/";
        }
        try {
            SQLiteDatabase sqLiteDatabase = Application.getContext().openOrCreateDatabase(dbPath+dbName+".db", Context.MODE_PRIVATE, null);
            sqLiteDatabase.beginTransaction();
            for (String createTable : createTables) {
                sqLiteDatabase.execSQL(createTable);
            }
            sqLiteDatabase.setTransactionSuccessful();
            sqLiteDatabase.endTransaction();
            sqLiteDatabase.close();
            return true;
        }catch (Exception ex){
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * 插入数据库
     * @param dbName 数据库名
     * @param table 数据库表
     * @param keys 插入字段名
     * @param values 插入字段值
     * @return 返回是否插入成功
     */
    public static boolean insertDb(String dbName,String table,String[] keys,String[] values){
        if(dbPath==null){
            dbPath=Application.getContext().getExternalFilesDir("db")+"/";
        }
        SQLiteDatabase sqLiteDatabase = Application.getContext().openOrCreateDatabase(dbPath+dbName+".db", Context.MODE_PRIVATE, null);
        ContentValues contentValues = new ContentValues();
        for (int i = 0; i < keys.length; i++) {
            contentValues.put(keys[i], values[i]);
        }
        sqLiteDatabase.beginTransaction();
        long ret = sqLiteDatabase.insert(table, null, contentValues);
        sqLiteDatabase.setTransactionSuccessful();
        sqLiteDatabase.endTransaction();
        sqLiteDatabase.close();
        return ret>0;
    }
    /**
     * 删除数据库
     * @param dbName 数据库名
     * @param table 数据库表
     * @param queryFields 条件字段
     * @param filters 条件值
     * @return 返回是否删除成功
     */
    public static boolean deleteDb(String dbName,String table,String[] queryFields,String[] filters){
        if(dbPath==null){
            dbPath=Application.getContext().getExternalFilesDir("db")+"/";
        }
        SQLiteDatabase sqLiteDatabase = Application.getContext().openOrCreateDatabase(dbPath+dbName+".db", Context.MODE_PRIVATE, null);
        StringBuilder queryField = new StringBuilder();
        for (String q : queryFields) {
            queryField.append(q).append("=?,");
        }
        String selections = queryField.substring(0, queryField.length() - 1);
        sqLiteDatabase.beginTransaction();
        long ret = sqLiteDatabase.delete(table, selections, filters);
        sqLiteDatabase.setTransactionSuccessful();
        sqLiteDatabase.endTransaction();
        sqLiteDatabase.close();
        return ret>0;
    }

    /**
     * 更新数据库
     * @param dbName 数据库名
     * @param table 数据库表
     * @param queryFields 条件字段
     * @param filters 条件值
     * @param keys 更新字段名
     * @param values 更新字段值
     * @return 返回是否更新成功
     */
    public static boolean updateDb(String dbName, String table, String[] queryFields, String[] filters, String[] keys, String[] values) {
        if(dbPath==null){
            dbPath=Application.getContext().getExternalFilesDir("db")+"/";
        }
        SQLiteDatabase sqLiteDatabase = Application.getContext().openOrCreateDatabase(dbPath+dbName+".db", Context.MODE_PRIVATE, null);

        ContentValues contentValues=new ContentValues();
        for (int i = 0; i < keys.length; i++) {
            contentValues.put(keys[i], values[i]);
        }

        StringBuilder queryField = new StringBuilder();
        for (String q : queryFields) {
            queryField.append(q).append("=?,");
        }
        String selections = queryField.substring(0, queryField.length() - 1);
        sqLiteDatabase.beginTransaction();
        long ret = sqLiteDatabase.update(table,contentValues, selections, filters);
        sqLiteDatabase.setTransactionSuccessful();
        sqLiteDatabase.endTransaction();
        sqLiteDatabase.close();
        return  ret>0;
    }

    /**
     * 查询数据库
     * @param dbName 数据库名
     * @param table 数据库表
     * @param queryFields 条件字段
     * @param filters 条件值
     * @return 返回查询结果 JSONArray
     */
    public static String queryDb(String dbName, String table, String[] queryFields, String[] filters) {
        if(dbPath==null){
            dbPath=Application.getContext().getExternalFilesDir("db")+"/";
        }
        SQLiteDatabase sqLiteDatabase = Application.getContext().openOrCreateDatabase(dbPath+dbName+".db", Context.MODE_PRIVATE, null);
        StringBuilder queryField = new StringBuilder();
        for (String q : queryFields) {
            queryField.append(q).append("=?,");
        }
        String selections=null;
        if(queryFields.length>0) {
            selections = queryField.substring(0, queryField.length() - 1);
        }
        sqLiteDatabase.beginTransaction();
        Cursor cursor = sqLiteDatabase.query(table,
                null,
                selections,
                filters,
                null, null, null);
        StringBuilder jsonArray= new StringBuilder("{");
        if (cursor.getCount() > 0) {
            int columnCount = cursor.getColumnCount();

            while (cursor.moveToNext()) {
                StringBuilder jsonObject= new StringBuilder();
                for (int i = 0; i < columnCount; i++) {
                    switch (cursor.getType(i)) {
                        case Cursor.FIELD_TYPE_STRING:
                            jsonObject.append(String.format("\"%s\":\"%s\",", cursor.getColumnName(i), cursor.getString(i)));
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            jsonObject.append(String.format("\"%s\":\"%s\",", cursor.getColumnName(i), cursor.getInt(i)));
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            jsonObject.append(String.format("\"%s\":\"%s\",", cursor.getColumnName(i), cursor.getFloat(i)));
                            break;
                        /*case Cursor.FIELD_TYPE_BLOB://暂不支持blob类型 因为js server在插入数据时就不支持blob
                            jsonObject=String.format("{\"%s\":\"%s\"}",cursor.getColumnName(i), cursor.getBlob(i));*/
                    }
                }
                jsonObject = new StringBuilder(jsonObject.substring(0, jsonObject.length() - 1));
                jsonArray.append(jsonObject.toString()).append("},{");
            }


        }
        if(jsonArray.length()>1) {
            jsonArray = new StringBuilder(jsonArray.substring(0, jsonArray.length() - 2));
        }else {
            jsonArray=new StringBuilder();
        }
        cursor.close();
        sqLiteDatabase.setTransactionSuccessful();
        sqLiteDatabase.endTransaction();
        sqLiteDatabase.close();
        return "["+jsonArray.toString()+"]";
    }
}
