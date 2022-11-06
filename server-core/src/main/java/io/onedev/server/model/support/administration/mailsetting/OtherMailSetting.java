package io.onedev.server.model.support.administration.mailsetting;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

import io.onedev.server.mail.BasicAuthPassword;
import io.onedev.server.mail.MailCheckSetting;
import io.onedev.server.mail.MailCredential;
import io.onedev.server.mail.MailSendSetting;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Password;

@Editable(order=10000, name="Other")
public class OtherMailSetting extends MailSetting {
	
	private static final long serialVersionUID = 1L;

	private String smtpHost;
	
	private int smtpPort = 587;
	
	private String smtpUser;
	
	private String smtpPassword;
	
	private String emailAddress;
	
	private OtherInboxPollSetting otherInboxPollSetting;
	
	private boolean enableStartTLS = true;

	@Editable(order=100, name="SMTP Host")
	@NotEmpty
	public String getSmtpHost() {
		return smtpHost;
	}

	public void setSmtpHost(String smtpHost) {
		this.smtpHost = smtpHost;
	}

	@Editable(order=200, name="SMTP Port")
	public int getSmtpPort() {
		return smtpPort;
	}

	public void setSmtpPort(int smtpPort) {
		this.smtpPort = smtpPort;
	}

	@Editable(order=300, name="SMTP User")
	public String getSmtpUser() {
		return smtpUser;
	}

	public void setSmtpUser(String smtpUser) {
		this.smtpUser = smtpUser;
	}

	@Editable(order=400, name="SMTP Password")
	@Password(autoComplete="new-password")
	public String getSmtpPassword() {
		return smtpPassword;
	}

	public void setSmtpPassword(String smtpPassword) {
		this.smtpPassword = smtpPassword;
	}

	@Editable(order=410, name="系统电子邮件地址", description="此地址将用作发件人地址 "
			+ "各种通知。 针对该地址及其在 IMAP 收件箱中的子地址的电子邮件将 "
			+ "还要检查是否 <code>检查传入的电子邮件</code> 选项在下面启用")
	@Email
	@NotEmpty
	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@Editable(order=430, name="启用 SMTP StartTLS", description="是否开启 StartTLS "
			+ "连接到 SMTP 服务器时")
	public boolean isEnableStartTLS() {
		return enableStartTLS;
	}

	public void setEnableStartTLS(boolean enableStartTLS) {
		this.enableStartTLS = enableStartTLS;
	}

	@Editable(order=450, name="检查传入的电子邮件", description="启用此功能以通过电子邮件发布问题和拉取请求评论. "
			+ "<b class='text-danger'>NOTE:</b> <a href='https://en.wikipedia.org/wiki/Email_address#Subaddressing' target='_blank'>子地址</a> "
			+ "需要为上述电子邮件地址启用，因为本系统使用它来跟踪问题和拉取请求上下文")
	public OtherInboxPollSetting getOtherInboxPollSetting() {
		return otherInboxPollSetting;
	}

	public void setOtherInboxPollSetting(OtherInboxPollSetting otherInboxPollSetting) {
		this.otherInboxPollSetting = otherInboxPollSetting;
	}

	@Override
	public MailSendSetting getSendSetting() {
		MailCredential smtpCredential;
		if (smtpPassword != null)
			smtpCredential = new BasicAuthPassword(smtpPassword);
		else
			smtpCredential = null;
		return new MailSendSetting(smtpHost, smtpPort, smtpUser, smtpCredential, emailAddress, enableStartTLS, getTimeout());
	}

	@Override
	public MailCheckSetting getCheckSetting() {
		if (otherInboxPollSetting != null) {
			String imapUser = otherInboxPollSetting.getImapUser();
			MailCredential imapCredential = new BasicAuthPassword(otherInboxPollSetting.getImapPassword());
			return new MailCheckSetting(otherInboxPollSetting.getImapHost(), otherInboxPollSetting.getImapPort(), 
					imapUser, imapCredential, emailAddress, otherInboxPollSetting.isEnableSSL(), 
					otherInboxPollSetting.getPollInterval(), getTimeout());
		} else {
			return null;
		}
	}

}
