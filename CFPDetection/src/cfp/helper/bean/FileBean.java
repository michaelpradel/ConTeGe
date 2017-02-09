package cfp.helper.bean;

public class FileBean {
	
	String desc;
	String objectId;
	String methodName;
	int globalTS;
	String threadId;
	public String getThreadId() {
		return threadId;
	}
	public void setThreadId(String threadId) {
		this.threadId = threadId;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public String getObjectId() {
		return objectId;
	}
	public FileBean() {
		super();
	}
	public FileBean(String desc, String objectId, String methodName,
			int globalTS, String threadId) {
		super();
		this.desc = desc;
		this.objectId = objectId;
		this.methodName = methodName;
		this.globalTS = globalTS;
		this.threadId = threadId;
	}
	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}
	public String getMethodName() {
		return methodName;
	}
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	public int getGlobalTS() {
		return globalTS;
	}
	public void setGlobalTS(int globalTS) {
		this.globalTS = globalTS;
	}

}
