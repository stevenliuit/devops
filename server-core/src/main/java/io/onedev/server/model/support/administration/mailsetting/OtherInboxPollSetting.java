package io.onedev.server.model.support.administration.mailsetting;

import javax.validation.constraints.NotEmpty;

import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Password;

@Editable
public class OtherInboxPollSetting extends InboxPollSetting {

	private static final long serialVersionUID = 1L;
	
	private String imapHost;
	
	private int imapPort = 993;
	
	private String imapUser;
	
	private String imapPassword;
	
	private boolean enableSSL = true;
	
	@Editable(order=100, name="IMAP Host")
	@NotEmpty
	public String getImapHost() {
		return imapHost;
	}

	public void setImapHost(String imapHost) {
		this.imapHost = imapHost;
	}

	@Editable(order=200, name="IMAP Port")
	public int getImapPort() {
		return imapPort;
	}

	public void setImapPort(int imapPort) {
		this.imapPort = imapPort;
	}

	@Editable(order=300, name="IMAP User", description="指定 IMAP 用户名.<br>"
			+ "<b class='text-danger'>NOTE: </b> 此帐户应该能够接收发送到系统的电子邮件 "
			+ "上面指定的电子邮件地址")
	@NotEmpty
	public String getImapUser() {
		return imapUser;
	}

	public void setImapUser(String imapUser) {
		this.imapUser = imapUser;
	}

	@Editable(order=400, name="IMAP Password")
	@Password(autoComplete="new-password")
	@NotEmpty
	public String getImapPassword() {
		return imapPassword;
	}

	public void setImapPassword(String imapPassword) {
		this.imapPassword = imapPassword;
	}

	@Editable(order=700, name="启用 IMAP SSL", description="连接 IMAP 服务器时是否启用 SSL")
	public boolean isEnableSSL() {
		return enableSSL;
	}

	public void setEnableSSL(boolean enableSSL) {
		this.enableSSL = enableSSL;
	}

}