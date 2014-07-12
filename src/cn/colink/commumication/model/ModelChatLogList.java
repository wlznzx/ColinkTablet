package cn.colink.commumication.model;

public class ModelChatLogList {
	private int _ID;
	public ModelChatLogList(){
		
	}
	public ModelChatLogList(String userJID, String chatTableName) {
		super();
		UserJID = userJID;
		ChatTableName = chatTableName;
	}
	private String UserJID;
	private String ChatTableName;
	public int get_ID() {
		return _ID;
	}
	public void set_ID(int _ID) {
		this._ID = _ID;
	}
	public String getUserJID() {
		return UserJID;
	}
	public void setUserJID(String userJID) {
		UserJID = userJID;
	}
	public String getChatTableName() {
		return ChatTableName;
	}
	public void setChatTableName(String chatTableName) {
		ChatTableName = chatTableName;
	}
	@Override
	public String toString() {
		return "ModelChatLogList [_ID=" + _ID + ", UserJID=" + UserJID
				+ ", ChatTableName=" + ChatTableName + "]";
	}
}
