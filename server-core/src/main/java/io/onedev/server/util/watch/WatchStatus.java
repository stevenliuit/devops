package io.onedev.server.util.watch;

public enum WatchStatus {
	DEFAULT("默认"), 
	WATCH("观看"), 
	DO_NOT_WATCH("不要看");
	
	private final String displayName;
	
	WatchStatus(String displayName) {
		this.displayName = displayName;
	}
	
	@Override
	public String toString() {
		return displayName;
	}
	
}