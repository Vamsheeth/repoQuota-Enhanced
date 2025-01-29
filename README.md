# **Artifactory Storage Quota Enforcement Script -- repoQuota Plugin**

This Groovy script enforces **storage quotas** on JFrog Artifactory repositories, preventing overuse and ensuring efficient storage management. It monitors repository size, sends warning emails when usage exceeds a threshold, and blocks new uploads if the quota is exceeded.  

---

## **Features**  
✅ **Storage Quota Enforcement**: Blocks uploads when a repository exceeds its assigned quota.  
✅ **Email Notifications**: Sends warnings when usage exceeds **85% of the quota**.  
✅ **Human-Readable Storage Representation**: Converts bytes into KB, MB, GB, etc.  
✅ **Admin-Only Property Management**: Restricts `repository.path.quota` and `repository.contact` modifications to admins.  
✅ **Customizable Email Alerts**: Configurable SMTP settings and recipient lists.  

---

## **Prerequisites**  
- **JFrog Artifactory Pro or Enterprise** (Required for user plugins).  
- **SMTP Mail Server** for sending email notifications.  
- **Administrator Access** to set up repository properties.  

---

## **Installation**  

### **Follow the steps in the JFrog Document**  
- https://jfrog.com/help/r/jfrog-integrations-documentation/user-plugins  


---

## **How It Works**  

### **1. Quota Validation**  
- Before uploading an artifact, the script checks the repository's **current size**.  
- If the repository **exceeds its quota**, the upload is **blocked** with a **413 Storage Quota Exceeded** error.  

### **2. Email Notifications**  
- When usage **exceeds 85%**, an email is sent to the **repository contact**.  
- Email includes:  
  ✅ Repository Name  
  ✅ Current Usage Percentage  
  ✅ Total Quota  
  ✅ Repository URL  

### **3. Admin Restrictions**  
- **Only Artifactory administrators** can create, modify, or delete:  
  - `repository.path.quota` (Quota property).  
  - `repository.contact` (Email recipient property).  

---

## **Customization**  

### **Modify the Quota Warning Threshold**  
By default, warnings are sent at **85% usage**. To change this, edit the line:  
```groovy
if (percent > 85) {
```
Replace `85` with your preferred threshold.  

### **Configure Email Settings**  
Update the **SMTP Server** and **Sender Email** in the script:  
```groovy
MAILER_HOST = "your-smtp-host"
Sender_MAIL = "Artifactory - Support<artifactory@example.com>"
```
Change the **default CC email recipients** in:  
```groovy
message.addRecipients(Message.RecipientType.CC, "<default-email@example.com>")
```
Change the **artifactory host name in** in:  
```groovy
def myMessage = """Hi Team,<br><br> <h3>Your <span style="color: #ff0000">${repoKey}</span> Repository Current usage is <span style="color: #ff0000">${percent}%</span></h3>

<p>
Repo URL = <a href="https://example.com/ui/#/artifacts/browse/tree/General/${repoKey}">https://example.com/ui/#/artifacts/browse/tree/General/${repoKey}</a><br><br>
Total Quota = ${quotaInHR}<br><br>
Current Used = ${currentSizeInHR}<br><br>

Please plan for the Housekeeping or reply to this same Mail(<a href="mailto:artifactory@example.com">artifactory@example.com</a>) for more info and hassle free uploads.<br><br>

<span style="font-size:15px;"><b>Note:</b></span> You are receiving this mail because you were mentioned as a Repository Contact in the Artifactory for this Repository. If you are not aware of this or want to unsubscribe from the Mailing List please reach out to the Artifactory Administrator.
</p>"""
    message.setText("${myMessage}", "utf-8", "html")
    Transport.send(message)
}

```

---

## **Error Handling**  

- If the repository exceeds its quota, uploads are **blocked** with this error:  
  ```
  Storage Quota Error - Repository (repo-name) storage quota exceeded.
  ```
- If no `repository.contact` is defined, a **default email list** is used.

---

## **Example Email Notification**  

**Subject:** `Repository Quota Warning | my-repo`  

📧 **Email Body:**  
```
Hi Team,

Your my-repo Repository Current usage is 87%.

Repo URL: https://your-artifactory-url/ui/#/artifacts/browse/tree/General/my-repo
Total Quota: 500GB
Current Used: 435GB

Please plan for housekeeping or contact the Artifactory team.
```

---

## **License**  
This script is open-source and available under the **MIT License**.  

## **Author**  
Vamsheeth Vadlamudi
vamsheethkennady@gmail.com
