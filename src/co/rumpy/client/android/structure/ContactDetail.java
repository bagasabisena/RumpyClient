package co.rumpy.client.android.structure;

public class ContactDetail {
	
	private String signum;
	private String fullname;
	private String imageThumbName;
	private String lastPresence;
	
	public ContactDetail(String signum, String fullname, String imageThumbName, String lastPresence) {
		this.signum = signum;
		this.fullname = fullname;
		this.imageThumbName = imageThumbName;
		this.lastPresence = lastPresence;
	}
	
	public String getSignum() {
		return this.signum;
	}
	
	public void setSignum(String signum) {
		this.signum = signum;
	}
	
	public String getFullname() {
		return this.fullname;
	}
	
	public void setFullname(String fullname) {
		this.fullname = fullname;
	}
	
	public String getImageThumbName() {
		return this.imageThumbName;
	}
	
	public void setImageThumbName(String imageThumbName) {
		this.imageThumbName = imageThumbName;
	}
	
	public String getLastPresence() {
		return this.lastPresence;
	}
	
	public void setLastPresence(String lastPresence) {
		this.lastPresence = lastPresence;
	}

}
