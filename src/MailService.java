import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.FlagTerm;

/**
 * 
 * This class retrieves e-mail attachments from your gmail account
 * 
 * July 2012
 * 
 * @author David H
 *
 */

public class MailService {
	
	// GMAIL PARAMETERS
	private static final String HOST = "imap.gmail.com"; // use Authenticator to avoid using open credentials
	private static final String USERNAME = "USERNAME HERE";
	private static final String PASSWORD = "PASSWORD HERE";
	
	private Session session;
	private Store store;
	private Folder folder;
	
	
	public MailService() {
		// Retrieve single default session
		Properties props = new Properties(); // create empty property list
		props.setProperty("mail.store.protocol", "imaps");
		this.session = Session.getDefaultInstance(props, null);
		this.session.setDebug(false);
		this.store = null;
		this.folder = null;
	}
	
	public ArrayList<Part> processNewMessages() {
		if (this.store == null || this.folder == null) {
			System.err.println("It seems as if the MailService has not been started...");
			return null;
		}
		Message[] messages = retrieveUnseenMessages();
		return retrieveAttachments(messages);
	}
	
	private ArrayList<Part> retrieveAttachments(Message[] messages) {

		ArrayList<Part> attachments = new ArrayList<Part>(); 
		try {
			// Iterate over all messages to retrieve dropbox attachments
			for (int i = 0, n = messages.length; i < n; i++) {
				//if (messages[i].getSubject().toLowerCase().contains("dropbox")) {
					Multipart mp = (Multipart) messages[i].getContent();
					
					// Look inside message to extract attachments
					for (int partNum = 0; partNum < mp.getCount(); partNum++) {
						Part part = mp.getBodyPart(partNum);
						
						String disposition = part.getDisposition();
						
						//System.out.println("Message " + i + ", part " + partNum + ": " + disposition);
						if ((disposition != null) && 
								(( disposition.toLowerCase().equals(Part.ATTACHMENT) ||
										disposition.toLowerCase().equals(Part.INLINE) ))) {
							attachments.add(part);
						}
					}
				//}
			}
		} catch (MessagingException e) {
			System.err.println("Error processing email in processAttachments(). " + e);
		} catch (IOException e) {
			System.err.println("Error retrieving multipart content of email in processAttachments(). " + e);
		}
		return attachments;
	}
	
	private Message[] retrieveUnseenMessages() {
		System.out.println("Getting messages...");
		// Get unread messages
		FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
		Message[] messages = null;
        try {
			 messages = folder.search(ft);
		} catch (MessagingException e) {
			System.err.println("Error searching for unseen emails. " + e);
			e.printStackTrace();
		}
		return messages;
	}
	
	public void start() {
		System.out.println("Starting MailService...");
		// Get store object
		// (abstract class that models a message store and its access protocol,
		// for storing and retrieving messages)
		try {
			System.out.println("Connecting to store...");
			store = session.getStore("imaps");
			store.connect(HOST, USERNAME, PASSWORD);
			
			System.out.println("Getting folder...");
			// Get folder object
			folder = store.getFolder("INBOX");
			folder.open(Folder.READ_WRITE);
		} catch (MessagingException e) {
			System.err.println("Error retrieving messages. " + e);
			e.printStackTrace();
		}
	}
	
	public void stop() {
		System.out.println("Stopping MailService...");
		try {
			if (folder != null) {
				folder.close(false);
				folder = null;
			}
			if (store != null) {
				store.close();
				store = null;
			}
		} catch (MessagingException e) {
			System.err.println("Error stopping mail service. " + e);
			e.printStackTrace();
		}
	}

}
