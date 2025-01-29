!/usr/bin/env groovy


import javax.mail.*
import javax.mail.internet.*
import groovy.json.JsonBuilder
import org.artifactory.repo.RepoPathFactory
import org.artifactory.exception.CancelException
import org.artifactory.security.User
//import org.artifactory.request.HttpHeadersContainter

Long base = 1024L
Integer decimals = 3
def prefix = ['', 'KB', 'MB', 'GB', 'TB']
def pattern = ~/(\d*(?:\.\d{0,$decimals})?)([${prefix.join('')}])?[B]?/

def toBytes = { String sizeText ->
        def m = sizeText.trim().toUpperCase() =~ pattern
        if(m.matches()) {
                def bytes = new BigDecimal(m.group(1)) * (m.group(2) ? base**prefix.indexOf(m.group(2)) : 1)
                return bytes.setScale(0, BigDecimal.ROUND_HALF_UP) as Long
        } else {
                -1L
        }
}

def toHumanString = { Long bytes ->
        int i = Math.log(bytes)/Math.log(base) as Integer
        i = (i >= prefix.size() ? prefix.size()-1 : i)
        return Math.round((bytes / base**i) * 10**decimals) / 10**decimals + prefix[i]
}

MAILER_HOST = "vistsmtp.visteon.com"  // "smtp-relay.gmail.com"
//RECIPIENT_EMAIL = "bpalepu1@visteon.com,vvadlamu@visteon.com"
Sender_MAIL = "Artifactory - Support<artifactory@visteon.com>"
props = new Properties()

//@Grab(group = 'com.sun.mail', module = 'javax.mail', version = '1.6.0')
def runScript(percent,projectContact,quotaInHR,currentSizeInHR,repoKey) {

    log.warn("used is in runScript: ${percent} ")
    log.warn("project contactis: ${projectContact} ")
    props.put("mail.host", MAILER_HOST);
    Session session = Session.getDefaultInstance(props)
    session.setDebug(true);
    MimeMessage message = new MimeMessage(session)
    message.setFrom(new InternetAddress(Sender_MAIL))
    message.addRecipients(Message.RecipientType.TO, projectContact);
    message.addRecipients(Message.RecipientType.CC, "artquota@visteon.com")
    message.setSubject("Repository Quota Warning | ${repoKey}")
    def myMessage = """Hi Team,<br><br> <h3>Your <span style="color: #ff0000">${repoKey}</span> Repository Current usage is <span style="color: #ff0000">${percent}%</span></h3>

<p>
Repo URL = <a href="https://jfrog.bangalore.visteon.com/ui/#/artifacts/browse/tree/General/${repoKey}">https://jfrog.bangalore.visteon.com/ui/#/artifacts/browse/tree/General/${repoKey}</a><br><br>
Total Quota = ${quotaInHR}<br><br>
Current Used = ${currentSizeInHR}<br><br>

Please plan for the Housekeeping or reply to this same Mail(<a href="mailto:artifactory@visteon.com">artifactory@visteon.com</a>) for more info and hassle free uploads.<br><br>

<span style="font-size:15px;"><b>Note:</b></span> You are receiving this mail because you were mentioned as a Repository Contact in the Artifactory for this Repository. If you are not aware of this or want to unsubscribe from the Mailing List please reach out to the Artifactory Administrator.
</p>"""
    message.setText("${myMessage}", "utf-8", "html")
    Transport.send(message)
}

storage {

 /**
 * Handle before create events.
 *
 * Closure parameters:
 * item (org.artifactory.fs.ItemInfo) - the original item being created.
 */
 beforeCreate { item ->
   asSystem {
     def repoPath = item.getRepoPath()
     def repoKey = item.getRepoKey()
     //def repoURL = item.getUri()
     //log.warn("RepoURL is ${repoURL}")
     def itemRepoPathStr = repoPath.toPath()
     while (!repoPath.isRoot()) {
       repoPath = repoPath.getParent()
       def repoPathStr = repoPath.toPath()
       log.debug("Checking exists repo path ${repoPathStr}")
       if (repositories.exists(repoPath)) {
         def properties = repositories.getProperties(repoPath)
         log.debug("Checking for property repository.path.quota for repo path ${repoPathStr}")
         if (properties.containsKey("repository.path.quota")) {
           log.debug("Checking quota condition for repo path ${repoPathStr}")
           def quotaInBytesStr = properties.getFirst("repository.path.quota")
          // def projectContact = properties.getFirst("repository.contact")
           log.debug("Quota is ${quotaInBytesStr} for repo path ${repoPathStr}")
           def quotaInBytes = quotaInBytesStr.isLong() ? (quotaInBytesStr as long) : null
           if (null == quotaInBytes) {
            log.warning("Repository path quota of ${quotaInBytesStr} for ${repoPathStr} is invalid")
           }
                 def currentSizeInBytes = repositories.getArtifactsSize(repoPath)
           def quotaInHR = toHumanString(toBytes("${quotaInBytes}"))
           def currentSizeInHR = toHumanString(toBytes("${currentSizeInBytes}"))
           int percent =  currentSizeInBytes/quotaInBytes * 100;
        if (properties.containsKey("repository.contact")) {
        projectContact = properties.getFirst("repository.contact")
        }else{
        projectContact = "vvadlamu@visteon.com,vreddy10@visteon.com,sgrace1@visteon.com"
        }
         if (percent>85) {
        log.warn("used is: ${percent} ")
        runScript(percent,projectContact,quotaInHR,currentSizeInHR,repoKey)
     }
           if (currentSizeInBytes >= quotaInBytes) {
             log.error("Repository path quota of ${quotaInBytesStr} exceeded for ${repoPathStr}")
             //throw new CancelException("Your Repository storage quota is ${quotaInBytes} and your Current size is  ${currentSizeInBytes}.", 413)
             throw new CancelException("Storage Quota Error - Repository (${repoPath}) storage quota exceeded. Repository Quote: ${quotaInHR} and Current Usage: ${currentSizeInHR}.", 413)
           }
         }
       }
     }
   }
 }
/**
 * Handle before property create events.
 *
 * Closure parameters:
 * item (org.artifactory.fs.ItemInfo) - the item on which the property is being set.
 * name (java.lang.String) - the name of the property being set.
 * values (java.lang.String[]) - A string array of values being assigned to the property.
 */
 beforePropertyCreate { item, name, values ->
   if (name == "repository.path.quota" && !security.isAdmin()) {
     throw new CancelException("Not authorized to create this property.", 401)
   }
   if (name == "repository.contact" && !security.isAdmin()) {
     throw new CancelException("Not authorized to create this property.", 401)
   }
 }
/**
 * Handle before property delete events.
 *
 * Closure parameters:
 * item (org.artifactory.fs.ItemInfo) - the item from which the property is being deleted.
 * name (java.lang.String) - the name of the property being deleted.
 */
 beforePropertyDelete { item, name ->
   if (name == "repository.path.quota" && !security.isAdmin()) {
     throw new CancelException("Not authorized to delete this property.", 401)
   }
   if (name == "repository.contact" && !security.isAdmin()) {
     throw new CancelException("Not authorized to delete this property.", 401)
   }

 }
}