package com.pirate.jacksparrow

import play.api.libs.ws.WSResponse
import scala.concurrent.Future


/**
  * Created by pnagarjuna on 29/12/15.
  */
object PirateUtils {

  def getPageHtml(link: String): Future[WSResponse] = {
    HttpHelper.client.url(link)
      .withHeaders("User-Agent" -> "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36")
      .withFollowRedirects(true).get()
  }


}
