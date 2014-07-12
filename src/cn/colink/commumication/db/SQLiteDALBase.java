package cn.colink.commumication.db;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public abstract class SQLiteDALBase extends SubjectChatLogDB{
	
	private Context m_Context;
	private SQLiteDatabase m_DataBase;
	private SQLiteOpenHelper mSQLiteOpenHelper;
	
	public SQLiteDALBase(Context p_Context,SQLiteOpenHelper pSQLiteOpenHelper){
		m_Context = p_Context;
		mSQLiteOpenHelper = pSQLiteOpenHelper;
	}
	
	protected Context GetContext(){
		return m_Context;
	}
	
	public SQLiteDatabase getDataBase(){
		if(m_DataBase == null){
			m_DataBase = mSQLiteOpenHelper.getWritableDatabase();
		}
		return m_DataBase;
	}
	
	public void beginTransaction(){
		m_DataBase.beginTransaction();
	}
	
	public void setTransactionSuccessful(){
		m_DataBase.setTransactionSuccessful();
	}
	
	public void endTransaction(){
		m_DataBase.endTransaction();
	}
	
	public int getCount(String p_Condition){
		String _String[] = GetTableNameAndPK();
		Cursor _Cursor = ExecSql("Select " + _String[1] + " From " + _String[0] + " Where 1=1 " + p_Condition);
		int _Count = _Cursor.getCount();
		_Cursor.close();
		return _Count;
	}
	
	public int getCount(String p_PK,String p_TableName, String p_Condition){
		Cursor _Cursor = ExecSql("Select " + p_PK + " From " + p_TableName + " Where 1=1 " + p_Condition);
		int _Count = _Cursor.getCount();
		_Cursor.close();
		return _Count;
	}
	
	protected int delete(String p_TableName, String p_Condition){
		return getDataBase().delete(p_TableName, " 1=1 " + p_Condition, null);
	}
	
	protected abstract String[] GetTableNameAndPK();
	
	protected List getList(String p_SqlText){
		Cursor _Cursor = ExecSql(p_SqlText);
		return CursorToList(_Cursor);
	}
	
	protected abstract Object findModel(Cursor p_Cursor);
	
	protected List CursorToList(Cursor p_Cursor){
		List _List = new ArrayList();
		while(p_Cursor.moveToNext())
		{
			Object _Object = findModel(p_Cursor);
			_List.add(_Object);
		}
		p_Cursor.close();
		return _List;
	}
	
	public Cursor ExecSql(String p_SqlText){
		return getDataBase().rawQuery(p_SqlText, null);
	}
}
