import java.util.List;

import javax.mail.Part;


public class Tester {
	
	/**
	 * 
	 * This method authenticates with your dropbox account,
	 * retrieves all unread messages, extracts the attachments
	 * and uploads them to your dropbox folder
	 * 
	 * @param args not required
	 */
	public static void main(String[] args) {
		
		// Authenticate with Dropbox
		DropboxAPIWrapper dropbox = new DropboxAPIWrapper();
		dropbox.authenticate();
		
		// start mail service
		MailService service = new MailService();
		service.start();
		
		// retrieve messages
		List<Part> attachments = service.processNewMessages();
		for (Part p : attachments) {
			// upload attachments
			dropbox.upload(p);
		}
		
		// stop mail service
		service.stop();
		
	}

}
