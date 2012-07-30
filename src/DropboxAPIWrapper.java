import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Scanner;
import javax.mail.MessagingException;
import javax.mail.Part;

import org.apache.commons.io.IOUtils;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.RequestTokenPair;
import com.dropbox.client2.session.WebAuthSession;
import com.dropbox.client2.session.Session.AccessType;

/**
 * 
 * This class provides some Dropbox API functionalities
 * 
 * For more information check https://www.dropbox.com/developers
 * 
 * July 2012
 * 
 * @author David H
 *
 */

public class DropboxAPIWrapper {
	
	// Configuration parameters - refer to Dropbox developers page
	private final static String APP_KEY = "APP KEY HERE";
	private final static String APP_SECRET = "APP SECRET HERE";
	private final static AccessType ACCESS_TYPE = AccessType.APP_FOLDER;
	private AppKeyPair appKeys;
	private DropboxAPI<WebAuthSession> api;
	
	public DropboxAPIWrapper() {
		// Initialize the session
		this.appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
		WebAuthSession session = new WebAuthSession(appKeys, ACCESS_TYPE);
		// Initialize DropboxAPI object
		api = new DropboxAPI<WebAuthSession>(session);
	}
	
	public void authenticate() {
		// check if already authenticated
		File tokensFile = new File("TOKENS"); 
		if (tokensFile.exists()) {
			// tokensFile does already exist
			System.out.println("tokensFile seems to exist. Attempting reauthentication...");
			reauthenticate();
		} else {
			// tokensFile does not exist, create (First authentication)
			System.out.println("tokensFile does not exist. Performing first authentication...");
			firstauthenticate();
		}
	}
	
	private void reauthenticate() {
		File tokensFile = new File("TOKENS");
		Scanner tokenScanner = null;
		try {
			tokenScanner = new Scanner(tokensFile);
		} catch (FileNotFoundException e) {
			System.err.println("I have problems parsing your tokensFile...");
			firstauthenticate();
		}
		// Initiate Scanner to read tokens from TOKEN file
		String ACCESS_TOKEN_KEY = tokenScanner.nextLine(); // Read key
		String ACCESS_TOKEN_SECRET = tokenScanner.nextLine(); // Read secret
		tokenScanner.close(); //Close Scanner

		// Re-auth
		AccessTokenPair reAuthTokens = new AccessTokenPair(ACCESS_TOKEN_KEY, ACCESS_TOKEN_SECRET);
		api.getSession().setAccessTokenPair(reAuthTokens);
		System.out.println("Re-authentication Sucessful!");

		// Run test command
		try {
			System.out.println("Hello there, " +
					api.accountInfo().displayName + "(" +
					api.accountInfo().quota + ")");
		} catch (DropboxException e) {
			System.err.println("Could not retrieve account info. " + e);
			e.printStackTrace();
		}
	}
	
	private void firstauthenticate() {
		File tokensFile = new File("TOKENS");
		// Get ready for some user input
		Scanner input = new Scanner(System.in);
		
		try {
			// Redirect user to browser url
			System.out.println("Please go to this url and hit \"Allow\": " +
					api.getSession().getAuthInfo().url);
		} catch (DropboxException e) {
			System.err.println("Could not retrieve AuthInfo from session. " + e);
			e.printStackTrace();
		}
		// Keep access token pair
		AccessTokenPair tokenPair = api.getSession().getAccessTokenPair();
		
		// Wait for user
		System.out.println("Finished? Enter 'ok' to continue.");
		if (input.next().equals("ok")) {
			// keep request token pair
			RequestTokenPair tokens = new RequestTokenPair(tokenPair.key, tokenPair.secret);
			try {
				// Perform Check
				api.getSession().retrieveWebAccessToken(tokens);
			} catch (DropboxException e) {
				System.err.println("Could not retrieve WebAccessTokens. " + e);
				e.printStackTrace();
			}
			
			// Write tokens to file
			try {
				PrintWriter tokenWriter = new PrintWriter(tokensFile);
				tokenWriter.println(api.getSession().getAccessTokenPair().key);
				tokenWriter.println(api.getSession().getAccessTokenPair().secret);
				tokenWriter.close();
			} catch (FileNotFoundException e) {
				System.out.println("");
				e.printStackTrace();
			}
			
			System.out.println("Authentication Successful!");
		}
	}
	
	public void upload(Part attachment) {
		try {
			System.out.println("Uploading attachment: " + 
					attachment.getFileName() + ", Size: " +
					attachment.getSize() + "...");
			
			// Workaround - this could potentially be improved
			// Just using attachment.getInputStream() in api.putFile did not work for some reason
			InputStream file = attachment.getInputStream();
			byte[] bytes = IOUtils.toByteArray(file);
			int length = bytes.length;
			ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
			
			// Upload file to Dropbox
			Entry newEntry = api.putFile("/" + attachment.getFileName(), stream, length, null, null);
			System.out.println("Successfully uploaded file. File's rev is: " + newEntry.rev);
			
		} catch (DropboxException e) {
			System.err.println("Error uploading file. " + e);
			e.printStackTrace();
		} catch (MessagingException e) {
			System.err.println("Error extracting attachment in upload(). " + e);
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Error uploading file in upload(). " + e);
			e.printStackTrace();
		}
	}
	
	

}
