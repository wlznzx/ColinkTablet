package cn.colink.commumication.model;

public class ModelChatLog {
	// 接受或发送
	public static final int SEND = 0;
	public static final int RECEIVE = 1;
	// 消息类型
	public static final int MESSAGE_TYPE_RECORD = 0;
	public static final int MESSAGE_TYPE_PICTURE = 1;
	public static final int MESSAGE_TYPE_COMMAND = 2;
	public static final int MESSAGE_TYPE_STANDARD = 3;
	// 是否已读
	public static final int IS_READ = 0;
	public static final int UN_READ = 1;

	private int _ID;
	private long Date;
	private int Dect;
	private String JID;
	private int MessageType;
	private String MessageBody;
	private int Read;
	public ModelChatLog(){
		
	}
	public ModelChatLog(long pDate,int pDect,String pJID,int pMessageType,String pMessageBody,int pRead){
		Date = pDate;
		Dect = pDect;
		JID = pJID;
		MessageType = pMessageType;
		MessageBody = pMessageBody;
		Read = pRead;
	}
	public int get_ID() {
		return _ID;
	}
	public void set_ID(int _ID) {
		this._ID = _ID;
	}
	public long getDate() {
		return Date;
	}
	public void setDate(long date) {
		Date = date;
	}
	public int getDect() {
		return Dect;
	}
	public void setDect(int dect) {
		Dect = dect;
	}
	
	public String getJID() {
		return JID;
	}
	public void setJID(String jID) {
		JID = jID;
	}
	public int getMessageType() {
		return MessageType;
	}
	public void setMessageType(int messageType) {
		MessageType = messageType;
	}
	public String getMessageBody() {
		return MessageBody;
	}
	public void setMessageBody(String messageBody) {
		MessageBody = messageBody;
	}
	public int getRead() {
		return Read;
	}
	public void setRead(int read) {
		Read = read;
	}
	@Override
	public String toString() {
		return "ModelChatLog [_ID=" + _ID + ", Date=" + Date + ", Dect=" + Dect
				+ ", JID=" + JID + ", MessageType=" + MessageType
				+ ", MessageBody=" + MessageBody + ", Read=" + Read + "]";
	}
}
