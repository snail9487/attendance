package com.example.attendance.constants;

public enum JobPosition {
	ADMIN(99,"Admin"),//
	SUPERVISOR(21, "Supervisor"),//
	DIRECTOR(20, "Director"),//
	SENIOR(2, "Senior"),//
	GENERAL(1, "General")
	;
	
	private int permission;
	
	private String title;

	private JobPosition(int permission, String title) {
		this.permission = permission;
		this.title = title;
	}

	public int getPermission() {
		return permission;
	}

	public String getTitle() {
		return title;
	}

	//沮场肚秈ㄓtitle把计眔癸莱舦(permission)
	//絋粄title把计琌琌﹚竡JobPositionい
	public static int parser(String title) {
		for(JobPosition item : JobPosition.values()) {
			if(title.equalsIgnoreCase(item.getTitle())) {
				return item.getPermission();
			}
		}
		return 0;
	}
	
	private static int reviewPermission = 20;
	
	public static boolean hasReviewPermission(String title) {
		int callerPermission = parser(title);
		return callerPermission >= reviewPermission ? true : false;
	}
}
